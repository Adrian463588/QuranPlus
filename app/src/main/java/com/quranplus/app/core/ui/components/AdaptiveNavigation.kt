package com.quranplus.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import com.quranplus.app.core.ui.theme.Spacing

enum class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val isPrimary: Boolean = true
) {
    QURAN("quran_home", "Al-Qur'an", Icons.AutoMirrored.Rounded.MenuBook),
    HADITH("hadith_home", "Hadist", Icons.Rounded.AutoStories),
    CHAT("chat_home", "Tanya AI", Icons.Rounded.AutoAwesome),
    TAHSIN("tahsin_home", "Tahsin", Icons.Rounded.School),
    MORE("more_home", "More", Icons.Rounded.MoreHoriz),
    BOOKMARKS("bookmarks_home", "Bookmark", Icons.Rounded.Bookmark, isPrimary = false),
    SETTINGS("settings_home", "Pengaturan", Icons.Rounded.Settings, isPrimary = false),
    WAQAF("waqaf_guide", "Waqaf", Icons.AutoMirrored.Rounded.MenuBook, isPrimary = false),
    GHARIB("gharib_directory", "Gharib", Icons.AutoMirrored.Rounded.MenuBook, isPrimary = false),
    AUDIO("audio_manager", "Audio", Icons.AutoMirrored.Rounded.MenuBook, isPrimary = false)
}

private fun isDestinationSelected(currentRoute: String, dest: AppDestination): Boolean {
    return when (dest) {
        AppDestination.QURAN -> currentRoute.startsWith(AppDestination.QURAN.route) ||
                currentRoute.startsWith("quran_reader") ||
                currentRoute.startsWith("quran_search")
        AppDestination.TAHSIN -> currentRoute.startsWith(AppDestination.TAHSIN.route) ||
                currentRoute.startsWith("tahsin_detail") ||
                currentRoute.startsWith("tahsin_quiz")
        AppDestination.CHAT -> currentRoute.startsWith(AppDestination.CHAT.route)
        AppDestination.HADITH -> currentRoute.startsWith(AppDestination.HADITH.route)
        AppDestination.BOOKMARKS -> currentRoute.startsWith(AppDestination.BOOKMARKS.route)
        AppDestination.MORE -> currentRoute.startsWith(AppDestination.MORE.route) ||
                currentRoute.startsWith(AppDestination.BOOKMARKS.route) ||
                currentRoute.startsWith(AppDestination.SETTINGS.route) ||
                currentRoute.startsWith(AppDestination.WAQAF.route) ||
                currentRoute.startsWith(AppDestination.GHARIB.route) ||
                currentRoute.startsWith(AppDestination.AUDIO.route)
        AppDestination.SETTINGS -> currentRoute.startsWith(AppDestination.SETTINGS.route)
        AppDestination.WAQAF -> currentRoute.startsWith(AppDestination.WAQAF.route)
        AppDestination.GHARIB -> currentRoute.startsWith(AppDestination.GHARIB.route)
        AppDestination.AUDIO -> currentRoute.startsWith(AppDestination.AUDIO.route)
    }
}

@Composable
private fun AnimatedNavigationIcon(
    destination: AppDestination,
    selected: Boolean
) {
    val scale = animateFloatAsState(
        targetValue = if (selected) 1.06f else 0.94f,
        animationSpec = tween(durationMillis = 120),
        label = "${destination.name.lowercase()}_icon_scale"
    )
    Icon(
        imageVector = destination.icon,
        contentDescription = destination.title,
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    )
}

@Composable
fun AdaptiveNavigationScaffold(
    currentRoute: String,
    widthSizeClass: WindowWidthSizeClass,
    onNavigateToDestination: (AppDestination) -> Unit,
    content: @Composable () -> Unit
) {
    val destinations = AppDestination.entries
    val primaryDestinations = destinations.filter(AppDestination::isPrimary)

    when (widthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            // Tablet / Desktop: Permanent Left Navigation Drawer
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(260.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.height(Spacing.xl))
                        Text(
                            text = "Quran Plus",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = Spacing.lg)
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        destinations.forEach { dest ->
                            val selected = isDestinationSelected(currentRoute, dest)
                            NavigationDrawerItem(
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title) },
                                selected = selected,
                                onClick = { onNavigateToDestination(dest) },
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
        WindowWidthSizeClass.Medium -> {
            // Foldable / Landscape: Navigation Rail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        destinations.forEach { dest ->
                            val selected = isDestinationSelected(currentRoute, dest)
                            NavigationRailItem(
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title, style = MaterialTheme.typography.labelSmall) },
                                selected = selected,
                                onClick = { onNavigateToDestination(dest) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    content()
                }
            }
        }
        else -> {
            // Phone (Compact): Standard Bottom Navigation Bar
            val showBottomBar = !currentRoute.startsWith("quran_reader") && !currentRoute.startsWith("quran_search")
            val useIconOnlyLabels = LocalDensity.current.fontScale >= 1.3f

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 2.dp
                        ) {
                            primaryDestinations.forEach { dest ->
                                val selected = isDestinationSelected(currentRoute, dest)
                                NavigationBarItem(
                                    icon = { AnimatedNavigationIcon(dest, selected) },
                                    label = if (useIconOnlyLabels) null else {
                                        { Text(dest.title, style = MaterialTheme.typography.labelMedium) }
                                    },
                                    selected = selected,
                                    onClick = { onNavigateToDestination(dest) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .semantics {
                                            role = Role.Tab
                                            contentDescription = dest.title
                                            this.selected = selected
                                            stateDescription = if (selected) "Terpilih" else "Tidak terpilih"
                                        },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (showBottomBar) padding else androidx.compose.foundation.layout.PaddingValues(0.dp))
                ) {
                    content()
                }
            }
        }
    }
}
