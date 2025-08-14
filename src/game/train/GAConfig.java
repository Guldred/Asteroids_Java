package game.train;

public class GAConfig {
    public int populationSize = 80;
    public int eliteCount = 2;
    public float mutationSigma = 0.05f;
    public float mutationDecay = 0.995f;
    public int generations = 50;
    public int episodesPerGenome = 2;
    public long episodeDurationMs = 60000; // 60s budget per episode
    public long seed = 1234L;

    public int inputSize = 8 + 3 * 6; // must match buildAgentState
    public int[] hidden = new int[]{32, 32};
    public int outputSize = 6;
    public float maxTurnDeg = 6f;

    // Serialization
    public String outputDir = "models";
    public String checkpointPath = outputDir + "/best_genome.bin";
    public String continueFromCheckpoint = null; // Path to checkpoint to continue from
}
