package game.train;

import game.ai.Agent;
import game.ai.NNAgent;
import game.component.GameCore;
import game.component.Vector2;
import game.nn.Network;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class GATrainer {
    private final GAConfig cfg;
    private final Random rng;

    public GATrainer(GAConfig cfg) {
        this.cfg = cfg;
        this.rng = new Random(cfg.seed);
    }

    public void run() {
        // Prepare a base network to get parameter count
        Network base = Network.mlp(cfg.inputSize, cfg.hidden, cfg.outputSize, cfg.seed);
        int paramCount = base.paramCount();

        // Initialize population
        List<Genome> pop = new ArrayList<>();
        for (int i = 0; i < cfg.populationSize; i++) {
            Genome g = new Genome(paramCount);
            // Start from base weights + small noise
            float[] p = new float[paramCount];
            base.getParams(p);
            for (int j = 0; j < paramCount; j++) {
                p[j] += (float) (rng.nextGaussian() * 0.02);
            }
            System.arraycopy(p, 0, g.params, 0, paramCount);
            pop.add(g);
        }

        float sigma = cfg.mutationSigma;
        Genome best = null;

        for (int gen = 0; gen < cfg.generations; gen++) {
            // Evaluate population
            for (Genome g : pop) {
                g.fitness = evaluateGenome(g);
            }
            // Sort by fitness descending
            pop.sort((a, b) -> Float.compare(b.fitness, a.fitness));
            if (best == null || pop.get(0).fitness > best.fitness) {
                best = pop.get(0).copy();
                // Save checkpoint
                saveGenome(best, cfg.checkpointPath);
            }

            // Save per-generation checkpoint for demos
            saveGenome(pop.get(0), String.format(cfg.outputDir + "/gen_%03d.bin", gen));

            float bestFit = pop.get(0).fitness;
            float mean = meanFitness(pop);
            float worst = pop.get(pop.size()-1).fitness;
            System.out.println(String.format("Gen %d | best=%.3f mean=%.3f worst=%.3f sigma=%.4f", gen, bestFit, mean, worst, sigma));

            // Create next generation
            List<Genome> next = new ArrayList<>();
            // Elites
            for (int i = 0; i < cfg.eliteCount && i < pop.size(); i++) {
                next.add(pop.get(i).copy());
            }
            // Fill rest by mutation of parents selected from top half
            int parentPool = Math.max(1, pop.size() / 2);
            while (next.size() < cfg.populationSize) {
                Genome parent = pop.get(rng.nextInt(parentPool));
                Genome child = mutate(parent, sigma);
                next.add(child);
            }
            pop = next;
            sigma *= cfg.mutationDecay;
        }

        if (best != null) {
            System.out.println("Training complete. Best fitness=" + best.fitness);
        }
    }

    private float meanFitness(List<Genome> pop) {
        double sum = 0;
        for (Genome g : pop) sum += g.fitness;
        return (float)(sum / pop.size());
        
    }

    private Genome mutate(Genome parent, float sigma) {
        Genome child = parent.copy();
        for (int i = 0; i < child.params.length; i++) {
            child.params[i] += (float) (rng.nextGaussian() * sigma);
        }
        return child;
    }

    private float evaluateGenome(Genome g) {
        // Build network and set genome params
        Network net = Network.mlp(cfg.inputSize, cfg.hidden, cfg.outputSize, rng.nextLong());
        net.setParams(g.params);
        Agent agent = new NNAgent(net, cfg.maxTurnDeg);

        // Create a headless GameCore and run episodes
        JFrame dummy = new JFrame();
        GameCore core = new GameCore(dummy);
        core.setAgent(agent);
        core.setAgentMode(true);
        core.startHeadless(1280, 720);

        float fitnessSum = 0f;
        for (int ep = 0; ep < cfg.episodesPerGenome; ep++) {
            // Reset by restarting the game state to PLAYING
            core.restartGame();
            float fit = core.runEpisodeHeadless(cfg.episodeDurationMs);
            fitnessSum += fit;
        }
        // Cleanup to prevent thread/memory buildup
        core.stopHeadless();
        return fitnessSum / cfg.episodesPerGenome;
    }

    private void saveGenome(Genome g, String path) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(g.params.length);
                for (float v : g.params) dos.writeFloat(v);
                dos.writeFloat(g.fitness);
            }
        } catch (IOException e) {
            System.err.println("Failed to save genome: " + e.getMessage());
        }
    }

    public static Genome loadGenome(String path) {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(path))) {
            int n = dis.readInt();
            Genome g = new Genome(n);
            for (int i = 0; i < n; i++) g.params[i] = dis.readFloat();
            g.fitness = dis.readFloat();
            return g;
        } catch (IOException e) {
            return null;
        }
    }
}
