package com.app.knowledgegraph.ui.navigation

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.rememberNavController

@Stable
class AppNavState(val navController: NavHostController) {

    var incomingFrom: IncomingFrom by mutableStateOf(IncomingFrom.NONE)
        private set

    private var lastNavTimeMs: Long = 0L

    fun navigate(
        route: String,
        direction: IncomingFrom,
        builder: NavOptionsBuilder.() -> Unit = {}
    ) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNavTimeMs < 300L) return
        lastNavTimeMs = now
        incomingFrom = direction
        navController.navigate(route) {
            builder()
            launchSingleTop = true
        }
    }

    fun navigateFromButton(
        route: String,
        rootRect: Rect?,
        buttonRect: Rect?,
        builder: NavOptionsBuilder.() -> Unit = {}
    ) {
        val direction = DirectionResolver.resolveFromButton(rootRect, buttonRect)
        navigate(route, direction, builder)
    }

    fun navigateTab(route: String, currentIndex: Int, targetIndex: Int) {
        if (currentIndex == targetIndex) return
        val direction = DirectionResolver.resolveFromTabIndex(currentIndex, targetIndex)
        val now = SystemClock.elapsedRealtime()
        if (now - lastNavTimeMs < 300L) return
        lastNavTimeMs = now
        incomingFrom = direction
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun popBackStack(direction: IncomingFrom = IncomingFrom.BOTTOM) {
        incomingFrom = direction
        navController.popBackStack()
    }
}

@Composable
fun rememberAppNavState(): AppNavState {
    val navController = rememberNavController()
    return remember(navController) { AppNavState(navController) }
}
