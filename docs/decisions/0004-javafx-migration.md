# 0004: Migrate UI from Swing to JavaFX 21

Status: accepted
Date: 2024-01-01
Deciders: project founders

## Context and Problem Statement

The original `application` module was built with Swing. Swing is in maintenance mode; Oracle
has committed to supporting it for the foreseeable future but is not actively developing it.
The project needs CSS-based theming, reactive property bindings for responsive board sizing,
and FXML-based layout separation. Should the UI remain in Swing or be migrated to JavaFX?

## Considered Options

1. Keep Swing — maintain the existing Swing codebase.
2. Migrate to JavaFX 21 — rewrite the `application` module using JavaFX.
3. Migrate to a web-based UI — replace the desktop UI with a browser front-end.

## Decision Outcome

Chosen option: "Migrate to JavaFX 21", because JavaFX provides CSS theming, FXML layout
separation, observable properties for reactive binding, and a `javafx.concurrent.Task` for
clean background threading — all features that the project requires and that Swing does not
provide idiomatically.

### Consequences

- Good: CSS-driven theming (`base.css` + four UI themes + three board themes) is implemented
  cleanly without manual `UIManager` configuration.
- Good: `javafx.concurrent.Task` provides a standard pattern for background move execution
  (`ExecuteMove extends Task<Void>`).
- Good: FXML + controller pattern separates layout from logic; controllers can be
  constructor-injected rather than reflection-constructed.
- Good: `Platform.enterNestedEventLoop` enables synchronous overlay dismissal without
  blocking the FX Application Thread.
- Bad: JavaFX requires a module-path setup for fat-JAR distribution. The maven-shade-plugin
  produces a JAR that requires `--module-path` and `--add-modules` at launch.
- Bad: JavaFX is not bundled with the JDK since Java 11; it must be added as a Maven
  dependency or installed separately for the fat-JAR launch path.
- Neutral: `core` and `server` are unchanged by this decision.

## Pros and Cons of the Options

### Keep Swing

- Good: No migration effort.
- Bad: No CSS theming; theming requires `UIManager` or custom `LookAndFeel`.
- Bad: Thread model (`SwingUtilities.invokeLater`) is equivalent to `Platform.runLater`
  but Swing's `javax.swing.Timer` is less expressive than JavaFX's animation APIs.

### JavaFX 21

- Good: FXML, CSS, property binding, Task, OverlayManager via nested event loop.
- Bad: Slightly more complex distribution (module path for fat JARs).

### Web-based UI

- Good: Cross-platform without JVM installation requirement.
- Bad: Requires a web framework, HTTP/WebSocket server, JavaScript front-end — a much
  larger scope change. The project is a desktop application.
