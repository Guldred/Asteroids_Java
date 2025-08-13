package game.ai;

public interface Agent {
    // Decide next action based on current observation state vector
    AgentInput decide(float[] state);
    default void onEpisodeStart() {}
}
