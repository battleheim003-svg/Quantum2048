package com.battleheim.quantum2048.audio

import android.media.AudioManager
import android.media.ToneGenerator

interface GameAudio {
    fun ambientStart()
    fun ambientStop()
    fun move()
    fun merge()
    fun reaction()
    fun collapseLow()
    fun collapseHigh()
    fun tunnel()
    fun win()
    fun gameOver()
    fun menu()
    fun select()
    fun release()
}

object SilentGameAudio : GameAudio {
    override fun ambientStart() = Unit
    override fun ambientStop() = Unit
    override fun move() = Unit
    override fun merge() = Unit
    override fun reaction() = Unit
    override fun collapseLow() = Unit
    override fun collapseHigh() = Unit
    override fun tunnel() = Unit
    override fun win() = Unit
    override fun gameOver() = Unit
    override fun menu() = Unit
    override fun select() = Unit
    override fun release() = Unit
}

class ToneGameAudio : GameAudio {
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull()
    private var ambientEnabled = false

    override fun ambientStart() {
        ambientEnabled = true
    }

    override fun ambientStop() {
        ambientEnabled = false
    }

    override fun move() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
    }

    override fun merge() {
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 70)
    }

    override fun reaction() {
        tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 90)
    }

    override fun collapseLow() {
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 55)
    }

    override fun collapseHigh() {
        tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 120)
    }

    override fun tunnel() {
        tone?.startTone(ToneGenerator.TONE_PROP_PROMPT, 95)
    }

    override fun win() {
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 220)
    }

    override fun gameOver() {
        tone?.startTone(ToneGenerator.TONE_PROP_NACK, 180)
    }

    override fun menu() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 45)
    }

    override fun select() {
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 50)
    }

    override fun release() {
        tone?.release()
    }
}
