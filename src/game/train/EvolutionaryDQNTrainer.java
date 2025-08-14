package game.train;

import game.ai.dqn.DQNAgent;
import game.ai.AgentInput;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Hybrid Evolutionary-DQN Training System
 * 
 * Combines individual DQN learning with population-level evolutionary selection:
 * - 10 agents compete simultaneously in shared arena
 * - Each agent learns individually through backpropagation (DQN)
 * - Population evolves through selection: top 5 survive, duplicate, worst 5 eliminated
 * - Generations cycle every 10 episodes
 * 
 * This demonstrates advanced AI research: Evolutionary Reinforcement Learning!
 */
public class EvolutionaryDQNTrainer extends JFrame implements ActionListener {
    private final Random random = new Random();
    
    // Population of 10 agents
    private static final int POPULATION_SIZE = 10;  // Now 10 agents competing!
    private static final int SURVIVORS = 5;         // Top 5 survive
    private static final int EPISODES_PER_GENERATION = 10;
    private List<AgentData> population = new ArrayList<>();
    
    // Shared game environment
    private float[] asteroids = new float[8 * 6]; // 8 asteroids: x, y, vx, vy, size, distToPlayer
    private List<float[]> bullets = new ArrayList<>(); // bullets: x, y, vx, vy, timeLeft, agentId
    private List<float[]> powerups = new ArrayList<>(); // powerups: x, y, type, timeLeft
    
    // Evolution tracking
    private int currentGeneration = 1;
    private int currentEpisode = 0;
    private long episodeStartTime = 0;
    private final long EPISODE_DURATION = 8000; // 8 seconds per episode
    
    // Training speed controls
    private float speedMultiplier = 1.0f;
    private int frameSkip = 1;
    private int frameCounter = 0;
    
    // Visualization
    private boolean showVisualization = true;
    private GamePanel gamePanel;
    private EvolutionStatsPanel statsPanel;
    private Timer trainingTimer;
    private boolean isPaused = false;
    private boolean running = true;
    private long lastUpdateTime = System.currentTimeMillis();
    
    // Agent colors for visualization
    private final Color[] AGENT_COLORS = {
        Color.GREEN, Color.BLUE, Color.RED, Color.YELLOW, Color.MAGENTA,
        Color.CYAN, Color.ORANGE, Color.PINK, new Color(128, 255, 128), Color.WHITE
    };
    
    /**
     * Data structure to track individual agent performance
     */
    public static class AgentData {
        public int id;
        public DQNAgent agent;
        public float x, y, angle;
        public boolean alive;
        public int health;
        public int ammunition;
        public float generationFitness;
        public float episodeFitness;
        public int asteroidsDestroyed;
        public Color color;
        public float vx, vy;
        public float shootCooldown;
        
        public AgentData(int id, DQNAgent agent, Color color) {
            this.id = id;
            this.agent = agent;
            this.color = color;
            this.generationFitness = 0;
            this.episodeFitness = 0;
            this.asteroidsDestroyed = 0;
            reset();
        }
        
        public void reset() {
            this.alive = true;
            this.health = 3;
            this.ammunition = 50;
            this.episodeFitness = 0;
            this.vx = 0;
            this.vy = 0;
            this.shootCooldown = 0;
        }
    }
    
    public EvolutionaryDQNTrainer() {
        population = new ArrayList<>();
        initializePopulation();
        
        // Create UI
        setupUI();
        
        // Initialize environment
        initializeEpisode();
        
        // Start training
        trainingTimer = new Timer((int) (16 / speedMultiplier), this::trainingStep);
        trainingTimer.start();
        
        System.out.println("🧬 EVOLUTIONARY-DQN TRAINING STARTED 🧬");
        System.out.printf("Population: %d agents, Generations: every %d episodes\n", 
                         POPULATION_SIZE, EPISODES_PER_GENERATION);
        System.out.println("Watch 10 AI agents compete and evolve in real-time!");
    }
    
    private void initializePopulation() {
        population.clear();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            DQNAgent agent = new DQNAgent(8 + 8 * 6 + 4); // Enhanced state size
            Color color = AGENT_COLORS[i % AGENT_COLORS.length];
            population.add(new AgentData(i, agent, color));
        }
        resetAgentPositions();
    }
    
    private void resetAgentPositions() {
        // Distribute agents randomly across the arena
        for (AgentData agentData : population) {
            float x, y, angle;
            x = 0.1f + random.nextFloat() * 0.8f;
            y = 0.1f + random.nextFloat() * 0.8f;
            angle = random.nextFloat() * 360;
            agentData.x = x;
            agentData.y = y;
            agentData.angle = angle;
        }
    }
    
    private void setupUI() {
        setTitle("🧬 Evolutionary DQN - 10 Competing AI Agents");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        // Create game panel
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);
        
        // Create stats panel
        statsPanel = new EvolutionStatsPanel(population, currentGeneration);
        add(statsPanel, BorderLayout.EAST);
        
        // Create control panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(Color.DARK_GRAY);
        
        JButton pauseBtn = new JButton("Pause/Resume");
        pauseBtn.addActionListener(this);
        
        // Speed control buttons - now including 1x!
        JButton speed1xBtn = new JButton("1x Speed");
        speed1xBtn.addActionListener(e -> setSpeedMultiplier(1.0f));
        
        JButton speed2xBtn = new JButton("2x Speed");
        speed2xBtn.addActionListener(e -> setSpeedMultiplier(2.0f));
        
        JButton speed5xBtn = new JButton("5x Speed");
        speed5xBtn.addActionListener(e -> setSpeedMultiplier(5.0f));
        
        JButton speed10xBtn = new JButton("10x Speed");
        speed10xBtn.addActionListener(e -> setSpeedMultiplier(10.0f));
        
        JButton resetBtn = new JButton("New Population");
        resetBtn.addActionListener(e -> resetPopulation());
        
        JLabel infoLabel = new JLabel("🧬 Watch 10 AI agents evolve through competition!");
        infoLabel.setForeground(Color.WHITE);
        
        controlPanel.add(pauseBtn);
        controlPanel.add(speed1xBtn);
        controlPanel.add(speed2xBtn);
        controlPanel.add(speed5xBtn);
        controlPanel.add(speed10xBtn);
        controlPanel.add(resetBtn);
        controlPanel.add(infoLabel);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Pause/Resume")) {
            togglePause();
        }
    }
    
    private void togglePause() {
        running = !running;
        if (running) {
            System.out.println("Training RESUMED");
            lastUpdateTime = System.currentTimeMillis(); // Reset timing to prevent large time jumps
        } else {
            System.out.println("Training PAUSED");
        }
    }
    
    private void trainingStep(ActionEvent e) {
        if (!running) return;
        
        // Step each agent
        for (AgentData agent : population) {
            if (agent.alive) {
                stepAgent(agent);
            }
        }
        
        // Update environment
        updateEnvironment();
        
        // Check episode end
        if (checkEpisodeEnd()) {
            finishEpisode();
        }
        
        // Update visualization
        if (showVisualization && frameCounter % frameSkip == 0) {
            gamePanel.repaint();
            statsPanel.updateStats(population, currentGeneration, currentEpisode);
        }
        
        frameCounter++;
    }
    
    private void stepAgent(AgentData agent) {
        // Get agent decision
        float[] state = buildAgentState(agent);
        AgentInput input = agent.agent.decide(state);
        
        // Debug rotation decisions (only for first agent to avoid spam)
        if (agent.id == 0 && input.angleDelta != 0) {
            System.out.printf("Agent 0 ROTATING: angleDelta=%.1f, currentAngle=%.1f\n", 
                            input.angleDelta, agent.angle);
        }
        
        // Apply agent input with speed scaling
        applyAgentInput(agent, input);
        
        // Update agent position
        updateAgentPosition(agent);
        
        // Calculate reward
        float reward = calculateAgentReward(agent);
        agent.episodeFitness += reward;
        
        // Agent learns from experience
        agent.agent.learn(state, reward, !agent.alive);
    }
    
    private float[] buildAgentState(AgentData agent) {
        float[] state = new float[8 + 8 * 6 + 4];
        // Add agent's own state
        state[0] = agent.x;
        state[1] = agent.y;
        state[2] = agent.angle;
        state[3] = agent.health;
        state[4] = agent.ammunition;
        state[5] = agent.vx;
        state[6] = agent.vy;
        state[7] = agent.alive ? 1 : 0;
        
        // Add asteroid states
        for (int i = 0; i < 8; i++) {
            int baseIdx = i * 6;
            state[8 + baseIdx] = asteroids[baseIdx];
            state[8 + baseIdx + 1] = asteroids[baseIdx + 1];
            state[8 + baseIdx + 2] = asteroids[baseIdx + 2];
            state[8 + baseIdx + 3] = asteroids[baseIdx + 3];
            state[8 + baseIdx + 4] = asteroids[baseIdx + 4];
            state[8 + baseIdx + 5] = asteroids[baseIdx + 5];
        }
        
        return state;
    }
    
    private void applyAgentInput(AgentData agent, AgentInput input) {
        if (input == null) return;
        
        // Apply rotation with speed scaling
        if (input.angleDelta != 0) {
            agent.angle += input.angleDelta * speedMultiplier;
            
            // Normalize angle to 0-360 range
            while (agent.angle < 0) agent.angle += 360;
            while (agent.angle >= 360) agent.angle -= 360;
        }
        
        // Apply thrust with speed scaling
        if (input.thrustForward) {
            float thrust = 0.05f * speedMultiplier; // Reduced from 0.2f for observable speed
            agent.vx += Math.cos(Math.toRadians(agent.angle)) * thrust;
            agent.vy += Math.sin(Math.toRadians(agent.angle)) * thrust;
        }
        
        if (input.thrustBack) {
            float brake = 0.02f * speedMultiplier; // Reduced from 0.1f for observable speed
            agent.vx -= Math.cos(Math.toRadians(agent.angle)) * brake;
            agent.vy -= Math.sin(Math.toRadians(agent.angle)) * brake;
        }
        
        // Add velocity damping for more controlled movement
        agent.vx *= 0.98f;  // Slight damping to prevent excessive acceleration
        agent.vy *= 0.98f;
        
        // Apply shooting with cooldown
        if (input.shoot && agent.shootCooldown <= 0) {
            fireBullet(agent);
            agent.shootCooldown = 10 / speedMultiplier;
        }
        
        // Update cooldowns
        if (agent.shootCooldown > 0) {
            agent.shootCooldown -= speedMultiplier;
        }
    }
    
    private void updateAgentPosition(AgentData agent) {
        // Update position
        agent.x += agent.vx * speedMultiplier;
        agent.y += agent.vy * speedMultiplier;
        
        // Bounce off walls
        if (agent.x < 0.02f) {
            agent.x = 0.02f;
            agent.vx = -agent.vx * 0.8f;
        }
        if (agent.x > 0.98f) {
            agent.x = 0.98f;
            agent.vx = -agent.vx * 0.8f;
        }
        if (agent.y < 0.02f) {
            agent.y = 0.02f;
            agent.vy = -agent.vy * 0.8f;
        }
        if (agent.y > 0.98f) {
            agent.y = 0.98f;
            agent.vy = -agent.vy * 0.8f;
        }
    }
    
    private void updateEnvironment() {
        // Update asteroids
        for (int i = 0; i < 8; i++) {
            int baseIdx = i * 6;
            asteroids[baseIdx] += asteroids[baseIdx + 2] * speedMultiplier;
            asteroids[baseIdx + 1] += asteroids[baseIdx + 3] * speedMultiplier;
            
            // Wrap asteroids
            if (asteroids[baseIdx] < 0 || asteroids[baseIdx] > 1) asteroids[baseIdx + 2] *= -1;
            if (asteroids[baseIdx + 1] < 0 || asteroids[baseIdx + 1] > 1) asteroids[baseIdx + 3] *= -1;
            
            // Update distances to all agents
            for (AgentData agent : population) {
                if (agent.alive) {
                    float dx = asteroids[baseIdx] - agent.x;
                    float dy = asteroids[baseIdx + 1] - agent.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    
                    // Check collision
                    if (distance < asteroids[baseIdx + 4]) {
                        agent.health--;
                        if (agent.health <= 0) {
                            agent.alive = false;
                        }
                    }
                }
            }
            asteroids[baseIdx + 5] = 0; // Reset distance field (not used for multiple agents)
        }
        
        // Update bullets
        for (int i = bullets.size() - 1; i >= 0; i--) {
            float[] bullet = bullets.get(i);
            bullet[0] += bullet[2] * speedMultiplier;
            bullet[1] += bullet[3] * speedMultiplier;
            bullet[4]--;
            
            boolean bulletRemoved = false;
            
            // Check collisions with asteroids
            for (int j = 0; j < 8; j++) {
                int baseIdx = j * 6;
                float dx = bullet[0] - asteroids[baseIdx];
                float dy = bullet[1] - asteroids[baseIdx + 1];
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance < asteroids[baseIdx + 4]) {
                    // Asteroid hit!
                    int agentId = (int) bullet[5];
                    if (agentId < population.size()) {
                        population.get(agentId).asteroidsDestroyed++;
                        population.get(agentId).episodeFitness += 10.0f; // Reward for hit
                    }
                    bullets.remove(i);
                    bulletRemoved = true;
                    respawnAsteroid(j);
                    break;
                }
            }
            
            // Remove bullet if out of bounds or expired (only if not already removed)
            if (!bulletRemoved && (bullet[4] <= 0 || bullet[0] < 0 || bullet[0] > 1 || bullet[1] < 0 || bullet[1] > 1)) {
                bullets.remove(i);
            }
        }
    }
    
    private void respawnAsteroid(int index) {
        int baseIdx = index * 6;
        asteroids[baseIdx] = random.nextFloat();
        asteroids[baseIdx + 1] = random.nextFloat();
        asteroids[baseIdx + 2] = (random.nextFloat() - 0.5f) * 0.01f;
        asteroids[baseIdx + 3] = (random.nextFloat() - 0.5f) * 0.01f;
        asteroids[baseIdx + 4] = 0.02f;
        asteroids[baseIdx + 5] = 0;
    }
    
    private float calculateAgentReward(AgentData agent) {
        if (!agent.alive) return -10.0f;
        
        float reward = 0.1f; // Base survival
        
        // Proximity penalties
        for (int i = 0; i < 8; i++) {
            float dx = asteroids[i * 6] - agent.x;
            float dy = asteroids[i * 6 + 1] - agent.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < 0.1f) {
                reward -= 2.0f;
            } else if (distance < 0.2f) {
                reward -= 0.5f;
            }
        }
        
        // Movement reward
        float movement = Math.abs(agent.vx) + Math.abs(agent.vy);
        reward += movement * 0.1f;
        
        // Health bonus
        reward += agent.health * 0.2f;
        
        return reward;
    }
    
    private boolean checkEpisodeEnd() {
        long elapsed = (long) ((System.currentTimeMillis() - episodeStartTime) * speedMultiplier);
        boolean allDead = population.stream().noneMatch(agent -> agent.alive);
        return elapsed > EPISODE_DURATION || allDead;
    }
    
    private void finishEpisode() {
        currentEpisode++;
        
        // Update generation fitness
        for (AgentData agent : population) {
            agent.generationFitness += agent.episodeFitness;
        }
        
        System.out.printf("Episode %d/%d completed - ", currentEpisode, EPISODES_PER_GENERATION);
        float avgFitness = (float) population.stream().mapToDouble(a -> a.episodeFitness).average().orElse(0);
        float maxFitness = (float) population.stream().mapToDouble(a -> a.episodeFitness).max().orElse(0);
        System.out.printf("Avg: %.2f, Max: %.2f\n", avgFitness, maxFitness);
        
        // Check if generation is complete
        if (currentEpisode >= EPISODES_PER_GENERATION) {
            evolvePopulation();
            currentEpisode = 0;
            currentGeneration++;
        }
        
        initializeEpisode();
    }
    
    private void evolvePopulation() {
        System.out.println("🧬 EVOLUTION: Generation " + currentGeneration + " complete!");
        
        // Sort by fitness
        population.sort((a, b) -> Float.compare(b.generationFitness, a.generationFitness));
        
        // Print generation results
        System.out.println("Top performers:");
        for (int i = 0; i < Math.min(5, population.size()); i++) {
            System.out.printf("  Agent %d: %.2f fitness, %d asteroids\n", 
                             population.get(i).id, population.get(i).generationFitness, 
                             population.get(i).asteroidsDestroyed);
        }
        
        // Keep top SURVIVORS
        List<AgentData> newPopulation = new ArrayList<>();
        for (int i = 0; i < SURVIVORS && i < population.size(); i++) {
            newPopulation.add(population.get(i));
            population.get(i).generationFitness = 0; // Reset for next generation
        }
        
        // Duplicate top performers to fill population
        while (newPopulation.size() < POPULATION_SIZE) {
            int parentIndex = random.nextInt(SURVIVORS);
            AgentData parent = newPopulation.get(parentIndex);
            AgentData child = new AgentData(parent.id, parent.agent.copy(), parent.color);
            newPopulation.add(child);
        }
        
        population = newPopulation;
        
        // Re-assign IDs and colors properly
        for (int i = 0; i < population.size(); i++) {
            population.get(i).id = i;
            population.get(i).color = AGENT_COLORS[i % AGENT_COLORS.length]; // Fix color assignment
        }
        
        System.out.printf("🧬 Generation %d: Top %d survived, %d offspring created\n", 
                         currentGeneration, SURVIVORS, POPULATION_SIZE - SURVIVORS);
        
        // Update stats panel with generation data
        if (statsPanel != null) {
            statsPanel.onGenerationComplete(currentGeneration, population);
        }
    }
    
    private void initializeEpisode() {
        episodeStartTime = System.currentTimeMillis();
        
        // Reset all agents
        for (AgentData agent : population) {
            agent.reset();
        }
        
        // Reset environment
        for (int i = 0; i < 8; i++) {
            respawnAsteroid(i);
        }
        bullets.clear();
        powerups.clear();
    }
    
    private void resetPopulation() {
        population.clear();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            DQNAgent agent = new DQNAgent(8 + 8 * 6 + 4); // Enhanced state size
            Color color = AGENT_COLORS[i % AGENT_COLORS.length];
            population.add(new AgentData(i, agent, color));
        }
        currentGeneration = 1;
        currentEpisode = 0;
        initializeEpisode();
    }
    
    private void setSpeedMultiplier(float multiplier) {
        speedMultiplier = multiplier;
        trainingTimer.setDelay((int) (16 / speedMultiplier));
    }
    
    private void fireBullet(AgentData agent) {
        float bulletVelX = 0.05f * (float) Math.cos(Math.toRadians(agent.angle));
        float bulletVelY = 0.05f * (float) Math.sin(Math.toRadians(agent.angle));
        bullets.add(new float[] {agent.x, agent.y, bulletVelX, bulletVelY, 100, agent.id});
        agent.ammunition--;
    }
    
    /**
     * Game visualization panel showing all 10 agents competing
     */
    private class GamePanel extends JPanel {
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
            
            // Draw asteroids
            g2.setColor(Color.RED);
            for (int i = 0; i < 8; i++) {
                int x = (int) (asteroids[i * 6] * width);
                int y = (int) (asteroids[i * 6 + 1] * height);
                int size = (int) (asteroids[i * 6 + 4] * width * 2);
                g2.fillOval(x - size/2, y - size/2, size, size);
            }
            
            // Draw bullets
            g2.setColor(Color.WHITE);
            for (float[] bullet : bullets) {
                int x = (int) (bullet[0] * width);
                int y = (int) (bullet[1] * height);
                g2.fillOval(x - 1, y - 1, 2, 2);
            }
            
            // Draw agents with different colors
            for (int i = 0; i < population.size(); i++) {
                AgentData agent = population.get(i);
                if (!agent.alive) continue;
                
                g2.setColor(agent.color);
                int px = (int) (agent.x * width);
                int py = (int) (agent.y * height);
                
                // Draw agent
                g2.fillOval(px - 4, py - 4, 8, 8);
                
                // Draw direction indicator
                int dx = (int) (Math.cos(Math.toRadians(agent.angle)) * 60); // Increased length for better visibility
                int dy = (int) (Math.sin(Math.toRadians(agent.angle)) * 60); // Increased length for better visibility
                g2.setStroke(new BasicStroke(2)); // Thicker line for better visibility
                g2.drawLine(px, py, px + dx, py + dy);
                
                // Draw agent ID
                g2.setColor(Color.WHITE);
                g2.drawString(String.valueOf(i), px + 6, py - 6);
            }
            
            // Draw info
            g2.setColor(Color.WHITE);
            g2.drawString("🧬 Generation: " + currentGeneration, 10, 20);
            g2.drawString("Episode: " + currentEpisode + "/" + EPISODES_PER_GENERATION, 10, 35);
            g2.drawString("Agents Alive: " + population.stream().mapToInt(a -> a.alive ? 1 : 0).sum(), 10, 50);
        }
    }
}
