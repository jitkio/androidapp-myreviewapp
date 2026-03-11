package com.app.knowledgegraph.ui.navigation

import android.os.SystemClock
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class AppNavState(
    val navController: NavHostController,
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {

    var incomingFrom: IncomingFrom by mutableStateOf(IncomingFrom.NONE)
        private set

    private var lastNavTimeMs: Long = 0L

    /** 跳转到详情页 */
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

    /** 点击底部 Tab 时调用，通过 Pager 动画滑动过去 */
    fun navigateTab(targetIndex: Int) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(targetIndex)
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
    val pagerState = rememberPagerState(pageCount = { Screen.bottomTabs.size })
    val coroutineScope = rememberCoroutineScope()
    return remember(navController, pagerState, coroutineScope) {
        AppNavState(navController, pagerState, coroutineScope)
    }
}
