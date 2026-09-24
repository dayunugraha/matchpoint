package com.matchpoint.app.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.matchpoint.app.R

/**
 * Shown for a fixed minimum duration right after the native system launch screen hands off to
 * the app — that handoff happens the instant the first Compose frame is ready, which for this
 * app is near-instant and would otherwise skip straight to Home. This re-displays the same mark
 * (the animated logo over the app's background) so the brand moment reads as intentional rather
 * than a flash. Ported 1:1 from iOS's SplashScreenView (static SplashLogo + Lottie animation).
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("splash_logo_animation.json"))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.splash_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(maxWidth * 0.62f)
        )
    }
}
