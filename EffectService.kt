package com.jarvis.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager

class EffectService : Service() {

    companion object {
        const val ACTION_STOP = "com.jarvis.app.STOP_EFFECT"

        fun show(ctx: Context, e: Effect) {
            ctx.startForegroundService(
                Intent(ctx, EffectService::class.java).putExtra("effect", e.name)
            )
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, EffectService::class.java))
        }
    }

    private var view: EffectView? = null
    private var foreground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        if (intent?.action == ACTION_STOP) {
            removeView()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val e = try {
            Effect.valueOf(intent?.getStringExtra("effect") ?: "")
        } catch (ex: Exception) {
            Effect.SNOW
        }
        showEffect(e)
        return START_NOT_STICKY
    }

    private fun ensureForeground() {
        if (foreground) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("effects", "Эффекты", NotificationManager.IMPORTANCE_LOW)
        )
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, EffectService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val action = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Выключить",
            stopPi
        ).build()
        val n = Notification.Builder(this, "effects")
            .setContentTitle("Эффект на экране")
            .setContentText("Скажи: Джарвис, выключи эффект")
            .setSmallIcon(android.R.drawable.star_on)
            .addAction(action)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        foreground = true
    }

    private fun showEffect(e: Effect) {
        removeView()
        val v = EffectView(this, e)
        // Слой полностью "прозрачен" для нажатий: экран под ним работает как обычно
        v.isClickable = false
        v.isFocusable = false
        v.isFocusableInTouchMode = false
        v.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(v, lp)
            view = v
        } catch (ex: Exception) {
            stopSelf()
        }
    }

    private fun removeView() {
        view?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (ex: Exception) {
            }
        }
        view = null
    }

    override fun onDestroy() {
        removeView()
        super.onDestroy()
    }
}
