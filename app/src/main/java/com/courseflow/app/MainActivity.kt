package com.courseflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.courseflow.app.data.ScheduleRepository
import com.courseflow.app.ui.CourseFlowApp
import com.courseflow.app.ui.theme.CourseFlowTheme

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        com.courseflow.app.widget.ScheduleWidgetProvider.updateAll(applicationContext)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = ScheduleRepository(applicationContext)
        setContent {
            CourseFlowTheme {
                CourseFlowApp(repository)
            }
        }
    }
}
