package app.betterout

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.betterout.ui.dive.SharkBite
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import app.betterout.ui.gear.LocalWeightUnit
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.data.ThemeMode
import app.betterout.ui.BetterOutApp
import app.betterout.ui.dive.DiveApp
import app.betterout.ui.home.LocalModeSwitch
import app.betterout.ui.home.ModeSwitch
import app.betterout.ui.theme.BetterOutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as BetterOutApplication).settings
        setContent {
            val mode by settings.theme.collectAsStateWithLifecycle()
            val dark = when (mode) { ThemeMode.SISTEMA -> isSystemInDarkTheme(); ThemeMode.CHIARO -> false; ThemeMode.NERO -> true }
            // Le icone della barra di stato seguono il tema scelto, non solo quello di sistema.
            LaunchedEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            val unit by settings.weightUnit.collectAsStateWithLifecycle()
            val diving by settings.diving.collectAsStateWithLifecycle()
            // Verso le immersioni: il morso dello squalo dura un secondo e il tema cambia quando le mascelle sono chiuse.
            val bite = remember { Animatable(0f) }
            var biting by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            val toggle: () -> Unit = {
                when {
                    biting -> Unit
                    diving || animationsOff() -> settings.setDiving(!diving)
                    else -> scope.launch {
                        biting = true
                        bite.snapTo(0f)
                        bite.animateTo(0.5f, tween(500, easing = LinearEasing))
                        settings.setDiving(true)
                        bite.animateTo(1f, tween(500, easing = LinearEasing))
                        biting = false
                    }
                }
            }
            BetterOutTheme(mode, diving) {
                CompositionLocalProvider(LocalWeightUnit provides unit, LocalModeSwitch provides ModeSwitch(diving, toggle)) {
                    Box(Modifier.fillMaxSize()) {
                        // Ogni modalità ha la sua navigazione: passando dall'una all'altra si riparte dalla schermata principale.
                        if (diving) DiveApp(application as BetterOutApplication) else BetterOutApp()
                        if (biting) SharkBite(bite.value)
                    }
                }
            }
        }
    }

    /** Le animazioni disattivate nelle impostazioni di Android (per esempio contro la chinetosi): niente morso. */
    private fun animationsOff(): Boolean = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}
