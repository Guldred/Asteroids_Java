package game.train;

import game.ai.dqn.DQNAgent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Statistics panel for Evolutionary DQN Training
 * 
 * Shows:
 * - Population fitness distribution
 * - Generation evolution progress
 * - Individual agent performance
 * - Evolution statistics and controls
 */
public class EvolutionStatsPanel extends JPanel {
    private final List<EvolutionaryDQNTrainer.AgentData> population;
    private final List<Float> generationBestFitness = new ArrayList<>();
    private final List<Float> generationAvgFitness = new ArrayList<>();
    private final int MAX_GENERATION_HISTORY = 50;
    
    // UI Components
    private JLabel generationLabel;
    private JLabel episodeLabel;
    private JLabel aliveCountLabel;
    private JLabel bestFitnessLabel;
    private JLabel avgFitnessLabel;
    private JLabel populationSizeLabel;
    private EvolutionGraphPanel graphPanel;
    private AgentTablePanel agentTablePanel;
    
    // Agent colors for display
    private final Color[] AGENT_COLORS = {
        Color.GREEN, Color.BLUE, Color.RED, Color.YELLOW, Color.MAGENTA,
        Color.CYAN, Color.ORANGE, Color.PINK, new Color(128, 255, 128)
    };
    
    public EvolutionStatsPanel(List<EvolutionaryDQNTrainer.AgentData> population, int generation) {
        this.population = population;
        
        setPreferredSize(new Dimension(350, 900));
        setBackground(Color.DARK_GRAY);
        setLayout(new BorderLayout());
        
        // Create components
        createStatsPanel();
        createGraphPanel();
        createAgentTable();
        
        // Layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(createStatsPanel(), BorderLayout.NORTH);
        topPanel.add(graphPanel, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);
        add(agentTablePanel, BorderLayout.CENTER);
        add(createControlsPanel(), BorderLayout.SOUTH);
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.DARK_GRAY);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.WHITE), 
            "🧬 Evolution Statistics", 0, 0, null, Color.WHITE));
        
        // Create labels
        generationLabel = createStyledLabel("Generation: 1");
        episodeLabel = createStyledLabel("Episode: 0/10");
        aliveCountLabel = createStyledLabel("Agents Alive: 10/10");
        bestFitnessLabel = createStyledLabel("Best Fitness: 0.00");
        avgFitnessLabel = createStyledLabel("Avg Fitness: 0.00");
        populationSizeLabel = createStyledLabel("Population: 10 agents");
        
        JLabel infoLabel1 = createStyledLabel("🔬 Hybrid Training:");
        JLabel infoLabel2 = createStyledLabel("• Individual: DQN Learning");
        JLabel infoLabel3 = createStyledLabel("• Population: Evolution");
        
        panel.add(generationLabel);
        panel.add(episodeLabel);
        panel.add(aliveCountLabel);
        panel.add(bestFitnessLabel);
        panel.add(avgFitnessLabel);
        panel.add(populationSizeLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(infoLabel1);
        panel.add(infoLabel2);
        panel.add(infoLabel3);
        
        return panel;
    }
    
    private void createGraphPanel() {
        graphPanel = new EvolutionGraphPanel();
        graphPanel.setPreferredSize(new Dimension(340, 200));
        graphPanel.setBackground(Color.BLACK);
    }
    
    private void createAgentTable() {
        agentTablePanel = new AgentTablePanel();
        agentTablePanel.setPreferredSize(new Dimension(340, 300));
    }
    
    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(Color.DARK_GRAY);
        
        JLabel instructions = new JLabel("<html><center>" +
            "🧬 EVOLUTIONARY AI<br>" +
            "Watch 10 agents compete!<br>" +
            "Top 5 survive each generation<br>" +
            "Neural networks evolve!" +
            "</center></html>");
        instructions.setForeground(Color.LIGHT_GRAY);
        instructions.setFont(new Font("Arial", Font.PLAIN, 10));
        
        panel.add(instructions);
        return panel;
    }
    
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return label;
    }
    
    public void updateStats(List<EvolutionaryDQNTrainer.AgentData> currentPopulation, 
                           int generation, int episode) {
        SwingUtilities.invokeLater(() -> {
            // Update basic stats
            generationLabel.setText("Generation: " + generation);
            episodeLabel.setText("Episode: " + episode + "/" + 10);
            
            int aliveCount = (int) currentPopulation.stream().mapToInt(a -> a.alive ? 1 : 0).sum();
            aliveCountLabel.setText("Agents Alive: " + aliveCount + "/" + currentPopulation.size());
            
            // Calculate fitness stats
            double bestFitness = currentPopulation.stream()
                .mapToDouble(a -> a.generationFitness + a.episodeFitness)
                .max().orElse(0);
            double avgFitness = currentPopulation.stream()
                .mapToDouble(a -> a.generationFitness + a.episodeFitness)
                .average().orElse(0);
            
            bestFitnessLabel.setText(String.format("Best Fitness: %.2f", bestFitness));
            avgFitnessLabel.setText(String.format("Avg Fitness: %.2f", avgFitness));
            populationSizeLabel.setText("Population: " + currentPopulation.size() + " agents");
            
            // Repaint components
            graphPanel.repaint();
            agentTablePanel.repaint();
        });
    }
    
    /**
     * Called when a generation completes - updates evolution history
     */
    public void onGenerationComplete(int generation, List<EvolutionaryDQNTrainer.AgentData> population) {
        SwingUtilities.invokeLater(() -> {
            // Calculate generation stats
            double bestFitness = population.stream()
                .mapToDouble(a -> a.generationFitness)
                .max().orElse(0);
            double avgFitness = population.stream()
                .mapToDouble(a -> a.generationFitness)
                .average().orElse(0);
            
            // Add to generation history
            generationBestFitness.add((float) bestFitness);
            generationAvgFitness.add((float) avgFitness);
            
            // Keep history manageable
            while (generationBestFitness.size() > MAX_GENERATION_HISTORY) {
                generationBestFitness.remove(0);
                generationAvgFitness.remove(0);
            }
            
            // Update display
            generationLabel.setText("Generation: " + (generation + 1)); // Next generation
            System.out.println("📊 Stats Panel: Generation " + generation + " recorded - Best: " + bestFitness + ", Avg: " + avgFitness);
            
            // Repaint graph with new data
            graphPanel.repaint();
        });
    }
    
    /**
     * Panel showing evolution progress over generations
     */
    private class EvolutionGraphPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            // Background
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, width, height);
            
            // Title
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("🧬 Evolution Progress", 10, 20);
            
            if (generationBestFitness.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("Waiting for generation data...", 10, height / 2);
                return;
            }
            
            // Draw axes
            int margin = 30;
            int graphWidth = width - 2 * margin;
            int graphHeight = height - 2 * margin - 30;
            
            g2.setColor(Color.GRAY);
            g2.drawLine(margin, height - margin, width - margin, height - margin); // X-axis
            g2.drawLine(margin, height - margin, margin, margin + 30); // Y-axis
            
            // Find fitness range
            float minFitness = Float.MAX_VALUE;
            float maxFitness = Float.MIN_VALUE;
            for (float fitness : generationBestFitness) {
                minFitness = Math.min(minFitness, fitness);
                maxFitness = Math.max(maxFitness, fitness);
            }
            for (float fitness : generationAvgFitness) {
                minFitness = Math.min(minFitness, fitness);
                maxFitness = Math.max(maxFitness, fitness);
            }
            
            if (maxFitness == minFitness) {
                maxFitness = minFitness + 1;
            }
            
            // Draw best fitness curve
            if (generationBestFitness.size() > 1) {
                g2.setColor(Color.GREEN);
                g2.setStroke(new BasicStroke(2));
                for (int i = 1; i < generationBestFitness.size(); i++) {
                    float prev = generationBestFitness.get(i - 1);
                    float curr = generationBestFitness.get(i);
                    
                    int x1 = margin + (i - 1) * graphWidth / (generationBestFitness.size() - 1);
                    int y1 = height - margin - (int) ((prev - minFitness) / (maxFitness - minFitness) * graphHeight);
                    
                    int x2 = margin + i * graphWidth / (generationBestFitness.size() - 1);
                    int y2 = height - margin - (int) ((curr - minFitness) / (maxFitness - minFitness) * graphHeight);
                    
                    g2.drawLine(x1, y1, x2, y2);
                }
            }
            
            // Draw average fitness curve
            if (generationAvgFitness.size() > 1) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(1));
                for (int i = 1; i < generationAvgFitness.size(); i++) {
                    float prev = generationAvgFitness.get(i - 1);
                    float curr = generationAvgFitness.get(i);
                    
                    int x1 = margin + (i - 1) * graphWidth / (generationAvgFitness.size() - 1);
                    int y1 = height - margin - (int) ((prev - minFitness) / (maxFitness - minFitness) * graphHeight);
                    
                    int x2 = margin + i * graphWidth / (generationAvgFitness.size() - 1);
                    int y2 = height - margin - (int) ((curr - minFitness) / (maxFitness - minFitness) * graphHeight);
                    
                    g2.drawLine(x1, y1, x2, y2);
                }
            }
            
            // Legend
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.setColor(Color.GREEN);
            g2.drawString("Best", width - 60, 35);
            g2.setColor(Color.YELLOW);
            g2.drawString("Average", width - 60, 50);
            
            // Axis labels
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("%.1f", maxFitness), 5, margin + 35);
            g2.drawString(String.format("%.1f", minFitness), 5, height - margin - 5);
        }
    }
    
    /**
     * Panel showing individual agent performance
     */
    private class AgentTablePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            // Background
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, width, height);
            
            // Title
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("👥 Agent Performance", 10, 20);
            
            // Table headers
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.drawString("ID", 10, 40);
            g2.drawString("Status", 40, 40);
            g2.drawString("Fitness", 90, 40);
            g2.drawString("Kills", 150, 40);
            g2.drawString("HP", 190, 40);
            g2.drawString("Ammo", 220, 40);
            
            // Agent data
            if (population != null) {
                for (int i = 0; i < Math.min(population.size(), 20); i++) {
                    EvolutionaryDQNTrainer.AgentData agent = population.get(i);
                    int y = 55 + i * 15;
                    
                    // Color by agent
                    g2.setColor(AGENT_COLORS[i % AGENT_COLORS.length]);
                    g2.fillRect(5, y - 10, 8, 8);
                    
                    g2.setColor(Color.WHITE);
                    g2.drawString(String.valueOf(agent.id), 10, y);
                    g2.drawString(agent.alive ? "ALIVE" : "DEAD", 40, y);
                    g2.drawString(String.format("%.1f", agent.generationFitness + agent.episodeFitness), 90, y);
                    g2.drawString(String.valueOf(agent.asteroidsDestroyed), 150, y);
                    g2.drawString(String.valueOf(agent.health), 190, y);
                    g2.drawString(String.valueOf(agent.ammunition), 220, y);
                }
            }
        }
    }
}
