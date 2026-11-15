package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ScreenTimeTrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WARNING = "com.example.action.SESSION_WARNING"
        const val ACTION_EXIT = "com.example.action.SESSION_EXIT"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        if (sessionId <= 0L) return

        val pendingResult = goAsync()
        val coordinator = (context.applicationContext as ScreenTimeTrackerApp).container.sessionCoordinator

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_WARNING -> coordinator.handleWarning(sessionId)
                    ACTION_EXIT -> coordinator.handleSessionExpired(sessionId, shouldSendHome = true)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
