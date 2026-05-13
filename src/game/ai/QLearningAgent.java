package game.ai;

import game.component.Vector2;
import game.object.Asteroid;
import game.object.Player;
import game.object.PowerUp;
import game.object.projectiles.Projectile;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QLearningAgent {
    
    // Network parameters
    private NeuralNetwork qNetwork;
    private NeuralNetwork targetNetwork;
    private ExperienceReplay experienceReplay;
    private static final int CHECKPOINT_VERSION = 1;
    private static final String CHECKPOINT_MAGIC = "ASTEROIDS_DQN";
    private static final Path CHECKPOINT_DIR = Path.of("training", "checkpoints");
    private static final Path LATEST_CHECKPOINT = CHECKPOINT_DIR.resolve("latest.bin");
    private static final Path REWARD_LOG = Path.of("training", "logs", "rewards.csv");
    private static final int CHECKPOINT_INTERVAL_EPISODES = 5;
    
    // Learning parameters
    private final double LEARNING_RATE = 0.001;
    private final double DISCOUNT_FACTOR = 0.95;
    private final double EPSILON = 0.1; // exploration rate
    private final double EPSILON_DECAY = 0.995;
    private final double MIN_EPSILON = 0.01;
    private double currentEpsilon;
    private double epsilonBeforePause;
    
    // State and action dimensions
    private final int STATE_SIZE = 24; // Size of state vector
    private final int ACTION_SIZE = 9; // one Q-value per discrete action
    
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
    private int lastActionIndex = -1;
    private double lastReward;
    private boolean learningEnabled = true;
    
    private Random random = new Random();
    
    private double pendingEventReward = 0;
    
    public QLearningAgent(int screenWidth, int screenHeight) {
        this(screenWidth, screenHeight, true);
    }

    public QLearningAgent(int screenWidth, int screenHeight, boolean loadCheckpoint) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.currentEpsilon = EPSILON;
        this.epsilonBeforePause = EPSILON;
        
        // Initialize neural networks
        this.qNetwork = new NeuralNetwork(STATE_SIZE, ACTION_SIZE, LEARNING_RATE);
        this.targetNetwork = new NeuralNetwork(STATE_SIZE, ACTION_SIZE, LEARNING_RATE);
        this.targetNetwork.copyWeightsFrom(qNetwork);
        
        // Initialize experience replay
        this.experienceReplay = new ExperienceReplay(10000);

        if (loadCheckpoint) {
            loadLatestCheckpoint();
        }
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
        double[] qValues = qNetwork.forward(state);

        int actionIndex;
        if (learningEnabled && random.nextDouble() < currentEpsilon) {
            actionIndex = random.nextInt(ACTION_SIZE);
        } else {
            actionIndex = findMaxIndex(qValues);
        }
        
        // Store for learning
        if (learningEnabled) {
            if (lastState != null && lastActionIndex >= 0) {
                // Calculate reward and learn from previous experience
                double reward = calculateReward();
                experienceReplay.addExperience(lastState, lastActionIndex, reward, state, false);
                trainNetwork();
                totalReward += reward;
                stepCount++;
            }
            
            lastState = state.clone();
            lastActionIndex = actionIndex;
        }
        
        return mapAction(actionIndex);
    }

    private AIAction mapAction(int actionIndex) {
        double currentAngle = controlledPlayer != null ? controlledPlayer.getPlayerViewAngle() : 0;
        double leftTurn = normalizeAngle(currentAngle - 18);
        double rightTurn = normalizeAngle(currentAngle + 18);

        switch (actionIndex) {
            case 1:
                return new AIAction(1.0, 0, currentAngle, false);
            case 2:
                return new AIAction(-0.7, 0, currentAngle, false);
            case 3:
                return new AIAction(0, -0.8, currentAngle, false);
            case 4:
                return new AIAction(0, 0.8, currentAngle, false);
            case 5:
                return new AIAction(0, 0, leftTurn, false);
            case 6:
                return new AIAction(0, 0, rightTurn, false);
            case 7:
                return new AIAction(0, 0, currentAngle, true);
            case 8:
                return new AIAction(0.6, 0, angleToNearestAsteroid(currentAngle), true);
            default:
                return new AIAction(0, 0, currentAngle, false);
        }
    }

    private double normalizeAngle(double angle) {
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;
        return angle;
    }

    private double angleToNearestAsteroid(double fallbackAngle) {
        if (controlledPlayer == null) return fallbackAngle;
        Asteroid nearest = findNearestAsteroid(controlledPlayer.getPosition());
        if (nearest == null) return fallbackAngle;

        Vector2 playerCenter = controlledPlayer.getCenter();
        Vector2 asteroidCenter = nearest.getCenter();
        double angle = Math.toDegrees(Math.atan2(asteroidCenter.y - playerCenter.y, asteroidCenter.x - playerCenter.x));
        return normalizeAngle(angle);
    }

    private int findMaxIndex(double[] values) {
        int maxIndex = 0;
        double max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
                maxIndex = i;
            }
        }
        return maxIndex;
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
        
        reward += 0.02; // small survival reward per decision
        reward += consumePendingReward();
        
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
        int currentHealth = controlledPlayer.getHealth();
        reward += (currentHealth / 100.0) * 0.05;
        
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

    public void saveCheckpoint() {
        try {
            Files.createDirectories(CHECKPOINT_DIR);
            Path tempFile = CHECKPOINT_DIR.resolve("latest.tmp");

            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(tempFile))) {
                out.writeUTF(CHECKPOINT_MAGIC);
                out.writeInt(CHECKPOINT_VERSION);
                out.writeInt(screenWidth);
                out.writeInt(screenHeight);
                out.writeInt(STATE_SIZE);
                out.writeInt(ACTION_SIZE);
                out.writeDouble(currentEpsilon);
                out.writeDouble(epsilonBeforePause);
                out.writeBoolean(learningEnabled);
                out.writeInt(episodeCount);
                out.writeDouble(averageReward);
                qNetwork.writeTo(out);
                targetNetwork.writeTo(out);
            }

            try {
                Files.move(tempFile, LATEST_CHECKPOINT, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, LATEST_CHECKPOINT, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Could not save AI checkpoint: " + e.getMessage());
        }
    }

    public boolean loadLatestCheckpoint() {
        if (!Files.exists(LATEST_CHECKPOINT)) {
            return false;
        }

        try (DataInputStream in = new DataInputStream(Files.newInputStream(LATEST_CHECKPOINT))) {
            String magic = in.readUTF();
            int version = in.readInt();
            int savedScreenWidth = in.readInt();
            int savedScreenHeight = in.readInt();
            int savedStateSize = in.readInt();
            int savedActionSize = in.readInt();

            if (!CHECKPOINT_MAGIC.equals(magic) || version != CHECKPOINT_VERSION ||
                savedScreenWidth != screenWidth || savedScreenHeight != screenHeight ||
                savedStateSize != STATE_SIZE || savedActionSize != ACTION_SIZE) {
                throw new IOException("Checkpoint metadata does not match this agent");
            }

            currentEpsilon = in.readDouble();
            epsilonBeforePause = in.readDouble();
            learningEnabled = in.readBoolean();
            episodeCount = in.readInt();
            averageReward = in.readDouble();
            qNetwork.readFrom(in);
            targetNetwork.readFrom(in);
            resetEpisodeState();
            System.out.println("Loaded AI checkpoint from " + LATEST_CHECKPOINT.toAbsolutePath());
            return true;
        } catch (IOException e) {
            System.err.println("Could not load AI checkpoint: " + e.getMessage());
            return false;
        }
    }

    public void resetLearning() {
        this.currentEpsilon = EPSILON;
        this.epsilonBeforePause = EPSILON;
        this.episodeCount = 0;
        this.averageReward = 0;
        this.totalReward = 0;
        this.stepCount = 0;
        this.pendingEventReward = 0;
        this.lastState = null;
        this.lastActionIndex = -1;
        this.qNetwork = new NeuralNetwork(STATE_SIZE, ACTION_SIZE, LEARNING_RATE);
        this.targetNetwork = new NeuralNetwork(STATE_SIZE, ACTION_SIZE, LEARNING_RATE);
        this.targetNetwork.copyWeightsFrom(qNetwork);
        this.experienceReplay = new ExperienceReplay(10000);
        clearRewardLog();
        saveCheckpoint();
    }

    public void copyPolicyFrom(QLearningAgent other) {
        this.qNetwork.copyWeightsFrom(other.qNetwork);
        this.targetNetwork.copyWeightsFrom(other.targetNetwork);
        this.currentEpsilon = other.currentEpsilon;
        this.epsilonBeforePause = other.epsilonBeforePause;
        this.learningEnabled = other.learningEnabled;
        resetEpisodeState();
    }

    public void mutatePolicy(double mutationRate, double mutationStrength, Random random) {
        this.qNetwork.mutate(mutationRate, mutationStrength, random);
        this.targetNetwork.copyWeightsFrom(qNetwork);
        resetEpisodeState();
    }

    public List<Double> getRecentRewardHistory(int maxEntries) {
        List<Double> rewards = new ArrayList<>();
        if (!Files.exists(REWARD_LOG)) {
            return rewards;
        }

        try {
            List<String> lines = Files.readAllLines(REWARD_LOG);
            int start = Math.max(1, lines.size() - maxEntries);
            for (int i = start; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                if (parts.length >= 2) {
                    rewards.add(Double.parseDouble(parts[1]));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Could not read AI reward history: " + e.getMessage());
        }
        return rewards;
    }

    private void appendRewardLog() {
        try {
            Files.createDirectories(REWARD_LOG.getParent());
            boolean writeHeader = !Files.exists(REWARD_LOG);

            try (BufferedWriter writer = Files.newBufferedWriter(REWARD_LOG, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                if (writeHeader) {
                    writer.write("episode,average_reward,epsilon");
                    writer.newLine();
                }
                writer.write(episodeCount + "," + averageReward + "," + currentEpsilon);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not append AI reward log: " + e.getMessage());
        }
    }

    private void clearRewardLog() {
        try {
            Files.deleteIfExists(REWARD_LOG);
        } catch (IOException e) {
            System.err.println("Could not clear AI reward log: " + e.getMessage());
        }
    }

    public void recordAsteroidDestroyed() {
        pendingEventReward += 30.0;
    }

    public void recordPowerUpCollected() {
        pendingEventReward += 8.0;
    }

    public void recordDamageTaken(int damage) {
        pendingEventReward -= Math.max(5.0, damage);
    }

    public void recordShotFired() {
        pendingEventReward -= 0.02;
    }

    public void onEpisodeEnd() {
        if (lastState == null || lastActionIndex < 0) {
            resetEpisode();
            return;
        }

        double[] terminalState = getStateVector();
        double reward = consumePendingReward() - 100.0;
        experienceReplay.addExperience(lastState, lastActionIndex, reward, terminalState, true);
        trainNetwork();
        totalReward += reward;
        stepCount++;
        episodeCount++;
        finishEpisode();
    }

    private double consumePendingReward() {
        double reward = pendingEventReward;
        pendingEventReward = 0;
        return reward;
    }
    
    private void finishEpisode() {
        if (stepCount > 0) {
            averageReward = totalReward / stepCount;
        }
        appendRewardLog();
        if (episodeCount % CHECKPOINT_INTERVAL_EPISODES == 0) {
            saveCheckpoint();
        }
        resetEpisodeState();
    }

    private void resetEpisode() {
        resetEpisodeState();
    }

    private void resetEpisodeState() {
        lastState = null;
        lastActionIndex = -1;
        stepCount = 0;
        totalReward = 0;
        pendingEventReward = 0;
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
        if (this.learningEnabled == enabled) {
            return;
        }
        this.learningEnabled = enabled;
        if (enabled) {
            currentEpsilon = Math.max(epsilonBeforePause, MIN_EPSILON);
        } else {
            epsilonBeforePause = currentEpsilon;
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
