package game.ai.dqn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Experience Replay Buffer - stores and samples game experiences for training.
 * 
 * Key concept in Deep Q-Learning:
 * Instead of learning immediately from each action, we store experiences
 * and learn from random batches later. This breaks correlation between
 * consecutive experiences and stabilizes training.
 * 
 * Experience = (state, action, reward, nextState, done)
 */
public class ExperienceReplay {
    private final List<Experience> buffer;
    private final int maxSize;
    private final Random random;
    private int currentIndex = 0;
    
    /**
     * Single experience storing one step of gameplay
     */
    public static class Experience {
        public final float[] state;      // Game state when action was taken
        public final int action;         // Action that was taken (0-5 for Asteroids)
        public final float reward;       // Reward received for this action
        public final float[] nextState;  // Game state after action was executed
        public final boolean done;       // Whether episode ended (player died)
        
        public Experience(float[] state, int action, float reward, float[] nextState, boolean done) {
            this.state = state.clone();
            this.action = action;
            this.reward = reward;
            this.nextState = nextState.clone();
            this.done = done;
        }
        
        @Override
        public String toString() {
            return String.format("Experience[action=%d, reward=%.2f, done=%b]", action, reward, done);
        }
    }
    
    public ExperienceReplay(int maxSize) {
        this.maxSize = maxSize;
        this.buffer = new ArrayList<>(maxSize);
        this.random = new Random();
    }
    
    /**
     * Store a new experience in the buffer
     * Uses circular buffer - overwrites oldest when full
     */
    public void store(float[] state, int action, float reward, float[] nextState, boolean done) {
        Experience experience = new Experience(state, action, reward, nextState, done);
        
        if (buffer.size() < maxSize) {
            // Buffer not full yet, just add
            buffer.add(experience);
        } else {
            // Buffer full, replace oldest (circular buffer)
            buffer.set(currentIndex, experience);
            currentIndex = (currentIndex + 1) % maxSize;
        }
    }
    
    /**
     * Sample a random batch of experiences for training
     * 
     * @param batchSize - number of experiences to sample
     * @return array of random experiences
     */
    public Experience[] sample(int batchSize) {
        if (buffer.size() < batchSize) {
            throw new IllegalStateException("Not enough experiences to sample. Have " + buffer.size() + ", need " + batchSize);
        }
        
        Experience[] batch = new Experience[batchSize];
        
        // Sample without replacement
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < buffer.size(); i++) {
            indices.add(i);
        }
        
        for (int i = 0; i < batchSize; i++) {
            int randomIndex = random.nextInt(indices.size());
            int bufferIndex = indices.remove(randomIndex);
            batch[i] = buffer.get(bufferIndex);
        }
        
        return batch;
    }
    
    /**
     * Check if we have enough experiences to start training
     */
    public boolean canSample(int batchSize) {
        return buffer.size() >= batchSize;
    }
    
    /**
     * Get current number of stored experiences
     */
    public int size() {
        return buffer.size();
    }
    
    /**
     * Check if buffer is full
     */
    public boolean isFull() {
        return buffer.size() >= maxSize;
    }
    
    /**
     * Clear all stored experiences
     */
    public void clear() {
        buffer.clear();
        currentIndex = 0;
    }
    
    /**
     * Get statistics about stored experiences
     */
    public ExperienceStats getStats() {
        if (buffer.isEmpty()) {
            return new ExperienceStats(0, 0, 0, 0, 0);
        }
        
        float totalReward = 0;
        float minReward = Float.MAX_VALUE;
        float maxReward = Float.MIN_VALUE;
        int doneCount = 0;
        
        for (Experience exp : buffer) {
            totalReward += exp.reward;
            minReward = Math.min(minReward, exp.reward);
            maxReward = Math.max(maxReward, exp.reward);
            if (exp.done) doneCount++;
        }
        
        float avgReward = totalReward / buffer.size();
        
        return new ExperienceStats(buffer.size(), avgReward, minReward, maxReward, doneCount);
    }
    
    /**
     * Statistics about stored experiences
     */
    public static class ExperienceStats {
        public final int count;
        public final float avgReward;
        public final float minReward;
        public final float maxReward;
        public final int episodeEnds;
        
        public ExperienceStats(int count, float avgReward, float minReward, float maxReward, int episodeEnds) {
            this.count = count;
            this.avgReward = avgReward;
            this.minReward = minReward;
            this.maxReward = maxReward;
            this.episodeEnds = episodeEnds;
        }
        
        @Override
        public String toString() {
            return String.format("ExperienceStats[count=%d, avgReward=%.2f, range=[%.2f, %.2f], episodes=%d]",
                               count, avgReward, minReward, maxReward, episodeEnds);
        }
    }
    
    @Override
    public String toString() {
        return String.format("ExperienceReplay[size=%d/%d, canTrain=%b]", 
                           buffer.size(), maxSize, canSample(32));
    }
    
    /**
     * Get a preview of recent experiences for debugging
     */
    public String getRecentExperiences(int count) {
        if (buffer.isEmpty()) return "No experiences stored";
        
        StringBuilder sb = new StringBuilder();
        sb.append("Recent experiences:\n");
        
        int start = Math.max(0, buffer.size() - count);
        for (int i = start; i < buffer.size(); i++) {
            sb.append(String.format("  [%d] %s\n", i, buffer.get(i)));
        }
        
        return sb.toString();
    }
}
