package ai.mayra.app.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.util.Locale

/**
 * Resolves a spoken app name to a launchable Android package.
 *
 * Resolution prefers an exact package name, then an exact application label, and finally a
 * partial label match. Only launchable activities are returned.
 */
class AndroidAppResolver(
    context: Context
) {
    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager

    fun resolve(packageOrName: String): ResolvedApp? {
        val query = packageOrName.trim()
        if (query.isEmpty()) return null

        packageManager.getLaunchIntentForPackage(query)?.let { launchIntent ->
            return ResolvedApp(
                packageName = query,
                label = applicationLabel(query) ?: query,
                launchIntent = launchIntent
            )
        }

        val normalizedQuery = query.lowercase(Locale.getDefault())
        val candidates = launchableApplications()

        return candidates.firstOrNull {
            it.label.lowercase(Locale.getDefault()) == normalizedQuery
        } ?: candidates.firstOrNull {
            it.label.lowercase(Locale.getDefault()).contains(normalizedQuery)
        }
    }

    private fun launchableApplications(): List<ResolvedApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim()
                    .orEmpty()
                    .ifEmpty { packageName }

                ResolvedApp(
                    packageName = packageName,
                    label = label,
                    launchIntent = launchIntent
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }

    private fun applicationLabel(packageName: String): String? = runCatching {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(applicationInfo).toString()
    }.getOrNull()
}

data class ResolvedApp(
    val packageName: String,
    val label: String,
    val launchIntent: Intent
)
