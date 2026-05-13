package game.train;

import game.ai.QLearningAgent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EvolutionTrainer {
    public interface Listener {
        void onGenerationComplete(GenerationStats stats, QLearningAgent champion);
    }

    private static final int POPULATION_SIZE = 32;
    private static final int ELITE_COUNT = 4;
    private static final double MUTATION_RATE = 0.035;
    private static final double MUTATION_STRENGTH = 0.08;

    private final Random random = new Random();
    private final Listener listener;
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
    );
    private List<QLearningAgent> population = new ArrayList<>();
    private boolean running = true;
    private int generation = 0;
    private double bestEverFitness = Double.NEGATIVE_INFINITY;

    public EvolutionTrainer(Listener listener) {
        this.listener = listener;
        initializePopulation();
    }

    public void run() {
        while (running) {
            runGeneration();
        }
    }

    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    private void initializePopulation() {
        QLearningAgent baseAgent = new QLearningAgent(TrainingArena.WIDTH, TrainingArena.HEIGHT);
        baseAgent.setLearningEnabled(false);

        for (int i = 0; i < POPULATION_SIZE; i++) {
            QLearningAgent agent = new QLearningAgent(TrainingArena.WIDTH, TrainingArena.HEIGHT, false);
            agent.copyPolicyFrom(baseAgent);
            if (i > 0) {
                agent.mutatePolicy(MUTATION_RATE, MUTATION_STRENGTH, random);
            }
            agent.setLearningEnabled(false);
            population.add(agent);
        }
    }

    private void runGeneration() {
        generation++;
        long generationSeed = random.nextLong();
        List<ScoredAgent> scoredAgents = new ArrayList<>();
        List<Future<ScoredAgent>> futures = new ArrayList<>();

        for (int i = 0; i < population.size() && running; i++) {
            QLearningAgent agent = population.get(i);
            futures.add(executor.submit(() -> {
                TrainingArena arena = new TrainingArena(agent, generationSeed);
                TrainingArena.TrainingResult result = arena.runEpisode();
                return new ScoredAgent(agent, result.fitness, result.destroyedAsteroids, result.ticks);
            }));
        }

        for (Future<ScoredAgent> future : futures) {
            if (!running) {
                return;
            }
            try {
                scoredAgents.add(future.get());
            } catch (Exception e) {
                System.err.println("Agent evaluation failed: " + e.getMessage());
            }
        }

        if (!running || scoredAgents.isEmpty()) {
            return;
        }

        scoredAgents.sort(Comparator.comparingDouble((ScoredAgent agent) -> agent.fitness).reversed());

        double totalFitness = 0;
        int totalKills = 0;
        for (ScoredAgent scoredAgent : scoredAgents) {
            totalFitness += scoredAgent.fitness;
            totalKills += scoredAgent.destroyedAsteroids;
        }

        ScoredAgent best = scoredAgents.get(0);
        QLearningAgent champion = cloneAgent(best.agent);
        if (best.fitness > bestEverFitness) {
            bestEverFitness = best.fitness;
            QLearningAgent checkpointAgent = cloneAgent(best.agent);
            checkpointAgent.setLearningEnabled(true);
            checkpointAgent.saveCheckpoint();
        }

        listener.onGenerationComplete(new GenerationStats(
                generation,
                best.fitness,
                totalFitness / scoredAgents.size(),
                bestEverFitness,
                best.destroyedAsteroids,
                totalKills / (double) scoredAgents.size(),
                scoredAgents.size()
        ), champion);

        population = breedNextGeneration(scoredAgents);
    }

    private List<QLearningAgent> breedNextGeneration(List<ScoredAgent> scoredAgents) {
        List<QLearningAgent> nextPopulation = new ArrayList<>();

        for (int i = 0; i < ELITE_COUNT; i++) {
            nextPopulation.add(cloneAgent(scoredAgents.get(i).agent));
        }

        while (nextPopulation.size() < POPULATION_SIZE) {
            ScoredAgent parent = tournamentSelect(scoredAgents);
            QLearningAgent child = cloneAgent(parent.agent);
            child.mutatePolicy(MUTATION_RATE, MUTATION_STRENGTH, random);
            nextPopulation.add(child);
        }

        return nextPopulation;
    }

    private ScoredAgent tournamentSelect(List<ScoredAgent> scoredAgents) {
        ScoredAgent best = null;
        int candidateLimit = Math.max(ELITE_COUNT, scoredAgents.size() / 2);

        for (int i = 0; i < 3; i++) {
            ScoredAgent candidate = scoredAgents.get(random.nextInt(candidateLimit));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }

        return best;
    }

    private QLearningAgent cloneAgent(QLearningAgent source) {
        QLearningAgent clone = new QLearningAgent(TrainingArena.WIDTH, TrainingArena.HEIGHT, false);
        clone.copyPolicyFrom(source);
        clone.setLearningEnabled(false);
        return clone;
    }

    private static class ScoredAgent {
        private final QLearningAgent agent;
        private final double fitness;
        private final int destroyedAsteroids;
        private final int ticks;

        private ScoredAgent(QLearningAgent agent, double fitness, int destroyedAsteroids, int ticks) {
            this.agent = agent;
            this.fitness = fitness;
            this.destroyedAsteroids = destroyedAsteroids;
            this.ticks = ticks;
        }
    }

    public static class GenerationStats {
        public final int generation;
        public final double bestFitness;
        public final double averageFitness;
        public final double bestEverFitness;
        public final int bestDestroyedAsteroids;
        public final double averageDestroyedAsteroids;
        public final int populationSize;

        public GenerationStats(int generation, double bestFitness, double averageFitness, double bestEverFitness,
                               int bestDestroyedAsteroids, double averageDestroyedAsteroids, int populationSize) {
            this.generation = generation;
            this.bestFitness = bestFitness;
            this.averageFitness = averageFitness;
            this.bestEverFitness = bestEverFitness;
            this.bestDestroyedAsteroids = bestDestroyedAsteroids;
            this.averageDestroyedAsteroids = averageDestroyedAsteroids;
            this.populationSize = populationSize;
        }
    }
}
