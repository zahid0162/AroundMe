package com.zahid.aroundme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zahid.aroundme.core.designsystem.AroundMeTheme
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import com.zahid.aroundme.feature.details.PlaceDetailsScreen
import com.zahid.aroundme.feature.explore.ExploreScreen
import com.zahid.aroundme.feature.profile.ProfileScreen
import com.zahid.aroundme.feature.saved.SavedScreen
import com.zahid.aroundme.feature.search.SearchScreen
import com.zahid.aroundme.navigation.AroundMeDestination
import org.koin.compose.koinInject

private data class TopLevelTab(
    val route: AroundMeDestination,
    val labelEn: String,
    val labelAr: String,
    val icon: ImageVector
)

private val topLevelTabs = listOf(
    TopLevelTab(AroundMeDestination.Explore, "Explore", "استكشف", Icons.Rounded.Explore),
    TopLevelTab(AroundMeDestination.Search, "Search", "بحث", Icons.Rounded.Search),
    TopLevelTab(AroundMeDestination.Saved, "Saved", "المحفوظات", Icons.Rounded.Bookmark),
    TopLevelTab(AroundMeDestination.Profile, "Profile", "الملف", Icons.Rounded.Person)
)

@Composable
fun AroundMeApp() {
    val prefsRepo: UserPreferencesRepository = koinInject()
    val prefs by prefsRepo.preferences.collectAsStateWithLifecycle()
    val isArabic = prefs.language == AppLanguage.ARABIC
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AroundMeTheme(themePreference = prefs.themePreference) {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination

            val showBottomBar = topLevelTabs.any { tab ->
                currentDestination?.hasRoute(tab.route::class) == true
            }

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            topLevelTabs.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = null) },
                                    label = { Text(if (isArabic) tab.labelAr else tab.labelEn) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                    NavHost(
                        navController = navController,
                        startDestination = AroundMeDestination.Explore
                    ) {
                        composable<AroundMeDestination.Explore> {
                            ExploreScreen(
                                onOpenSearch = {
                                    navController.navigate(AroundMeDestination.Search) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onOpenPlaceDetails = { placeId ->
                                    navController.navigate(AroundMeDestination.PlaceDetails(placeId))
                                }
                            )
                        }
                        composable<AroundMeDestination.Search> {
                            SearchScreen(
                                onOpenPlaceDetails = { placeId, sessionToken ->
                                    navController.navigate(
                                        AroundMeDestination.PlaceDetails(
                                            placeId = placeId,
                                            sessionToken = sessionToken
                                        )
                                    )
                                }
                            )
                        }
                        composable<AroundMeDestination.Saved> {
                            SavedScreen(
                                onOpenPlaceDetails = { placeId ->
                                    navController.navigate(AroundMeDestination.PlaceDetails(placeId))
                                }
                            )
                        }
                        composable<AroundMeDestination.Profile> {
                            ProfileScreen()
                        }
                        composable<AroundMeDestination.PlaceDetails> { entry ->
                            val route = entry.toRoute<AroundMeDestination.PlaceDetails>()
                            PlaceDetailsScreen(
                                placeId = route.placeId,
                                sessionToken = route.sessionToken,
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}
