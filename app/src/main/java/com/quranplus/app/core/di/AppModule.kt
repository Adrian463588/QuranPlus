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
import com.quranplus.app.features.rag.data.TfLiteEmbeddingService
import com.quranplus.app.features.rag.data.VectorRetrieverImpl
import com.quranplus.app.features.rag.domain.RagPipeline
import com.quranplus.app.features.rag.domain.VectorRetriever
import com.quranplus.app.features.settings.data.PreferencesManager
import com.quranplus.app.features.settings.presentation.SettingsViewModel
import com.quranplus.app.features.tahsin.data.TahsinRepositoryImpl
import com.quranplus.app.features.tahsin.domain.GetTahsinLessonByIdUseCase
import com.quranplus.app.features.tahsin.domain.GetTahsinLessonsUseCase
import com.quranplus.app.features.tahsin.domain.TahsinRepository
import com.quranplus.app.features.tahsin.domain.UpdateTahsinProgressUseCase
import com.quranplus.app.features.tahsin.presentation.TahsinViewModel
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

    // Core Managers & Services
    single { PreferencesManager(androidContext()) }
    single { ResumableDownloader(androidContext()) }
    single { TfLiteEmbeddingService(androidContext()) }
    single<VectorRetriever> { VectorRetrieverImpl(get(), get(), get()) }
    single { RagPipeline(get()) }
    single { ModelRepository(androidContext()) }
    single { LiteRtLmRunner(androidContext(), get()) }

    // Repositories
    single<QuranRepository> { QuranRepositoryImpl(get(), get(), get()) }
    single<TahsinRepository> { TahsinRepositoryImpl(get()) }
    single<ChatRepository> { ChatRepositoryImpl(get(), get(), get(), get(), get()) }

    // Use Cases — Quran
    factory { GetSurahListUseCase(get()) }
    factory { GetSurahDetailUseCase(get()) }
    factory { GetAyahsBySurahUseCase(get()) }
    factory { SearchQuranUseCase(get()) }
    factory { ToggleBookmarkUseCase(get()) }
    factory { GetBookmarksUseCase(get()) }
    factory { DeleteBookmarkUseCase(get()) }
    factory { SaveLastReadUseCase(get()) }
    factory { GetLastReadUseCase(get()) }

    // Use Cases — Tahsin
    factory { GetTahsinLessonsUseCase(get()) }
    factory { GetTahsinLessonByIdUseCase(get()) }
    factory { UpdateTahsinProgressUseCase(get()) }

    // Use Cases — Chat
    factory { GetChatHistoryUseCase(get()) }
    factory { SaveChatMessageUseCase(get()) }
    factory { ClearChatHistoryUseCase(get()) }
    factory { GenerateRagAnswerUseCase(get()) }

    // ViewModels
    viewModel { QuranViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ChatViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { TahsinViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
