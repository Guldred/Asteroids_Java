package game.train;

import game.ai.dqn.DQNAgent;
import game.ai.dqn.ExperienceReplay;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Real-time statistics panel showing DQN learning progress.
 * 
 * Displays:
 * - Episode rewards over time (learning curve)
 * - Current exploration rate (epsilon)
 * - Network training statistics
 * - Experience replay buffer status
 * - Live performance metrics
 */
public class DQNStatsPanel extends JPanel {
    private final DQNAgent agent;
    private final List<Float> rewardHistory;
    private final List<Float> epsilonHistory;
    private final int MAX_HISTORY = 100; // Show last 100 episodes
    private long lastUpdateTime = 0;
    private int lastRecordedEpisode = 0;
    
    // UI components
    private JLabel episodeLabel;
    private JLabel avgRewardLabel;
    private JLabel epsilonLabel;
    private JLabel bufferSizeLabel;
    private JLabel networkUpdatesLabel;
    private JPanel graphPanel;
    
    public DQNStatsPanel(DQNAgent agent) {
        this.agent = agent;
        this.rewardHistory = new ArrayList<>();
        this.epsilonHistory = new ArrayList<>();
        
        setupUI();
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(350, 600));
        setBackground(Color.DARK_GRAY);
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.CYAN, 2), 
            "DQN Learning Statistics",
            0, 0, new Font("Arial", Font.BOLD, 14), Color.CYAN));
        
        // Create info panel (top)
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.NORTH);
        
        // Create graph panel (center)
        graphPanel = new GraphPanel();
        add(graphPanel, BorderLayout.CENTER);
        
        // Create controls panel (bottom)
        JPanel controlsPanel = createControlsPanel();
        add(controlsPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(8, 1, 5, 5));
        panel.setBackground(Color.DARK_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create labels with consistent styling
        episodeLabel = createStyledLabel("Episodes: 0");
        avgRewardLabel = createStyledLabel("Avg Reward: 0.00");
        epsilonLabel = createStyledLabel("Exploration (ε): 1.000");
        bufferSizeLabel = createStyledLabel("Experience Buffer: 0/10000");
        networkUpdatesLabel = createStyledLabel("Network Updates: 0");
        
        // Add network architecture info
        JLabel archLabel = createStyledLabel("Architecture: Input→64→64→6");
        JLabel learningLabel = createStyledLabel("Learning Rate: 0.001");
        JLabel gammaLabel = createStyledLabel("Discount (γ): 0.99");
        
        panel.add(episodeLabel);
        panel.add(avgRewardLabel);
        panel.add(epsilonLabel);
        panel.add(bufferSizeLabel);
        panel.add(networkUpdatesLabel);
        panel.add(archLabel);
        panel.add(learningLabel);
        panel.add(gammaLabel);
        
        return panel;
    }
    
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return label;
    }
    
    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBackground(Color.DARK_GRAY);
        
        // Add control instructions
        JLabel instructions = new JLabel("<html><center>" +
            "SPACE: Pause/Resume<br>" +
            "S: Toggle Stats<br>" +
            "R: Reset Agent<br>" +
            "ESC: Exit" +
            "</center></html>");
        instructions.setForeground(Color.LIGHT_GRAY);
        instructions.setFont(new Font("Arial", Font.PLAIN, 10));
        
        panel.add(instructions);
        return panel;
    }
    
    /**
     * Update all statistics from the agent
     */
    public void updateStats() {
        // Only update at reasonable intervals to prevent graph spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < 100) { // Max 10 updates per second
            return;
        }
        lastUpdateTime = currentTime;
        
        SwingUtilities.invokeLater(() -> {
            // Update text labels
            episodeLabel.setText("Episodes: " + agent.getEpisodeCount());
            avgRewardLabel.setText(String.format("Avg Reward: %.2f", agent.getAverageReward()));
            epsilonLabel.setText(String.format("Exploration (ε): %.3f", agent.getEpsilon()));
            
            ExperienceReplay buffer = agent.getReplayBuffer();
            bufferSizeLabel.setText(String.format("Experience Buffer: %d/10000", buffer.size()));
            networkUpdatesLabel.setText("Network Updates: " + agent.getUpdateCount());
            
            // Update reward history for graph (only once per episode)
            int currentEpisode = agent.getEpisodeCount();
            if (currentEpisode > lastRecordedEpisode && currentEpisode > 0) {
                rewardHistory.add(agent.getLastEpisodeReward());
                epsilonHistory.add(agent.getEpsilon());
                lastRecordedEpisode = currentEpisode;
                
                // Keep only recent history
                while (rewardHistory.size() > MAX_HISTORY) {
                    rewardHistory.remove(0);
                    epsilonHistory.remove(0);
                }
                
                // Only repaint when we actually add new data
                graphPanel.repaint();
            }
        });
    }
    
    /**
     * Custom panel for drawing learning curves
     */
    private class GraphPanel extends JPanel {
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
            
            // Draw title
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.drawString("Learning Progress", 10, 20);
            
            if (rewardHistory.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("Waiting for training data...", 10, height / 2);
                return;
            }
            
            // Draw axes
            int margin = 30;
            int graphWidth = width - 2 * margin;
            int graphHeight = height - 2 * margin - 30; // Extra space for title
            
            g2.setColor(Color.GRAY);
            g2.drawLine(margin, height - margin, width - margin, height - margin); // X-axis
            g2.drawLine(margin, height - margin, margin, margin + 30); // Y-axis
            
            // Draw reward curve
            if (rewardHistory.size() > 1) {
                drawRewardCurve(g2, margin, graphWidth, graphHeight, height);
            }
            
            // Draw epsilon curve  
            if (epsilonHistory.size() > 1) {
                drawEpsilonCurve(g2, margin, graphWidth, graphHeight, height);
            }
            
            // Draw legend
            drawLegend(g2, width, margin);
        }
        
        private void drawRewardCurve(Graphics2D g2, int margin, int graphWidth, int graphHeight, int totalHeight) {
            g2.setColor(Color.GREEN);
            g2.setStroke(new BasicStroke(2));
            
            // Find reward range
            float minReward = Float.MAX_VALUE;
            float maxReward = Float.MIN_VALUE;
            for (float reward : rewardHistory) {
                minReward = Math.min(minReward, reward);
                maxReward = Math.max(maxReward, reward);
            }
            
            // Avoid division by zero
            if (maxReward == minReward) {
                maxReward = minReward + 1;
            }
            
            // Draw line segments
            for (int i = 1; i < rewardHistory.size(); i++) {
                float prevReward = rewardHistory.get(i - 1);
                float currReward = rewardHistory.get(i);
                
                int x1 = margin + (i - 1) * graphWidth / (rewardHistory.size() - 1);
                int y1 = totalHeight - margin - (int) ((prevReward - minReward) / (maxReward - minReward) * graphHeight);
                
                int x2 = margin + i * graphWidth / (rewardHistory.size() - 1);
                int y2 = totalHeight - margin - (int) ((currReward - minReward) / (maxReward - minReward) * graphHeight);
                
                g2.drawLine(x1, y1, x2, y2);
            }
            
            // Draw axis labels
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString(String.format("%.1f", maxReward), 5, margin + 35);
            g2.drawString(String.format("%.1f", minReward), 5, totalHeight - margin - 5);
        }
        
        private void drawEpsilonCurve(Graphics2D g2, int margin, int graphWidth, int graphHeight, int totalHeight) {
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(1));
            
            // Epsilon is always 0.0 to 1.0, so we can use fixed scale
            for (int i = 1; i < epsilonHistory.size(); i++) {
                float prevEpsilon = epsilonHistory.get(i - 1);
                float currEpsilon = epsilonHistory.get(i);
                
                int x1 = margin + (i - 1) * graphWidth / (epsilonHistory.size() - 1);
                int y1 = totalHeight - margin - (int) (prevEpsilon * graphHeight);
                
                int x2 = margin + i * graphWidth / (epsilonHistory.size() - 1);
                int y2 = totalHeight - margin - (int) (currEpsilon * graphHeight);
                
                g2.drawLine(x1, y1, x2, y2);
            }
        }
        
        private void drawLegend(Graphics2D g2, int width, int margin) {
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            
            // Reward line legend
            g2.setColor(Color.GREEN);
            g2.drawLine(width - 120, margin + 40, width - 100, margin + 40);
            g2.setColor(Color.WHITE);
            g2.drawString("Episode Reward", width - 95, margin + 44);
            
            // Epsilon line legend
            g2.setColor(Color.YELLOW);
            g2.drawLine(width - 120, margin + 55, width - 100, margin + 55);
            g2.setColor(Color.WHITE);
            g2.drawString("Exploration (ε)", width - 95, margin + 59);
        }
    }
}
