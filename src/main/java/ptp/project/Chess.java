package ptp.project;
import ptp.project.window.MainFrame;
import javax.swing.*;

public class Chess {
    public static void main(String[] args) {
        System.out.println("Program started!");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MainFrame();
            }
        });
    }
}