package game.train;

public class TrainerMain {
    public static void main(String[] args) {
        GAConfig cfg = new GAConfig();
        // Simple CLI overrides
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--preset":
                    if (i + 1 < args.length) {
                        String p = args[++i];
                        if (p.equalsIgnoreCase("fast")) {
                            cfg.populationSize = 20;
                            cfg.episodesPerGenome = 1;
                            cfg.episodeDurationMs = 4000;
                            cfg.generations = 8;
                            cfg.eliteCount = 2;
                            cfg.mutationSigma = 0.08f;
                            cfg.mutationDecay = 0.99f;
                            cfg.outputDir = "models_fast";
                            cfg.checkpointPath = cfg.outputDir + "/best_genome.bin";
                        }
                    }
                    break;
                case "--pop":
                    if (i + 1 < args.length) cfg.populationSize = Integer.parseInt(args[++i]);
                    break;
                case "--gens":
                    if (i + 1 < args.length) cfg.generations = Integer.parseInt(args[++i]);
                    break;
                case "--episodeMs":
                    if (i + 1 < args.length) cfg.episodeDurationMs = Long.parseLong(args[++i]);
                    break;
                case "--episodesPerGenome":
                    if (i + 1 < args.length) cfg.episodesPerGenome = Integer.parseInt(args[++i]);
                    break;
                case "--sigma":
                    if (i + 1 < args.length) cfg.mutationSigma = Float.parseFloat(args[++i]);
                    break;
                case "--out":
                    if (i + 1 < args.length) { cfg.outputDir = args[++i]; cfg.checkpointPath = cfg.outputDir + "/best_genome.bin"; }
                    break;
                case "--continue":
                    if (i + 1 < args.length) cfg.continueFromCheckpoint = args[++i];
                    break;
                default:
                    break;
            }
        }
        GATrainer trainer = new GATrainer(cfg);
        trainer.run();
    }
}
