package com.app.knowledgegraph.ui.navigation

import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

object DirectionResolver {

    fun resolveFromButton(rootRect: Rect?, sourceRect: Rect?): IncomingFrom {
        if (rootRect == null || sourceRect == null) return IncomingFrom.BOTTOM

        val containerCenter = rootRect.center
        val buttonCenter = sourceRect.center
        val dx = buttonCenter.x - containerCenter.x
        val dy = buttonCenter.y - containerCenter.y

        val deadZoneX = rootRect.width * 0.08f
        val deadZoneY = rootRect.height * 0.08f

        if (abs(dx) < deadZoneX && abs(dy) < deadZoneY) return IncomingFrom.BOTTOM

        return if (abs(dx) > abs(dy)) {
            if (dx > 0) IncomingFrom.RIGHT else IncomingFrom.LEFT
        } else {
            if (dy > 0) IncomingFrom.BOTTOM else IncomingFrom.TOP
        }
    }

    fun resolveFromTabIndex(currentIndex: Int, targetIndex: Int): IncomingFrom = when {
        targetIndex > currentIndex -> IncomingFrom.RIGHT
        targetIndex < currentIndex -> IncomingFrom.LEFT
        else -> IncomingFrom.NONE
    }
}
