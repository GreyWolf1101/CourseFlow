// Kotlin port of WakeUpDecoder by airline233 (Apache-2.0).
// Modified for CourseFlow: typed arrays, bounds checks and isolated protocol operations.
// Source: https://github.com/airline233/WakeUpDecoder/blob/master/wakeup_share_sim.py
// License: app/src/main/assets/licenses/WakeUpDecoder-LICENSE.txt
package com.courseflow.app.importer

import java.security.MessageDigest

internal object WakeUpProtocol {
    private val IP = intArrayOf(57, 49, 41, 33, 25, 17, 9, 1, 59, 51, 43, 35, 27, 19, 11, 3, 61, 53, 45, 37, 29, 21, 13, 5, 63, 55, 47, 39, 31, 23, 15, 7, 56, 48, 40, 32, 24, 16, 8, 0, 58, 50, 42, 34, 26, 18, 10, 2, 60, 52, 44, 36, 28, 20, 12, 4, 62, 54, 46, 38, 30, 22, 14, 6)
    private val FP = intArrayOf(39, 7, 47, 15, 55, 23, 63, 31, 38, 6, 46, 14, 54, 22, 62, 30, 37, 5, 45, 13, 53, 21, 61, 29, 36, 4, 44, 12, 52, 20, 60, 28, 35, 3, 43, 11, 51, 19, 59, 27, 34, 2, 42, 10, 50, 18, 58, 26, 33, 1, 41, 9, 49, 17, 57, 25, 32, 0, 40, 8, 48, 16, 56, 24)
    private val E = intArrayOf(31, 0, 1, 2, 3, 4, 3, 4, 5, 6, 7, 8, 7, 8, 9, 10, 11, 12, 11, 12, 13, 14, 15, 16, 15, 16, 17, 18, 19, 20, 19, 20, 21, 22, 23, 24, 23, 24, 25, 26, 27, 28, 27, 28, 29, 30, 31, 0)
    private val P = intArrayOf(15, 6, 19, 20, 28, 11, 27, 16, 0, 14, 22, 25, 4, 17, 30, 9, 1, 7, 23, 13, 31, 26, 2, 8, 18, 12, 29, 5, 21, 10, 3, 24)
    private val PC1 = intArrayOf(56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3)
    private val PC2 = intArrayOf(13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9, 22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1, 40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 46, 43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31)
    private val SHIFTS = intArrayOf(1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)
    private val SBOX = arrayOf(
        arrayOf(intArrayOf(14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7), intArrayOf(0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8), intArrayOf(4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0), intArrayOf(15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13)),
        arrayOf(intArrayOf(15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10), intArrayOf(3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5), intArrayOf(0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15), intArrayOf(13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9)),
        arrayOf(intArrayOf(10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8), intArrayOf(13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1), intArrayOf(13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7), intArrayOf(1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12)),
        arrayOf(intArrayOf(7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15), intArrayOf(13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9), intArrayOf(10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4), intArrayOf(3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14)),
        arrayOf(intArrayOf(2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9), intArrayOf(14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6), intArrayOf(4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14), intArrayOf(11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3)),
        arrayOf(intArrayOf(12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11), intArrayOf(10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8), intArrayOf(9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6), intArrayOf(4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13)),
        arrayOf(intArrayOf(4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1), intArrayOf(13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6), intArrayOf(1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2), intArrayOf(6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12)),
        arrayOf(intArrayOf(13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7), intArrayOf(1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2), intArrayOf(7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8), intArrayOf(2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11))
    )

    fun md5(value: String): String = MessageDigest.getInstance("MD5").digest(value.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun bits(bytes: ByteArray) = bytes.flatMap { byte -> (0..7).map { (byte.toInt() ushr it) and 1 } }.toIntArray()
    private fun permute(bits: IntArray, table: IntArray) = IntArray(table.size) { bits[table[it]] }
    private fun rotate(bits: IntArray, n: Int) = IntArray(bits.size) { bits[(it + n) % bits.size] }
    private fun subkeys(key: String): List<IntArray> {
        require(key.toByteArray().size == 8)
        val bits = permute(bits(key.toByteArray()), PC1)
        var left = bits.copyOfRange(0, 28)
        var right = bits.copyOfRange(28, 56)
        return SHIFTS.map { shift ->
            left = rotate(left, shift); right = rotate(right, shift)
            permute(left + right, PC2)
        }
    }
    private fun block(bytes: ByteArray, keys: List<IntArray>): ByteArray {
        val bits = permute(bits(bytes), IP)
        var left = bits.copyOfRange(0, 32)
        var right = bits.copyOfRange(32, 64)
        keys.forEach { key ->
            val expanded = permute(right, E)
            val mixed = IntArray(48) { expanded[it] xor key[it] }
            val substituted = IntArray(32)
            repeat(8) { box ->
                val offset = box * 6
                val row = mixed[offset] * 2 + mixed[offset + 5]
                val column = mixed[offset + 1] * 8 + mixed[offset + 2] * 4 + mixed[offset + 3] * 2 + mixed[offset + 4]
                val value = SBOX[box][row][column]
                repeat(4) { substituted[box * 4 + it] = (value ushr (3 - it)) and 1 }
            }
            val permuted = permute(substituted, P)
            val next = IntArray(32) { left[it] xor permuted[it] }
            left = right; right = next
        }
        val result = permute(right + left, FP)
        return ByteArray(8) { byte -> (0..7).sumOf { result[byte * 8 + it] shl it }.toByte() }
    }
    fun encrypt(text: String, key: String): ByteArray {
        val bytes = text.toByteArray()
        val padding = 8 - bytes.size % 8
        val padded = bytes.copyOf(bytes.size + padding)
        padded[padded.lastIndex] = padding.toByte()
        return crypt(padded, subkeys(key))
    }
    private fun crypt(bytes: ByteArray, keys: List<IntArray>): ByteArray {
        require(bytes.isNotEmpty() && bytes.size % 8 == 0)
        val result = ByteArray(bytes.size)
        for (offset in bytes.indices step 8) block(bytes.copyOfRange(offset, offset + 8), keys).copyInto(result, offset)
        return result
    }
    fun decrypt(bytes: ByteArray, key: String): ByteArray {
        val result = crypt(bytes, subkeys(key).reversed())
        val padding = result.last().toInt()
        require(padding in 1..8) { "WakeUp 响应解密失败" }
        return result.copyOf(result.size - padding)
    }
    private fun rev4(n: Int) = ((n and 1) shl 3) or ((n and 2) shl 1) or ((n and 4) ushr 1) or ((n and 8) ushr 3)
    fun hexEncode(bytes: ByteArray): String = bytes.joinToString("") {
        "%02x%02x".format(rev4(it.toInt() and 15), rev4((it.toInt() ushr 4) and 15))
    }
    fun hexDecode(value: String): ByteArray {
        val clean = value.trim()
        require(clean.isNotEmpty() && clean.length % 4 == 0 && clean.length <= 8192 && clean.all { it in "0123456789abcdefABCDEF" })
        return ByteArray(clean.length / 4) { index ->
            (rev4(clean[index * 4 + 1].digitToInt(16)) or (rev4(clean[index * 4 + 3].digitToInt(16)) shl 4)).toByte()
        }
    }
    fun key(token: String, version: String): String {
        val third = md5("[$token]@")
        val mixed = (md5("@#AIjd83#@6B") + md5(version) + third.substring(17).reversed() + third.substring(15, 17) + third.substring(0, 15).reversed()).toCharArray()
        fun swap(chars: CharArray, count: Int) { repeat(count) { val old = chars[it]; chars[it] = chars[chars.lastIndex - it]; chars[chars.lastIndex - it] = old } }
        swap(mixed, 3)
        val result = (String(mixed) + md5(String(mixed))).toCharArray()
        swap(result, 60)
        return String(result)
    }
    fun rc4(bytes: ByteArray, key: String): ByteArray {
        val keyBytes = key.toByteArray()
        require(keyBytes.isNotEmpty())
        val state = IntArray(256) { it }
        var j = 0
        fun swap(a: Int, b: Int) { val old = state[a]; state[a] = state[b]; state[b] = old }
        repeat(256) { i -> j = (j + state[i] + (keyBytes[i % keyBytes.size].toInt() and 255)) and 255; swap(i, j) }
        var i = 0; j = 0
        return ByteArray(bytes.size) { index ->
            i = (i + 1) and 255; j = (j + state[i]) and 255; swap(i, j)
            (bytes[index].toInt() xor state[(state[i] + state[j]) and 255]).toByte()
        }
    }
}
