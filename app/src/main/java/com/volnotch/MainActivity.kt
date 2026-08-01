package com.volnotch

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.database.ContentObserver
import android.media.AudioManager
import android.media.audiofx.DynamicsProcessing
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView

/**
 * メディア等の音量を調整する単一 Activity。
 *
 * - 粗調整: [AudioManager.setStreamVolume] でストリームのインデックスを 1 刻みで設定。
 *   端末依存の最大インデックス（Fire Max 11 では 25）が下限の分解能になる。
 * - 微調整: [DynamicsProcessing] をグローバル出力(session 0)へ適用し、出力全体を
 *   dB 単位で減衰させる。実処理は [AttenuationService]（前面サービス）が保持するため、
 *   他アプリ使用中や本アプリを閉じた後も減衰が維持される。通知の「解除」など Activity
 *   外での変更は [AttenuationService.ACTION_STATE_CHANGED] を受けて UI へ反映する。
 */
class MainActivity : Activity() {

    // 微調整のスケールは AttenuationService と共有する。
    private val fineMax get() = AttenuationService.FINE_MAX

    private lateinit var audio: AudioManager
    private lateinit var prefs: SharedPreferences

    // 表示名 -> AudioManager のストリーム種別。先頭（メディア）を既定選択にする。
    // 切替セグメントもこのリストから生成するため、ストリームの定義はここだけにある。
    private val streams = listOf(
        "メディア" to AudioManager.STREAM_MUSIC,
        "着信音" to AudioManager.STREAM_RING,
        "通知" to AudioManager.STREAM_NOTIFICATION,
        "アラーム" to AudioManager.STREAM_ALARM,
        "システム" to AudioManager.STREAM_SYSTEM,
    )
    private var currentStream: Int = AudioManager.STREAM_MUSIC

    // 粗調整（ストリームインデックス）
    private lateinit var streamSegments: RadioGroup
    private lateinit var volumeTitle: TextView
    private lateinit var volumeValueBlock: View
    private lateinit var volumeValue: TextView
    private lateinit var volumeMax: TextView
    private lateinit var seekBar: SeekBar

    // 微調整（全体減衰）
    private lateinit var effectStatus: TextView
    private lateinit var fineValueBlock: View
    private lateinit var fineValueText: TextView
    private lateinit var fineSeekBar: SeekBar
    private var effectAvailable = false
    private var fineValue: Int = AttenuationService.FINE_MAX  // FINE_MAX = 0 dB（減衰なし）

    private val handler = Handler(Looper.getMainLooper())

    // 物理ボタン・他アプリ・本アプリのいずれの音量変更でも UI を追従させる。
    private val volumeObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            syncUiFromSystem()
        }
    }

    // サービス側（通知の「解除」等）での減衰状態変化を UI に反映する。
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AttenuationService.ACTION_STATE_CHANGED) {
                val v = intent.getIntExtra(AttenuationService.EXTRA_FINE_VALUE, fineMax)
                syncFineUi(v)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        prefs = getSharedPreferences(AttenuationService.PREFS, Context.MODE_PRIVATE)

        streamSegments = findViewById(R.id.streamSegments)
        volumeTitle = findViewById(R.id.volumeTitle)
        volumeValueBlock = findViewById(R.id.volumeValueBlock)
        volumeValue = findViewById(R.id.volumeValue)
        volumeMax = findViewById(R.id.volumeMax)
        seekBar = findViewById(R.id.volumeSeekBar)
        effectStatus = findViewById(R.id.effectStatus)
        fineValueBlock = findViewById(R.id.fineValueBlock)
        fineValueText = findViewById(R.id.fineValue)
        fineSeekBar = findViewById(R.id.fineSeekBar)

        setupStreamControls()
        setupFineControls()
    }

    // ---- 粗調整（ストリームインデックス） ------------------------------------

    private fun setupStreamControls() {
        // セグメントは [streams] から生成する（XML にストリーム名を複製しない）。
        // id 未設定の子は RadioGroup が追加時に自動採番するため、こちらでは付けない。
        streams.forEach { (name, _) ->
            val segment = layoutInflater
                .inflate(R.layout.item_stream_segment, streamSegments, false) as RadioButton
            segment.text = name
            streamSegments.addView(segment)
        }
        // 既定は currentStream と同じ先頭（メディア）。リスナ登録前に選んでおく。
        (streamSegments.getChildAt(0) as RadioButton).isChecked = true
        streamSegments.contentDescription = getString(R.string.stream_label)
        streamSegments.setOnCheckedChangeListener { group, checkedId ->
            val position = group.indexOfChild(group.findViewById<View>(checkedId))
            if (position >= 0) {
                currentStream = streams[position].second
                syncUiFromSystem()
            }
        }

        // SeekBar は整数 = 1 刻み。ユーザー操作のときだけ音量へ反映する
        // （プログラムからの progress 変更は fromUser=false になり、下の分岐で無視される）。
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) applyVolume(progress)
            }

            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        findViewById<Button>(R.id.minusButton).setOnClickListener {
            applyVolume(audio.getStreamVolume(currentStream) - 1)
        }
        findViewById<Button>(R.id.plusButton).setOnClickListener {
            applyVolume(audio.getStreamVolume(currentStream) + 1)
        }
        findViewById<Button>(R.id.setOneButton).setOnClickListener {
            applyVolume(1)
        }

        syncUiFromSystem()
    }

    /** index を 0..max にクランプして設定し、実値を読み直して UI 同期する。 */
    private fun applyVolume(index: Int) {
        val max = audio.getStreamMaxVolume(currentStream)
        val clamped = VolumeMath.coerceVolumeIndex(index, max)
        try {
            audio.setStreamVolume(currentStream, clamped, 0)
        } catch (e: SecurityException) {
            // 着信音/通知を 0 にする際、端末により通知ポリシー(DND)アクセスが
            // 必要で例外になることがある。UI は実値へ戻すだけにする。
        }
        syncUiFromSystem()
    }

    /** 現在のストリームの実値を UI（見出し / 数値 / SeekBar）へ反映する。 */
    private fun syncUiFromSystem() {
        val max = audio.getStreamMaxVolume(currentStream)
        val cur = audio.getStreamVolume(currentStream)
        val name = streams.first { it.second == currentStream }.first
        volumeTitle.text = getString(R.string.stream_volume_title, name)
        volumeValue.text = cur.toString()
        volumeMax.text = getString(R.string.volume_of_max, max)
        // 表示が数値と単位に分かれているので、読み上げと uiautomator 用に 1 文を持たせる。
        volumeValueBlock.contentDescription =
            getString(R.string.volume_value_a11y, name, cur, max)
        if (seekBar.max != max) seekBar.max = max
        if (seekBar.progress != cur) seekBar.progress = cur
    }

    // ---- 微調整（全体減衰 / 前面サービス） -----------------------------------

    private fun setupFineControls() {
        // グローバル出力(session 0)へ効果を出せる端末かを、生成→即解放で確認する。
        effectAvailable = try {
            DynamicsProcessing(0).release()
            true
        } catch (e: RuntimeException) {
            false
        }

        // 使えるのが当たり前なので、成功時は何も出さず、非対応のときだけ警告行を出す。
        effectStatus.visibility = if (effectAvailable) View.GONE else View.VISIBLE

        fineValue = prefs.getInt(AttenuationService.KEY_FINE, fineMax).coerceIn(0, fineMax)
        fineSeekBar.max = fineMax
        fineSeekBar.progress = fineValue

        fineSeekBar.isEnabled = effectAvailable
        findViewById<Button>(R.id.fineMinusButton).isEnabled = effectAvailable
        findViewById<Button>(R.id.finePlusButton).isEnabled = effectAvailable
        findViewById<Button>(R.id.fineResetButton).isEnabled = effectAvailable

        fineSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) applyFine(progress)
            }

            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        findViewById<Button>(R.id.fineMinusButton).setOnClickListener { applyFine(fineValue - 1) }
        findViewById<Button>(R.id.finePlusButton).setOnClickListener { applyFine(fineValue + 1) }
        findViewById<Button>(R.id.fineResetButton).setOnClickListener { applyFine(fineMax) }

        updateFineLabel()

        // 前回の減衰値が残っていれば、サービスへ再適用して状態を一致させる。
        if (effectAvailable && fineValue < fineMax) {
            applyFine(fineValue)
        }
    }

    /**
     * 全体減衰を設定する。value は 0..FINE_MAX で、ゲイン = (value - FINE_MAX) dB。
     * value=FINE_MAX なら 0 dB（サービス停止=素通し）、小さいほど大きく減衰。
     * ユーザー操作起点の変更に使う（サービスの start/stop を伴う）。
     */
    private fun applyFine(value: Int) {
        val v = value.coerceIn(0, fineMax)
        fineValue = v
        prefs.edit().putInt(AttenuationService.KEY_FINE, v).apply()

        val gainDb = VolumeMath.fineToGainDb(v, fineMax)
        if (effectAvailable) {
            if (gainDb < 0f) {
                AttenuationService.start(this, gainDb)
            } else {
                AttenuationService.stop(this)
            }
        }
        if (fineSeekBar.progress != v) fineSeekBar.progress = v
        updateFineLabel()
    }

    /**
     * UI（スライダー / ラベル）だけを value に合わせる。サービスや設定は変更しない。
     * サービス側の状態変化ブロードキャストや、復帰時の prefs 反映に使う。
     */
    private fun syncFineUi(value: Int) {
        val v = value.coerceIn(0, fineMax)
        fineValue = v
        if (fineSeekBar.progress != v) fineSeekBar.progress = v
        updateFineLabel()
    }

    private fun updateFineLabel() {
        val gainDb = VolumeMath.fineToGainDb(fineValue, fineMax).toInt()  // 0 または負
        // 数字とハイフンが並ぶと読みにくいので、表示にはマイナス記号 U+2212 を使う。
        fineValueText.text = if (gainDb < 0) "−${-gainDb}" else gainDb.toString()
        fineValueBlock.contentDescription = getString(R.string.fine_value, gainDb)
    }

    // ---- ライフサイクル ------------------------------------------------------

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeObserver
        )
        registerReceiver(stateReceiver, IntentFilter(AttenuationService.ACTION_STATE_CHANGED))
        syncUiFromSystem()
        // 停止中にサービス側で変わった減衰状態を反映（UI のみ）。
        syncFineUi(prefs.getInt(AttenuationService.KEY_FINE, fineMax))
    }

    override fun onResume() {
        super.onResume()
        syncUiFromSystem()
    }

    override fun onStop() {
        super.onStop()
        contentResolver.unregisterContentObserver(volumeObserver)
        unregisterReceiver(stateReceiver)
    }
}
