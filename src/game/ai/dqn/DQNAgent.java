package game.ai.dqn;

import game.ai.Agent;
import game.ai.AgentInput;
import java.util.List;

/**
 * Deep Q-Network Agent - learns to play Asteroids in real-time using backpropagation.
 * 
 * This is the "brain" that combines all components:
 * - DQNNetwork: Makes decisions based on game state
 * - ExperienceReplay: Stores and learns from past experiences  
 * - Target Network: Stabilizes training by providing stable targets
 * 
 * Learning Process:
 * 1. Observe game state
 * 2. Choose action (explore vs exploit)
 * 3. Execute action and observe reward
 * 4. Store experience in replay buffer
 * 5. Learn from batch of stored experiences
 * 6. Repeat and improve!
 */
public class DQNAgent implements Agent {
    // Network architecture
    private final DQNNetwork mainNetwork;    // Main Q-network (learning)
    private final DQNNetwork targetNetwork;  // Target Q-network (stable targets)
    
    // Learning components
    private final ExperienceReplay replayBuffer;
    
    // Learning parameters
    private float epsilon = 1.0f;
    private final float epsilonMin = 0.05f;
    private final float epsilonDecay = 0.998f;
    private final float learningRate = 0.0005f;
    private final float discountFactor = 0.95f;
    
    // Agent velocity for physics simulation
    public float velX = 0.0f;
    public float velY = 0.0f;
    
    // Training parameters
    private final int batchSize = 32;       // Training batch size
    private final int targetUpdateFreq = 100; // How often to update target network
    
    // Statistics
    private float totalReward = 0;
    private int episodeCount = 0;
    private float averageReward = 0;
    private float lastEpisodeReward = 0;
    
    // Action mapping for Asteroids - Discrete actions + continuous turning
    public static final int ACTION_NOTHING = 0;
    public static final int ACTION_THRUST = 1;
    public static final int ACTION_SHOOT = 2;
    public static final int ACTION_BRAKE = 3;
    public static final int NUM_ACTIONS = 4;  // No discrete turning actions
    
    // Network output indices  
    public static final int OUTPUT_ACTION_PROBS = 0; // First 4 outputs for action probabilities
    public static final int OUTPUT_ANGLE = 4;        // 5th output for continuous turning angle (0-1)
    
    // Training state
    private float[] lastState;
    private int lastAction;
    private int stepCount = 0;
    private int updateCount = 0;
    private float currentAngle = 0.0f; // Current angle of the agent
    
    public DQNAgent(int stateSize) {
        // Create main and target networks with same architecture
        // Architecture: stateSize → 64 → 64 → NUM_ACTIONS + 1 (for continuous angle)
        int[] hiddenSizes = {64, 64};
        this.mainNetwork = new DQNNetwork(stateSize, hiddenSizes, NUM_ACTIONS + 1);
        this.targetNetwork = mainNetwork.copy(); // Target starts as copy of main
        
        // Create experience replay buffer (stores 10,000 experiences)
        this.replayBuffer = new ExperienceReplay(10000);
        
        System.out.println("DQNAgent initialized:");
        System.out.println(mainNetwork.toString());
        System.out.println("Experience buffer size: " + replayBuffer.size());
    }
    
    @Override
    public AgentInput decide(float[] state) {
        // Store current state for learning
        lastState = state.clone();
        
        // Get Q-values for all actions
        float[] qValues = mainNetwork.forward(state);
        
        // Select action using epsilon-greedy policy
        lastAction = mainNetwork.selectAction(qValues, epsilon);
        
        // Extract and validate continuous angle output with sigmoid activation
        float angleOutput = 0.5f; // Default
        if (qValues.length > OUTPUT_ANGLE) {
            // Apply sigmoid activation to angle output for natural 0-1 range
            float rawAngle = qValues[OUTPUT_ANGLE];
            angleOutput = 1.0f / (1.0f + (float)Math.exp(-rawAngle)); // Sigmoid function
        }
        
        // Ensure angle output is valid (handle NaN and infinite values) 
        if (Float.isNaN(angleOutput) || Float.isInfinite(angleOutput)) {
            angleOutput = 0.5f; // Default to neutral angle
        }
        
        // Convert action to game input
        return actionToInput(lastAction, angleOutput);
    }
    
    /**
     * Learn from the outcome of the last action
     * 
     * @param newState - game state after action was executed
     * @param reward - reward received for the action
     * @param done - whether episode ended (player died)
     */
    public void learn(float[] newState, float reward, boolean done) {
        stepCount++;
        totalReward += reward;
        
        // Store experience in replay buffer
        if (lastState != null) {
            replayBuffer.store(lastState, lastAction, reward, newState, done);
        }
        
        // Learn from experience batch if we have enough data
        if (replayBuffer.canSample(batchSize)) {
            trainOnBatch();
        }
        
        // Update target network periodically
        if (stepCount % targetUpdateFreq == 0) {
            updateTargetNetwork();
        }
        
        // Decay exploration rate
        if (epsilon > epsilonMin) {
            epsilon *= epsilonDecay;
        }
        
        // Episode ended - update statistics
        if (done) {
            lastEpisodeReward = totalReward;
            episodeCount++;
            if (episodeCount > 0) {
                averageReward = (averageReward * (episodeCount - 1) + totalReward) / episodeCount;
            }
            
            System.out.printf("Episode %d: Reward=%.2f, AvgReward=%.2f, Epsilon=%.3f, BufferSize=%d\n",
                             episodeCount, totalReward, averageReward, epsilon, replayBuffer.size());
            
            totalReward = 0; // Reset for next episode
        }
    }
    
    /**
     * Train the network on a batch of experiences
     */
    private void trainOnBatch() {
        // Sample random batch of experiences
        ExperienceReplay.Experience[] batch = replayBuffer.sample(batchSize);
        
        // Process each experience in the batch
        for (ExperienceReplay.Experience exp : batch) {
            // Get current Q-values
            float[] currentQ = mainNetwork.forward(exp.state);
            
            // Calculate target Q-value for the action that was taken
            float targetQ;
            if (exp.done) {
                // Episode ended - no future rewards
                targetQ = exp.reward;
            } else {
                // Episode continues - add discounted future reward
                float[] nextQ = targetNetwork.forward(exp.nextState);
                float maxNextQ = getMaxValue(nextQ);
                targetQ = exp.reward + discountFactor * maxNextQ;
            }
            
            // Update the Q-value for the action that was taken
            float[] targetQValues = currentQ.clone();
            targetQValues[exp.action] = targetQ;
            
            // Train network using backpropagation
            mainNetwork.backward(targetQValues, learningRate);
        }
        
        updateCount++;
    }
    
    /**
     * Update target network with current main network weights
     */
    private void updateTargetNetwork() {
        float[] mainParams = mainNetwork.getAllParameters();
        targetNetwork.setAllParameters(mainParams);
        System.out.printf("Target network updated (step %d)\n", stepCount);
    }
    
    /**
     * Convert DQN action to game input
     */
    private AgentInput actionToInput(int action, float angle) {
        AgentInput input = new AgentInput();
        
        switch (action) {
            case ACTION_NOTHING: 
                // Do nothing
                break;
            case ACTION_THRUST: 
                input.thrustForward = true; 
                break;
            case ACTION_SHOOT: 
                input.shoot = true; 
                break;
            case ACTION_BRAKE: 
                input.thrustBack = true; 
                break;
        }
        
        // Convert network output (0-1) to absolute agent direction (0-360°)
        if (Float.isNaN(angle) || Float.isInfinite(angle)) {
            System.out.printf("WARNING: Invalid angle input %.3f, using default\n", angle);
            angle = 0.5f;
        }
        
        float desiredAngle = angle * 360.0f;  // 0-1 → 0-360°
        
        // Set agent to face desired direction INSTANTLY
        // Calculate the angle change needed to reach desired direction
        float currentNormalized = currentAngle % 360.0f;
        if (currentNormalized < 0) currentNormalized += 360.0f;
        
        float angleDifference = desiredAngle - currentNormalized;
        
        // Normalize to shortest rotation path
        if (angleDifference > 180.0f) {
            angleDifference -= 360.0f;
        } else if (angleDifference < -180.0f) {
            angleDifference += 360.0f;
        }
        
        // INSTANT DIRECTION SETTING - no turn rate limit!
        input.angleDelta = angleDifference;
        
        // Update our tracking
        currentAngle = desiredAngle;
        
        // Debug output (remove after testing)
        if (stepCount % 100 == 0) {
            System.out.printf("Agent INSTANT turn: networkOutput=%.3f → desired=%.1f° (was=%.1f°, delta=%.1f°)\n", 
                            angle, desiredAngle, currentNormalized, angleDifference);
        }
        
        return input;
    }
    
    /**
     * Get maximum value from array
     */
    private float getMaxValue(float[] values) {
        float max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }
    
    // Getters for visualization and statistics
    public float getEpsilon() { return epsilon; }
    public float getAverageReward() { return averageReward; }
    public float getLastEpisodeReward() { return lastEpisodeReward; }
    public int getEpisodeCount() { return episodeCount; }
    public int getUpdateCount() { return updateCount; }
    public ExperienceReplay getReplayBuffer() { return replayBuffer; }
    public DQNNetwork getMainNetwork() { return mainNetwork; }
    public DQNNetwork getTargetNetwork() { return targetNetwork; }
    
    /**
     * Save agent state to file (for checkpointing)
     */
    public void saveToFile(String filename) {
        // TODO: Implement serialization of network parameters and training state
        System.out.println("Saving DQN agent to: " + filename);
    }
    
    /**
     * Load agent state from file
     */
    public void loadFromFile(String filename) {
        // TODO: Implement deserialization of network parameters and training state
        System.out.println("Loading DQN agent from: " + filename);
    }
    
    /**
     * Get detailed training statistics
     */
    public String getTrainingStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DQN Agent Training Statistics ===\n");
        sb.append(String.format("Episodes: %d\n", episodeCount));
        sb.append(String.format("Total Steps: %d\n", stepCount));
        sb.append(String.format("Network Updates: %d\n", updateCount));
        sb.append(String.format("Epsilon (exploration): %.3f\n", epsilon));
        sb.append(String.format("Last Episode Reward: %.2f\n", lastEpisodeReward));
        sb.append(String.format("Average Reward: %.2f\n", averageReward));
        sb.append(String.format("Experience Buffer: %d/%d\n", replayBuffer.size(), 10000));
        sb.append(String.format("Network Architecture: %s\n", mainNetwork.toString().split("\n")[0]));
        
        return sb.toString();
    }
    
    /**
     * Create a copy of this agent with identical neural network weights
     * Used for evolutionary selection and duplication
     */
    public DQNAgent copy() {
        // Create new agent with same architecture
        DQNAgent copy = new DQNAgent(mainNetwork.getStateSize());
        
        // Copy neural network weights
        float[] mainParams = mainNetwork.getAllParameters();
        copy.mainNetwork.setAllParameters(mainParams);
        copy.targetNetwork.setAllParameters(mainParams);
        
        // Copy learning progress
        copy.epsilon = this.epsilon;
        copy.stepCount = this.stepCount;
        copy.totalReward = this.totalReward;
        
        return copy;
    }
    
    /**
     * Build state vector from asteroids data for evolutionary system
     * Used when integrated with evolutionary trainer
     */
    public float[] buildState(float[] asteroidData) {
        // This method is called by the evolutionary system
        // The evolutionary trainer will pre-process asteroid data and pass it as float array
        return asteroidData;
    }
    
    @Override
    public String toString() {
        return String.format("DQNAgent[episodes=%d, avgReward=%.2f, epsilon=%.3f]",
                           episodeCount, averageReward, epsilon);
    }
}
