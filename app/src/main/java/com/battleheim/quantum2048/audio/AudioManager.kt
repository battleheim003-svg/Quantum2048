package com.battleheim.quantum2048.audio
interface GameAudio { fun move(); fun merge(); fun gameOver(); fun release() }
object SilentGameAudio : GameAudio { override fun move() = Unit; override fun merge() = Unit; override fun gameOver() = Unit; override fun release() = Unit }
