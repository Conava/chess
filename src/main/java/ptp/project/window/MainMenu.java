package ptp.project.window;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainMenu extends JFrame {
    public MainMenu() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLayout(new FlowLayout());

        JButton onlineButton = new JButton("Online");
        onlineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new ChessGame().setVisible(true);
                dispose();
            }
        });

        JButton offlineButton = new JButton("Offline");
        offlineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new ChessGame().setVisible(true);
                dispose();
            }
        });

        JButton settingsButton = new JButton("Settings");
        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Settings().setVisible(true);
            }
        });

        add(onlineButton);
        add(offlineButton);
        add(settingsButton);
    }

    public static void main(String[] args) {
        new MainMenu().setVisible(true);
    }
}