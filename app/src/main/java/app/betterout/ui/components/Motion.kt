package app.betterout.ui.components

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

// Movimento di Material 3: curve "emphasized" e durate brevi. Senza queste la navigazione userebbe
// la dissolvenza predefinita di 700 ms, che fa sembrare l'app lenta.
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/**
 * Transizioni della navigazione. Fra due schede principali ([tabs]) una dissolvenza rapida ("fade through");
 * verso una schermata di secondo livello uno scorrimento breve con dissolvenza ("shared axis" orizzontale),
 * al contrario tornando indietro.
 */
class NavMotion(private val tabs: Set<String>) {
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs() =
        initialState.destination.route in tabs && targetState.destination.route in tabs

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (betweenTabs()) fadeIn(tween(210, delayMillis = 90, easing = EmphasizedDecelerate)) + scaleIn(tween(210, delayMillis = 90, easing = EmphasizedDecelerate), initialScale = 0.96f)
        else slideInHorizontally(tween(350, easing = EmphasizedDecelerate)) { it / 8 } + fadeIn(tween(200, delayMillis = 50))
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (betweenTabs()) fadeOut(tween(90, easing = EmphasizedAccelerate))
        else slideOutHorizontally(tween(250, easing = EmphasizedAccelerate)) { -it / 12 } + fadeOut(tween(120))
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (betweenTabs()) fadeIn(tween(210, delayMillis = 90, easing = EmphasizedDecelerate))
        else slideInHorizontally(tween(350, easing = EmphasizedDecelerate)) { -it / 12 } + fadeIn(tween(200, delayMillis = 50))
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (betweenTabs()) fadeOut(tween(90, easing = EmphasizedAccelerate))
        else slideOutHorizontally(tween(250, easing = EmphasizedAccelerate)) { it / 8 } + fadeOut(tween(120))
    }
}
