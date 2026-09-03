package com.courseflow.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.courseflow.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class ReleaseInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String?,
)

sealed interface UpdateCheckResult {
    data class UpToDate(val latestVersion: String) : UpdateCheckResult
    data class Available(val release: ReleaseInfo) : UpdateCheckResult
}

enum class InstallLaunchResult { Launched, PermissionRequired }

class AppUpdateManager(context: Context) {
    private val appContext = context.applicationContext

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val json = requestJson(
            "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
        )
        val version = json.getString("tag_name").trim().removePrefix("v").removePrefix("V")
        val assets = json.getJSONArray("assets")
        val apkAsset = (0 until assets.length())
            .asSequence()
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?: throw IOException("最新版本没有可安装的 APK 文件")

        val digest = apkAsset.optString("digest")
            .takeIf { it.startsWith("sha256:", ignoreCase = true) }
            ?.substringAfter(':')
        val release = ReleaseInfo(
            versionName = version,
            releaseNotes = json.optString("body").trim(),
            downloadUrl = apkAsset.getString("browser_download_url"),
            fileName = apkAsset.getString("name"),
            sizeBytes = apkAsset.optLong("size"),
            sha256 = digest,
        )
        if (isVersionNewer(version, BuildConfig.VERSION_NAME)) {
            UpdateCheckResult.Available(release)
        } else {
            UpdateCheckResult.UpToDate(version)
        }
    }

    suspend fun downloadUpdate(release: ReleaseInfo, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw IOException("无法访问应用下载目录")
        directory.mkdirs()
        val finalFile = File(directory, "kexu-${release.versionName}.apk")
        val partFile = File(directory, "${finalFile.name}.part")

        val connection = openConnection(release.downloadUrl)
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) throw IOException("下载更新失败（HTTP $responseCode）")
            val total = connection.contentLength.toLong().takeIf { it > 0 } ?: release.sizeBytes
            var copied = 0L
            var lastProgress = -1
            partFile.outputStream().buffered().use { output ->
                connection.inputStream.buffered().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        if (total > 0) {
                            val progress = ((copied * 100) / total).toInt().coerceIn(0, 100)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }
            release.sha256?.let { expected ->
                val actual = partFile.sha256()
                if (!actual.equals(expected, ignoreCase = true)) {
                    partFile.delete()
                    throw IOException("安装包校验失败，请重新下载")
                }
            }
            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) throw IOException("无法保存更新安装包")
            onProgress(100)
            finalFile
        } finally {
            connection.disconnect()
        }
    }

    fun launchInstaller(apkFile: File): InstallLaunchResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !appContext.packageManager.canRequestPackageInstalls()) {
            appContext.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${appContext.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return InstallLaunchResult.PermissionRequired
        }
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile,
        )
        appContext.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return InstallLaunchResult.Launched
    }

    private fun requestJson(url: String): JSONObject {
        val connection = openConnection(url)
        try {
            return when (val responseCode = connection.responseCode) {
                in 200..299 -> JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                404 -> throw IOException("暂时没有可用的正式版本")
                else -> throw IOException("检查更新失败（HTTP $responseCode）")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "Kexu-Android/${BuildConfig.VERSION_NAME}")
    }
}

internal fun isVersionNewer(latest: String, current: String): Boolean {
    fun parts(value: String) = Regex("\\d+").findAll(value).map { it.value.toIntOrNull() ?: 0 }.toList()
    val left = parts(latest)
    val right = parts(current)
    repeat(maxOf(left.size, right.size)) { index ->
        val comparison = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
        if (comparison != 0) return comparison > 0
    }
    return false
}

private fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
