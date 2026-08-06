package com.battleheim.quantum2048.audio

import android.media.AudioManager
import android.media.ToneGenerator

interface GameAudio {
    fun move()
    fun merge()
    fun collapse()
    fun gameOver()
    fun release()
}

object SilentGameAudio : GameAudio {
    override fun move() = Unit
    override fun merge() = Unit
    override fun collapse() = Unit
    override fun gameOver() = Unit
    override fun release() = Unit
}

class ToneGameAudio : GameAudio {
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull()

    override fun move() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
    }

    override fun merge() {
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 70)
    }

    override fun collapse() {
        tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 90)
    }

    override fun gameOver() {
        tone?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
    }

    override fun release() {
        tone?.release()
    }
}
