package game.ai;

import game.component.Vector2;
import game.object.Asteroid;
import game.object.Player;
import game.object.PowerUp;
import game.object.projectiles.Projectile;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class QLearningAgent {
    
    // Network parameters
    private NeuralNetwork qNetwork;
    private NeuralNetwork targetNetwork;
    private ExperienceReplay experienceReplay;
    
    // Learning parameters
    private final double LEARNING_RATE = 0.001;
    private final double DISCOUNT_FACTOR = 0.95;
    private final double EPSILON = 0.1; // exploration rate
    private final double EPSILON_DECAY = 0.995;
    private final double MIN_EPSILON = 0.01;
    private double currentEpsilon;
    
    // State and action dimensions
    private final int STATE_SIZE = 24; // Size of state vector
    private final int ACTION_SIZE = 4; // forward/back thrust, side thrust, turn angle, fire
    
    // Game environment references
    private Player controlledPlayer;
    private List<Asteroid> asteroids;
    private List<PowerUp> powerUps;
    private List<Projectile> projectiles;
    private int screenWidth, screenHeight;
    
    // Learning statistics
    private int episodeCount = 0;
    private double totalReward = 0;
    private double averageReward = 0;
    private int stepCount = 0;
    private long lastUpdateTime = System.currentTimeMillis();
    
    // Experience tracking
    private double[] lastState;
    private double[] lastAction;
    private double lastReward;
    private boolean learningEnabled = true;
    
    private Random random = new Random();
    
    // Reward tracking for better feedback
    private int lastAsteroidCount = 0;
    private int lastPowerUpCount = 0;
    private int lastHealth = 100;
    private boolean justTookDamage = false;
    
    public QLearningAgent(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.currentEpsilon = EPSILON;
        
        // Initialize neural networks
        this.qNetwork = new NeuralNetwork(STATE_SIZE, ACTION_SIZE, LEARNING_RATE);
        this.targetNetwork = new NeuralNetwork(STATE_SIZE, ACTION_SIZE, LEARNING_RATE);
        this.targetNetwork.copyWeightsFrom(qNetwork);
        
        // Initialize experience replay
        this.experienceReplay = new ExperienceReplay(10000);
    }
    
    public void setGameEnvironment(Player player, List<Asteroid> asteroids, 
                                 List<PowerUp> powerUps, List<Projectile> projectiles) {
        this.controlledPlayer = player;
        this.asteroids = asteroids;
        this.powerUps = powerUps;
        this.projectiles = projectiles;
    }
    
    public AIAction getAction() {
        if (controlledPlayer == null) {
            return new AIAction(0, 0, 0, false);
        }
        
        double[] state = getStateVector();
        double[] qValues;
        
        // Epsilon-greedy action selection
        if (learningEnabled && random.nextDouble() < currentEpsilon) {
            // Random exploration
            qValues = new double[ACTION_SIZE];
            for (int i = 0; i < ACTION_SIZE; i++) {
                qValues[i] = (random.nextDouble() - 0.5) * 2; // [-1, 1] for continuous actions
            }
        } else {
            // Exploit learned policy
            qValues = qNetwork.forward(state);
        }
        
        // Convert Q-values to actions
        double forwardThrust = Math.tanh(qValues[0]); // [-1, 1]
        double sideThrust = Math.tanh(qValues[1]); // [-1, 1] 
        double turnAngle = (Math.tanh(qValues[2]) + 1) * 180; // [0, 360]
        boolean fire = qValues[3] > 0;
        
        // Store for learning
        if (learningEnabled) {
            if (lastState != null) {
                // Calculate reward and learn from previous experience
                double reward = calculateReward();
                experienceReplay.addExperience(lastState, lastAction, reward, state, false);
                trainNetwork();
                totalReward += reward;
                stepCount++;
            }
            
            lastState = state.clone();
            lastAction = qValues.clone();
        }
        
        return new AIAction(forwardThrust, sideThrust, turnAngle, fire);
    }
    
    private double[] getStateVector() {
        if (controlledPlayer == null) {
            return new double[STATE_SIZE];
        }
        
        double[] state = new double[STATE_SIZE];
        Vector2 playerPos = controlledPlayer.getPosition();
        Vector2 playerVel = controlledPlayer.getVelocity();
        float playerAngle = controlledPlayer.getPlayerViewAngle();
        
        // Player state (6 values)
        state[0] = playerPos.x / screenWidth; // Normalized position
        state[1] = playerPos.y / screenHeight;
        state[2] = playerVel.x; // Velocity
        state[3] = playerVel.y;
        state[4] = Math.sin(Math.toRadians(playerAngle)); // Angle as sin/cos
        state[5] = Math.cos(Math.toRadians(playerAngle));
        
        // Player health and status (2 values)
        state[6] = controlledPlayer.getHealth() / 100.0; // Normalized health
        state[7] = controlledPlayer.isInvulnerable() ? 1.0 : 0.0;
        
        // Nearest asteroid information (8 values)
        Asteroid nearestAsteroid = findNearestAsteroid(playerPos);
        if (nearestAsteroid != null) {
            Vector2 astPos = nearestAsteroid.getPosition();
            Vector2 astVel = nearestAsteroid.getVelocity();
            Vector2 relativePos = new Vector2(astPos.x - playerPos.x, astPos.y - playerPos.y);
            
            state[8] = relativePos.x / screenWidth; // Relative position
            state[9] = relativePos.y / screenHeight;
            state[10] = astVel.x; // Asteroid velocity
            state[11] = astVel.y;
            state[12] = playerPos.distance(astPos) / Math.max(screenWidth, screenHeight); // Distance
            state[13] = nearestAsteroid.getSize() / 64.0; // Normalized size
            
            // Angle to asteroid
            double angleToAst = Math.atan2(relativePos.y, relativePos.x);
            state[14] = Math.sin(angleToAst);
            state[15] = Math.cos(angleToAst);
        }
        
        // Nearest power-up information (4 values)
        PowerUp nearestPowerUp = findNearestPowerUp(playerPos);
        if (nearestPowerUp != null) {
            Vector2 puPos = nearestPowerUp.getPosition();
            Vector2 relativePuPos = new Vector2(puPos.x - playerPos.x, puPos.y - playerPos.y);
            
            state[16] = relativePuPos.x / screenWidth;
            state[17] = relativePuPos.y / screenHeight;
            state[18] = playerPos.distance(puPos) / Math.max(screenWidth, screenHeight);
            state[19] = nearestPowerUp.getType().ordinal() / 3.0; // Power-up type
        }
        
        // Screen edge distances (4 values)
        state[20] = playerPos.x / screenWidth; // Distance to left edge
        state[21] = (screenWidth - playerPos.x) / screenWidth; // Distance to right edge
        state[22] = playerPos.y / screenHeight; // Distance to top edge
        state[23] = (screenHeight - playerPos.y) / screenHeight; // Distance to bottom edge
        
        return state;
    }
    
    private double calculateReward() {
        if (controlledPlayer == null) return 0;
        
        double reward = 0;
        
        // === SURVIVAL REWARDS ===
        reward += 0.05; // Small base survival reward per step
        
        // === ASTEROID DESTRUCTION REWARDS (HUGE) ===
        int currentAsteroidCount = asteroids != null ? asteroids.size() : 0;
        if (lastAsteroidCount > 0 && currentAsteroidCount < lastAsteroidCount) {
            // AI successfully destroyed asteroid(s)!
            int destroyedCount = lastAsteroidCount - currentAsteroidCount;
            reward += destroyedCount * 50.0; // HUGE reward for shooting asteroids!
            System.out.println("AI destroyed " + destroyedCount + " asteroid(s)! Reward: +" + (destroyedCount * 50.0));
        }
        lastAsteroidCount = currentAsteroidCount;
        
        // === POWER-UP COLLECTION REWARDS (GOOD) ===
        int currentPowerUpCount = powerUps != null ? powerUps.size() : 0;
        if (lastPowerUpCount > 0 && currentPowerUpCount < lastPowerUpCount) {
            // AI collected power-up(s)!
            int collectedCount = lastPowerUpCount - currentPowerUpCount;
            reward += collectedCount * 20.0; // Good reward for collecting power-ups
            System.out.println("AI collected " + collectedCount + " power-up(s)! Reward: +" + (collectedCount * 20.0));
        }
        lastPowerUpCount = currentPowerUpCount;
        
        // === DAMAGE PENALTIES (VERY BAD) ===
        int currentHealth = controlledPlayer.getHealth();
        if (currentHealth < lastHealth) {
            // AI took damage!
            int damageTaken = lastHealth - currentHealth;
            reward -= damageTaken * 2.0; // Large penalty for taking damage
            justTookDamage = true;
            System.out.println("AI took " + damageTaken + " damage! Penalty: -" + (damageTaken * 2.0));
        }
        lastHealth = currentHealth;
        
        // === DEATH PENALTY (VERY VERY BAD) ===
        if (!controlledPlayer.isAlive()) {
            reward -= 100.0; // Massive death penalty
            episodeCount++;
            System.out.println("AI died! Episode " + episodeCount + " ended. Death penalty: -100.0");
            resetEpisode();
            return reward; // Return early on death
        }
        
        // === PROXIMITY PENALTIES (BAD) ===
        // Penalty for being too close to asteroids
        if (asteroids != null) {
            for (Asteroid asteroid : asteroids) {
                if (!asteroid.isDestroyed()) {
                    double distance = controlledPlayer.getPosition().distance(asteroid.getPosition());
                    double dangerZone = 80.0; // Danger zone radius
                    if (distance < dangerZone) {
                        double proximityPenalty = (dangerZone - distance) / dangerZone * 5.0;
                        reward -= proximityPenalty;
                    }
                }
            }
        }
        
        // === SMALL POSITIVE REWARDS ===
        // Health maintenance bonus
        reward += (currentHealth / 100.0) * 0.1;
        
        // Power-up seeking reward (small bonus for moving toward power-ups)
        PowerUp nearestPowerUp = findNearestPowerUp(controlledPlayer.getPosition());
        if (nearestPowerUp != null) {
            double distance = controlledPlayer.getPosition().distance(nearestPowerUp.getPosition());
            if (distance < 50.0) {
                reward += (50.0 - distance) / 50.0 * 0.5; // Small reward for approaching power-ups
            }
        }
        
        return reward;
    }
    
    private void trainNetwork() {
        if (experienceReplay.size() > 100) { // Start training after collecting some experiences
            var batch = experienceReplay.sample(32);
            qNetwork.trainBatch(batch);
            
            // Update epsilon
            if (currentEpsilon > MIN_EPSILON) {
                currentEpsilon *= EPSILON_DECAY;
            }
        }
        
        // Update target network periodically
        if (stepCount % 1000 == 0) {
            targetNetwork.copyWeightsFrom(qNetwork);
        }
    }
    
    private void resetEpisode() {
        if (stepCount > 0) {
            averageReward = totalReward / stepCount;
        }
        lastState = null;
        lastAction = null;
        stepCount = 0;
        totalReward = 0;
    }
    
    private Asteroid findNearestAsteroid(Vector2 position) {
        if (asteroids == null || asteroids.isEmpty()) return null;
        
        Asteroid nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Asteroid asteroid : asteroids) {
            if (!asteroid.isDestroyed()) {
                double distance = position.distance(asteroid.getPosition());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = asteroid;
                }
            }
        }
        
        return nearest;
    }
    
    private PowerUp findNearestPowerUp(Vector2 position) {
        if (powerUps == null || powerUps.isEmpty()) return null;
        
        PowerUp nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (PowerUp powerUp : powerUps) {
            if (!powerUp.isCollected()) {
                double distance = position.distance(powerUp.getPosition());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = powerUp;
                }
            }
        }
        
        return nearest;
    }
    
    // Getters for statistics
    public int getEpisodeCount() { return episodeCount; }
    public double getAverageReward() { return averageReward; }
    public double getCurrentEpsilon() { return currentEpsilon; }
    public int getStepCount() { return stepCount; }
    public double getTotalReward() { return totalReward; }
    public boolean isLearningEnabled() { return learningEnabled; }
    
    public void setLearningEnabled(boolean enabled) { 
        this.learningEnabled = enabled;
        if (!enabled) {
            currentEpsilon = 0; // No exploration when learning disabled
        }
    }
    
    // Inner class for AI actions
    public static class AIAction {
        public final double forwardThrust; // [-1, 1]
        public final double sideThrust; // [-1, 1]
        public final double turnAngle; // [0, 360]
        public final boolean fire;
        
        public AIAction(double forwardThrust, double sideThrust, double turnAngle, boolean fire) {
            this.forwardThrust = forwardThrust;
            this.sideThrust = sideThrust;
            this.turnAngle = turnAngle;
            this.fire = fire;
        }
    }
}
