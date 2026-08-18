package com.quranplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quranplus.app.core.ui.components.AdaptiveNavigationScaffold
import com.quranplus.app.core.ui.components.AppDestination
import com.quranplus.app.core.ui.theme.QuranPlusTheme
import com.quranplus.app.features.chatbot.data.ModelRepository
import com.quranplus.app.features.chatbot.presentation.ChatScreen
import com.quranplus.app.features.chatbot.presentation.ChatViewModel
import com.quranplus.app.features.chatbot.presentation.ModelGateScreen
import com.quranplus.app.features.quran.presentation.BookmarksScreen
import com.quranplus.app.features.quran.presentation.QuranReaderScreen
import com.quranplus.app.features.quran.presentation.QuranViewModel
import com.quranplus.app.features.quran.presentation.SearchScreen
import com.quranplus.app.features.quran.presentation.SurahListScreen
import com.quranplus.app.features.settings.data.PreferencesManager
import com.quranplus.app.features.settings.presentation.SettingsScreen
import com.quranplus.app.features.settings.presentation.SettingsViewModel
import com.quranplus.app.features.tahsin.presentation.LessonDetailScreen
import com.quranplus.app.features.tahsin.presentation.TahsinHomeScreen
import com.quranplus.app.features.tahsin.presentation.TahsinViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

class MainActivity : ComponentActivity() {

    private val preferencesManager: PreferencesManager by inject()
    private val modelRepository: ModelRepository by inject()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkMode by preferencesManager.isDarkMode.collectAsState(initial = true)
            val windowSizeClass = calculateWindowSizeClass(this)

            QuranPlusTheme(darkTheme = isDarkMode) {
                AppMain(
                    widthSizeClass = windowSizeClass.widthSizeClass,
                    preferencesManager = preferencesManager,
                    modelRepository = modelRepository
                )
            }
        }
    }
}

@Composable
fun AppMain(
    widthSizeClass: WindowWidthSizeClass,
    preferencesManager: PreferencesManager,
    modelRepository: ModelRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppDestination.QURAN.route

    val quranViewModel: QuranViewModel = koinViewModel()
    val chatViewModel: ChatViewModel = koinViewModel()
    val tahsinViewModel: TahsinViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()

    AdaptiveNavigationScaffold(
        currentRoute = currentRoute,
        widthSizeClass = widthSizeClass,
        onNavigateToDestination = { destination ->
            navController.navigate(destination.route) {
                popUpTo(AppDestination.QURAN.route) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) {
        AppNavHost(
            navController = navController,
            quranViewModel = quranViewModel,
            chatViewModel = chatViewModel,
            tahsinViewModel = tahsinViewModel,
            settingsViewModel = settingsViewModel,
            preferencesManager = preferencesManager,
            modelRepository = modelRepository
        )
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    quranViewModel: QuranViewModel,
    chatViewModel: ChatViewModel,
    tahsinViewModel: TahsinViewModel,
    settingsViewModel: SettingsViewModel,
    preferencesManager: PreferencesManager,
    modelRepository: ModelRepository
) {
    val isModelReady by chatViewModel.isModelReady.collectAsState()

    NavHost(
        navController = navController,
        startDestination = AppDestination.QURAN.route,
        enterTransition = { fadeIn(tween(260)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) },
        exitTransition = { fadeOut(tween(220)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220)) },
        popEnterTransition = { fadeIn(tween(260)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) },
        popExitTransition = { fadeOut(tween(220)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220)) }
    ) {
        // --- 1. Al-Quran Navigation ---
        composable(AppDestination.QURAN.route) {
            SurahListScreen(
                viewModel = quranViewModel,
                onSurahClick = { surahNumber ->
                    navController.navigate("quran_reader/$surahNumber")
                },
                onSearchClick = {
                    navController.navigate("quran_search")
                }
            )
        }

        composable(
            route = "quran_reader/{surahNumber}?initialAyah={initialAyah}",
            arguments = listOf(
                navArgument("surahNumber") { type = NavType.IntType },
                navArgument("initialAyah") {
                    type = NavType.IntType
                    defaultValue = 1
                }
            )
        ) { backStackEntry ->
            val surahNumber = backStackEntry.arguments?.getInt("surahNumber") ?: 1
            val initialAyah = backStackEntry.arguments?.getInt("initialAyah") ?: 1
            QuranReaderScreen(
                surahNumber = surahNumber,
                initialAyahNumber = initialAyah,
                viewModel = quranViewModel,
                preferencesManager = preferencesManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("quran_search") {
            SearchScreen(
                viewModel = quranViewModel,
                onAyahClick = { surahNumber, ayahNumber ->
                    navController.navigate("quran_reader/$surahNumber?initialAyah=$ayahNumber")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- 2. Tanya AI (Chatbot RAG) Navigation ---
        composable(AppDestination.CHAT.route) {
            if (isModelReady) {
                ChatScreen(
                    viewModel = chatViewModel,
                    preferencesManager = preferencesManager,
                    onNavigateToAyah = { surahNumber, ayahNumber ->
                        navController.navigate("quran_reader/$surahNumber?initialAyah=$ayahNumber")
                    }
                )
            } else {
                ModelGateScreen(
                    viewModel = chatViewModel,
                    modelRepository = modelRepository,
                    onModelReady = { chatViewModel.checkModelStatus() }
                )
            }
        }

        // --- 3. Tahsin Navigation ---
        composable(AppDestination.TAHSIN.route) {
            TahsinHomeScreen(
                viewModel = tahsinViewModel,
                onLessonClick = { lessonId ->
                    navController.navigate("tahsin_detail/$lessonId")
                }
            )
        }

        composable(
            route = "tahsin_detail/{lessonId}",
            arguments = listOf(navArgument("lessonId") { type = NavType.IntType })
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getInt("lessonId") ?: 1
            LessonDetailScreen(
                lessonId = lessonId,
                viewModel = tahsinViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // --- 4. Bookmarks Navigation ---
        composable(AppDestination.BOOKMARKS.route) {
            BookmarksScreen(
                viewModel = quranViewModel,
                onBookmarkClick = { surahNumber, _ ->
                    navController.navigate("quran_reader/$surahNumber")
                }
            )
        }

        // --- 5. Settings Navigation ---
        composable(AppDestination.SETTINGS.route) {
            SettingsScreen(viewModel = settingsViewModel)
        }
    }
}
