package game.ai;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class AIStatsPanel extends JPanel {
    
    private QLearningAgent agent;
    private JLabel episodeLabel;
    private JLabel rewardLabel;
    private JLabel epsilonLabel;
    private JLabel stepsLabel;
    private JLabel learningStatusLabel;
    private JButton toggleLearningButton;
    private JButton resetButton;
    
    // Graph components
    private RewardGraphPanel rewardGraph;
    private List<Double> rewardHistory;
    private final int MAX_HISTORY_SIZE = 100;
    
    private DecimalFormat df = new DecimalFormat("#.####");
    private Timer updateTimer;
    
    public AIStatsPanel() {
        this.rewardHistory = new ArrayList<>();
        initializeComponents();
        setupLayout();
        startUpdateTimer();
    }
    
    public void setAgent(QLearningAgent agent) {
        this.agent = agent;
    }
    
    private void initializeComponents() {
        setPreferredSize(new Dimension(300, 600));
        setBorder(new TitledBorder("Q-Learning AI Statistics"));
        setBackground(Color.BLACK);
        
        // Create labels
        episodeLabel = createLabel("Episodes: 0");
        rewardLabel = createLabel("Avg Reward: 0.0000");
        epsilonLabel = createLabel("Exploration (ε): 0.1000");
        stepsLabel = createLabel("Steps: 0");
        learningStatusLabel = createLabel("Status: Learning");
        learningStatusLabel.setForeground(Color.GREEN);
        
        // Create buttons
        toggleLearningButton = new JButton("Pause Learning");
        toggleLearningButton.setBackground(Color.DARK_GRAY);
        toggleLearningButton.setForeground(Color.WHITE);
        toggleLearningButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (agent != null) {
                    boolean currentStatus = agent.isLearningEnabled();
                    agent.setLearningEnabled(!currentStatus);
                    updateLearningStatus(!currentStatus);
                }
            }
        });
        
        resetButton = new JButton("Reset Learning");
        resetButton.setBackground(Color.DARK_GRAY);
        resetButton.setForeground(Color.WHITE);
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (agent != null) {
                    // Reset the agent (would need to add this method)
                    rewardHistory.clear();
                    rewardGraph.updateData(rewardHistory);
                }
            }
        });
        
        // Create reward graph
        rewardGraph = new RewardGraphPanel();
        rewardGraph.setPreferredSize(new Dimension(280, 200));
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Consolas", Font.PLAIN, 12));
        return label;
    }
    
    private void setupLayout() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // Statistics section
        JPanel statsPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        statsPanel.setBackground(Color.BLACK);
        statsPanel.add(episodeLabel);
        statsPanel.add(rewardLabel);
        statsPanel.add(epsilonLabel);
        statsPanel.add(stepsLabel);
        statsPanel.add(learningStatusLabel);
        
        // Control buttons section
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        buttonPanel.setBackground(Color.BLACK);
        buttonPanel.add(toggleLearningButton);
        buttonPanel.add(resetButton);
        
        // Add components
        add(Box.createVerticalStrut(10));
        add(statsPanel);
        add(Box.createVerticalStrut(10));
        add(buttonPanel);
        add(Box.createVerticalStrut(10));
        
        // Add graph
        JPanel graphContainer = new JPanel(new BorderLayout());
        graphContainer.setBackground(Color.BLACK);
        graphContainer.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.GRAY), 
                                                   "Reward History", TitledBorder.CENTER, 
                                                   TitledBorder.TOP, null, Color.WHITE));
        graphContainer.add(rewardGraph, BorderLayout.CENTER);
        add(graphContainer);
        
        add(Box.createVerticalGlue());
    }
    
    private void startUpdateTimer() {
        updateTimer = new Timer(500, new ActionListener() { // Update every 500ms
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStats();
            }
        });
        updateTimer.start();
    }
    
    private void updateStats() {
        if (agent == null) return;
        
        episodeLabel.setText("Episodes: " + agent.getEpisodeCount());
        rewardLabel.setText("Avg Reward: " + df.format(agent.getAverageReward()));
        epsilonLabel.setText("Exploration (ε): " + df.format(agent.getCurrentEpsilon()));
        stepsLabel.setText("Steps: " + agent.getStepCount());
        
        // Update reward history
        if (agent.getEpisodeCount() > rewardHistory.size()) {
            rewardHistory.add(agent.getAverageReward());
            
            // Keep history size manageable
            if (rewardHistory.size() > MAX_HISTORY_SIZE) {
                rewardHistory.remove(0);
            }
            
            rewardGraph.updateData(rewardHistory);
        }
    }
    
    private void updateLearningStatus(boolean learning) {
        if (learning) {
            learningStatusLabel.setText("Status: Learning");
            learningStatusLabel.setForeground(Color.GREEN);
            toggleLearningButton.setText("Pause Learning");
        } else {
            learningStatusLabel.setText("Status: Exploiting");
            learningStatusLabel.setForeground(Color.YELLOW);
            toggleLearningButton.setText("Resume Learning");
        }
    }
    
    public void stop() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }
    
    // Inner class for the reward graph
    private class RewardGraphPanel extends JPanel {
        private List<Double> data;
        private double minReward = Double.MAX_VALUE;
        private double maxReward = Double.MIN_VALUE;
        
        public RewardGraphPanel() {
            setBackground(Color.BLACK);
            data = new ArrayList<>();
        }
        
        public void updateData(List<Double> newData) {
            this.data = new ArrayList<>(newData);
            
            // Update min/max for scaling
            minReward = Double.MAX_VALUE;
            maxReward = Double.MIN_VALUE;
            
            for (Double reward : data) {
                if (reward < minReward) minReward = reward;
                if (reward > maxReward) maxReward = reward;
            }
            
            // Ensure some range for display
            if (maxReward - minReward < 0.1) {
                double center = (maxReward + minReward) / 2;
                minReward = center - 0.05;
                maxReward = center + 0.05;
            }
            
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (data.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("No data yet...", 10, getHeight() / 2);
                return;
            }
            
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            int margin = 20;
            
            // Draw axes
            g2.setColor(Color.GRAY);
            g2.drawLine(margin, height - margin, width - margin, height - margin); // X-axis
            g2.drawLine(margin, margin, margin, height - margin); // Y-axis
            
            // Draw grid lines
            g2.setColor(Color.DARK_GRAY);
            for (int i = 1; i < 5; i++) {
                int y = margin + i * (height - 2 * margin) / 5;
                g2.drawLine(margin, y, width - margin, y);
            }
            
            // Draw reward line
            if (data.size() > 1) {
                g2.setColor(Color.CYAN);
                g2.setStroke(new BasicStroke(2f));
                
                for (int i = 0; i < data.size() - 1; i++) {
                    int x1 = margin + i * (width - 2 * margin) / Math.max(1, data.size() - 1);
                    int x2 = margin + (i + 1) * (width - 2 * margin) / Math.max(1, data.size() - 1);
                    
                    int y1 = height - margin - (int) ((data.get(i) - minReward) / (maxReward - minReward) * (height - 2 * margin));
                    int y2 = height - margin - (int) ((data.get(i + 1) - minReward) / (maxReward - minReward) * (height - 2 * margin));
                    
                    g2.drawLine(x1, y1, x2, y2);
                }
            }
            
            // Draw labels
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString(df.format(maxReward), 2, margin + 10);
            g2.drawString(df.format(minReward), 2, height - margin - 5);
            g2.drawString("Episodes", width / 2 - 20, height - 5);
        }
    }
}
