package game.main;

import game.component.GameCore;
import game.ai.AIStatsPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main extends JFrame {

    private GameCore gameCore;
    private AIStatsPanel aiStatsPanel;

    public Main() {
        init();
    }

    private void init() {
        setTitle("Java Asteroids Game - Q-Learning AI");
        setSize(1580, 720); // Increased width to accommodate AI panel
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create game core
        gameCore = new GameCore(this);
        
        // Create AI statistics panel
        aiStatsPanel = new AIStatsPanel();
        
        // Create main content panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(gameCore, BorderLayout.CENTER);
        mainPanel.add(aiStatsPanel, BorderLayout.EAST);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Set up the AI stats panel to track the AI agent from game core
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                gameCore.start();
                
                // Connect AI stats panel to the AI agent after game starts
                SwingUtilities.invokeLater(() -> {
                    try {
                        Thread.sleep(1000); // Give game time to initialize
                        if (gameCore.getAIPlayer() != null && gameCore.getAIPlayer().getAIAgent() != null) {
                            aiStatsPanel.setAgent(gameCore.getAIPlayer().getAIAgent());
                        }
                    } catch (Exception ex) {
                        System.err.println("Error connecting AI stats panel: " + ex.getMessage());
                    }
                });
            }
            
            @Override
            public void windowClosing(WindowEvent e) {
                if (aiStatsPanel != null) {
                    aiStatsPanel.stop();
                }
            }
        });
    }

    public static void main(String[] args) {
        // Set look and feel for better appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }
        
        SwingUtilities.invokeLater(() -> {
            Main main = new Main();
            main.setVisible(true);
        });
    }
}
