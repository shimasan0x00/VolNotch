package com.volnotch

/**
 * 音量に関する端末非依存の純粋計算ロジック。
 *
 * Android フレームワークへの依存を持たないため、plain JUnit で検証できる。
 * Activity / Service に散っていた変換・クランプはここへ集約し、二重管理を防ぐ。
 */
object VolumeMath {

    /** 微調整スケールの上限。値=FINE_MAX が 0 dB（減衰なし）を表す。 */
    const val FINE_MAX = 40

    /** 音量インデックスを 0..max にクランプする。 */
    fun coerceVolumeIndex(index: Int, max: Int): Int = index.coerceIn(0, max)

    /**
     * 微調整値 fine（0..fineMax）を減衰ゲイン(dB)へ変換する。
     * fine=fineMax で 0 dB（素通し）、小さいほど大きく減衰（負の dB）。
     */
    fun fineToGainDb(fine: Int, fineMax: Int = FINE_MAX): Float = (fine - fineMax).toFloat()

    /**
     * 減衰ゲイン(dB)を微調整値 fine（0..fineMax）へ逆変換する。
     * [fineToGainDb] の逆写像で、範囲外は 0..fineMax にクランプする。
     */
    fun gainDbToFine(gainDb: Float, fineMax: Int = FINE_MAX): Int =
        (fineMax + gainDb.toInt()).coerceIn(0, fineMax)
}
