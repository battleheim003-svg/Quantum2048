# Quantum 2048 Roadmap

## Core Vision

Quantum 2048 has two modes:

- Classic: a clean 2048 board with normal numeric merges.
- Quantum: an arcade synthesis lab. The board starts from particles, then the player fuses them into elements.

The quantum mode is intentionally game-first. It borrows scientific language, but it is balanced around readable rules, satisfying merges, and short mobile sessions.

## Phase 1 - Playable Core

Implemented rules:

- Spawn particles in Quantum mode: Electron and Proton.
- Electron + Proton synthesizes Hydrogen.
- Two identical synthesized elements fuse into the next tier.
- Current chain: H, He, Li, Be, B, C, N, O, Ne, Si, Fe, Au.
- Classic mode remains regular numeric 2048.
- Quantum tiles show only particles/elements, never numeric 2048 values.

## Phase 2 - Game Shell

Next target:

- Main menu with Continue, New Game, Mode Select, Settings, and Lab Codex.
- Pause menu during a game.
- Clear onboarding cards for Classic and Quantum.
- Separate saved games per mode.
- Better end-game screens with final element reached, score, and moves.

## Phase 3 - Quantum Depth

Add strategic identity:

- Fusion recipes beyond same-pair merges.
- Rare catalyst tiles such as Neutron, Photon, and Plasma.
- Energy economy tuning: rewards for chain merges, cost scaling for high collapse.
- Lab Codex that unlocks entries when elements are synthesized.

## Phase 4 - Polish

Make it feel finished:

- Tile movement animations.
- Spawn, merge, collapse, and win effects.
- Sound hooks for movement, synthesis, collapse, error, and game over.
- Haptics for merge and collapse.
- Accessibility pass: contrast, dynamic type, reduced motion.

## Phase 5 - Progression

Longer-term loop:

- Daily seed challenge.
- Element collection milestones.
- Optional challenge boards: 5x5 reactor, low-energy run, unstable lab.
- Local stats: best element, best score, longest chain, fastest Silicon.
