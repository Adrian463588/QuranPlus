# Quran Plus: Local LLM Chatbot with RAG — Complete Android Development Guide

---

## Overview

**Quran Plus** is an offline-first Android application built with Jetpack Compose that combines:
- A complete Quran reading experience with translations, tajwid, transliteration, search, and bookmarks
- A local LLM chatbot powered by on-device AI (optimized for Poco X7 Pro and similar devices)
- Retrieval-Augmented Generation (RAG) using Quran, Sahih Sunnah, and user-uploaded documents
- Tahsin (recitation improvement) features using kitab tahsin
- Customizable AI persona prompts

All processing happens entirely offline — no data ever leaves your device.

---

## Step-by-Step Development Guide

### Phase 1: Project Setup & Architecture

#### Step 1.1: Initialize the Project
```bash
# Create new Android project with:
# - Empty Compose Activity template
# - Minimum SDK: API 26 (Android 8.0)
# - Target SDK: API 35
# - Kotlin 2.0+
```

#### Step 1.2: Define Clean Architecture Layers
Following **Clean Architecture** with **SOLID principles**:

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                   │
│  Compose UI Screens │ ViewModels │ UI State Management │
├─────────────────────────────────────────────────────────┤
│                     DOMAIN LAYER                       │
│  Use Cases │ Entities │ Repository Interfaces          │
├─────────────────────────────────────────────────────────┤
│                      DATA LAYER                        │
│  Repository Implementations │ Local Data Sources       │
│  Room Database │ Vector Database │ File System         │
└─────────────────────────────────────────────────────────┘
```

#### Step 1.3: Module Structure (Feature-Modular)
```
app/
├── core/                    # Shared utilities, DI, base classes
│   ├── di/                  # Dependency injection modules
│   ├── network/             # Download manager, connectivity
│   ├── database/            # Room database setup
│   └── utils/               # Extensions, helpers
├── features/
│   ├── quran/               # Quran reading feature
│   │   ├── presentation/    # Screens, ViewModels
│   │   ├── domain/          # Use cases, entities
│   │   └── data/            # Repositories, data sources
│   ├── chatbot/             # LLM chatbot feature
│   │   ├── presentation/
│   │   ├── domain/
│   │   └── data/
│   ├── rag/                 # RAG pipeline feature
│   │   ├── presentation/
│   │   ├── domain/
│   │   └── data/
│   ├── tahsin/              # Tahsin feature
│   └── settings/            # Settings & persona management
└── build.gradle.kts
```

---

### Phase 2: Data Layer Setup

#### Step 2.1: Room Database for Quran Data
```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}
```

**Entities:**
- `Surah` — Surah metadata (name, verses, revelation type)
- `Ayah` — Verse text (Arabic, translations, transliteration)
- `Translation` — Translations in multiple languages (Indonesian, English, etc.)
- `TajwidRule` — Tajwid color coding rules
- `Bookmark` — User bookmarks
- `SearchIndex` — FTS5 index for fast search

#### Step 2.2: Vector Database for RAG
Use **sqlite-vec** — a zero-dependency SQLite extension for vector search.

```kotlin
// Create vector table for document embeddings
CREATE VIRTUAL TABLE doc_embeddings USING vec0(
    chunk_id INTEGER PRIMARY KEY,
    embedding FLOAT[384]  // 384-dim from all-MiniLM-L6-v2
);

// Query: find top-5 nearest chunks
SELECT chunk_id, distance 
FROM doc_embeddings 
WHERE embedding MATCH ? 
ORDER BY distance LIMIT 5;
```

For corpora under 50,000 chunks, brute-force search runs in single-digit milliseconds.

#### Step 2.3: Document Ingestion Pipeline
```kotlin
class DocumentIngestionUseCase(
    private val embedder: OnDeviceEmbedder,
    private val vectorStore: VectorStore
) {
    suspend fun ingestDocument(content: String, source: DocumentSource) {
        // 1. Chunk with 512-token chunks, 50-token overlap
        val chunks = chunkText(content, chunkSize = 512, overlap = 50)
        
        // 2. Generate embeddings on-device
        val embeddings = chunks.map { embedder.embed(it) }
        
        // 3. Store in vector database
        vectorStore.insertChunks(chunks, embeddings, source)
    }
}
```

---

### Phase 3: Local LLM Integration

#### Step 3.1: Choose Inference Engine

**Google LiteRT-LM (Recommended)** — Google's official on-device LLM inference API, successor to MediaPipe LLM Inference.

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.mediapipe:tasks-genai:0.10.27")
}
```

**Alternative: llama.cpp via JNI** — For GGUF model support

#### Step 3.2: Model Selection for Poco X7 Pro

**Recommended Models** (quantized for 8-12GB RAM devices):

| Model | Size | Quality | Use Case |
|-------|------|---------|----------|
| **Gemma-3-1B-IT (4-bit)** | ~600MB | Excellent | General chatbot |
| **Qwen2.5-1.5B-Instruct** | ~800MB | Very Good | Islamic QA |
| **Llama 3.2 1B/3B** | ~700MB-1.5GB | Good | Balanced performance |
| **TinyLlama 1.1B Chat** | ~600MB | Good | Lightweight testing |

**Islamic Fine-tuned Models:**
- `ahmedtamseer3/alif-islamic-v4-base` — Qwen2.5-1.5B fine-tuned for Islamic QA
- Models fine-tuned on Quran, Hadith, and Fiqh datasets

#### Step 3.3: LLM Inference Implementation
```kotlin
class LocalLLMService(
    private val context: Context,
    private val modelPath: String
) {
    private lateinit var llmInference: LlmInference
    
    fun initialize() {
        val options = LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTopK(64)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
    }
    
    suspend fun generateResponse(
        prompt: String,
        systemPrompt: String,
        onToken: (String) -> Unit
    ) {
        val fullPrompt = "$systemPrompt\n\nUser: $prompt\n\nAssistant:"
        // Stream response
        llmInference.generateResponseAsync(fullPrompt) { partial, done ->
            onToken(partial)
        }
    }
}
```

---

### Phase 4: RAG Pipeline

#### Step 4.1: Embedding with ONNX Runtime
Use quantized `all-MiniLM-L6-v2` (384-dim, ~23MB after INT8 quantization).

```kotlin
class OnDeviceEmbedder(context: Context) {
    private val session: OrtSession
    
    init {
        val env = OrtEnvironment.getEnvironment()
        session = env.createSession(
            context.assets.open("minilm-quantized.onnx").readBytes(),
            SessionOptions().apply {
                setIntraOpNumThreads(2)  // Prevent thermal throttling
            }
        )
    }
    
    fun embed(text: String): FloatArray {
        // Tokenize, run inference, mean pool
    }
}
```

#### Step 4.2: RAG Query Flow
```
User Query → Embedding → Vector Search → Context Retrieval → 
Prompt Augmentation → LLM Generation → Stream Response
```

```kotlin
class RAGQueryUseCase(
    private val embedder: OnDeviceEmbedder,
    private val vectorStore: VectorStore,
    private val llmService: LocalLLMService
) {
    suspend fun query(
        question: String,
        personaPrompt: String,
        onToken: (String) -> Unit
    ) {
        // 1. Embed the question
        val queryEmbedding = embedder.embed(question)
        
        // 2. Retrieve top-k relevant chunks
        val relevantChunks = vectorStore.search(queryEmbedding, topK = 5)
        
        // 3. Build augmented prompt
        val context = relevantChunks.joinToString("\n")
        val augmentedPrompt = """
            $personaPrompt
            
            Context from Quran and Sunnah:
            $context
            
            Question: $question
            
            Answer based ONLY on the context above:
        """.trimIndent()
        
        // 4. Generate response
        llmService.generateResponse(augmentedPrompt, onToken)
    }
}
```

---

### Phase 5: Download Manager with Resume Support

#### Step 5.1: Implement Resumable Download
Android's `DownloadManager` does **not** have built-in pause/resume. Use a custom downloader with:

```kotlin
class ResumableDownloader(
    private val context: Context,
    private val connectivityManager: ConnectivityManager
) {
    // Features:
    // - Chunked download with byte range requests
    // - Persist download progress in Room
    // - Auto-pause on network loss
    // - Auto-resume on network reconnection
    // - File integrity verification (SHA-256)
    
    suspend fun download(url: String, destination: File): Result<File> {
        // Check if partial download exists
        val existingProgress = downloadDao.getProgress(url)
        val startByte = existingProgress?.bytesDownloaded ?: 0
        
        // Request byte range
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$startByte-")
            .build()
        
        // Download with progress tracking
        // Save progress periodically to Room
        // Verify integrity on completion
    }
}
```

**Reference:** `anjiemo/Downloader` — supports chunked download, resumable, network monitoring.

---

### Phase 6: Quran Reading Features

#### Step 6.1: Quran Data Sources
Import data from:
- **Quran.com Images** — Madani mushaf pages
- **QuranEnc** — Translations and Tafsir
- **Tanzil** — Additional translations

#### Step 6.2: Key Features Implementation

**Tajwid Color Coding:**
```kotlin
@Composable
fun AyahWithTajwid(ayah: Ayah, tajwidRules: List<TajwidRule>) {
    // Apply color rules to Arabic text
    // Rules: Idgham (green), Iqlab (red), Ikhfa (blue), etc.
}
```

**Search with FTS5:**
```kotlin
@Query("SELECT * FROM ayah WHERE ayah_fts MATCH :query")
suspend fun searchAyah(query: String): List<Ayah>
```

**Bookmarks:**
```kotlin
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val id: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val note: String?,
    val timestamp: Long
)
```

---

### Phase 7: Tahsin Feature

#### Step 7.1: Tahsin Knowledge Base
Import **Kitab Tahsin** content as structured data:
- Makharij (articulation points)
- Sifat (characteristics of letters)
- Tajweed rules with examples

#### Step 7.2: Pronunciation Feedback
Use **ML Kit's Text-to-Speech** or **on-device TTS** for pronunciation comparison.

```kotlin
class TahsinCoach(
    private val tts: TextToSpeech,
    private val tahsinRules: List<TahsinRule>
) {
    fun analyzeRecitation(ayah: String, userRecording: File): List<Correction> {
        // Compare user pronunciation against rules
        // Provide feedback on makharij and sifat
    }
}
```

---

### Phase 8: AI Persona Feature

#### Step 8.1: Persona Management
Allow users to define custom system prompts:

```kotlin
@Entity(tableName = "personas")
data class Persona(
    @PrimaryKey val id: String,
    val name: String,
    val systemPrompt: String,
    val isDefault: Boolean
)
```

**Example Personas:**
- **Mufti** — Formal Islamic scholar
- **Teacher** — Educational, patient explanation
- **Friend** — Casual, conversational

---

## Complete Tech Stack

### Core Technologies
| Layer | Technology | Purpose |
|-------|------------|---------|
| **Language** | Kotlin 2.0+ | Primary language |
| **UI** | Jetpack Compose + Material3 | Declarative UI |
| **Architecture** | Clean Architecture + MVVM | Separation of concerns |
| **DI** | Dagger Hilt | Dependency injection |
| **Navigation** | Jetpack Navigation Compose | Screen navigation |
| **State Management** | StateFlow / SharedFlow | Reactive UI state |
| **Concurrency** | Kotlin Coroutines + Flow | Async operations |

### Data & Persistence
| Layer | Technology | Purpose |
|-------|------------|---------|
| **Database** | Room | Quran data, bookmarks, chat history |
| **Vector DB** | sqlite-vec | RAG vector storage |
| **Full-Text Search** | SQLite FTS5 | Quran search |
| **File Storage** | Android File System | Model files, documents |

### AI & ML
| Layer | Technology | Purpose |
|-------|------------|---------|
| **LLM Inference** | LiteRT-LM (Google AI Edge) | On-device LLM |
| **Alternative** | llama.cpp via JNI | GGUF model support |
| **Embeddings** | ONNX Runtime + all-MiniLM-L6-v2 | Document embeddings |
| **Model Format** | `.task` (LiteRT-LM) or `.gguf` (llama.cpp) | Model packaging |

### Download & Network
| Layer | Technology | Purpose |
|-------|------------|---------|
| **Download** | Custom DownloadManager with resume | Model/document downloads |
| **Networking** | Retrofit + OkHttp | Hugging Face API |
| **Connectivity** | ConnectivityManager | Network state monitoring |

### Build & Quality
| Layer | Technology | Purpose |
|-------|------------|---------|
| **Build** | Gradle Kotlin DSL | Build configuration |
| **Static Analysis** | Detekt, KtLint | Code quality |
| **Testing** | JUnit, Espresso, MockK | Unit/UI testing |

---

## Best Practices Implementation

### SOLID Principles
| Principle | Implementation |
|-----------|----------------|
| **Single Responsibility** | Each Use Case handles one specific task |
| **Open/Closed** | Repository interfaces allow extension |
| **Liskov Substitution** | Interchangeable repository implementations |
| **Interface Segregation** | Small, focused interfaces |
| **Dependency Inversion** | Domain depends on abstractions, not concretions |

### DRY (Don't Repeat Yourself)
- Shared utilities in `:core` module
- Base ViewModel classes for common patterns
- Reusable Compose components

### YAGNI (You Aren't Gonna Need It)
- Start with minimal feature set
- Add complexity only when required
- Avoid over-engineering

### KISS (Keep It Simple, Stupid)
- Simple state management with StateFlow
- Straightforward navigation
- Clear, readable code

---

## Recommended Models for Poco X7 Pro

**Poco X7 Pro Specifications:**
- **RAM:** 8GB / 12GB
- **Processor:** Dimensity 9300+ (or similar)
- **Storage:** 256GB+

### Model Recommendations

| Model | Format | Size | RAM Required | Use Case |
|-------|--------|------|--------------|----------|
| **Gemma-3-1B-IT (4-bit)** | `.task` | ~600MB | 4GB+ | Best all-around |
| **Qwen2.5-1.5B-Instruct** | `.gguf` Q4 | ~800MB | 4GB+ | Islamic QA |
| **Llama-3.2-1B** | `.task` | ~650MB | 4GB+ | Fast responses |
| **DeepSeek-R1-Distill-Qwen-1.5B** | `.task` | ~700MB | 4GB+ | Reasoning |

### Embedding Models
| Model | Format | Size | Use Case |
|-------|--------|------|----------|
| **all-MiniLM-L6-v2** | ONNX INT8 | ~23MB | Document retrieval |
| **EmbeddingGemma** | LiteRT | ~50MB | High-quality embeddings |

---

## Top 10 Reference Repositories

### Android Local LLM
| # | Repository | Description | Key Takeaway |
|---|------------|-------------|--------------|
| 1 | **[BEKO2210/OFF-Line-AI-LLM](https://github.com/BEKO2210/OFF-Line-AI-LLM)** | Full Android app with llama.cpp, Compose UI, Room DB | Complete LLM app structure |
| 2 | **[YashBhadange2006/TinyAI](https://github.com/YashBhadange2006/TinyAI)** | MediaPipe + LiteRT-LM, Hugging Face integration | Multi-engine support, model downloads |
| 3 | **[NiqueWrld/Conversational-AI](https://github.com/NiqueWrld/Conversational-AI)** | MediaPipe LLM Inference, multi-model | Offline chat UI patterns |
| 4 | **[parttimenerd/local-android-ai](https://github.com/parttimenerd/local-android-ai)** | Local AI server, MediaPipe integration | Server-exposed AI |

### RAG on Android
| # | Repository | Description | Key Takeaway |
|---|------------|-------------|--------------|
| 5 | **[nicolas-raoul/offline-rag-android](https://github.com/nicolas-raoul/offline-rag-android)** | Complete offline RAG with Jetpack Compose | RAG architecture pattern |
| 6 | **On-Device RAG Guide** ([DEV.to](https://dev.to/software_mvp-factory/on-device-rag-for-android-4a7g))** | ONNX Runtime + sqlite-vec** | Embedding + vector search implementation |
| 7 | **[dev07060/mobile_rag_engine](https://github.com/dev07060/mobile_rag_engine)** | Rust core + SQLite vector, hybrid search | High-performance RAG engine |

### Quran Apps
| # | Repository | Description | Key Takeaway |
|---|------------|-------------|--------------|
| 8 | **[jonyszone/quran_android_compose](https://github.com/jonyszone/quran_android_compose)** | Quran app with Compose migration | Quran data structure |
| 9 | **[meshari3355/quran-android](https://github.com/meshari3355/quran-android)** | Full Islamic app with Compose | Feature-rich Islamic app |
| 10 | **[Hotaro26/QuranReader](https://github.com/Hotaro26/QuranReader)** | Modern Compose UI for Quran** | Clean Compose UI patterns |

### Architecture Reference
| # | Repository | Description | Key Takeaway |
|---|------------|-------------|--------------|
| Bonus | **[igorwojda/android-showcase](https://github.com/igorwojda/android-showcase)** | Clean Architecture, SOLID, best practices | Architecture patterns |

---

## Development Timeline Estimate

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| **Project Setup & Architecture** | 1 week | Clean Architecture, modules, DI |
| **Quran Data Layer** | 2 weeks | Room DB, data import, FTS5 |
| **Quran UI Features** | 2 weeks | Reading, tajwid, search, bookmarks |
| **RAG Pipeline** | 2 weeks | Embeddings, vector DB, document ingestion |
| **LLM Integration** | 2 weeks | LiteRT-LM, model download, inference |
| **Chatbot + Persona** | 1 week | Chat UI, persona management |
| **Tahsin Feature** | 1 week | Tahsin rules, pronunciation guide |
| **Download Manager** | 1 week | Resumable downloads, network monitoring |
| **Testing & Polish** | 1 week | Unit tests, UI tests, performance |
| **Total** | **~13 weeks** | Full production-ready app |

---

## Key Takeaways

1. **Use LiteRT-LM** over MediaPipe LLM Inference (mediapipe is in maintenance mode)
2. **Implement sqlite-vec** for vector search — lightweight, no native library headaches
3. **Limit embedding threads to 2** to prevent thermal throttling
4. **Use 512-token chunks with 50-token overlap** for optimal RAG on mobile
5. **Custom download manager** with byte-range requests for resume support
6. **Follow Clean Architecture** with clear domain/data/presentation separation
7. **Model files**: 4-bit quantized models under 1GB work best on 8GB RAM devices
8. **Test on physical devices** — emulators are unreliable for LLM inference

---

## Quick Start Commands

```bash
# Clone template repositories
git clone https://github.com/BEKO2210/OFF-Line-AI-LLM.git
git clone https://github.com/nicolas-raoul/offline-rag-android.git

# Download recommended model
# Gemma-3-1B-IT (4-bit)
# From: https://huggingface.co/litert-community/Gemma3-1B-IT

# Push model to device
adb shell mkdir -p /data/local/tmp/llm/
adb push model.task /data/local/tmp/llm/
```

---

*This guide provides a complete roadmap for building Quran Plus. Adapt the recommendations based on your specific requirements and device testing results.*

Here is a comprehensive, step-by-step development blueprint for **Quran Plus**, an offline-first Android application built with Jetpack Compose that integrates a local LLM, RAG, and a full-featured Quran reader.

---

## 1. Core Technology Stack (Best Practice)

| Layer | Technology | Rationale |
| :--- | :--- | :--- |
| **UI** | Jetpack Compose + Material 3 | Modern, declarative UI toolkit for building native interfaces efficiently. |
| **Architecture** | MVVM + Clean Architecture | Separates concerns (Data, Domain, Presentation), making the code testable, maintainable, and scalable. |
| **Local LLM Inference** | **LiteRT-LM (ex-TFLite)** / **MediaPipe LLM Inference API** | Best for on-device inference. MediaPipe provides a `.task` bundle for optimized performance on Android. |
| **Alternative LLM Engine** | **llama.cpp** (via `litertlm-kmp` or similar) | For running `.gguf` models. Offers broad model support and is widely used in the community. |
| **Vector Database (RAG)** | **SQLite + sqlite-vec** or **Room + custom HNSW** | For on-device vector storage and similarity search. Lightweight and fully offline. |
| **Text Embeddings** | **MediaPipe TextEmbedder** (USE-Lite) | Generates vector embeddings locally for RAG. |
| **Resumable Downloads** | **WorkManager** + **OkHttp** | Handles background downloads with automatic resume on network reconnection. |
| **Local Database** | **Room** (SQLite) | Stores Quran text, translations, bookmarks, and RAG documents. |
| **Dependency Injection** | **Dagger Hilt** | Standard for managing dependencies in large Android projects. |
| **Asynchronous Programming** | **Kotlin Coroutines & Flow** | Manages background tasks (inference, DB operations) seamlessly. |
| **Quran Data Processing** | Custom parser for **Tajweed** and **translations** | Render colored Tajweed text and handle multiple translations efficiently. |

---

## 2. Recommended Local LLM Models

For a device like the **Poco X7 Pro** (Snapdragon 8s Gen 3, 8GB+ RAM), the following models are excellent choices:

1.  **Qwen2.5-1.5B-Instruct (MediaPipe .task)**: Specifically packaged for Islamic apps (e.g., Alif app). Great for QA on Quran/Hadith.
2.  **Gemma 2B IT (MediaPipe)**: A solid, well-supported model from Google.
3.  **Phi-3.5-mini-instruct (GGUF)**: High quality for its size, runs well on 8GB RAM devices.
4.  **Qwen3.5-4B (GGUF/MNN)**: Offers superior output quality and can achieve 40-60 tokens/sec on Snapdragon 8 Gen 3 devices.

---

## 3. Step-by-Step Development Guide

### Phase 1: Project Setup & Core Architecture
1.  **Create Project**: Use Android Studio with Kotlin and Jetpack Compose.
2.  **Add Dependencies**: Add Compose, Room, Hilt, WorkManager, OkHttp, and MediaPipe/LiteRT dependencies.
3.  **Setup Clean Architecture**: Create three modules: `data`, `domain`, and `presentation`.
4.  **Setup Dagger Hilt**: Configure DI for the entire app.

### Phase 2: Quran Reader Module
1.  **Data Source**: Integrate a Quran database (e.g., from `quran_android_compose`) containing Arabic text, translations (Indonesian, English, etc.), and transliterations.
2.  **Tajweed Rendering**: Implement a custom composable to render colored Tajweed. For complex rendering, use `AndroidView` with a WebView or a custom layout.
3.  **Search**: Implement full-text search on the Quran text and translations using Room's `FTS4`/`FTS5`.
4.  **Bookmarks**: Use Room to save user bookmarks for verses.

### Phase 3: Local LLM & Inference Engine
1.  **Download Manager**: Implement a service using `WorkManager` to download model files (`.task` or `.gguf`) from a URL. Ensure it handles network interruptions and can resume downloads.
2.  **Model Loading**: Load the downloaded model using the LiteRT-LM or MediaPipe API.
3.  **Inference Service**: Create a service that takes a prompt and returns a streaming response (using Kotlin Flow) to the UI.

### Phase 4: RAG (Retrieval-Augmented Generation) System
1.  **Document Ingestion**: Allow users to upload documents (PDF, TXT) via SAF (Storage Access Framework).
2.  **Chunking & Embedding**: Split documents into chunks and generate embeddings using the on-device `TextEmbedder`.
3.  **Vector Storage**: Store the embeddings in a local vector database (e.g., SQLite with vector extension).
4.  **Retrieval**: When a user asks a question, generate an embedding for the query and perform a similarity search to find relevant document chunks.
5.  **Generation**: Feed the retrieved chunks into the prompt for the LLM to generate a context-aware answer.

### Phase 5: AI Persona & Tahsin Feature
1.  **Persona Prompt**: Allow users to edit the system prompt (e.g., "You are a knowledgeable Islamic scholar..."). Save this in `DataStore`.
2.  **Tahsin Module**: Integrate a kitab (book) on Tajweed rules. This can be a separate set of documents in the RAG system or a dedicated UI with audio/pronunciation guides.

---

## 4. Best Practices Enforcement

| Principle | Implementation |
| :--- | :--- |
| **SOLID** | Use interfaces for repositories and use cases. Each class has a single responsibility (e.g., `QuranRepository`, `LLMInferenceUseCase`). |
| **DRY** | Abstract common operations (e.g., network calls, database operations) into base classes or utility functions. |
| **KISS** | Keep UI logic simple. Composable functions should only handle UI state, not business logic. |
| **YAGNI** | Do not add features until they are explicitly required. Start with the core Quran reader and basic LLM chat. |
| **Clean Code** | Follow Kotlin coding conventions. Use meaningful names. Keep functions small (max 20-30 lines). Write unit tests for the domain and data layers. |

---

## 5. Top 5 GitHub Repositories for Reference

1.  **nicolas-raoul/offline-rag-android**
    - *Why:* A complete, fully offline RAG system on Android using Compose. It demonstrates on-device LLM inference and vector similarity search, which is the core of your RAG feature.

2.  **ahmedtamseer3/alif-islamic-v4-base**
    - *Why:* A specific on-device LLM for Islamic Q&A (Qwen2.5-1.5B) packaged as a MediaPipe task. Perfect for understanding how to integrate an Islamic-focused model.

3.  **nikki914/tflite-llm-mobile**
    - *Why:* A chat app that supports both local (TFLite) and remote LLMs. Great reference for building the chat UI and integrating TFLite models.

4.  **jonyszone/quran_android_compose**
    - *Why:* A clean, Jetpack Compose-based Quran reading app. Excellent reference for building the Quran reader UI, search, and navigation.

5.  **CoderBDK/Al-Quran**
    - *Why:* Features Tajweed colors, bookmarks, and a smooth Compose UI. Directly relevant for implementing the colored Tajweed and transliteration features.

---

## 6. High-Level Module Structure

```
app/
├── src/main/java/com/quranplus/
│   ├── data/                 (Data Layer)
│   │   ├── local/            (Room DB, DataStore)
│   │   ├── model/            (LLM, Embedding, Download)
│   │   └── repository/       (Implementations)
│   ├── domain/               (Domain Layer)
│   │   ├── model/            (Entities)
│   │   └── usecase/          (Business Logic)
│   └── presentation/         (Presentation Layer)
│       ├── ui/               (Compose Screens)
│       │   ├── quran/        (Reader, Search, Bookmarks)
│       │   ├── chatbot/      (Chat UI, RAG)
│       │   └── settings/     (Persona, Downloads)
│       └── viewmodel/        (State holders)
└── res/                      (Resources)
```

This architecture, combined with the recommended stack, will ensure **Quran Plus** is robust, maintainable, and delivers a seamless offline experience on devices like the Poco X7 Pro.
