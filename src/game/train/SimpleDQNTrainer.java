package game.train;

import game.ai.dqn.DQNAgent;
import game.ai.AgentInput;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simplified DQN Trainer - immediate live training without GameCore integration issues.
 * 
 * This creates a minimal training environment where you can watch the AI learn
 * basic decision making in real-time. Perfect for demonstration and learning!
 */
public class SimpleDQNTrainer extends JFrame {
    private final DQNAgent agent;
    private final DQNStatsPanel statsPanel;
    private final JPanel gamePanel;
    private final Timer trainingTimer;
    private final Random random = new Random();
    
    // Simplified game state
    private float playerX = 0.5f, playerY = 0.5f, playerAngle = 0.0f;
    private float playerVelX = 0.0f, playerVelY = 0.0f;
    private float[] asteroids = new float[8 * 6]; // 8 asteroids: x, y, vx, vy, size, distToPlayer
    private List<float[]> bullets = new ArrayList<>(); // bullets: x, y, vx, vy, timeLeft
    private List<float[]> powerups = new ArrayList<>(); // powerups: x, y, type, timeLeft
    private int asteroidsDestroyed = 0;
    private int lastAsteroidsDestroyed = 0; // Track previous count for reward calculation
    
    // Realistic game mechanics
    private long lastFireTime = 0;
    private final long FIRE_COOLDOWN = 200; // 200ms between shots
    private int ammunition = 50; // Limited ammo
    private int playerHealth = 3; // Health system instead of instant death
    private int score = 0;
    private boolean hasShield = false;
    private long shieldEndTime = 0;
    private boolean hasRapidFire = false;
    private long rapidFireEndTime = 0;
    
    // Episode management
    private int episodeCount = 0;
    private float episodeReward = 0;
    private long episodeStartTime;
    private final long EPISODE_DURATION = 8000; // 8 seconds per episode
    
    // Training speed controls
    private float speedMultiplier = 1.0f; // 1x, 2x, 5x, 10x speed
    private int frameSkip = 1; // Skip visual frames for speed
    private int frameCounter = 0;
    private boolean turboMode = false; // Maximum speed training
    
    // Visualization
    private boolean showVisualization = true;
    
    public SimpleDQNTrainer() {
        // Create DQN agent (same state size as your main game)
        int stateSize = 8 + 8 * 6 + 4; // player + asteroids + tactical info
        this.agent = new DQNAgent(stateSize);
        
        // Create UI components
        this.statsPanel = new DQNStatsPanel(agent);
        this.gamePanel = new GameVisualizationPanel();
        
        // Initialize game state
        initializeEpisode();
        
        // Setup UI
        setupUI();
        
        // Create training timer (60 FPS)
        trainingTimer = new Timer((int) (16 / speedMultiplier), this::trainingStep);
        trainingTimer.start();
        
        System.out.println("=== Simplified DQN Live Training Started ===");
        System.out.println("Watch the AI learn decision-making in real-time!");
        System.out.println("Green dot = AI player, Red circles = Asteroids");
    }
    
    private void setupUI() {
        setTitle("DQN Live Training - Simplified Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Game visualization (left)
        gamePanel.setPreferredSize(new Dimension(600, 600));
        gamePanel.setBackground(Color.BLACK);
        add(gamePanel, BorderLayout.CENTER);
        
        // Statistics (right)
        add(statsPanel, BorderLayout.EAST);
        
        // Controls (bottom)
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(Color.DARK_GRAY);
        
        JButton pauseBtn = new JButton("Pause/Resume");
        pauseBtn.addActionListener(e -> togglePause());
        
        JButton resetBtn = new JButton("Reset Agent");
        resetBtn.addActionListener(e -> resetAgent());
        
        JButton speed1xBtn = new JButton("1x Speed");
        speed1xBtn.addActionListener(e -> setSpeedMultiplier(1.0f));
        
        JButton speed2xBtn = new JButton("2x Speed");
        speed2xBtn.addActionListener(e -> setSpeedMultiplier(2.0f));
        
        JButton speed5xBtn = new JButton("5x Speed");
        speed5xBtn.addActionListener(e -> setSpeedMultiplier(5.0f));
        
        JButton speed10xBtn = new JButton("10x Speed");
        speed10xBtn.addActionListener(e -> setSpeedMultiplier(10.0f));
        
        JButton turboModeBtn = new JButton("Turbo Mode");
        turboModeBtn.addActionListener(e -> toggleTurboMode());
        
        JLabel infoLabel = new JLabel("Watch AI learn to avoid red asteroids!");
        infoLabel.setForeground(Color.WHITE);
        
        controlPanel.add(pauseBtn);
        controlPanel.add(resetBtn);
        controlPanel.add(speed1xBtn);
        controlPanel.add(speed2xBtn);
        controlPanel.add(speed5xBtn);
        controlPanel.add(speed10xBtn);
        controlPanel.add(turboModeBtn);
        controlPanel.add(infoLabel);
        add(controlPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    /**
     * Single training step - called 60 times per second
     */
    private void trainingStep(ActionEvent e) {
        // Build current state vector
        float[] state = buildState();
        
        // Agent makes decision
        AgentInput input = agent.decide(state);
        
        // Apply agent's decision to game state
        applyAgentInput(input);
        
        // Update game physics
        updateGameState();
        
        // Calculate reward
        float reward = calculateReward();
        episodeReward += reward;
        
        // Check if episode should end
        boolean done = checkEpisodeEnd();
        
        // Agent learns from this step
        agent.learn(state, reward, done);
        
        // Handle episode end
        if (done) {
            finishEpisode();
        }
        
        // Update visualization
        if (showVisualization && frameCounter % frameSkip == 0) {
            gamePanel.repaint();
            statsPanel.updateStats();
        }
        
        frameCounter++;
    }
    
    private float[] buildState() {
        float[] state = new float[8 + 8 * 6 + 4]; // player + asteroids + tactical info
        int idx = 0;
        
        // Player state (8 values)
        state[idx++] = playerX;
        state[idx++] = playerY;
        state[idx++] = playerVelX;
        state[idx++] = playerVelY;
        state[idx++] = (float) Math.sin(Math.toRadians(playerAngle));
        state[idx++] = (float) Math.cos(Math.toRadians(playerAngle));
        state[idx++] = playerHealth / 3.0f; // Normalized health (0-1)
        state[idx++] = ammunition / 50.0f; // Normalized ammunition (0-1)
        
        // Asteroids (8 × 6 = 48 values)
        System.arraycopy(asteroids, 0, state, idx, 48);
        idx += 48;
        
        // Tactical information (4 values)
        float closestDistance = Float.MAX_VALUE;
        float closestDx = 0, closestDy = 0;
        for (int i = 0; i < 8; i++) {
            float distance = asteroids[i * 6 + 5];
            if (distance < closestDistance) {
                closestDistance = distance;
                closestDx = asteroids[i * 6] - playerX;
                closestDy = asteroids[i * 6 + 1] - playerY;
            }
        }
        state[idx++] = Math.min(closestDistance, 1.0f); // Closest threat distance
        state[idx++] = closestDx; // Direction to closest threat
        state[idx++] = closestDy;
        state[idx++] = (System.currentTimeMillis() - lastFireTime) / (float)FIRE_COOLDOWN; // Shoot readiness (0-1+)
        
        return state;
    }
    
    private void applyAgentInput(AgentInput input) {
        // Apply thrust
        if (input.thrustForward) {
            playerVelX += 0.01f * (float) Math.cos(Math.toRadians(playerAngle)) * speedMultiplier;
            playerVelY += 0.01f * (float) Math.sin(Math.toRadians(playerAngle)) * speedMultiplier;
        }
        if (input.thrustBack) {
            playerVelX -= 0.005f * (float) Math.cos(Math.toRadians(playerAngle)) * speedMultiplier;
            playerVelY -= 0.005f * (float) Math.sin(Math.toRadians(playerAngle)) * speedMultiplier;
        }
        
        // Apply rotation
        playerAngle += input.angleDelta * speedMultiplier;
        
        // Limit velocity
        float maxVel = 0.02f;
        float velMag = (float) Math.sqrt(playerVelX * playerVelX + playerVelY * playerVelY);
        if (velMag > maxVel) {
            playerVelX = playerVelX / velMag * maxVel;
            playerVelY = playerVelY / velMag * maxVel;
        }
        
        // Fire bullet
        long currentFireCooldown = hasRapidFire ? FIRE_COOLDOWN / 3 : FIRE_COOLDOWN;
        currentFireCooldown = (long) (currentFireCooldown / speedMultiplier); // Scale cooldown with speed
        if (input.shoot && System.currentTimeMillis() - lastFireTime > currentFireCooldown && ammunition > 0) {
            float bulletVelX = 0.05f * (float) Math.cos(Math.toRadians(playerAngle));
            float bulletVelY = 0.05f * (float) Math.sin(Math.toRadians(playerAngle));
            bullets.add(new float[] {playerX, playerY, bulletVelX, bulletVelY, 100});
            lastFireTime = System.currentTimeMillis();
            ammunition--;
        }
    }
    
    private void updateGameState() {
        // Update player position
        playerX += playerVelX * speedMultiplier;
        playerY += playerVelY * speedMultiplier;
        
        // Bounce off walls instead of wrapping
        if (playerX < 0.02f) {
            playerX = 0.02f;
            playerVelX = -playerVelX * 0.8f; // Bounce with energy loss
        }
        if (playerX > 0.98f) {
            playerX = 0.98f;
            playerVelX = -playerVelX * 0.8f;
        }
        if (playerY < 0.02f) {
            playerY = 0.02f;
            playerVelY = -playerVelY * 0.8f;
        }
        if (playerY > 0.98f) {
            playerY = 0.98f;
            playerVelY = -playerVelY * 0.8f;
        }
        
        // Update asteroids (simple circular motion)
        for (int i = 0; i < 8; i++) {
            int baseIdx = i * 6;
            asteroids[baseIdx] += asteroids[baseIdx + 2] * speedMultiplier; // x += vx * speed
            asteroids[baseIdx + 1] += asteroids[baseIdx + 3] * speedMultiplier; // y += vy * speed
            
            // Wrap asteroids
            if (asteroids[baseIdx] < 0 || asteroids[baseIdx] > 1) asteroids[baseIdx + 2] *= -1;
            if (asteroids[baseIdx + 1] < 0 || asteroids[baseIdx + 1] > 1) asteroids[baseIdx + 3] *= -1;
            
            // Update distance to player
            float dx = asteroids[baseIdx] - playerX;
            float dy = asteroids[baseIdx + 1] - playerY;
            asteroids[baseIdx + 5] = (float) Math.sqrt(dx * dx + dy * dy);
        }
        
        // Update bullets
        for (int i = bullets.size() - 1; i >= 0; i--) {
            float[] bullet = bullets.get(i);
            bullet[0] += bullet[2] * speedMultiplier;
            bullet[1] += bullet[3] * speedMultiplier;
            bullet[4]--;
            
            // Check collision with asteroids
            for (int j = 0; j < 8; j++) {
                int baseIdx = j * 6;
                float dx = bullet[0] - asteroids[baseIdx];
                float dy = bullet[1] - asteroids[baseIdx + 1];
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance < asteroids[baseIdx + 4]) {
                    // Asteroid destroyed!
                    asteroidsDestroyed++;
                    score += 100; // Score for destroying asteroid
                    bullets.remove(i);
                    
                    // Spawn powerup occasionally (20% chance)
                    if (random.nextFloat() < 0.2f) {
                        int powerupType = random.nextInt(3); // 0=shield, 1=rapidfire, 2=ammo
                        powerups.add(new float[] {
                            asteroids[baseIdx], asteroids[baseIdx + 1], 
                            powerupType, 300 // 300 steps lifespan
                        });
                    }
                    
                    // Respawn asteroid at random edge location
                    respawnAsteroid(j);
                    break;
                }
            }
            
            // Remove bullet if out of bounds or time's up
            if (bullet[4] <= 0 || bullet[0] < 0 || bullet[0] > 1 || bullet[1] < 0 || bullet[1] > 1) {
                bullets.remove(i);
            }
        }
        
        // Update powerups
        for (int i = powerups.size() - 1; i >= 0; i--) {
            float[] powerup = powerups.get(i);
            powerup[3]--;
            
            // Check if powerup is picked up
            float dx = powerup[0] - playerX;
            float dy = powerup[1] - playerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < 0.05f) {
                // Apply powerup effect
                if (powerup[2] == 0) {
                    // Shield
                    hasShield = true;
                    shieldEndTime = System.currentTimeMillis() + 5000;
                } else if (powerup[2] == 1) {
                    // Rapid fire
                    hasRapidFire = true;
                    rapidFireEndTime = System.currentTimeMillis() + 5000;
                } else if (powerup[2] == 2) {
                    // Extra ammo
                    ammunition += 10;
                }
                powerups.remove(i);
            }
            
            // Remove powerup if out of bounds or time's up
            if (powerup[3] <= 0 || powerup[0] < 0 || powerup[0] > 1 || powerup[1] < 0 || powerup[1] > 1) {
                powerups.remove(i);
            }
        }
        
        // Update shield and rapid fire effects
        if (hasShield && System.currentTimeMillis() > shieldEndTime) {
            hasShield = false;
        }
        if (hasRapidFire && System.currentTimeMillis() > rapidFireEndTime) {
            hasRapidFire = false;
        }
        
        // Check for collision with asteroids
        for (int i = 0; i < 8; i++) {
            int baseIdx = i * 6;
            float dx = asteroids[baseIdx] - playerX;
            float dy = asteroids[baseIdx + 1] - playerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance < asteroids[baseIdx + 4] && !hasShield) {
                // Player hit!
                playerHealth--;
            }
        }
    }
    
    private void respawnAsteroid(int index) {
        int baseIdx = index * 6;
        asteroids[baseIdx] = random.nextFloat(); // x
        asteroids[baseIdx + 1] = random.nextFloat(); // y
        asteroids[baseIdx + 2] = (random.nextFloat() - 0.5f) * 0.01f; // vx
        asteroids[baseIdx + 3] = (random.nextFloat() - 0.5f) * 0.01f; // vy
        asteroids[baseIdx + 4] = 0.02f; // size
        
        // Calculate initial distance
        float dx = asteroids[baseIdx] - playerX;
        float dy = asteroids[baseIdx + 1] - playerY;
        asteroids[baseIdx + 5] = (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    private float calculateReward() {
        float reward = 0.1f; // Base survival reward
        
        // Penalty for being too close to asteroids
        for (int i = 0; i < 8; i++) {
            float distance = asteroids[i * 6 + 5];
            if (distance < 0.1f && !hasShield) {
                reward -= 2.0f; // Large penalty for collision risk
            } else if (distance < 0.2f && !hasShield) {
                reward -= 0.5f; // Small penalty for being close
            }
        }
        
        // Reward for movement (encourages exploration)
        float movement = Math.abs(playerVelX) + Math.abs(playerVelY);
        reward += movement * 0.1f;
        
        // Reward for destroying asteroids
        reward += (asteroidsDestroyed - lastAsteroidsDestroyed) * 10.0f;
        lastAsteroidsDestroyed = asteroidsDestroyed;
        
        // Reward for collecting powerups
        reward += (powerups.size() > 0) ? 0.5f : 0.0f;
        
        // Penalty for low ammunition (encourages ammo conservation)
        if (ammunition < 10) {
            reward -= 0.3f;
        }
        
        // Bonus for maintaining health
        reward += playerHealth * 0.2f;
        
        return reward;
    }
    
    private boolean checkEpisodeEnd() {
        // Episode ends after time limit or collision
        long elapsed = System.currentTimeMillis() - episodeStartTime;
        elapsed = (long) (elapsed / speedMultiplier); // Scale time with speed
        
        // Check collision
        for (int i = 0; i < 8; i++) {
            float distance = asteroids[i * 6 + 5];
            if (distance < 0.05f && !hasShield) {
                return true; // Collision!
            }
        }
        
        return elapsed > EPISODE_DURATION || playerHealth <= 0;
    }
    
    private void finishEpisode() {
        episodeCount++;
        System.out.printf("Episode %d finished: Reward=%.2f, Avg=%.2f, Score=%d\n", 
                        episodeCount, episodeReward, agent.getAverageReward(), score);
        initializeEpisode();
    }
    
    private void initializeEpisode() {
        episodeStartTime = System.currentTimeMillis();
        episodeReward = 0;
        asteroidsDestroyed = 0;
        lastAsteroidsDestroyed = 0;
        
        // Reset player
        playerX = 0.5f;
        playerY = 0.5f;
        playerVelX = 0;
        playerVelY = 0;
        playerAngle = random.nextFloat() * 360;
        playerHealth = 3;
        
        // Reset asteroids
        for (int i = 0; i < 8; i++) {
            int baseIdx = i * 6;
            asteroids[baseIdx] = random.nextFloat(); // x
            asteroids[baseIdx + 1] = random.nextFloat(); // y
            asteroids[baseIdx + 2] = (random.nextFloat() - 0.5f) * 0.01f; // vx
            asteroids[baseIdx + 3] = (random.nextFloat() - 0.5f) * 0.01f; // vy
            asteroids[baseIdx + 4] = 0.02f; // size
            
            // Calculate initial distance
            float dx = asteroids[baseIdx] - playerX;
            float dy = asteroids[baseIdx + 1] - playerY;
            asteroids[baseIdx + 5] = (float) Math.sqrt(dx * dx + dy * dy);
        }
        
        // Clear bullets
        bullets.clear();
        
        // Clear powerups
        powerups.clear();
        
        // Reset shield and rapid fire effects
        hasShield = false;
        hasRapidFire = false;
        
        // Reset ammunition
        ammunition = 50;
        
        // Reset score
        score = 0;
    }
    
    private void togglePause() {
        if (trainingTimer.isRunning()) {
            trainingTimer.stop();
            System.out.println("Training paused");
        } else {
            trainingTimer.start();
            System.out.println("Training resumed");
        }
    }
    
    private void resetAgent() {
        // Create new agent (this resets learning)
        System.out.println("Resetting agent - starting fresh!");
        // Note: In a full implementation, we'd create a new agent here
        // For now, just reset episode
        initializeEpisode();
    }
    
    private void setSpeedMultiplier(float multiplier) {
        speedMultiplier = multiplier;
        trainingTimer.setDelay((int) (16 / speedMultiplier));
    }
    
    private void toggleTurboMode() {
        turboMode = !turboMode;
        if (turboMode) {
            setSpeedMultiplier(100.0f);
            frameSkip = 10;
        } else {
            setSpeedMultiplier(1.0f);
            frameSkip = 1;
        }
    }
    
    /**
     * Simple game visualization panel
     */
    private class GameVisualizationPanel extends JPanel {
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
            
            // Draw asteroids (red circles)
            g2.setColor(Color.RED);
            for (int i = 0; i < 8; i++) {
                int x = (int) (asteroids[i * 6] * width);
                int y = (int) (asteroids[i * 6 + 1] * height);
                int size = (int) (asteroids[i * 6 + 4] * width * 2);
                g2.fillOval(x - size/2, y - size/2, size, size);
            }
            
            // Draw bullets (white dots)
            g2.setColor(Color.WHITE);
            for (float[] bullet : bullets) {
                int x = (int) (bullet[0] * width);
                int y = (int) (bullet[1] * height);
                g2.fillOval(x - 2, y - 2, 4, 4);
            }
            
            // Draw powerups (yellow dots)
            g2.setColor(Color.YELLOW);
            for (float[] powerup : powerups) {
                int x = (int) (powerup[0] * width);
                int y = (int) (powerup[1] * height);
                g2.fillOval(x - 2, y - 2, 4, 4);
            }
            
            // Draw player (green dot with direction line)
            g2.setColor(Color.GREEN);
            int px = (int) (playerX * width);
            int py = (int) (playerY * height);
            g2.fillOval(px - 5, py - 5, 10, 10);
            
            // Direction indicator
            int dx = (int) (10 * Math.cos(Math.toRadians(playerAngle)));
            int dy = (int) (10 * Math.sin(Math.toRadians(playerAngle)));
            g2.drawLine(px, py, px + dx, py + dy);
            
            // Info text
            g2.setColor(Color.WHITE);
            g2.drawString("Episode: " + episodeCount, 10, 20);
            g2.drawString("Reward: " + String.format("%.1f", episodeReward), 10, 35);
            g2.drawString("Asteroids Destroyed: " + asteroidsDestroyed, 10, 50);
            g2.drawString("Ammunition: " + ammunition, 10, 65);
            g2.drawString("Health: " + playerHealth, 10, 80);
            g2.drawString("Score: " + score, 10, 95);
            g2.drawString("AI Learning Progress:", 10, height - 40);
            g2.drawString("Green=Player, Red=Asteroids, White=Bullets, Yellow=Powerups", 10, height - 25);
            g2.drawString("Watch AI learn to avoid collisions!", 10, height - 10);
        }
    }
}
