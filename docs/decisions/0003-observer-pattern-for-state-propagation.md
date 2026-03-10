# 0003: Observer Pattern for State Propagation

Status: accepted
Date: 2024-01-01
Deciders: project founders

## Context and Problem Statement

After a move is executed, the UI must update the board display. For online games, the update
may arrive from a background network thread. How should game state changes be communicated to
the UI without coupling the game engine to the rendering layer?

## Considered Options

1. Return value / callback — `movePiece` returns the new board state directly.
2. Polling — the UI reads `game.getState()` on a timer.
3. Observer pattern — the game engine maintains an observer list and calls
   `onGameStateChanged()` after every state change.

## Decision Outcome

Chosen option: "Observer pattern", because it decouples the timing and source of state changes
from the UI rendering, supports multiple simultaneous observers (UI, server), and works
correctly for both local and remote move events.

### Consequences

- Good: `Game` has no knowledge of JavaFX, controllers, or the server. It only calls
  `notifyObservers()`.
- Good: The `CopyOnWriteArrayList` backing in `Observable` allows concurrent
  `addObserver` / `removeObserver` calls without locking `notifyObservers()`.
- Good: `GameInstance` (server module) can observe the same `Game` instance to detect
  terminal states without polling.
- Bad: Observers must remember to use `Platform.runLater()` in the application module;
  forgetting causes threading bugs that are difficult to reproduce.
- Bad: The callback carries no parameters — observers must re-read all state they care about
  via the façade, which may be slightly wasteful.
- Neutral: `addObserver` throws `IllegalStateException` when called before `startGame()`,
  requiring controllers to register in the correct lifecycle phase.

## Pros and Cons of the Options

### Return value / callback

- Good: Type-safe; the new state is guaranteed to be fresh.
- Bad: Only one consumer can receive the return value; does not generalize to server
  observation or multiple UI panels.
- Bad: Breaks for remote events — the network thread receives moves that the UI must also
  display, but the move was not initiated by the UI.

### Polling

- Good: Simple to implement.
- Bad: Wastes CPU; introduces latency between state change and UI update; explicitly banned
  by Architecture Law 3.

### Observer pattern

- Good: Decoupled; works for any number of observers; works for both local and remote events.
- Bad: Threading discipline required (`Platform.runLater` in JavaFX context).
