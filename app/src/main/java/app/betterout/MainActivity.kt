package app.betterout

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.data.ThemeMode
import app.betterout.ui.BetterOutApp
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
            BetterOutTheme(mode) {
                BetterOutApp()
            }
        }
    }
}
