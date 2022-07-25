package game.main;

import game.component.GameCore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main extends JFrame {

    public Main() {
        init();
    }

    private void init() {
        setTitle("Java Asteroids Game");
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        GameCore gameCore = new GameCore();
        add(gameCore);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                gameCore.start();
            }
        });
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.setVisible(true);

    }
}
