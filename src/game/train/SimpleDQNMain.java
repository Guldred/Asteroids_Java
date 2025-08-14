package game.train;

/**
 * Launch the Simplified DQN Live Training!
 * 
 * This provides immediate visual AI learning without complex integration.
 * Perfect for watching neural networks learn decision-making in real-time.
 * 
 * Usage:
 *   java -cp out game.train.SimpleDQNMain
 * 
 * What you'll see:
 *   - Green dot (AI player) learning to avoid red circles (asteroids)
 *   - Real-time learning statistics showing improvement
 *   - Epsilon-greedy exploration gradually becoming more strategic
 *   - Learning curve showing AI getting better over time
 */
public class SimpleDQNMain {
    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("    🤖 DEEP Q-LEARNING LIVE TRAINING DEMO 🤖   ");
        System.out.println("================================================");
        System.out.println();
        System.out.println("🎯 EDUCATIONAL NEURAL NETWORK ARCHITECTURE:");
        System.out.println("   Perceptron → Layer → Network → Agent");
        System.out.println("   Individual neurons → Parallel processing → Complete network → Learning behavior");
        System.out.println();
        System.out.println("🧠 LEARNING PROCESS:");
        System.out.println("   • Episodes 1-10:   Random chaotic movement");
        System.out.println("   • Episodes 20-50:  Basic pattern recognition");
        System.out.println("   • Episodes 100+:   Strategic avoidance behavior");
        System.out.println("   • Episodes 500+:   Sophisticated decision making");
        System.out.println();
        System.out.println("📊 LIVE STATISTICS:");
        System.out.println("   • Real-time learning curves");
        System.out.println("   • Exploration rate (epsilon) decay");  
        System.out.println("   • Episode rewards and progress");
        System.out.println("   • Experience replay buffer status");
        System.out.println();
        System.out.println("🎮 CONTROLS:");
        System.out.println("   • Pause/Resume - Toggle training");
        System.out.println("   • Reset Agent - Start learning from scratch");
        System.out.println("   • Speed Controls - 1x, 2x, 5x, 10x faster training");
        System.out.println("   • Turbo Mode - Maximum speed (100x) with frame skipping");
        System.out.println();
        System.out.println("🚀 Starting live training interface...");
        System.out.println();
        
        try {
            // Create and show the training window
            SimpleDQNTrainer trainer = new SimpleDQNTrainer();
            trainer.setVisible(true);
            
            System.out.println("✅ Training interface launched successfully!");
            System.out.println();
            System.out.println("🎯 WATCH THE AI LEARN:");
            System.out.println("   Green dot = AI player (learning to survive)");
            System.out.println("   Red circles = Asteroids (obstacles to avoid)");
            System.out.println("   Statistics panel = Real-time learning progress");
            System.out.println();
            System.out.println("🧠 NEURAL NETWORK LEARNING:");
            System.out.println("   The AI uses backpropagation to learn from each decision.");
            System.out.println("   You'll see immediate improvement as it learns to avoid collisions!");
            System.out.println();
            System.out.println("📈 EXPECTED PROGRESSION:");
            System.out.println("   Initial: Random movement, frequent collisions");
            System.out.println("   Learning: Gradual collision avoidance");
            System.out.println("   Mastered: Strategic movement and survival");
            
        } catch (Exception e) {
            System.err.println("❌ Error starting DQN training: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
