package app.betterout

import android.graphics.Color
import android.os.Bundle
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
            BetterOutTheme(mode, diving) {
                CompositionLocalProvider(LocalWeightUnit provides unit, LocalModeSwitch provides ModeSwitch(diving) { settings.setDiving(!diving) }) {
                    // Ogni modalità ha la sua navigazione: passando dall'una all'altra si riparte dalla schermata principale.
                    if (diving) DiveApp(application as BetterOutApplication) else BetterOutApp()
                }
            }
        }
    }
}
