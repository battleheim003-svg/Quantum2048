package com.battleheim.quantum2048.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.battleheim.quantum2048.R
import com.battleheim.quantum2048.domain.AppSettings
import kotlin.math.ln
import kotlin.random.Random

interface GameAudio {
    fun applySettings(settings: AppSettings)
    fun menuMusic()
    fun gameMusic()
    fun ambientStart()
    fun ambientStop()
    fun move()
    fun merge(value: Int = 0)
    fun proton()
    fun reaction()
    fun synthesis()
    fun collapseLow()
    fun collapseHigh()
    fun tunnel()
    fun unlock()
    fun star()
    fun share()
    fun duelTurn()
    fun win()
    fun gameOver()
    fun menu()
    fun select()
    fun release()
}

object SilentGameAudio : GameAudio {
    override fun applySettings(settings: AppSettings) = Unit
    override fun menuMusic() = Unit
    override fun gameMusic() = Unit
    override fun ambientStart() = Unit
    override fun ambientStop() = Unit
    override fun move() = Unit
    override fun merge(value: Int) = Unit
    override fun proton() = Unit
    override fun reaction() = Unit
    override fun synthesis() = Unit
    override fun collapseLow() = Unit
    override fun collapseHigh() = Unit
    override fun tunnel() = Unit
    override fun unlock() = Unit
    override fun star() = Unit
    override fun share() = Unit
    override fun duelTurn() = Unit
    override fun win() = Unit
    override fun gameOver() = Unit
    override fun menu() = Unit
    override fun select() = Unit
    override fun release() = Unit
}

class AndroidGameAudio(context: Context) : GameAudio {
    private val appContext = context.applicationContext
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(attributes)
        .build()
    private val rng = Random(2048)
    private var settings = AppSettings()
    private var currentMusic: MediaPlayer? = null
    private var currentMusicRes: Int? = null
    private var musicRequested = false
    private var released = false

    private val sounds = mapOf(
        Sound.MOVE to soundPool.load(appContext, R.raw.sfx_tile_move_01, 1),
        Sound.MOVE_ALT to soundPool.load(appContext, R.raw.sfx_tile_move_02, 1),
        Sound.MERGE_LOW to soundPool.load(appContext, R.raw.sfx_tile_merge_low, 1),
        Sound.MERGE_MID to soundPool.load(appContext, R.raw.sfx_tile_merge_mid, 1),
        Sound.MERGE_HIGH to soundPool.load(appContext, R.raw.sfx_tile_merge_high, 1),
        Sound.PROTON to soundPool.load(appContext, R.raw.sfx_proton_spawn, 1),
        Sound.PROTON_MERGE to soundPool.load(appContext, R.raw.sfx_proton_merge, 1),
        Sound.REACTION to soundPool.load(appContext, R.raw.sfx_reaction, 1),
        Sound.SYNTHESIS to soundPool.load(appContext, R.raw.sfx_synthesis_success, 1),
        Sound.COLLAPSE_LOW to soundPool.load(appContext, R.raw.sfx_quantum_collapse_low, 1),
        Sound.COLLAPSE_HIGH to soundPool.load(appContext, R.raw.sfx_quantum_collapse_high, 1),
        Sound.TUNNEL to soundPool.load(appContext, R.raw.sfx_quantum_tunnel, 1),
        Sound.UNLOCK to soundPool.load(appContext, R.raw.sfx_quantum_unlock, 1),
        Sound.STAR to soundPool.load(appContext, R.raw.sfx_star_fill, 1),
        Sound.SHARE to soundPool.load(appContext, R.raw.sfx_share, 1),
        Sound.DUEL_TURN to soundPool.load(appContext, R.raw.sfx_duel_turn, 1),
        Sound.WIN to soundPool.load(appContext, R.raw.sfx_win, 1),
        Sound.GAME_OVER to soundPool.load(appContext, R.raw.sfx_game_over, 1),
        Sound.MENU to soundPool.load(appContext, R.raw.sfx_menu_blip, 1),
        Sound.SELECT to soundPool.load(appContext, R.raw.sfx_select, 1),
    )

    override fun applySettings(settings: AppSettings) {
        this.settings = settings
        updateMusicVolume()
        if (!settings.musicEnabled) ambientStop() else if (musicRequested) ambientStart()
    }

    override fun menuMusic() = switchMusic(R.raw.music_menu_loop)

    override fun gameMusic() = switchMusic(R.raw.music_game_calm_loop)

    override fun ambientStart() {
        musicRequested = true
        if (settings.musicEnabled) ensureMusic()
    }

    override fun ambientStop() {
        musicRequested = false
        currentMusic?.pause()
    }

    override fun move() {
        play(if (rng.nextBoolean()) Sound.MOVE else Sound.MOVE_ALT, volume = 0.55f, rate = 0.96f + rng.nextFloat() * 0.1f)
    }

    override fun merge(value: Int) {
        val sound = when {
            value >= 512 -> Sound.MERGE_HIGH
            value >= 64 -> Sound.MERGE_MID
            else -> Sound.MERGE_LOW
        }
        play(sound, volume = if (sound == Sound.MERGE_HIGH) 0.95f else 0.72f, rate = mergeRate(value))
    }

    override fun proton() = play(Sound.PROTON, volume = 0.66f, rate = 1.08f)

    override fun reaction() = play(Sound.REACTION, volume = 0.78f)

    override fun synthesis() = play(Sound.SYNTHESIS, volume = 0.9f)

    override fun collapseLow() = play(Sound.COLLAPSE_LOW, volume = 0.72f)

    override fun collapseHigh() = play(Sound.COLLAPSE_HIGH, volume = 1f)

    override fun tunnel() = play(Sound.TUNNEL, volume = 0.88f)

    override fun unlock() = play(Sound.UNLOCK, volume = 1f)

    override fun star() = play(Sound.STAR, volume = 0.82f)

    override fun share() = play(Sound.SHARE, volume = 0.74f)

    override fun duelTurn() = play(Sound.DUEL_TURN, volume = 0.68f)

    override fun win() = play(Sound.WIN, volume = 1f)

    override fun gameOver() = play(Sound.GAME_OVER, volume = 0.86f)

    override fun menu() = play(Sound.MENU, volume = 0.52f)

    override fun select() = play(Sound.SELECT, volume = 0.72f)

    override fun release() {
        if (released) return
        released = true
        currentMusic?.release()
        currentMusic = null
        soundPool.release()
    }

    private fun switchMusic(resId: Int) {
        musicRequested = true
        if (currentMusicRes == resId && currentMusic != null) {
            ambientStart()
            return
        }
        currentMusic?.release()
        currentMusic = MediaPlayer.create(appContext, resId)?.apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
        }
        currentMusicRes = resId
        updateMusicVolume()
        ambientStart()
    }

    private fun ensureMusic() {
        if (currentMusic == null) switchMusic(R.raw.music_game_calm_loop)
        currentMusic?.let { if (!it.isPlaying) it.start() }
    }

    private fun updateMusicVolume() {
        val volume = if (settings.musicEnabled) settings.masterVolume * settings.musicVolume else 0f
        currentMusic?.setVolume(volume, volume)
    }

    private fun play(sound: Sound, volume: Float, rate: Float = 1f) {
        if (released) return
        if (!settings.soundEnabled) return
        val id = sounds[sound] ?: return
        val gain = settings.masterVolume * settings.sfxVolume * volume
        soundPool.play(id, gain, gain, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    private fun mergeRate(value: Int): Float {
        if (value <= 0) return 1f
        return (0.88f + (ln(value.toFloat()) / ln(2f)) * 0.022f).coerceIn(0.88f, 1.22f)
    }

    private enum class Sound {
        MOVE,
        MOVE_ALT,
        MERGE_LOW,
        MERGE_MID,
        MERGE_HIGH,
        PROTON,
        PROTON_MERGE,
        REACTION,
        SYNTHESIS,
        COLLAPSE_LOW,
        COLLAPSE_HIGH,
        TUNNEL,
        UNLOCK,
        STAR,
        SHARE,
        DUEL_TURN,
        WIN,
        GAME_OVER,
        MENU,
        SELECT,
    }
}

typealias ToneGameAudio = AndroidGameAudio
