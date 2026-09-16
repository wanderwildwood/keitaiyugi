package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.library.MetaSystemID

fun NavGraphBuilder.composable(
    route: MainRoute,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    this.composable(route = route.route, arguments = route.arguments, content = content)
}

fun NavController.navigateToRoute(route: MainRoute) {
    this.navigate(route.route)
}

enum class MainRoute(
    val route: String,
    @StringRes val titleId: Int,
    val parent: MainRoute? = null,
    val arguments: List<NamedNavArgument> = emptyList(),
    val showBottomNavigation: Boolean = true,
) {
    HOME(
        // The bar above the first screen says the app's name, not "Home". The bottom tab
        // still says Home -- it is one of four and needs to say which of the four it is,
        // where the header is the only place the app gets to introduce itself. They used
        // to share a single string, which is why the header said Home.
        route = "home",
        titleId = R.string.lemuroid_name,
    ),
    FAVORITES(
        route = "favorites",
        titleId = R.string.favorites,
    ),
    SEARCH(
        route = "search",
        titleId = R.string.title_search,
    ),
    SYSTEMS(
        route = "systems/home",
        titleId = R.string.title_systems,
    ),
    SYSTEM_GAMES(
        route = "systems/{metaSystemId}",
        titleId = R.string.title_games,
        // No parent, deliberately. This is a top-level destination here: the bottom bar
        // opens it directly, so a back arrow pointing at the systems chooser would offer
        // a way into the one screen this app has decided not to have.
        arguments = listOf(navArgument("metaSystemId") { type = NavType.StringType }),
    ),
    SETTINGS(
        route = "settings/home",
        titleId = R.string.title_settings,
        showBottomNavigation = false,
    ),
    SETTINGS_ADVANCED(
        route = "settings/advanced",
        titleId = R.string.settings_title_advanced_settings,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    SETTINGS_BIOS(
        route = "settings/bios",
        titleId = R.string.settings_title_display_bios_info,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    SETTINGS_CORES_SELECTION(
        route = "settings/cores",
        titleId = R.string.settings_title_open_cores_selection,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    SETTINGS_INPUT_DEVICES(
        route = "settings/inputdevices",
        titleId = R.string.settings_title_gamepad_settings,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    SETTINGS_SAVE_SYNC(
        route = "settings/savesync",
        titleId = R.string.settings_title_save_sync,
        parent = SETTINGS,
        showBottomNavigation = false,
    ),
    ;

    val root = root()

    private fun root(): MainRoute {
        return parent?.root() ?: this
    }

    companion object {
        fun findByRoute(route: String): MainRoute {
            return values().first { it.route == route }
        }
    }
}

enum class MainNavigationRoutes(
    val route: MainRoute,
    @StringRes val titleId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    /** Where the tab goes, when that is not simply [route]. */
    val navigationRoute: String? = null,
) {
    HOME(MainRoute.HOME, R.string.title_home, Icons.Outlined.Home, Icons.Outlined.Home),
    FAVORITES(MainRoute.FAVORITES, R.string.favorites, Icons.Outlined.Favorite, Icons.Outlined.FavoriteBorder),

    /**
     * Upstream's tab here opens a chooser of systems. This app has one system and always
     * will, so that screen is a list of length one standing between you and the games --
     * a tap that can only ever have one answer. The tab goes straight to the games.
     *
     * The chooser screen itself is left in place, unrouted: it costs nothing, keeps the
     * diff against upstream small, and is correct again the day a second core is added --
     * though whoever does that will need to give SYSTEM_GAMES its parent back, or arrive
     * at a game list with no way up.
     */
    GAMES(
        MainRoute.SYSTEM_GAMES,
        R.string.title_games,
        Icons.Outlined.VideogameAsset,
        Icons.Outlined.VideogameAsset,
        navigationRoute = "systems/" + MetaSystemID.GB.name,
    ),
    SEARCH(MainRoute.SEARCH, R.string.title_search, Icons.Outlined.Search, Icons.Outlined.Search),
}
