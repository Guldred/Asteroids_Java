package game.train;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EvolutionTrainingMain extends JFrame {
    private EvolutionTrainingPanel trainingPanel;

    public EvolutionTrainingMain() {
        setTitle("Asteroids Evolution Training");
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        trainingPanel = new EvolutionTrainingPanel();
        add(trainingPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                trainingPanel.stopTrainer();
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> new EvolutionTrainingMain().setVisible(true));
    }
}
