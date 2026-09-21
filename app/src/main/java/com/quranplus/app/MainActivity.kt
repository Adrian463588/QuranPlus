package com.quranplus.app

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quranplus.app.core.audio.AudioPlayerManager
import com.quranplus.app.core.ui.components.AdaptiveNavigationScaffold
import com.quranplus.app.core.ui.components.AppDestination
import com.quranplus.app.core.ui.components.AppEmptyState
import com.quranplus.app.core.ui.theme.QuranPlusTheme
import com.quranplus.app.features.audio.presentation.AudioDownloadViewModel
import com.quranplus.app.features.audio.presentation.AudioManagerScreen
import com.quranplus.app.features.chatbot.data.ModelRepository
import com.quranplus.app.features.chatbot.presentation.ChatScreen
import com.quranplus.app.features.chatbot.presentation.ChatViewModel
import com.quranplus.app.features.chatbot.presentation.ModelGateScreen
import com.quranplus.app.features.gharib.presentation.GharibScreen
import com.quranplus.app.features.hadith.presentation.HadithScreen
import com.quranplus.app.features.hadith.presentation.HadithViewModel
import com.quranplus.app.features.hadith.data.HadithBundleWorkState
import com.quranplus.app.features.quran.presentation.BookmarksScreen
import com.quranplus.app.features.quran.presentation.QuranReaderScreen
import com.quranplus.app.features.quran.presentation.QuranViewModel
import com.quranplus.app.features.quran.presentation.SearchScreen
import com.quranplus.app.features.quran.presentation.SurahListScreen
import com.quranplus.app.features.rag.presentation.RagDocumentViewModel
import com.quranplus.app.features.rag.presentation.RagImportState
import com.quranplus.app.features.rag.domain.CitationTargetValidator
import com.quranplus.app.features.settings.data.PreferencesManager
import com.quranplus.app.features.dzikir.presentation.DzikirScreen
import com.quranplus.app.features.dzikir.presentation.DzikirViewModel
import com.quranplus.app.features.settings.presentation.MoreScreen
import com.quranplus.app.features.settings.presentation.SettingsScreen
import com.quranplus.app.features.settings.presentation.SettingsViewModel
import com.quranplus.app.features.tahsin.presentation.LessonDetailScreen
import com.quranplus.app.features.tahsin.presentation.TahsinHomeScreen
import com.quranplus.app.features.tahsin.presentation.TahsinQuizScreen
import com.quranplus.app.features.tahsin.presentation.TahsinViewModel
import com.quranplus.app.features.tahsin.presentation.QuizViewModel
import com.quranplus.app.features.waqaf.presentation.WaqafGuideScreen
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val preferencesManager: PreferencesManager by inject()
    private val modelRepository: ModelRepository by inject()
    private val audioPlayerManager: AudioPlayerManager by inject()
    private val ragDocumentViewModel: RagDocumentViewModel by viewModel()
    private val openStorageTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            ragDocumentViewModel.linkStorageTree(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }
    private val openRagDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            ragDocumentViewModel.importDocument(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkMode by preferencesManager.isDarkMode.collectAsStateWithLifecycle(initialValue = true)
            val windowSizeClass = calculateWindowSizeClass(this)

            QuranPlusTheme(darkTheme = isDarkMode) {
                AppMain(
                    widthSizeClass = windowSizeClass.widthSizeClass,
                    preferencesManager = preferencesManager,
                    modelRepository = modelRepository,
                    audioPlayerManager = audioPlayerManager,
                    ragDocumentViewModel = ragDocumentViewModel,
                    onRequestRagDocument = {
                        openStorageTreeLauncher.launch(null)
                    },
                    onRequestRagDocumentFile = {
                        openRagDocumentLauncher.launch(
                            arrayOf("text/plain", "text/markdown", "application/json", "application/pdf")
                        )
                    },
                    onOpenExternalUrl = ::openExternalUrl
                )
            }
        }
    }

    private fun openExternalUrl(url: String) {
        val safeUrl = CitationTargetValidator.validateHttpsUrl(url) ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // The citation remains visible; devices without a browser simply do nothing.
        }
    }
}

@Composable
fun AppMain(
    widthSizeClass: WindowWidthSizeClass,
    preferencesManager: PreferencesManager,
    modelRepository: ModelRepository,
    audioPlayerManager: AudioPlayerManager,
    ragDocumentViewModel: RagDocumentViewModel,
    onRequestRagDocument: () -> Unit,
    onRequestRagDocumentFile: () -> Unit,
    onOpenExternalUrl: (String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppDestination.QURAN.route

    val quranViewModel: QuranViewModel = koinViewModel()
    val chatViewModel: ChatViewModel = koinViewModel()
    val tahsinViewModel: TahsinViewModel = koinViewModel()
    val hadithViewModel: HadithViewModel = koinViewModel()
    val quizViewModel: QuizViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val audioDownloadViewModel: AudioDownloadViewModel = koinViewModel()
    val dzikirViewModel: DzikirViewModel = koinViewModel()

    AdaptiveNavigationScaffold(
        currentRoute = currentRoute,
        widthSizeClass = widthSizeClass,
        onNavigateToDestination = { destination ->
            if (destination == AppDestination.QURAN) {
                navController.navigateToQuranRoot()
            } else if (destination == AppDestination.HADITH) {
                hadithViewModel.resetToCatalog()
                navController.navigate(destination.route) {
                    popUpTo(AppDestination.QURAN.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                navController.navigate(destination.route) {
                    popUpTo(AppDestination.QURAN.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) {
        AppNavHost(
            navController = navController,
            widthSizeClass = widthSizeClass,
            quranViewModel = quranViewModel,
            chatViewModel = chatViewModel,
            tahsinViewModel = tahsinViewModel,
            hadithViewModel = hadithViewModel,
            quizViewModel = quizViewModel,
            settingsViewModel = settingsViewModel,
            audioDownloadViewModel = audioDownloadViewModel,
            dzikirViewModel = dzikirViewModel,
            preferencesManager = preferencesManager,
            modelRepository = modelRepository,
            audioPlayerManager = audioPlayerManager,
            ragDocumentViewModel = ragDocumentViewModel,
            onRequestRagDocument = onRequestRagDocument,
            onRequestRagDocumentFile = onRequestRagDocumentFile,
            onOpenExternalUrl = onOpenExternalUrl
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
    quranViewModel: QuranViewModel,
    chatViewModel: ChatViewModel,
    tahsinViewModel: TahsinViewModel,
    hadithViewModel: HadithViewModel,
    quizViewModel: QuizViewModel,
    settingsViewModel: SettingsViewModel,
    audioDownloadViewModel: AudioDownloadViewModel,
    dzikirViewModel: DzikirViewModel,
    preferencesManager: PreferencesManager,
    modelRepository: ModelRepository,
    audioPlayerManager: AudioPlayerManager,
    ragDocumentViewModel: RagDocumentViewModel,
    onRequestRagDocument: () -> Unit,
    onRequestRagDocumentFile: () -> Unit,
    onOpenExternalUrl: (String) -> Unit
) {
    val isModelReady by chatViewModel.isModelReady.collectAsStateWithLifecycle()
    val hadithBundleState by chatViewModel.hadithBundleState.collectAsStateWithLifecycle()
    val selectedEmbeddingModelId by chatViewModel.selectedEmbeddingModelId.collectAsStateWithLifecycle()
    val isSafStorageReady by ragDocumentViewModel.storageStatus.collectAsStateWithLifecycle()
    val ragState by ragDocumentViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(ragState) {
        if (ragState is RagImportState.Indexing ||
            ragState is RagImportState.Indexed ||
            ragState is RagImportState.IndexBlocked
        ) {
            chatViewModel.checkModelStatus()
        }
    }

    // Hadist import changes the corpus after the chat screen may already have
    // marked its index current. Re-requesting here makes the next retrieval
    // include the newly verified bundle without coupling ViewModels to data.
    LaunchedEffect(hadithBundleState.workState, hadithBundleState.localRecordCount) {
        if (hadithBundleState.workState is HadithBundleWorkState.Completed ||
            hadithBundleState.localRecordCount > 0
        ) {
            ragDocumentViewModel.buildIndex()
        }
    }

    LaunchedEffect(selectedEmbeddingModelId) {
        if (selectedEmbeddingModelId.isNotBlank()) {
            ragDocumentViewModel.buildIndex()
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.QURAN.route,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(220)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(220)) }
    ) {
        // --- 1. Al-Quran Navigation ---
        composable(AppDestination.QURAN.route) {
            SurahListScreen(
                viewModel = quranViewModel,
                onSurahClick = { surahNumber, ayahNumber ->
                    navController.navigateToReader(surahNumber, ayahNumber)
                },
                onSearchClick = {
                    navController.navigate("quran_search")
                },
                onNavigateToSettings = {
                    navController.navigate(AppDestination.SETTINGS.route)
                },
                widthSizeClass = widthSizeClass
            )
        }

        composable(
            route = "quran_reader/{surahNumber}?initialAyah={initialAyah}",
            arguments = listOf(
                navArgument("surahNumber") { type = NavType.IntType },
                navArgument("initialAyah") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val surahNumber = backStackEntry.arguments?.getInt("surahNumber")
                ?.takeIf { it in 1..114 }
            val initialAyah = backStackEntry.arguments?.getInt("initialAyah")
                ?.takeIf { it > 0 }
            if (surahNumber == null || initialAyah == null) {
                RouteArgumentError("Tujuan reader tidak memiliki surah atau ayat yang valid")
            } else {
                QuranReaderScreen(
                    surahNumber = surahNumber,
                    initialAyahNumber = initialAyah,
                    viewModel = quranViewModel,
                    preferencesManager = preferencesManager,
                    audioPlayerManager = audioPlayerManager,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToQuranRoot = { navController.navigateToQuranRoot() },
                    onNavigateToAyah = { targetSurah, targetAyah ->
                        navController.navigateToReader(targetSurah, targetAyah)
                    }
                )
            }
        }

        composable("quran_search") {
            SearchScreen(
                viewModel = quranViewModel,
                onAyahClick = { surahNumber, ayahNumber ->
                    navController.navigateToReader(surahNumber, ayahNumber)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- 2. Tanya AI (Chatbot RAG) Navigation ---
        composable(
            route = "${AppDestination.HADITH.route}?collectionId={collectionId}&hadithNumber={hadithNumber}",
            arguments = listOf(
                navArgument("collectionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("hadithNumber") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val initialCollectionId = backStackEntry.arguments?.getString("collectionId")
            val initialHadithNumber = backStackEntry.arguments?.getInt("hadithNumber")
                ?.takeIf { it > 0 }
            LaunchedEffect(initialCollectionId, initialHadithNumber) {
                if (initialCollectionId != null && initialHadithNumber != null) {
                    hadithViewModel.openReference(initialCollectionId, initialHadithNumber)
                }
            }
            HadithScreen(
                viewModel = hadithViewModel,
                onRequestStorage = onRequestRagDocument,
                onBundleReadyForAi = ragDocumentViewModel::buildIndex
            )
        }

        composable(AppDestination.MORE.route) {
            MoreScreen(
                onNavigateToBookmarks = {
                    navController.navigateToSecondary(AppDestination.BOOKMARKS)
                },
                onNavigateToSettings = {
                    navController.navigateToSecondary(AppDestination.SETTINGS)
                },
                onNavigateToWaqaf = {
                    navController.navigateToSecondary(AppDestination.WAQAF)
                },
                onNavigateToGharib = {
                    navController.navigateToSecondary(AppDestination.GHARIB)
                },
                onNavigateToAudio = {
                    navController.navigateToSecondary(AppDestination.AUDIO)
                },
                onNavigateToQuiz = {
                    navController.navigate("tahsin_quiz")
                },
                onNavigateToDzikir = {
                    navController.navigateToSecondary(AppDestination.DZIKIR)
                }
            )
        }

        // --- 3. Tanya AI (Chatbot RAG) Navigation ---
        composable(AppDestination.CHAT.route) {
            LaunchedEffect(Unit) {
                ragDocumentViewModel.ensureIndex()
            }
            if (isModelReady) {
                ChatScreen(
                    viewModel = chatViewModel,
                    preferencesManager = preferencesManager,
                    onNavigateToAyah = { surahNumber, ayahNumber ->
                        navController.navigateToReader(surahNumber, ayahNumber)
                    },
                    onNavigateToHadith = { collectionId, hadithNumber ->
                        navController.navigateToHadith(collectionId, hadithNumber)
                    },
                    onNavigateToExternalUrl = onOpenExternalUrl,
                    onRequestStorage = onRequestRagDocument
                )
            } else {
                ModelGateScreen(
                    viewModel = chatViewModel,
                    modelRepository = modelRepository,
                    onModelReady = {
                        ragDocumentViewModel.buildIndex()
                        chatViewModel.checkModelStatus()
                    },
                    readiness = chatViewModel.readiness,
                    onRequestStorage = onRequestRagDocument,
                    isStorageReady = isSafStorageReady?.isAccessible == true
                )
            }
        }

        // --- 4. Tahsin & Quiz Navigation ---
        composable(AppDestination.TAHSIN.route) {
            TahsinHomeScreen(
                viewModel = tahsinViewModel,
                onLessonClick = { lessonId ->
                    navController.navigate("tahsin_detail/$lessonId")
                },
                onQuizClick = {
                    navController.navigate("tahsin_quiz")
                }
            )
        }

        composable(
            route = "tahsin_detail/{lessonId}",
            arguments = listOf(navArgument("lessonId") { type = NavType.IntType })
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getInt("lessonId")?.takeIf { it > 0 }
            if (lessonId == null) {
                RouteArgumentError("Tujuan Tahsin tidak memiliki lesson yang valid")
            } else {
                LessonDetailScreen(
                    lessonId = lessonId,
                    viewModel = tahsinViewModel,
                    audioPlayerManager = audioPlayerManager,
                    onNavigateToAyah = { surahNumber, ayahNumber ->
                        navController.navigateToReader(surahNumber, ayahNumber)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable("tahsin_quiz") {
            TahsinQuizScreen(
                viewModel = quizViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- 4. Sprint 2 Knowledge & Tools Screens ---
        composable("gharib_directory") {
            GharibScreen(
                audioPlayerManager = audioPlayerManager,
                onNavigateToAyah = { surah, ayah ->
                    navController.navigate("quran_reader/$surah?initialAyah=$ayah")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("waqaf_guide") {
            WaqafGuideScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("audio_manager") {
            AudioManagerScreen(
                audioPlayerManager = audioPlayerManager,
                quranViewModel = quranViewModel,
                downloadViewModel = audioDownloadViewModel,
                onRequestStorage = onRequestRagDocument,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- 5. Bookmarks Navigation ---
        composable(AppDestination.BOOKMARKS.route) {
            BookmarksScreen(
                viewModel = quranViewModel,
                onBookmarkClick = { surahNumber, ayahNumber ->
                    navController.navigateToReader(surahNumber, ayahNumber)
                }
            )
        }

        // --- 6. Settings Navigation ---
        composable(AppDestination.SETTINGS.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToAudioManager = { navController.navigate("audio_manager") },
                onNavigateToWaqafGuide = { navController.navigate("waqaf_guide") },
                onNavigateToGharib = { navController.navigate("gharib_directory") },
                onNavigateToQuiz = { navController.navigate("tahsin_quiz") },
                ragDocumentViewModel = ragDocumentViewModel,
                onRequestRagDocument = onRequestRagDocument,
                onRequestRagDocumentFile = onRequestRagDocumentFile
            )
        }

        // --- 7. Dzikir, Wirid & Hizib Navigation ---
        composable(AppDestination.DZIKIR.route) {
            DzikirScreen(
                viewModel = dzikirViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private fun NavHostController.navigateToQuranRoot() {
    if (!popBackStack(AppDestination.QURAN.route, inclusive = false)) {
        navigate(AppDestination.QURAN.route) {
            launchSingleTop = true
        }
    }
}

private fun NavHostController.navigateToReader(surahNumber: Int, ayahNumber: Int) {
    require(surahNumber in 1..114) { "Nomor surah tidak valid" }
    require(ayahNumber > 0) { "Nomor ayat tidak valid" }
    navigate("quran_reader/$surahNumber?initialAyah=$ayahNumber")
}

private fun NavHostController.navigateToHadith(collectionId: String, hadithNumber: Int) {
    if (collectionId.isBlank() || hadithNumber <= 0) return
    navigate(
        "${AppDestination.HADITH.route}?collectionId=${Uri.encode(collectionId)}&hadithNumber=$hadithNumber"
    )
}

private fun NavHostController.navigateToSecondary(destination: AppDestination) {
    require(!destination.isPrimary) { "Destination sekunder tidak valid: ${destination.route}" }
    navigate(destination.route) {
        popUpTo(AppDestination.MORE.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun RouteArgumentError(message: String) {
    AppEmptyState(
        icon = Icons.Rounded.ErrorOutline,
        title = "Tujuan tidak tersedia",
        description = message
    )
}
