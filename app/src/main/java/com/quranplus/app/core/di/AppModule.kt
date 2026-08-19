package com.quranplus.app.core.di

import com.quranplus.app.core.database.QuranDatabase
import com.quranplus.app.core.network.ResumableDownloader
import com.quranplus.app.features.chatbot.data.ChatRepositoryImpl
import com.quranplus.app.features.chatbot.data.LiteRtLmRunner
import com.quranplus.app.features.chatbot.data.ModelRepository
import com.quranplus.app.features.chatbot.domain.ChatRepository
import com.quranplus.app.features.chatbot.domain.ClearChatHistoryUseCase
import com.quranplus.app.features.chatbot.domain.GenerateRagAnswerUseCase
import com.quranplus.app.features.chatbot.domain.GetChatHistoryUseCase
import com.quranplus.app.features.chatbot.domain.SaveChatMessageUseCase
import com.quranplus.app.features.chatbot.presentation.ChatViewModel
import com.quranplus.app.features.quran.data.QuranRepositoryImpl
import com.quranplus.app.features.quran.domain.DeleteBookmarkUseCase
import com.quranplus.app.features.quran.domain.RestoreBookmarkUseCase
import com.quranplus.app.features.quran.domain.UpdateBookmarkNoteUseCase
import com.quranplus.app.features.quran.domain.GetAyahsBySurahUseCase
import com.quranplus.app.features.quran.domain.GetBookmarksUseCase
import com.quranplus.app.features.quran.domain.GetLastReadUseCase
import com.quranplus.app.features.quran.domain.GetSurahDetailUseCase
import com.quranplus.app.features.quran.domain.GetSurahListUseCase
import com.quranplus.app.features.quran.domain.QuranRepository
import com.quranplus.app.features.quran.domain.SaveLastReadUseCase
import com.quranplus.app.features.quran.domain.SearchQuranUseCase
import com.quranplus.app.features.quran.domain.ToggleBookmarkUseCase
import com.quranplus.app.features.quran.presentation.QuranViewModel
import com.quranplus.app.features.rag.data.EmbeddingService
import com.quranplus.app.features.rag.data.OnnxEmbeddingService
import com.quranplus.app.features.rag.data.VectorRetrieverImpl
import com.quranplus.app.features.rag.domain.RagPipeline
import com.quranplus.app.features.rag.domain.VectorRetriever
import com.quranplus.app.features.rag.data.SafDocumentImporter
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
    single { get<QuranDatabase>().quranDao() }
    single { get<QuranDatabase>().bookmarkDao() }
    single { get<QuranDatabase>().lastReadDao() }
    single { get<QuranDatabase>().tahsinDao() }
    single { get<QuranDatabase>().hadithDao() }
    single { get<QuranDatabase>().knowledgeChunkDao() }
    single { get<QuranDatabase>().chatDao() }
    single { get<QuranDatabase>().quizDao() }

    // Core Managers & Services
    single { PreferencesManager(androidContext()) }
    single { com.quranplus.app.core.audio.AudioPlayerManager(androidContext()) }
    single { ResumableDownloader(androidContext()) }
    single<EmbeddingService> { OnnxEmbeddingService(androidContext()) }
    single<VectorRetriever> { VectorRetrieverImpl(get()) }
    single { RagPipeline() }
    single { ModelRepository(androidContext()) }
    single { LiteRtLmRunner(androidContext(), get()) }
    single { SafDocumentImporter(androidContext()) }

    // Repositories
    single<QuranRepository> { QuranRepositoryImpl(get(), get(), get()) }
    single<TahsinRepository> { TahsinRepositoryImpl(get()) }
    single<QuizRepository> { QuizRepositoryImpl(get()) }
    single<ChatRepository> { ChatRepositoryImpl(get(), get(), get(), get(), get()) }

    // Use Cases — Quran
    factory { GetSurahListUseCase(get()) }
    factory { GetSurahDetailUseCase(get()) }
    factory { GetAyahsBySurahUseCase(get()) }
    factory { SearchQuranUseCase(get()) }
    factory { ToggleBookmarkUseCase(get()) }
    factory { GetBookmarksUseCase(get()) }
    factory { DeleteBookmarkUseCase(get()) }
    factory { RestoreBookmarkUseCase(get()) }
    factory { UpdateBookmarkNoteUseCase(get()) }
    factory { SaveLastReadUseCase(get()) }
    factory { GetLastReadUseCase(get()) }

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

    // ViewModels
    viewModel { QuranViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ChatViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TahsinViewModel(get(), get(), get()) }
    viewModel { QuizViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { RagDocumentViewModel(get()) }
}
