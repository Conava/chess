# 0005: ServerConnection Interface for Network Abstraction

Status: accepted
Date: 2024-01-01
Deciders: project founders

## Context and Problem Statement

`OnlineGame` (in `core`) needs to send TCP messages to the chess server. However, `core` must
not import any application classes (Architecture Law 4: `core` is logic-only). How can
`OnlineGame` call the TCP client without creating a dependency on the `application` module?

## Considered Options

1. Move the TCP client code into `core` — `OnlineGame` constructs a `Socket` directly.
2. Define an interface in `core` — the `application` module implements it; `OnlineGame`
   depends only on the interface.
3. Callback/lambda — `OnlineGame` accepts a `Consumer<String>` for outgoing messages.

## Decision Outcome

Chosen option: "Define an interface in `core`", because it is the standard dependency
inversion pattern and makes the abstraction explicit with a named type (`ServerConnection`)
rather than a raw functional interface. This keeps the dependency arrow pointing from
`application` to `core`, never the reverse.

### Consequences

- Good: `core` has no socket, I/O, or application imports. Architecture Law 4 is maintained.
- Good: `OnlineGame` can be unit-tested with a mock `ServerConnection` (no TCP socket needed).
- Good: The interface is explicit about the three operations: `sendMessage`, `closeConnection`,
  `isConnected`.
- Bad: An additional interface (`ServerConnection`) is defined in `core` even though it has
  no implementation there.
- Neutral: `ServerCommunicationTask` in `application` implements both `ServerConnection` and
  `Runnable`, coupling the send API to the network thread lifecycle.

## Interface definition

```java
// core/src/main/java/.../logic/game/ServerConnection.java
public interface ServerConnection {
    void sendMessage(String message);
    void closeConnection();
    boolean isConnected();
}
```

`ServerCommunicationTask` in the `application` module is the only production implementation.
Test implementations can be created inline with anonymous classes or Mockito.

## Pros and Cons of the Options

### Socket in `core`

- Good: No interface needed.
- Bad: Violates Architecture Law 4; `core` would depend on `java.net.*` I/O.
- Bad: Cannot be unit-tested without a live server.

### Interface in `core` (chosen)

- Good: Clean inversion of control; `core` defines the contract, `application` provides the implementation.
- Good: Testable with mocks.

### Consumer lambda

- Good: No new type needed.
- Bad: Only covers the `sendMessage` direction; `isConnected()` and `closeConnection()` cannot
  be expressed as a single `Consumer<String>`.
