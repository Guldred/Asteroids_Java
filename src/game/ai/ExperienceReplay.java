package game.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ExperienceReplay {
    private List<Experience> buffer;
    private int maxSize;
    private int currentIndex;
    private boolean isFull;
    private Random random;
    
    public ExperienceReplay(int maxSize) {
        this.maxSize = maxSize;
        this.buffer = new ArrayList<>(maxSize);
        this.currentIndex = 0;
        this.isFull = false;
        this.random = new Random();
        
        // Pre-allocate buffer space
        for (int i = 0; i < maxSize; i++) {
            buffer.add(null);
        }
    }
    
    public void addExperience(double[] state, double[] action, double reward, 
                             double[] nextState, boolean done) {
        Experience exp = new Experience(state, action, reward, nextState, done);
        
        buffer.set(currentIndex, exp);
        currentIndex = (currentIndex + 1) % maxSize;
        
        if (currentIndex == 0) {
            isFull = true;
        }
    }
    
    public List<Experience> sample(int batchSize) {
        int availableSize = isFull ? maxSize : currentIndex;
        
        if (batchSize > availableSize) {
            batchSize = availableSize;
        }
        
        List<Experience> batch = new ArrayList<>(batchSize);
        List<Integer> indices = new ArrayList<>(availableSize);
        
        // Create list of valid indices
        for (int i = 0; i < availableSize; i++) {
            indices.add(i);
        }
        
        // Shuffle and take first batchSize elements
        Collections.shuffle(indices, random);
        
        for (int i = 0; i < batchSize; i++) {
            int index = indices.get(i);
            Experience exp = buffer.get(index);
            if (exp != null) {
                batch.add(exp);
            }
        }
        
        return batch;
    }
    
    public int size() {
        return isFull ? maxSize : currentIndex;
    }
    
    public boolean isEmpty() {
        return currentIndex == 0 && !isFull;
    }
    
    public void clear() {
        currentIndex = 0;
        isFull = false;
        for (int i = 0; i < maxSize; i++) {
            buffer.set(i, null);
        }
    }
    
    // Inner class for storing experiences
    public static class Experience {
        public final double[] state;
        public final double[] action;
        public final double reward;
        public final double[] nextState;
        public final boolean done;
        
        public Experience(double[] state, double[] action, double reward, 
                         double[] nextState, boolean done) {
            this.state = state != null ? state.clone() : new double[0];
            this.action = action != null ? action.clone() : new double[0];
            this.reward = reward;
            this.nextState = nextState != null ? nextState.clone() : new double[0];
            this.done = done;
        }
    }
}
