package com.courseflow.app.importer

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import java.security.SecureRandom
import java.util.Base64

/** Public share retrieval only. No course upload, account login or device data collection. */
class WakeUpShareClient(
    private val post: (String, String, Map<String, String>) -> JSONObject = ::postWakeUpForm,
) {
    fun fetch(code: String): String {
        require(Regex("[a-fA-F0-9]{32}").matches(code)) { "WakeUp 口令格式错误" }
        val cuid = WakeUpProtocol.md5("com.baidu0000000000000000").uppercase() + "|0"
        val prefix = WakeUpProtocol.md5("alpha.beta0000000000000000")
        val adid = prefix + prefix.chunked(8).map { it.toLong(16) }.reduce { a, b -> a xor b }.toString(16).padStart(8, '0')
        val common = listOf("area" to "", "screensize" to "1080x2400", "cuid" to cuid, "os" to "android", "city" to "",
            "abis" to "arm64-v8a", "channel" to "100271a", "appBit" to "64", "vc" to "530", "deviceId" to "",
            "token" to "1_XPXQH3c5HRPtFHkSwi3sCCURmT25QfxM", "adid" to adid, "province" to "",
            "pkgName" to "com.suda.yzune.wakeupschedule", "appId" to "wakeup", "download_type" to "1",
            "vcname" to "6.4.0", "sdk" to "35", "device" to "Pixel 7", "brand" to "google", "operatorid" to "")
        val headers = mapOf("na__zyb_source__" to "wakeup", "zyb-cuid" to cuid, "zyb-adid" to adid)
        val random = SecureRandom()
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val nonce = (1..10).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        val signA = WakeUpProtocol.hexEncode(WakeUpProtocol.encrypt("8&%d*##$nonce##318c6d4f74655d4f032fb0466bcfdfbc##$cuid", "@fG2SuLA"))
        val handshake = post("/pluto/app/antispam", form(listOf("data" to signA) + common) + "&", headers)
        val signB = responseData(handshake).ifBlank { handshake.optJSONObject("result")?.optString("data").orEmpty() }
        require(signB.isNotBlank()) { "WakeUp 服务校验未通过（错误码 ${handshake.optInt("errNo", -1)}），尚未读取课表；请稍后重试或导入 HTML 文件" }
        val plain = WakeUpProtocol.decrypt(WakeUpProtocol.hexDecode(signB), nonce.take(5) + "#G4")
        require(plain.size >= 22 && String(plain.copyOfRange(0, 10), Charsets.ISO_8859_1) == nonce) { "WakeUp 分享服务响应校验失败" }
        val token = String(plain.copyOfRange(12, 22), Charsets.ISO_8859_1)
        val key = WakeUpProtocol.key(token, "530")
        val data = Base64.getEncoder().encodeToString(WakeUpProtocol.rc4("key=${encode(code)}".toByteArray(), key))
        val timing = listOf("nt" to "wifi", "_t_" to (System.currentTimeMillis() / 1000).toString(), "kakorrhaphiophobia" to (System.nanoTime() / 1_000_000).toString())
        val params = listOf("data" to data) + common + timing
        val toSign = Base64.getEncoder().encodeToString(params.map { "${it.first}=${it.second}" }.sorted().joinToString("").toByteArray())
        val sign = WakeUpProtocol.md5("8&%d*[${WakeUpProtocol.md5(token)}]@$toSign")
        val response = post("/share_schedule/getv2", "&" + form(params + ("sign" to sign)), headers)
        require(response.optInt("errNo", -1) == 0) { "WakeUp 拒绝读取课表（错误码 ${response.optInt("errNo", -1)}）；请确认口令仍在30分钟有效期内，或稍后重试" }
        val encrypted = responseData(response)
        require(encrypted.isNotBlank()) { "WakeUp 没有返回课表内容，请重新分享" }
        val decoded = try {
            JSONObject(String(WakeUpProtocol.rc4(Base64.getDecoder().decode(encrypted), key), Charsets.UTF_8))
        } catch (error: Exception) {
            throw IllegalStateException("WakeUp 已返回内容，但分享响应解码失败，可能是服务协议发生变化", error)
        }
        return decoded.optString("shareData").also { require(it.isNotBlank()) { "WakeUp 口令已失效或课表为空" } }
    }

    private fun responseData(json: JSONObject): String = when (val data = json.opt("data")) {
        is JSONObject -> data.optString("data")
        is String -> data
        else -> ""
    }
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun form(params: List<Pair<String, String>>) = params.joinToString("&") { "${it.first}=${encode(it.second)}" }
}

private fun postWakeUpForm(path: String, body: String, headers: Map<String, String>): JSONObject {
    val stage = if (path.endsWith("antispam")) "服务校验" else "读取课表"
    val connection = URL("https://api.wakeup.fun$path").openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = false
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        connection.setRequestProperty("User-Agent", "okhttp/4.12.0")
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        require(connection.responseCode == 200) { "WakeUp 服务返回 HTTP ${connection.responseCode}，请稍后重试" }
        val text = connection.inputStream.bufferedReader().use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(8192)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                require(result.length + count <= 2 * 1024 * 1024) { "WakeUp 响应过大" }
                result.append(buffer, 0, count)
            }
            result.toString()
        }
        return try { JSONObject(text) } catch (error: org.json.JSONException) {
            throw IllegalStateException("WakeUp $stage 返回了无法识别的响应，请稍后重试", error)
        }
    } catch (error: java.io.IOException) {
        val reason = when (error) {
            is SocketTimeoutException -> "连接或读取超时"
            is UnknownHostException -> "无法解析服务地址"
            is SSLException -> "安全连接失败"
            else -> "网络连接失败"
        }
        throw IllegalStateException("WakeUp $stage：$reason，尚不能判断口令是否过期。请检查网络或切换网络后重试", error)
    } finally {
        connection.disconnect()
    }
}
