package game.train;

import game.ai.QLearningAgent;
import game.component.GameCore;
import game.component.Vector2;
import game.object.Asteroid;
import game.object.Player;
import game.object.PowerUp;
import game.object.projectiles.Projectile;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrainingArena {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    private static final float DELTA_TIME = 1.5f;
    private static final int MAX_ASTEROIDS = 8;
    private static final int INITIAL_ASTEROIDS = 5;
    private static final int MAX_TICKS = 2400;
    private static final int ACTION_INTERVAL_TICKS = 6;
    private static final int FIRE_COOLDOWN_TICKS = 12;
    private static final int SPAWN_INTERVAL_TICKS = 140;

    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Asteroid> asteroids = new ArrayList<>();

    private Random random;
    private Player player;
    private QLearningAgent agent;
    private QLearningAgent.AIAction currentAction;
    private int tick;
    private int actionTick;
    private int fireCooldown;
    private int spawnCooldown;
    private int invulnerabilityTicks;
    private double fitness;
    private int destroyedAsteroids;
    private boolean done;

    public TrainingArena(QLearningAgent agent, long seed) {
        reset(agent, seed);
    }

    public void reset(QLearningAgent agent, long seed) {
        GameCore.screenSize = new Vector2(WIDTH, HEIGHT);
        this.agent = agent;
        this.random = new Random(seed);
        this.player = new Player(null);
        this.player.setPosition(new Vector2(WIDTH / 2f - (float) Player.PLAYER_DIMENSIONS / 2f,
                HEIGHT / 2f - (float) Player.PLAYER_DIMENSIONS / 2f));
        this.player.setPlayerViewAngle(0);
        this.currentAction = new QLearningAgent.AIAction(0, 0, 0, false);
        this.tick = 0;
        this.actionTick = ACTION_INTERVAL_TICKS;
        this.fireCooldown = 0;
        this.spawnCooldown = SPAWN_INTERVAL_TICKS;
        this.invulnerabilityTicks = 0;
        this.fitness = 0;
        this.destroyedAsteroids = 0;
        this.done = false;
        this.powerUps.clear();
        this.projectiles.clear();
        this.asteroids.clear();

        for (int i = 0; i < INITIAL_ASTEROIDS; i++) {
            spawnAsteroid(360f);
        }
    }

    public TrainingResult runEpisode() {
        while (!done) {
            step();
        }
        return new TrainingResult(fitness, tick, destroyedAsteroids);
    }

    public void step() {
        if (done) {
            return;
        }

        tick++;
        fitness += 0.04;
        actionTick++;
        fireCooldown = Math.max(0, fireCooldown - 1);
        invulnerabilityTicks = Math.max(0, invulnerabilityTicks - 1);
        spawnCooldown--;

        if (spawnCooldown <= 0 && asteroids.size() < MAX_ASTEROIDS) {
            spawnAsteroid(240f);
            spawnCooldown = SPAWN_INTERVAL_TICKS;
        }

        if (actionTick >= ACTION_INTERVAL_TICKS) {
            agent.setGameEnvironment(player, asteroids, powerUps, projectiles);
            currentAction = agent.getAction();
            actionTick = 0;
        }

        applyAction();
        updateProjectiles();
        updateAsteroids();
        checkProjectileAsteroidCollisions();
        checkPlayerAsteroidCollisions();

        if (tick >= MAX_TICKS || !player.isAlive()) {
            done = true;
        }
    }

    private void applyAction() {
        player.setPlayerViewAngle((float) currentAction.turnAngle);

        if (Math.abs(currentAction.forwardThrust) > 0.1) {
            player.accelerate(currentAction.forwardThrust > 0 ? 0 : 180,
                    DELTA_TIME * (float) Math.abs(currentAction.forwardThrust));
        }

        if (Math.abs(currentAction.sideThrust) > 0.1) {
            player.accelerate(currentAction.sideThrust > 0 ? 90 : 270,
                    DELTA_TIME * (float) Math.abs(currentAction.sideThrust));
        }

        if (currentAction.fire && fireCooldown == 0) {
            projectiles.add(player.shoot(1));
            fitness -= 0.02;
            fireCooldown = FIRE_COOLDOWN_TICKS;
        }

        player.updatePos(DELTA_TIME);
    }

    private void updateProjectiles() {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.update(DELTA_TIME);
            if (projectile.outOfBounds(WIDTH, HEIGHT)) {
                projectile.stop();
                projectiles.remove(i);
            }
        }
    }

    private void updateAsteroids() {
        for (int i = asteroids.size() - 1; i >= 0; i--) {
            Asteroid asteroid = asteroids.get(i);
            asteroid.update(DELTA_TIME);
            if (asteroid.isDestroyed()) {
                asteroids.remove(i);
            }
        }
    }

    private void checkProjectileAsteroidCollisions() {
        for (Asteroid asteroid : new ArrayList<>(asteroids)) {
            for (Projectile projectile : new ArrayList<>(projectiles)) {
                if (!asteroid.isDestroyed() && asteroid.getCollisionBounds().intersects(projectile.getCollisionBounds())) {
                    projectile.stop();
                    projectiles.remove(projectile);
                    asteroid.destroy();
                    destroyedAsteroids++;
                    fitness += 100.0;
                    break;
                }
            }
        }
    }

    private void checkPlayerAsteroidCollisions() {
        if (invulnerabilityTicks > 0) {
            return;
        }

        Rectangle2D playerBounds = player.getCollisionBounds();
        for (Asteroid asteroid : asteroids) {
            if (!asteroid.isDestroyed() && playerBounds.intersects(asteroid.getCollisionBounds())) {
                player.takeDamage(20);
                player.setInvulnerable(false);
                invulnerabilityTicks = 80;
                fitness -= 80.0;
                if (!player.isAlive()) {
                    fitness -= 250.0;
                }
                return;
            }
        }
    }

    private void spawnAsteroid(float minDistanceFromPlayer) {
        Vector2 position;
        int attempts = 0;
        do {
            position = randomEdgePosition();
            attempts++;
        } while (attempts < 50 && position.distance(player.getCenter()) < minDistanceFromPlayer);

        Vector2 center = new Vector2(WIDTH / 2f, HEIGHT / 2f);
        Vector2 direction = new Vector2(center.x - position.x, center.y - position.y);
        direction.normalize();

        float angleJitter = (random.nextFloat() - 0.5f) * 70f;
        double angle = Math.atan2(direction.y, direction.x) + Math.toRadians(angleJitter);
        float speed = 0.7f + random.nextFloat() * 1.4f;
        int[] sizes = {32, 48, 64};

        asteroids.add(new Asteroid(position,
                new Vector2((float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed),
                sizes[random.nextInt(sizes.length)]));
    }

    private Vector2 randomEdgePosition() {
        switch (random.nextInt(4)) {
            case 0:
                return new Vector2(random.nextFloat() * WIDTH, -64);
            case 1:
                return new Vector2(WIDTH + 64, random.nextFloat() * HEIGHT);
            case 2:
                return new Vector2(random.nextFloat() * WIDTH, HEIGHT + 64);
            default:
                return new Vector2(-64, random.nextFloat() * HEIGHT);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public List<Asteroid> getAsteroids() {
        return asteroids;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public double getFitness() {
        return fitness;
    }

    public int getDestroyedAsteroids() {
        return destroyedAsteroids;
    }

    public boolean isDone() {
        return done;
    }

    public static class TrainingResult {
        public final double fitness;
        public final int ticks;
        public final int destroyedAsteroids;

        public TrainingResult(double fitness, int ticks, int destroyedAsteroids) {
            this.fitness = fitness;
            this.ticks = ticks;
            this.destroyedAsteroids = destroyedAsteroids;
        }
    }
}
