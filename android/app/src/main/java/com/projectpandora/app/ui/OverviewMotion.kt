package com.projectpandora.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin

/** Native Compose motion follows the system animator duration scale, including disabled animations. */
fun overviewTransition(origin: TransformOrigin): ContentTransform =
    (fadeIn(tween(480,easing=FastOutSlowInEasing)) + scaleIn(initialScale=0.60f,transformOrigin=origin,animationSpec=tween(560,easing=FastOutSlowInEasing))) togetherWith
        (fadeOut(tween(180)) + scaleOut(targetScale=0.97f,animationSpec=tween(280)))
