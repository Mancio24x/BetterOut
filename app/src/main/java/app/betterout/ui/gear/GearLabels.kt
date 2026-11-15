package app.betterout.ui.gear

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import app.betterout.R
import app.betterout.domain.GearFunction
import app.betterout.domain.GearKind
import app.betterout.domain.WeightFormat
import app.betterout.ui.trips.currentLocale
import java.io.File

fun kindLabelRes(kind: GearKind): Int = when (kind) { GearKind.BASE -> R.string.tipo_base; GearKind.INDOSSATO -> R.string.tipo_indossato; GearKind.CONSUMABILE -> R.string.tipo_consumabile }
@Composable fun kindLabel(kind: GearKind): String = stringResource(kindLabelRes(kind))

@Composable
fun functionLabel(function: GearFunction): String {
    val context = LocalContext.current
    val id = context.resources.getIdentifier("uso_" + function.name.lowercase(), "string", context.packageName)
    return if (id != 0) stringResource(id) else function.name
}

@Composable fun weight(grams: Long): String = WeightFormat.format(grams, locale = currentLocale())

fun formatLiters(liters: Double, locale: java.util.Locale): String = if (liters % 1.0 == 0.0) String.format(locale, "%.0f", liters) else String.format(locale, "%.1f", liters)

/** Scrive il testo in un file temporaneo dell'app e apre il foglio di condivisione di Android. */
fun shareTextFile(context: Context, fileName: String, mime: String, content: String, chooserTitle: String, byteOrderMark: Boolean = true) {
    val dir = File(context.cacheDir, "esportazioni").apply { mkdirs() }
    val file = File(dir, fileName).apply { writeText(if (byteOrderMark) Char(0xFEFF) + content else content, Charsets.UTF_8) }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
