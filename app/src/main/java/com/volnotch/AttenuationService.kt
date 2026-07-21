package com.volnotch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.audiofx.DynamicsProcessing
import android.os.IBinder

/**
 * 全体減衰（[DynamicsProcessing] をグローバル出力 session 0 へ適用）を保持する前面 Service。
 *
 * エフェクトを Activity ではなく常駐サービスが持つことで、他アプリ使用中や
 * VolNotch を閉じた後も減衰が維持される（プロセスが生き続けるため）。
 *
 * 通知の「解除」など Activity 外から状態が変わったときは、[SharedPreferences] を
 * 更新し [ACTION_STATE_CHANGED] をブロードキャストして UI を追従させる。
 */
class AttenuationService : Service() {

    companion object {
        // 微調整のスケール（MainActivity と共有する単一の定義）
        const val FINE_MAX = 40        // 値=FINE_MAX が 0 dB（減衰なし）
        const val PREFS = "volnotch"
        const val KEY_FINE = "fine_value"

        const val EXTRA_GAIN_DB = "gain_db"
        const val ACTION_STOP = "com.volnotch.action.STOP"

        // 状態変化を UI へ知らせるブロードキャスト
        const val ACTION_STATE_CHANGED = "com.volnotch.action.STATE_CHANGED"
        const val EXTRA_FINE_VALUE = "fine_value"

        private const val CHANNEL_ID = "volnotch_attenuation"
        private const val NOTIF_ID = 1

        /** gainDb（負の dB）で減衰を開始/更新する。 */
        fun start(context: Context, gainDb: Float) {
            val intent = Intent(context, AttenuationService::class.java)
                .putExtra(EXTRA_GAIN_DB, gainDb)
            context.startForegroundService(intent)
        }

        /** 減衰を解除してサービスを止める。 */
        fun stop(context: Context) {
            context.stopService(Intent(context, AttenuationService::class.java))
        }
    }

    private var dynamics: DynamicsProcessing? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // 通知の「解除」から呼ばれる。UI と設定を 0 dB に戻してから止める。
            persistAndBroadcast(FINE_MAX)
            stopSelf()
            return START_NOT_STICKY
        }
        val gainDb = intent?.getFloatExtra(EXTRA_GAIN_DB, 0f) ?: 0f
        // 5 秒以内に startForeground する必要があるため先に呼ぶ。
        startForeground(NOTIF_ID, buildNotification(gainDb))
        applyGain(gainDb)
        // プロセスが殺されても最後の intent を再配達し、減衰を復帰させる。
        return START_REDELIVER_INTENT
    }

    private fun applyGain(gainDb: Float) {
        try {
            val dp = dynamics ?: DynamicsProcessing(0).also { dynamics = it }
            if (gainDb < 0f) {
                dp.setInputGainAllChannelsTo(gainDb)
                dp.setEnabled(true)
            } else {
                dp.setEnabled(false)
            }
        } catch (e: RuntimeException) {
            // 効果適用に失敗したら 0 dB 扱いで止め、UI もリセットさせる。
            persistAndBroadcast(FINE_MAX)
            stopSelf()
        }
    }

    /** 現在の状態を SharedPreferences へ保存し、UI へブロードキャストする。 */
    private fun persistAndBroadcast(fineValue: Int) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_FINE, fineValue).apply()
        sendBroadcast(
            Intent(ACTION_STATE_CHANGED)
                .putExtra(EXTRA_FINE_VALUE, fineValue)
                .setPackage(packageName)
        )
    }

    private fun releaseEffect() {
        dynamics?.setEnabled(false)
        dynamics?.release()
        dynamics = null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseEffect()
    }

    // ---- 通知 ---------------------------------------------------------------

    private fun buildNotification(gainDb: Float): Notification {
        createChannel()

        val openPending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPending = PendingIntent.getService(
            this, 1, Intent(this, AttenuationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_launcher_foreground),
            getString(R.string.notif_stop),
            stopPending
        ).build()

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text, gainDb.toInt()))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPending)
            .addAction(stopAction)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
    }
}
