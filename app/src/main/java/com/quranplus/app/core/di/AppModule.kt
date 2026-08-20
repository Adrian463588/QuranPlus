package com.quranplus.app.core.di

import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.database.ReferenceAssetSynchronizer
import com.quranplus.app.core.network.ResumableDownloader
import com.quranplus.app.features.chatbot.data.ChatRepositoryImpl
import com.quranplus.app.features.chatbot.data.LiteRtLmRunner
import com.quranplus.app.features.chatbot.data.ModelDownloadScheduler
import com.quranplus.app.features.chatbot.data.ModelRepository
import com.quranplus.app.features.chatbot.data.AiReadinessChecker
import com.quranplus.app.features.chatbot.domain.ChatRepository
import com.quranplus.app.features.chatbot.domain.ClearChatHistoryUseCase
import com.quranplus.app.features.chatbot.domain.GenerateRagAnswerUseCase
import com.quranplus.app.features.chatbot.domain.GetChatHistoryUseCase
import com.quranplus.app.features.chatbot.domain.SaveChatMessageUseCase
import com.quranplus.app.features.chatbot.presentation.ChatViewModel
import com.quranplus.app.features.audio.data.AudioDownloadScheduler
import com.quranplus.app.features.audio.presentation.AudioDownloadViewModel
import com.quranplus.app.features.hadith.data.HadithRepositoryImpl
import com.quranplus.app.features.hadith.data.HadithReferenceImporter
import com.quranplus.app.features.hadith.data.HadithBundleDownloadScheduler
import com.quranplus.app.features.hadith.data.HadithBundleImporter
import com.quranplus.app.features.hadith.data.HadithBundleManager
import com.quranplus.app.features.hadith.domain.GetHadithCollectionsUseCase
import com.quranplus.app.features.hadith.domain.HadithRepository
import com.quranplus.app.features.hadith.domain.SearchHadithUseCase
import com.quranplus.app.features.hadith.presentation.HadithViewModel
import com.quranplus.app.features.quran.data.QuranRepositoryImpl
import com.quranplus.app.features.quran.data.WordByWordRepositoryImpl
import com.quranplus.app.features.quran.domain.DeleteBookmarkUseCase
import com.quranplus.app.features.quran.domain.RestoreBookmarkUseCase
import com.quranplus.app.features.quran.domain.UpdateBookmarkNoteUseCase
import com.quranplus.app.features.quran.domain.GetAyahsBySurahUseCase
import com.quranplus.app.features.quran.domain.GetFirstAyahByJuzUseCase
import com.quranplus.app.features.quran.domain.GetFirstAyahByPageUseCase
import com.quranplus.app.features.quran.domain.GetBookmarksUseCase
import com.quranplus.app.features.quran.domain.GetLastReadUseCase
import com.quranplus.app.features.quran.domain.GetWordsBySurahUseCase
import com.quranplus.app.features.quran.domain.GetSurahDetailUseCase
import com.quranplus.app.features.quran.domain.GetSurahListUseCase
import com.quranplus.app.features.quran.domain.QuranRepository
import com.quranplus.app.features.quran.domain.WordByWordRepository
import com.quranplus.app.features.quran.domain.SaveLastReadUseCase
import com.quranplus.app.features.quran.domain.SearchQuranUseCase
import com.quranplus.app.features.quran.domain.ToggleBookmarkUseCase
import com.quranplus.app.features.quran.presentation.QuranViewModel
import com.quranplus.app.features.rag.data.EmbeddingService
import com.quranplus.app.features.rag.data.OnnxEmbeddingService
import com.quranplus.app.features.rag.data.VectorRetrieverImpl
import com.quranplus.app.features.rag.data.SqliteVecVectorIndex
import com.quranplus.app.features.rag.data.RagCorpusIndexer
import com.quranplus.app.features.rag.domain.IndexCorpusUseCase
import com.quranplus.app.features.rag.domain.RagPipeline
import com.quranplus.app.features.rag.domain.VectorIndex
import com.quranplus.app.features.rag.domain.VectorRetriever
import com.quranplus.app.features.rag.data.SafDocumentImporter
import com.quranplus.app.features.rag.data.SafAssetStore
import com.quranplus.app.features.rag.presentation.RagDocumentViewModel
import com.quranplus.app.features.settings.data.PreferencesManager
import com.quranplus.app.features.settings.presentation.SettingsViewModel
import com.quranplus.app.features.tahsin.data.TahsinRepositoryImpl
import com.quranplus.app.features.tahsin.domain.GetTahsinLessonByIdUseCase
import com.quranplus.app.features.tahsin.domain.GetTahsinLessonsUseCase
import com.quranplus.app.features.tahsin.domain.TahsinRepository
import com.quranplus.app.features.tahsin.domain.UpdateTahsinProgressUseCase
import com.quranplus.app.features.tahsin.presentation.TahsinViewModel
import com.quranplus.app.features.tahsin.data.QuizRepositoryImpl
import com.quranplus.app.features.tahsin.domain.GetQuizQuestionsUseCase
import com.quranplus.app.features.tahsin.domain.QuizRepository
import com.quranplus.app.features.tahsin.domain.RecordQuizAttemptUseCase
import com.quranplus.app.features.tahsin.presentation.QuizViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database & DAOs
    single { QuranDatabase.getInstance(androidContext()) }
    single { ReferenceAssetSynchronizer(androidContext(), get()) }
    single { get<QuranDatabase>().quranDao() }
    single { get<QuranDatabase>().wordByWordDao() }
    single { get<QuranDatabase>().bookmarkDao() }
    single { get<QuranDatabase>().lastReadDao() }
    single { get<QuranDatabase>().tahsinDao() }
    single { get<QuranDatabase>().hadithDao() }
    single { get<QuranDatabase>().knowledgeChunkDao() }
    single { get<QuranDatabase>().chatDao() }
    single { get<QuranDatabase>().quizDao() }

    // Core Managers & Services
    single { PreferencesManager(androidContext()) }
    single { AudioDownloadScheduler(androidContext()) }
    single { com.quranplus.app.core.audio.AudioPlayerManager(androidContext()) }
    single { ResumableDownloader(androidContext()) }
    single { ModelDownloadScheduler(androidContext()) }
    single { HadithBundleDownloadScheduler(androidContext()) }
    single<EmbeddingService> { OnnxEmbeddingService(androidContext(), get()) }
    single<VectorIndex> { SqliteVecVectorIndex(get()) }
    single<VectorRetriever> { VectorRetrieverImpl(get()) }
    single { RagCorpusIndexer(get(), get(), get()) }
    single { RagPipeline() }
    single { ModelRepository(androidContext(), get()) }
    single { AiReadinessChecker(get(), get(), get(), get()) }
    single { LiteRtLmRunner(androidContext(), get()) }
    single { SafAssetStore(androidContext(), get()) }
    single { SafDocumentImporter(androidContext(), get(), get()) }
    single { HadithReferenceImporter(androidContext(), get()) }
    single { HadithBundleImporter(get(), get()) }
    single { HadithBundleManager(get(), get(), get(), get()) }

    // Repositories
    single<QuranRepository> { QuranRepositoryImpl(get(), get(), get()) }
    single<WordByWordRepository> { WordByWordRepositoryImpl(get()) }
    single<HadithRepository> { HadithRepositoryImpl(get()) }
    single<TahsinRepository> { TahsinRepositoryImpl(get()) }
    single<QuizRepository> { QuizRepositoryImpl(get()) }
    single<ChatRepository> { ChatRepositoryImpl(get(), get(), get(), get(), get()) }

    // Use Cases — Quran
    factory { GetSurahListUseCase(get()) }
    factory { GetSurahDetailUseCase(get()) }
    factory { GetAyahsBySurahUseCase(get()) }
    factory { GetFirstAyahByPageUseCase(get()) }
    factory { GetFirstAyahByJuzUseCase(get()) }
    factory { SearchQuranUseCase(get()) }
    factory { ToggleBookmarkUseCase(get()) }
    factory { GetBookmarksUseCase(get()) }
    factory { DeleteBookmarkUseCase(get()) }
    factory { RestoreBookmarkUseCase(get()) }
    factory { UpdateBookmarkNoteUseCase(get()) }
    factory { SaveLastReadUseCase(get()) }
    factory { GetLastReadUseCase(get()) }
    factory { GetWordsBySurahUseCase(get()) }

    // Use Cases — Hadist
    factory { GetHadithCollectionsUseCase(get()) }
    factory { SearchHadithUseCase(get()) }

    // Use Cases — Tahsin
    factory { GetTahsinLessonsUseCase(get()) }
    factory { GetTahsinLessonByIdUseCase(get()) }
    factory { UpdateTahsinProgressUseCase(get()) }
    factory { GetQuizQuestionsUseCase(get()) }
    factory { RecordQuizAttemptUseCase(get()) }

    // Use Cases — Chat
    factory { GetChatHistoryUseCase(get()) }
    factory { SaveChatMessageUseCase(get()) }
    factory { ClearChatHistoryUseCase(get()) }
    factory { GenerateRagAnswerUseCase(get()) }
    factory { IndexCorpusUseCase(get()) }

    // ViewModels
    viewModel { QuranViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AudioDownloadViewModel(get()) }
    viewModel { ChatViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HadithViewModel(get(), get(), get()) }
    viewModel { TahsinViewModel(get(), get(), get()) }
    viewModel { QuizViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { RagDocumentViewModel(get(), get(), get(), get(), get()) }
}
