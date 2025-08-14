package game.train;

/**
 * Launch the Revolutionary Hybrid Evolutionary-DQN Training System!
 * 
 * This combines two powerful AI techniques:
 * 1. Deep Q-Learning (DQN) - Individual neural network learning through backpropagation
 * 2. Evolutionary Selection - Population-level survival of the fittest
 * 
 * What you'll see:
 * - 10 AI agents competing simultaneously in the same asteroid field
 * - Each agent learning individually through experience replay
 * - Every 10 episodes: evolutionary selection (top 5 survive, duplicate, worst 5 eliminated)
 * - Generations of increasingly intelligent AI behavior
 * 
 * This is cutting-edge AI research: Population-Based Training!
 * 
 * Usage:
 *   java -cp out game.train.EvolutionaryDQNMain
 */
public class EvolutionaryDQNMain {
    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("  HYBRID EVOLUTIONARY-DQN TRAINING SYSTEM      ");
        System.out.println("========================================================");
        System.out.println();
        System.out.println(" DUAL-LEVEL AI LEARNING:");
        System.out.println("   Individual Level: Each agent learns through DQN (backpropagation)");
        System.out.println("   Population Level: Evolution through natural selection");
        System.out.println();
        System.out.println(" NEURAL NETWORK ARCHITECTURE:");
        System.out.println("   Perceptron → Layer → Network → Agent → Population → Evolution");
        System.out.println();
        System.out.println(" EVOLUTIONARY ALGORITHM:");
        System.out.println("   • 10 agents compete for 10 episodes");
        System.out.println("   • Top 5 performers survive");
        System.out.println("   • Top 5 get duplicated (neural network weights copied)");
        System.out.println("   • Worst 5 from previous generation eliminated");
        System.out.println("   • Process repeats creating increasingly intelligent generations");
        System.out.println();
        System.out.println(" SHARED ARENA COMPETITION:");
        System.out.println("   • All 10 agents compete in same asteroid field");
        System.out.println("   • Direct resource competition for asteroids");
        System.out.println("   • Real-time survival pressure");
        System.out.println("   • Each agent has unique color for identification");
        System.out.println();
        System.out.println(" ADVANCED VISUALIZATION:");
        System.out.println("   • Evolution progress graphs");
        System.out.println("   • Individual agent performance metrics");
        System.out.println("   • Generation fitness tracking");
        System.out.println("   • Population statistics and survival rates");
        System.out.println();
        System.out.println(" SPEED CONTROLS:");
        System.out.println("   • 2x, 5x, 10x speed for faster evolution observation");
        System.out.println("   • Pause/Resume for detailed analysis");
        System.out.println("   • Population reset for fresh evolution cycles");
        System.out.println();
        System.out.println(" Starting evolutionary training system...");
        System.out.println();
        
        try {
            // Create and show the evolutionary training window
            EvolutionaryDQNTrainer trainer = new EvolutionaryDQNTrainer();
            trainer.setVisible(true);
            
            System.out.println(" Evolutionary training interface launched successfully!");
            System.out.println();
            System.out.println(" WATCH THE EVOLUTION:");
            System.out.println("   10 colored dots = 10 AI agents (each learning individually)");
            System.out.println("   Red circles = Asteroids (shared resources to compete for)");
            System.out.println("   White dots = Bullets from competing agents");
            System.out.println("   Right panel = Evolution statistics and agent performance");
            System.out.println();
            System.out.println(" HYBRID LEARNING IN ACTION:");
            System.out.println("   • Watch agents develop different strategies");
            System.out.println("   • Observe natural selection in real-time");
            System.out.println("   • See successful strategies spread through population");
            System.out.println("   • Experience emergent AI behavior from competition");
            System.out.println();
            System.out.println(" EXPECTED EVOLUTIONARY PROGRESSION:");
            System.out.println("   Generation 1: Random chaotic behavior");
            System.out.println("   Generation 2-5: Basic survival strategies emerge");
            System.out.println("   Generation 5-10: Advanced tactics and coordination");
            System.out.println("   Generation 10+: Sophisticated AI behavior patterns");
            System.out.println();
            System.out.println(" ADVANCED AI RESEARCH TECHNIQUES:");
            System.out.println("   This system demonstrates Population-Based Training,");
            System.out.println("   a cutting-edge technique used in modern AI research!");
            
        } catch (Exception e) {
            System.err.println(" Error starting evolutionary DQN training: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
