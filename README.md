# Chess Application

This is a Chess application developed as a university project in Java. It allows users to play chess games in both online and offline modes. The application also provides an API for controlling the game programmatically. This project is not intended for publication, but rather as a demonstration of software development skills and understanding of the Java language and Maven build system.

## Project Description

The Chess application is designed with a focus on object-oriented programming principles. It features a robust API that allows for the control of game flow, including moving pieces, checking game state, and switching between different views (game and main menu). The application can be run with or without a GUI, making it suitable for testing or integration with different user interfaces.

## Features

- Play chess games in offline mode.
- Play chess games in online mode.
- Control the game programmatically using the provided API.
- Switch between game and main menu.
- Open settings window.

## Development

This project is developed using Java and Maven. The main class is `Chess.java` which contains the main method to start the application. The application can be started with a `nogui` parameter to start it without the GUI and control it with the API methods defined in `Chess.java`.

## Build Process

This project uses Maven for dependency management and build automation. Here are the steps to build the project:

1. **Compile the Project**: Use the following command to compile the project:

    ```bash
    mvn compile
    ```

    This command compiles the source code of the project and places the output in the `target` directory.

2. **Package the Project**: Use the following command to package the compiled code into a JAR file:

    ```bash
    mvn package
    ```

    This command compiles the source code of the project and packages the result into a JAR file within the `target` directory.

3. **Run the Project**: After packaging the project, you can run the application using the following command:

    ```bash
    java -jar target/Chess-1.0-SNAPSHOT.jar
    ```

    To run the application without a GUI (for testing or using with a different GUI), use the following command:

    ```bash
    java -jar target/Chess-1.0-SNAPSHOT.jar nogui
    ```

    Please note that when running the application without a GUI, you will need to use the API methods in `Chess.java` to control the game.

Please ensure that you have Maven and Java installed on your system and that they are added to your system's PATH.