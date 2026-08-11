package com.battleheim.quantum2048.engine

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuelBotTest {
    @Test fun bots_never_choose_invalid_moves_when_a_move_exists() {
        val state = GameEngine(SeededRandomProvider(1)).newGame(Difficulty.QUANTUM, 4)

        BotDifficulty.entries.forEach { difficulty ->
            val bot = BotScoring.botFor(difficulty, SeededRandomProvider(10))
            val move = bot.chooseMove(state)

            assertNotNull(move)
            assertTrue(GameEngine(SeededRandomProvider(2)).move(state, move!!).changed)
        }
    }

    @Test fun quantum_hard_scores_above_normal_above_easy_statistically() {
        val easy = averageBotScore(BotDifficulty.EASY)
        val normal = averageBotScore(BotDifficulty.NORMAL)
        val hard = averageBotScore(BotDifficulty.QUANTUM_HARD)

        assertTrue("normal=$normal easy=$easy", normal >= easy)
        assertTrue("hard=$hard normal=$normal", hard >= normal)
    }

    @Test fun duel_defaults_to_fast_four_by_four_board() {
        val duel = DuelEngine(SeededRandomProvider(4)).newDuel(
            DuelConfig(difficulty = Difficulty.QUANTUM, opponent = DuelOpponent.BOT, botDifficulty = BotDifficulty.QUANTUM_HARD, boardSize = 8),
        )

        assertTrue(duel.playerOne.size == 4 && duel.playerTwo.size == 4)
    }

    @Test fun duel_sandbox_forces_quantum_unlock_bypass() {
        val duel = DuelEngine(SeededRandomProvider(4)).newDuel(
            DuelConfig(difficulty = Difficulty.HARDCORE, opponent = DuelOpponent.PASS_AND_PLAY, sandboxUnlocksQuantum = false),
        )

        assertTrue(duel.config.sandboxUnlocksQuantum)
        assertTrue(duel.playerOne.mode == GameMode.QUANTUM && duel.playerTwo.mode == GameMode.QUANTUM)
        assertTrue(duel.config.difficulty == Difficulty.HARDCORE)
    }

    private fun averageBotScore(difficulty: BotDifficulty): Double {
        var total = 0L
        repeat(200) { seed ->
            var state = GameEngine(SeededRandomProvider(seed.toLong())).newGame(Difficulty.QUANTUM, 4)
            val bot = BotScoring.botFor(difficulty, SeededRandomProvider(seed + 99L))
            repeat(28) {
                val move = bot.chooseMove(state) ?: return@repeat
                val result = GameEngine(SeededRandomProvider(seed * 97L + it)).move(state, move)
                if (!result.changed) return@repeat
                state = result.state
            }
            total += state.score
        }
        return total / 200.0
    }
}
