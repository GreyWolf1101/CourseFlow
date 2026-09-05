package com.courseflow.app.importer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect

/** Detect ruled seven-day tables before OCR, preventing recognition across column borders. */
internal object PdfTableCells {
    fun find(bitmap: Bitmap): List<Pair<Int, Rect>> {
        fun dark(x: Int, y: Int): Boolean {
            val color = bitmap.getPixel(x, y)
            return Color.red(color) < 130 && Color.green(color) < 130 && Color.blue(color) < 130
        }
        fun centers(values: List<Int>): List<Int> {
            val groups = mutableListOf<MutableList<Int>>()
            values.forEach { value ->
                if (groups.isEmpty() || value - groups.last().last() > 4) groups += mutableListOf(value)
                else groups.last() += value
            }
            return groups.map { it.average().toInt() }
        }
        val xs = centers((0 until bitmap.width).filter { x ->
            (0 until bitmap.height step 3).count { y -> dark(x, y) } > bitmap.height / 3 * .45
        }).takeLast(8)
        if (xs.size != 8) return emptyList()
        val widths = xs.zipWithNext { a, b -> b - a }
        if (widths.min() < bitmap.width / 15 || widths.max() > widths.min() * 1.15) return emptyList()
        return xs.zipWithNext().flatMapIndexed { day, (left, right) ->
            val sample = (left + 8 until right - 8 step 3)
            val ys = centers((0 until bitmap.height).filter { y ->
                sample.count { x -> dark(x, y) } > sample.count() * .88
            })
            ys.zipWithNext().mapNotNull { (top, bottom) ->
                if (bottom - top < 25) return@mapNotNull null
                val rect = Rect(left + 5, top + 5, right - 5, bottom - 5)
                val ink = (rect.top until rect.bottom step 4).sumOf { y ->
                    (rect.left until rect.right step 4).count { x -> dark(x, y) }
                }
                if (ink < 20) null else (day + 1) to rect
            }
        }
    }
}
