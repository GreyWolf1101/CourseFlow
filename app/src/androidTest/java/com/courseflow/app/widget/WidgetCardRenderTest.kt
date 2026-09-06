package com.courseflow.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.courseflow.app.R
import com.courseflow.app.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class WidgetCardRenderTest {
    private val date = LocalDate.of(2026, 9, 7)
    private val state = ScheduleState(SemesterConfig(startDate = date.toString()), listOf(
        CourseSession(name="大学物理", room="电教202", dayOfWeek=1, startPeriod=1, periodSpan=2, colorIndex=0),
        CourseSession(name="概率与数学统计", room="主教404", dayOfWeek=1, startPeriod=3, periodSpan=2, colorIndex=1),
        CourseSession(name="军事理论", room="电教501", dayOfWeek=1, startPeriod=5, periodSpan=2, colorIndex=2),
        CourseSession(name="中国近代史纲要", room="厚德楼B301", dayOfWeek=1, startPeriod=7, periodSpan=2, colorIndex=3)))

    @Test fun cardsRenderAndResizeAndClear() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            fun render(width: Int, height: Int, value: ScheduleState = state): View {
                return ScheduleWidgetProvider.render(context, value, date, width, height)
                    .apply(context, FrameLayout(context))
            }
            fun save(view: View, width: Int, height: Int, name: String) {
                val density = context.resources.displayMetrics.density
                val w = (width*density).toInt()
                val h = (height*density).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                view.layout(0,0,w,h)
                fun checkText(node: View) {
                    if (node.visibility != View.VISIBLE) return
                    if (node is TextView && node.layout != null) {
                        assertTrue("Clipped text: ${node.text}, line=${node.layout.getLineBottom(node.lineCount - 1)}, height=${node.height}, padding=${node.compoundPaddingTop}+${node.compoundPaddingBottom}", node.layout.getLineBottom(node.lineCount - 1) <= node.height - node.compoundPaddingTop - node.compoundPaddingBottom)
                    }
                    if (node is android.view.ViewGroup) {
                        for (index in 0 until node.childCount) checkText(node.getChildAt(index))
                    }
                }
                checkText(view)
                val bitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                File(context.cacheDir,name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                bitmap.recycle()
            }
            val wide = render(400,210)
            val left = wide.findViewById<LinearLayout>(R.id.widget_left)
            assertEquals(2,left.childCount)
            assertEquals(2,wide.findViewById<LinearLayout>(R.id.widget_right).childCount)
            assertEquals("大学物理",left.getChildAt(0).findViewById<TextView>(R.id.course_name).text.toString())
            assertEquals("08:00–09:40  电教202",left.getChildAt(0).findViewById<TextView>(R.id.course_time).text.toString())
            save(wide,400,210,"widget-cards-preview.png")
            val narrow = render(280,250)
            assertEquals(View.VISIBLE,narrow.findViewById<View>(R.id.course_room).visibility)
            save(narrow,280,250,"widget-cards-narrow.png")
            val small = render(320,140)
            assertEquals("还有2门课 · 点击查看",small.findViewById<TextView>(R.id.widget_more).text.toString())
            ScheduleWidgetProvider.render(context,state.copy(courses=emptyList()),date,400,210).reapply(context,wide)
            assertEquals(0,wide.findViewById<LinearLayout>(R.id.widget_left).childCount)
            assertEquals(View.VISIBLE,wide.findViewById<View>(R.id.widget_empty).visibility)
        }
    }
}
