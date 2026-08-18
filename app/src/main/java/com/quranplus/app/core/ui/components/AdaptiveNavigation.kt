package com.quranplus.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranplus.app.core.ui.theme.Spacing

enum class AppDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    QURAN("quran_home", "Al-Qur'an", Icons.Rounded.MenuBook),
    CHAT("chat_home", "Tanya AI", Icons.Rounded.AutoAwesome),
    TAHSIN("tahsin_home", "Tahsin", Icons.Rounded.School),
    BOOKMARKS("bookmarks_home", "Bookmark", Icons.Rounded.Bookmark),
    SETTINGS("settings_home", "Pengaturan", Icons.Rounded.Settings)
}

private fun isDestinationSelected(currentRoute: String, dest: AppDestination): Boolean {
    return when (dest) {
        AppDestination.QURAN -> currentRoute.startsWith(AppDestination.QURAN.route) ||
                currentRoute.startsWith("quran_reader") ||
                currentRoute.startsWith("quran_search")
        AppDestination.TAHSIN -> currentRoute.startsWith(AppDestination.TAHSIN.route) ||
                currentRoute.startsWith("tahsin_detail")
        AppDestination.CHAT -> currentRoute.startsWith(AppDestination.CHAT.route)
        AppDestination.BOOKMARKS -> currentRoute.startsWith(AppDestination.BOOKMARKS.route)
        AppDestination.SETTINGS -> currentRoute.startsWith(AppDestination.SETTINGS.route)
    }
}

@Composable
fun AdaptiveNavigationScaffold(
    currentRoute: String,
    widthSizeClass: WindowWidthSizeClass,
    onNavigateToDestination: (AppDestination) -> Unit,
    content: @Composable () -> Unit
) {
    val destinations = AppDestination.entries

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
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    content()
                }
            }
        }
        else -> {
            // Phone (Compact): Standard Bottom Navigation Bar
            val showBottomBar = !currentRoute.startsWith("quran_reader") && !currentRoute.startsWith("quran_search")

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 2.dp
                        ) {
                            destinations.forEach { dest ->
                                val selected = isDestinationSelected(currentRoute, dest)
                                NavigationBarItem(
                                    icon = { Icon(dest.icon, contentDescription = dest.title) },
                                    label = { Text(dest.title, style = MaterialTheme.typography.labelMedium) },
                                    selected = selected,
                                    onClick = { onNavigateToDestination(dest) },
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
