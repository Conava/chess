package ptp.project.window;

import javax.swing.*;

public class MainFrame extends JFrame {
    private final MainMenu mainMenu;
    private final ChessGame chessGame;

    public MainFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);

        mainMenu = new MainMenu(this);
        chessGame = new ChessGame(this);

        setContentPane(mainMenu);
    }

    public void switchToGame() {
        setContentPane(chessGame);
        validate();
    }

    public void switchToMenu() {
        setContentPane(mainMenu);
        validate();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }
}