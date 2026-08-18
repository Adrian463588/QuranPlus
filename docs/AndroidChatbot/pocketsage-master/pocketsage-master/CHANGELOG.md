# Changelog

All notable changes to PocketSage are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [0.1.0] — 2026-05-06

### Added

- **PDF ingestion** — Import any PDF via the Storage Access Framework; text is extracted on-device using `pdfbox-android`, split into overlapping 800-character chunks, and stored in Room.
- **LiteRT MiniLM embeddings** — Each chunk and every user query is embedded locally using a quantised `all-MiniLM-L6-v2` TFLite model via LiteRT, producing 384-dimensional L2-normalised vectors.
- **Top-K cosine retrieval** — At query time the app ranks all stored chunk embeddings against the query vector using cosine similarity (dot product over unit vectors) and returns the top-4 matches.
- **LiteRT-LM streaming — Gemma 4 E2B Instruct** — Retrieved chunks are injected into a grounded prompt and fed to `gemma-4-E2B-it.litertlm` via the LiteRT-LM `Engine` / `Session` API; response tokens stream into the UI via `callbackFlow`.
- **Library screen** — Lists all imported documents with chunk counts and import dates; long-press to delete a document and all its embeddings.
- **Chat screen** — Per-document or all-documents chat scope; streaming assistant bubbles with a typing indicator; collapsible source-snippet panel under each answer.
- **Fully offline architecture** — No `INTERNET` permission; zero network calls at any stage of the pipeline; model weights and embeddings live exclusively in app-private storage.
- **Secure model provisioning** — `ModelGate` screen guides first-time setup; model is written to a `.tmp` file and renamed only on successful copy to prevent partial-import corruption.