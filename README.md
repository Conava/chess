# Chess Application

This is a Chess application developed as a university project in Java. It allows users to play chess games in both online and offline modes. The application also provides an API for controlling the game programmatically, enabling better testing abilities and allowing the project to be used as a module in different programs. It can be started in a `nogui` mode for this purpose.

## Project Description

The Chess application is designed with a focus on object-oriented programming principles and follows a modularized architecture. The project is divided into 3 main modules: `application`, `server` and `core`.

### Modules

- **application**: The GUI application for playing chess games.
- **server**: The server to handle online games.
- **core**: Contains the core game logic and data structures used by all other modules`


## Features

- Play chess games in offline mode with 2 local players on one computer.
- Play chess games in online mode with a client-server architecture.
- Control the game programmatically using the provided API.
- Switch between game and main menu.
- Open settings window.

## Development

This project is developed using Java and Maven. The main class is `Chess.java` in the `application` module, which contains the main method to start the application. The application can be started with a `nogui` parameter to start it without the GUI and control it with the API methods defined in `Chess.java`.

## Build Process

This project uses Maven for dependency management and build automation. Here are the steps to build the project:

1. **Install the Project**: Use the following command to compile the project:

    ```sh
    mvn install
    ```

   This command compiles the source code of the project and places the output in the `target` directory of the respective module.
   The JAR files for Application will be created in the `Releases` directory in a subfolder of the current version number as defined in the `pom.xml` file.

2. **Run the Application**: After packaging the project, you can run the application using the following command:

    ```sh
    java -jar Releases/1.0/application-1.0.jar
    ```
   
    This command starts the regular application with the GUI.

    To run the application without a GUI (for testing or using with a different GUI), use the following command:

    ```sh
    java -jar Releases/1.0/application-1.0.jar nogui
    ```

   Please note that when running the application without a GUI, you will need to use the API methods in `Chess.java` to control the game.

3. **Run the Server**: To run the server, use the following command:

    ```sh
    java -jar Releases/1.0/server-1.0.jar
    ```

    This command starts the server for online games.

## Requirements

Please ensure that you have Maven and Java installed on your system and that they are added to your system's PATH.

## API Usage

The `Chess` class provides several methods to control the game programmatically:

- `startGame(int online, RulesetOptions selectedRuleset, String playerWhiteName, String playerBlackName)`: Starts a new game.
- `getState()`: Returns the current state of the game.
- `getBoard()`: Returns the current game board.
- `addObserver(GameObserver observer)`: Adds an observer to the game.
- `removeObserver(GameObserver observer)`: Removes an observer from the game.
- `endGame()`: Ends the current game.
- `getCurrentPlayer()`: Returns the current player.
- `getPlayerWhite()`: Returns the white player.
- `getPlayerBlack()`: Returns the black player.
- `getPieceAt(Square position)`: Returns the piece at a given position.
- `getLegalSquares(Square position)`: Returns the legal squares for a piece at a given position.
- `getMoveList()`: Returns the list of moves made in the game.
- `movePiece(Square start, Square end)`: Moves a piece in the game.
- `promoteMove(Square start, Square end, Pieces targetPiece)`: Promotes a piece during a move.

For detailed usage, refer to the Javadoc comments in the `Chess.java` file.