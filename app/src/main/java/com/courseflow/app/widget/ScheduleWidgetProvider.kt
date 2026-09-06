package com.courseflow.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.courseflow.app.MainActivity
import com.courseflow.app.R
import com.courseflow.app.data.ScheduleRepository
import com.courseflow.app.model.ScheduleState
import java.time.LocalDate

class ScheduleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAll(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateAll(context)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_BOOT_COMPLETED)) updateAll(context)
    }
    companion object {
        fun updateAll(context: Context, state: ScheduleState? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ScheduleWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val current = state ?: ScheduleRepository(context).state.value
            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                manager.updateAppWidget(id, render(context, current, LocalDate.now(),
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320),
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 210)))
            }
        }

        internal fun render(context: Context, state: ScheduleState, date: LocalDate, width: Int, height: Int): RemoteViews {
            val week = ScheduleWidgetContent.week(state, date)
            val weekText = when { week < 1 -> "未开学"; week > state.config.totalWeeks -> "已结课"; else -> "第${week}周" }
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val courses = ScheduleWidgetContent.courses(state, date)
            val wide = width >= 320
            val scale = context.resources.configuration.fontScale.coerceAtLeast(1f)
            val rowHeight = (if (wide) 72 else 88) * scale
            val availableRows = ((height - 64 * scale) / rowHeight).toInt().coerceIn(1, 6)
            val rows = if (courses.size > availableRows * 2)
                ((height - 80 * scale) / rowHeight).toInt().coerceIn(1, 6) else availableRows
            val shown = courses.take(rows * 2)
            val split = (shown.size + 1) / 2
            val backgrounds = intArrayOf(R.drawable.widget_card_0, R.drawable.widget_card_1, R.drawable.widget_card_2, R.drawable.widget_card_3, R.drawable.widget_card_4, R.drawable.widget_card_5)
            val accents = intArrayOf(R.drawable.widget_accent_0, R.drawable.widget_accent_1, R.drawable.widget_accent_2, R.drawable.widget_accent_3, R.drawable.widget_accent_4, R.drawable.widget_accent_5)
            return RemoteViews(context.packageName, R.layout.schedule_widget).apply {
                setTextViewText(R.id.widget_date, "${date.monthValue}.${date.dayOfMonth} 周${"一二三四五六日"[date.dayOfWeek.value - 1]} · $weekText")
                setTextViewText(R.id.widget_count, "共${courses.size}门课")
                removeAllViews(R.id.widget_left)
                removeAllViews(R.id.widget_right)
                setViewVisibility(R.id.widget_grid, if (courses.isEmpty()) View.GONE else View.VISIBLE)
                setViewVisibility(R.id.widget_empty, if (courses.isEmpty()) View.VISIBLE else View.GONE)
                setTextViewText(R.id.widget_empty, ScheduleWidgetContent.emptyMessage(state, date))
                shown.forEachIndexed { index, course ->
                    val color = Math.floorMod(course.colorIndex, backgrounds.size)
                    val time = ScheduleWidgetContent.time(state, course)
                    val room = course.room.ifBlank { "教室待补充" }
                    val card = RemoteViews(context.packageName, if (wide) R.layout.widget_course_card_wide else R.layout.widget_course_card).apply {
                        setInt(R.id.course_card, "setBackgroundResource", backgrounds[color])
                        setInt(R.id.course_accent, "setBackgroundResource", accents[color])
                        setTextViewText(R.id.course_name, course.name)
                        setTextViewText(R.id.course_time, if (wide) "$time  $room" else time)
                        setTextViewText(R.id.course_room, room)
                        setViewVisibility(R.id.course_room, if (wide) View.GONE else View.VISIBLE)
                        setContentDescription(R.id.course_card, "${course.name}，$time，$room")
                        setOnClickPendingIntent(R.id.course_card, open)
                    }
                    addView(if (index < split) R.id.widget_left else R.id.widget_right, card)
                }
                setViewVisibility(R.id.widget_more, if (courses.size > shown.size) View.VISIBLE else View.GONE)
                setTextViewText(R.id.widget_more, "还有${courses.size - shown.size}门课 · 点击查看")
                setOnClickPendingIntent(R.id.widget_root, open)
            }
        }
    }
}
