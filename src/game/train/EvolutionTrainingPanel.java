package game.train;

import game.ai.QLearningAgent;
import game.component.Vector2;
import game.object.Asteroid;
import game.object.Player;
import game.object.projectiles.Projectile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class EvolutionTrainingPanel extends JPanel implements EvolutionTrainer.Listener {
    private final DecimalFormat df = new DecimalFormat("#0.00");
    private final JLabel generationLabel = statLabel("Generation: 0");
    private final JLabel populationLabel = statLabel("Population: 0");
    private final JLabel bestFitnessLabel = statLabel("Best fitness: 0.00");
    private final JLabel averageFitnessLabel = statLabel("Average fitness: 0.00");
    private final JLabel bestEverLabel = statLabel("Best ever: 0.00");
    private final JLabel bestKillsLabel = statLabel("Best kills: 0");
    private final JLabel averageKillsLabel = statLabel("Average kills: 0.00");
    private final PreviewPanel previewPanel = new PreviewPanel();
    private final FitnessGraphPanel fitnessGraphPanel = new FitnessGraphPanel();

    private EvolutionTrainer trainer;
    private Thread trainerThread;

    public EvolutionTrainingPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        add(previewPanel, BorderLayout.CENTER);
        add(createSidePanel(), BorderLayout.EAST);
        startTrainer();
    }

    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setPreferredSize(new Dimension(320, 720));
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(Color.BLACK);
        sidePanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Evolution Training", TitledBorder.CENTER, TitledBorder.TOP, null, Color.WHITE));

        JPanel stats = new JPanel(new GridLayout(7, 1, 4, 4));
        stats.setBackground(Color.BLACK);
        stats.add(generationLabel);
        stats.add(populationLabel);
        stats.add(bestFitnessLabel);
        stats.add(averageFitnessLabel);
        stats.add(bestEverLabel);
        stats.add(bestKillsLabel);
        stats.add(averageKillsLabel);

        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(stats);
        sidePanel.add(Box.createVerticalStrut(12));
        sidePanel.add(fitnessGraphPanel);
        sidePanel.add(Box.createVerticalGlue());
        return sidePanel;
    }

    private static JLabel statLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Consolas", Font.PLAIN, 13));
        return label;
    }

    private void startTrainer() {
        trainer = new EvolutionTrainer(this);
        trainerThread = new Thread(trainer::run, "EvolutionTrainer");
        trainerThread.setDaemon(true);
        trainerThread.start();
    }

    public void stopTrainer() {
        if (trainer != null) {
            trainer.stop();
        }
    }

    @Override
    public void onGenerationComplete(EvolutionTrainer.GenerationStats stats, QLearningAgent champion) {
        SwingUtilities.invokeLater(() -> {
            generationLabel.setText("Generation: " + stats.generation);
            populationLabel.setText("Population: " + stats.populationSize);
            bestFitnessLabel.setText("Best fitness: " + df.format(stats.bestFitness));
            averageFitnessLabel.setText("Average fitness: " + df.format(stats.averageFitness));
            bestEverLabel.setText("Best ever: " + df.format(stats.bestEverFitness));
            bestKillsLabel.setText("Best kills: " + stats.bestDestroyedAsteroids);
            averageKillsLabel.setText("Average kills: " + df.format(stats.averageDestroyedAsteroids));
            fitnessGraphPanel.addPoint(stats.bestFitness, stats.averageFitness);
            previewPanel.setChampion(champion, stats.generation);
        });
    }

    private static class PreviewPanel extends JPanel {
        private TrainingArena arena;
        private QLearningAgent champion;
        private int championGeneration;
        private long previewSeed = 1L;
        private final Timer timer;

        private PreviewPanel() {
            setBackground(Color.BLACK);
            timer = new Timer(16, e -> {
                if (arena != null) {
                    for (int i = 0; i < 2; i++) {
                        arena.step();
                        if (arena.isDone()) {
                            arena.reset(champion, ++previewSeed);
                        }
                    }
                    repaint();
                }
            });
            timer.start();
        }

        private void setChampion(QLearningAgent newChampion, int generation) {
            champion = new QLearningAgent(TrainingArena.WIDTH, TrainingArena.HEIGHT, false);
            champion.copyPolicyFrom(newChampion);
            champion.setLearningEnabled(false);
            championGeneration = generation;
            previewSeed++;
            arena = new TrainingArena(champion, previewSeed);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawGrid(g2);
            if (arena == null) {
                drawCenteredText(g2, "Waiting for first generation...");
                return;
            }

            double scaleX = getWidth() / (double) TrainingArena.WIDTH;
            double scaleY = getHeight() / (double) TrainingArena.HEIGHT;
            AffineTransform old = g2.getTransform();
            g2.scale(scaleX, scaleY);
            drawArena(g2);
            g2.setTransform(old);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            g2.drawString("Preview: generation " + championGeneration, 16, 24);
            g2.drawString("Fitness " + new DecimalFormat("#0.00").format(arena.getFitness())
                    + "   Kills " + arena.getDestroyedAsteroids(), 16, 44);
        }

        private void drawArena(Graphics2D g2) {
            for (Asteroid asteroid : arena.getAsteroids()) {
                Vector2 pos = asteroid.getPosition();
                int size = asteroid.getSize();
                g2.setColor(new Color(170, 170, 170));
                g2.fillOval((int) pos.x, (int) pos.y, size, size);
                g2.setColor(new Color(70, 70, 70));
                g2.drawOval((int) pos.x, (int) pos.y, size, size);
            }

            g2.setColor(new Color(255, 80, 60));
            for (Projectile projectile : arena.getProjectiles()) {
                Vector2 pos = projectile.getPosition();
                int size = Math.max(4, (int) projectile.getSize());
                g2.fillOval((int) pos.x, (int) pos.y, size, size);
            }

            Player player = arena.getPlayer();
            Vector2 center = player.getCenter();
            AffineTransform old = g2.getTransform();
            g2.translate(center.x, center.y);
            g2.rotate(Math.toRadians(player.getPlayerViewAngle()));
            Polygon ship = new Polygon(
                    new int[]{28, -22, -10, -22},
                    new int[]{0, -18, 0, 18},
                    4
            );
            g2.setColor(new Color(60, 210, 255));
            g2.fillPolygon(ship);
            g2.setColor(Color.WHITE);
            g2.drawPolygon(ship);
            g2.setTransform(old);
        }

        private void drawGrid(Graphics2D g2) {
            g2.setColor(new Color(18, 24, 28));
            for (int x = 0; x < getWidth(); x += 48) {
                g2.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += 48) {
                g2.drawLine(0, y, getWidth(), y);
            }
        }

        private void drawCenteredText(Graphics2D g2, String text) {
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("Consolas", Font.PLAIN, 16));
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, getHeight() / 2);
        }
    }

    private static class FitnessGraphPanel extends JPanel {
        private final List<Double> bestFitness = new ArrayList<>();
        private final List<Double> averageFitness = new ArrayList<>();
        private static final int MAX_POINTS = 120;

        private FitnessGraphPanel() {
            setPreferredSize(new Dimension(300, 260));
            setMaximumSize(new Dimension(300, 260));
            setBackground(Color.BLACK);
            setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                    "Fitness", TitledBorder.CENTER, TitledBorder.TOP, null, Color.WHITE));
        }

        private void addPoint(double best, double average) {
            bestFitness.add(best);
            averageFitness.add(average);
            while (bestFitness.size() > MAX_POINTS) {
                bestFitness.remove(0);
                averageFitness.remove(0);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bestFitness.size() < 2) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 28;
            int top = 28;
            int right = getWidth() - 12;
            int bottom = getHeight() - 24;

            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (int i = 0; i < bestFitness.size(); i++) {
                min = Math.min(min, Math.min(bestFitness.get(i), averageFitness.get(i)));
                max = Math.max(max, Math.max(bestFitness.get(i), averageFitness.get(i)));
            }
            if (Math.abs(max - min) < 0.001) {
                max += 1;
                min -= 1;
            }

            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(left, top, right - left, bottom - top);
            drawLine(g2, bestFitness, min, max, left, top, right, bottom, new Color(80, 220, 120));
            drawLine(g2, averageFitness, min, max, left, top, right, bottom, new Color(80, 170, 255));
        }

        private void drawLine(Graphics2D g2, List<Double> values, double min, double max,
                              int left, int top, int right, int bottom, Color color) {
            g2.setColor(color);
            for (int i = 0; i < values.size() - 1; i++) {
                int x1 = left + i * (right - left) / Math.max(1, values.size() - 1);
                int x2 = left + (i + 1) * (right - left) / Math.max(1, values.size() - 1);
                int y1 = bottom - (int) ((values.get(i) - min) / (max - min) * (bottom - top));
                int y2 = bottom - (int) ((values.get(i + 1) - min) / (max - min) * (bottom - top));
                g2.drawLine(x1, y1, x2, y2);
            }
        }
    }
}
