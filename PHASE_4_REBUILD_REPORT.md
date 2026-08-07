# Phase 4 Rebuild Report - Duel Mode

## Changed

- Added turn-based duel models: `DuelConfig`, `DuelState`, `DuelOpponent`, `DuelPlayer`, and `BotDifficulty`.
- Added `DuelEngine` with two independent boards, turn switching, pass/timeout handling, and winner detection.
- Duel always normalizes to a fast 4x4 board.
- Added three independent bot levels:
  - Easy: valid/random-safe move selection.
  - Normal: greedy heuristic using empty cells, max tile, corner placement, and score.
  - Quantum-Hard: one-step lookahead plus chemistry-aware reaction potential.
- Added New Game UI choices for Solo/Duel, Bot/Pass & Play, and bot difficulty.
- Added duel UI on the game screen: current turn, opponent type, 12-second timer, pass button, winner text, and inactive-board summary.
- Bot turns run automatically after Player 1 moves.

## Design Decision

- Timed-out turns pass automatically instead of forcing a random move. This keeps pass-and-play fair and quick without letting the game make surprising moves for a human player.

## Tests

- Added `DuelBotTest` for valid bot moves, 200-run statistical ordering (`Quantum-Hard >= Normal >= Easy`), and forced 4x4 duel boards.

## Verification

- `.\gradlew.bat assembleDebug --no-daemon` succeeded.
- `.\gradlew.bat testDebugUnitTest --no-daemon --max-workers=1` succeeded.

## Remaining

- Device/emulator video capture for full human-vs-bot and pass-and-play hands is still needed.
- Full bot-vs-bot tournament UI/reporting can be expanded later; current automated coverage validates the bot ordering statistically through unit tests.
