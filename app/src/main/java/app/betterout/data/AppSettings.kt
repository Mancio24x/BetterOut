package app.betterout.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SISTEMA, CHIARO, NERO }

/** Preferenze dell'app, salvate sul telefono. Si leggono subito all'avvio, così il tema giusto c'è dal primo fotogramma. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("impostazioni", Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(runCatching { ThemeMode.valueOf(prefs.getString("tema", null).orEmpty()) }.getOrDefault(ThemeMode.SISTEMA))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString("tema", mode.name).apply()
        _theme.value = mode
    }
}
