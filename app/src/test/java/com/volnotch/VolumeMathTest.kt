package com.volnotch

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [VolumeMath] の純粋ロジックに対する仕様（振る舞い）テスト。
 * 1 テスト 1 概念・AAA（Arrange / Act / Assert）で記述する。
 */
class VolumeMathTest {

    // ---- coerceVolumeIndex ---------------------------------------------------

    @Test
    fun `範囲内の index はそのまま返す`() {
        // Arrange
        val index = 5
        val max = 25

        // Act
        val result = VolumeMath.coerceVolumeIndex(index, max)

        // Assert
        assertEquals(5, result)
    }

    @Test
    fun `下限未満の負値は 0 にクランプする`() {
        // Arrange
        val index = -3
        val max = 25

        // Act
        val result = VolumeMath.coerceVolumeIndex(index, max)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun `上限超過は max にクランプする`() {
        // Arrange
        val index = 30
        val max = 25

        // Act
        val result = VolumeMath.coerceVolumeIndex(index, max)

        // Assert
        assertEquals(25, result)
    }

    // ---- fineToGainDb --------------------------------------------------------

    @Test
    fun `fine が FINE_MAX なら減衰なしの 0dB`() {
        // Arrange
        val fine = VolumeMath.FINE_MAX

        // Act
        val gainDb = VolumeMath.fineToGainDb(fine)

        // Assert
        assertEquals(0f, gainDb, 0f)
    }

    @Test
    fun `fine が 0 なら最大減衰の マイナス40dB`() {
        // Arrange
        val fine = 0

        // Act
        val gainDb = VolumeMath.fineToGainDb(fine)

        // Assert
        assertEquals(-40f, gainDb, 0f)
    }

    @Test
    fun `fine が 20 なら マイナス20dB`() {
        // Arrange
        val fine = 20

        // Act
        val gainDb = VolumeMath.fineToGainDb(fine)

        // Assert
        assertEquals(-20f, gainDb, 0f)
    }

    // ---- gainDbToFine --------------------------------------------------------

    @Test
    fun `0dB は fine の 40 に対応する`() {
        // Arrange
        val gainDb = 0f

        // Act
        val fine = VolumeMath.gainDbToFine(gainDb)

        // Assert
        assertEquals(40, fine)
    }

    @Test
    fun `マイナス40dB は fine の 0 に対応する`() {
        // Arrange
        val gainDb = -40f

        // Act
        val fine = VolumeMath.gainDbToFine(gainDb)

        // Assert
        assertEquals(0, fine)
    }

    @Test
    fun `マイナス20dB は fine の 20 に対応する`() {
        // Arrange
        val gainDb = -20f

        // Act
        val fine = VolumeMath.gainDbToFine(gainDb)

        // Assert
        assertEquals(20, fine)
    }

    @Test
    fun `正のゲインは FINE_MAX にクランプする`() {
        // Arrange
        val gainDb = 5f

        // Act
        val fine = VolumeMath.gainDbToFine(gainDb)

        // Assert
        assertEquals(VolumeMath.FINE_MAX, fine)
    }

    @Test
    fun `過大な負値は 0 にクランプする`() {
        // Arrange
        val gainDb = -100f

        // Act
        val fine = VolumeMath.gainDbToFine(gainDb)

        // Assert
        assertEquals(0, fine)
    }

    // ---- round-trip ----------------------------------------------------------

    @Test
    fun `fine の全域で gainDbToFine と fineToGainDb は往復一致する`() {
        // Arrange
        for (fine in 0..VolumeMath.FINE_MAX) {
            // Act
            val roundTripped = VolumeMath.gainDbToFine(VolumeMath.fineToGainDb(fine))

            // Assert
            assertEquals(fine, roundTripped)
        }
    }
}
