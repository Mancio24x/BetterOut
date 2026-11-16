package app.betterout.data

import android.content.Context
import app.betterout.domain.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SISTEMA, CHIARO, NERO }

/** Preferenze dell'app, salvate sul telefono. Si leggono subito all'avvio, così il tema giusto c'è dal primo fotogramma. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("impostazioni", Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(runCatching { ThemeMode.valueOf(prefs.getString("tema", null).orEmpty()) }.getOrDefault(ThemeMode.SISTEMA))
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _weightUnit = MutableStateFlow(if (prefs.getString("peso", null) == WeightUnit.G.name) WeightUnit.G else WeightUnit.KG)
    /** Chilogrammi o grammi: vale per i pesi mostrati e per quelli inseriti. */
    val weightUnit: StateFlow<WeightUnit> = _weightUnit.asStateFlow()

    fun setWeightUnit(unit: WeightUnit) {
        prefs.edit().putString("peso", unit.name).apply()
        _weightUnit.value = unit
    }

    private val _diving = MutableStateFlow(prefs.getBoolean("immersioni", false))
    /** Modalità immersioni: tema blu e logbook al posto delle uscite. Si ricorda alla riapertura. */
    val diving: StateFlow<Boolean> = _diving.asStateFlow()

    fun setDiving(on: Boolean) {
        prefs.edit().putBoolean("immersioni", on).apply()
        _diving.value = on
    }

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString("tema", mode.name).apply()
        _theme.value = mode
    }
}
