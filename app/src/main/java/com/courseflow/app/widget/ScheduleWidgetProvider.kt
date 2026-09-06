package com.courseflow.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
            val today = LocalDate.now()
            val week = ScheduleWidgetContent.week(current, today)
            val weekText = when { week < 1 -> "未开学"; week > current.config.totalWeeks -> "已结课"; else -> "第${week}周" }
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            ids.forEach { id ->
                val height = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 170)
                val width = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                val lineLength = ((width / 2 - 28) / 13).coerceIn(6, 24)
                val capacity = ((height - 110) / 74).coerceIn(1, 6)
                val views = RemoteViews(context.packageName, R.layout.schedule_widget)
                views.setTextViewText(R.id.widget_title, current.config.name)
                views.setTextViewText(R.id.widget_date, "${today.monthValue}.${today.dayOfMonth}  $weekText  周${"一二三四五六日"[today.dayOfWeek.value - 1]}")
                views.setTextViewText(R.id.widget_today, ScheduleWidgetContent.day(current, today, "今天", capacity, lineLength, height < 190))
                views.setTextViewText(R.id.widget_tomorrow, ScheduleWidgetContent.day(current, today.plusDays(1), "明天", capacity, lineLength, height < 190))
                views.setTextViewTextSize(R.id.widget_today, android.util.TypedValue.COMPLEX_UNIT_SP, if (height < 190) 11f else 13f)
                views.setTextViewTextSize(R.id.widget_tomorrow, android.util.TypedValue.COMPLEX_UNIT_SP, if (height < 190) 11f else 13f)
                views.setOnClickPendingIntent(R.id.widget_root, open)
                manager.updateAppWidget(id, views)
            }
        }
    }
}
