package com.app.knowledgegraph.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

object AnimationDuration {
    const val clickFeedback = 100
    const val tabSwitch = 450
    const val bottomSheetHide = 200
    const val pageTransition = 500
    const val modalShow = 250
    const val bottomSheetShow = 320
    const val successAnimation = 400
    const val shimmer = 1500
}

object AnimationEasing {
    val easeOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val easeInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val easeIn = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val emphasizedDecelerate = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    val emphasizedAccelerate = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
    val emphasized = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
}

object AnimationSpec {
    val clickFeedback = tween<Float>(
        durationMillis = AnimationDuration.clickFeedback,
        easing = AnimationEasing.easeOut
    )
    val tabSwitch = tween<Float>(
        durationMillis = AnimationDuration.tabSwitch,
        easing = AnimationEasing.emphasizedDecelerate
    )
    val pageTransition = tween<Float>(
        durationMillis = AnimationDuration.pageTransition,
        easing = AnimationEasing.emphasizedDecelerate
    )
    val bottomSheetShow = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow
    )
    val modalShow = tween<Float>(
        durationMillis = AnimationDuration.modalShow,
        easing = AnimationEasing.easeOut
    )
}

object AnimationParams {
    const val buttonPressScale = 0.97f
    const val cardPressScale = 0.99f
    const val tabIconBounceScale = 1.15f
    const val pressedAlpha = 0.15f
    const val parallaxExitRatio = 0.30f
    const val exitScale = 0.95f
}
