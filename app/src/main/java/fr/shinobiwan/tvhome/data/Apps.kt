package fr.shinobiwan.tvhome.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TvApp(val packageName:String, val label:String, val icon:Bitmap?, val banner:Bitmap? = null)
class Apps(private val context:Context) {
    suspend fun installed():List<TvApp> = withContext(Dispatchers.IO) {
        val pm=context.packageManager
        val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).mapNotNull { info ->
            if(info.activityInfo.packageName==context.packageName) return@mapNotNull null
            val bitmap=try {
                val drawable=info.loadIcon(pm)
                Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888).also { drawable.setBounds(0,0,96,96); drawable.draw(Canvas(it)) }
            } catch (_:Exception){null}
            val banner = try {
                val drawable = info.activityInfo.loadBanner(pm)
                drawable?.let {
                    val width = it.intrinsicWidth.coerceAtLeast(1)
                    val height = it.intrinsicHeight.coerceAtLeast(1)
                    val scale = minOf(1f, 480f / width, 270f / height)
                    val out = Bitmap.createBitmap((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    it.setBounds(0, 0, out.width, out.height)
                    it.draw(Canvas(out))
                    out
                }
            } catch (_: Exception) { null }
            TvApp(info.activityInfo.packageName,info.loadLabel(pm).toString(),bitmap,banner)
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
    fun launch(packageName:String):Boolean=try {
        val intent=context.packageManager.getLeanbackLaunchIntentForPackage(packageName)
        if(intent==null) false else { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }
    } catch (_:Exception){false}
}
