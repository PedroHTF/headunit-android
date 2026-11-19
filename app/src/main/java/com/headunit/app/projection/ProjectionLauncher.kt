package com.headunit.app.projection

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.headunit.app.settings.LaunchTarget
import com.headunit.app.settings.ProjectionMode
import com.headunit.app.settings.SettingsRepository

data class AppEntry(val label: String, val packageName: String)
data class ActivityEntry(val name: String, val label: String)

/**
 * Abre o app de projeção (Zlink ou outro) para Android Auto ou CarPlay.
 *
 * Sem configuração, procura sozinho um app com "zlink" no pacote, ou com nome
 * "Zlink"/"Car Auto"/"CarLink". Nas Configurações dá para escolher o app e,
 * se o app tiver telas separadas para cada modo, a tela exata.
 */
class ProjectionLauncher(private val context: Context, private val settings: SettingsRepository) {
    private val pm: PackageManager get() = context.packageManager

    /** Alvo efetivo: o escolhido pelo usuário ou o detectado. */
    fun resolve(mode: ProjectionMode): LaunchTarget? =
        settings.settings.value.target(mode)?.takeIf { isInstalled(it.packageName) } ?: detect()

    fun detect(): LaunchTarget? = launchableApps()
        .firstOrNull { app ->
            app.packageName.contains("zlink", ignoreCase = true) || DETECT_LABEL.containsMatchIn(app.label)
        }
        ?.let { LaunchTarget(it.packageName) }

    fun describe(mode: ProjectionMode): String {
        val chosen = settings.settings.value.target(mode)
        val target = resolve(mode) ?: return "Nenhum app encontrado"
        val label = appLabel(target.packageName)
        val screen = target.activity?.let { " · " + it.substringAfterLast('.') } ?: ""
        val origin = if (chosen != null && chosen.packageName == target.packageName) "" else " (detectado)"
        return "$label$screen$origin"
    }

    /** Abre o alvo do modo. Retorna false (e avisa na tela) se não conseguiu. */
    fun launch(mode: ProjectionMode): Boolean {
        val target = resolve(mode)
        if (target == null) {
            toast("${mode.label}: Zlink não encontrado. Escolha o app em Configurações.")
            return false
        }
        val intent = if (target.activity != null) {
            Intent(Intent.ACTION_MAIN).setClassName(target.packageName, target.activity)
        } else {
            pm.getLaunchIntentForPackage(target.packageName)
        }
        if (intent == null) {
            toast("${mode.label}: não foi possível abrir ${target.packageName}")
            return false
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }
            .onFailure { toast("${mode.label}: falha ao abrir (${it.javaClass.simpleName})") }
            .isSuccess
    }

    fun launchableApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** Telas (activities) que outros apps podem abrir dentro do pacote. */
    fun exportedActivities(packageName: String): List<ActivityEntry> {
        val info = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            }
        }.getOrNull() ?: return emptyList()
        return info.activities.orEmpty()
            .filter(ActivityInfo::exported)
            .map { ActivityEntry(it.name, it.loadLabel(pm).toString()) }
            .sortedBy { it.name }
    }

    fun appLabel(packageName: String): String = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    private fun isInstalled(packageName: String) = runCatching { pm.getApplicationInfo(packageName, 0) }.isSuccess

    private fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    private companion object {
        val DETECT_LABEL = Regex("zlink|car ?auto|carlink", RegexOption.IGNORE_CASE)
    }
}
