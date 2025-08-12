package game.component;

import game.object.Asteroid;
import game.object.Player;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AsteroidManager {
    private List<Asteroid> asteroids;
    private Random random;
    private int screenWidth;
    private int screenHeight;
    private int maxAsteroids;
    private long lastSpawnTime;
    private long spawnInterval; // milliseconds between spawns
    private Player player; // reference for fair spawning
    private float safeSpawnDistance = 220f; // min distance from player for regular spawns
    private float initialSpawnDistance = 360f; // larger buffer for level start
    private long graceEndTime = 0; // during grace, no periodic spawns
    private long arcBlockEndTime = 0; // during this window, avoid player's forward arc
    private float arcBlockWidthDeg = 70f;
    private long telegraphDelayMs = 0; // telegraphs disabled
    private float speedMultiplier = 1.0f; // scales asteroid speeds for difficulty
    
    public AsteroidManager(int screenWidth, int screenHeight, int maxAsteroids) {
        this.asteroids = new ArrayList<>();
        this.random = new Random();
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.maxAsteroids = maxAsteroids;
        this.lastSpawnTime = System.currentTimeMillis();
        this.spawnInterval = 3000; // spawn every 3 seconds
    }
    
    public void setPlayer(Player player) {
        this.player = player;
    }
    
    public void setSafeSpawnDistance(float distance) { this.safeSpawnDistance = distance; }
    public void setInitialSpawnDistance(float distance) { this.initialSpawnDistance = distance; }
    public void setSpawnInterval(long ms) { this.spawnInterval = ms; }
    public void setTelegraphDelayMs(long ms) { this.telegraphDelayMs = 0; }
    public void setArcBlockWidthDeg(float degrees) { this.arcBlockWidthDeg = degrees; }
    public void setSpeedMultiplier(float mult) { this.speedMultiplier = Math.max(0.5f, mult); }
    
    public void startGracePeriod(long ms) {
        this.graceEndTime = System.currentTimeMillis() + ms;
    }
    
    public void startLevelWindow(long graceMs, long arcMs) {
        long now = System.currentTimeMillis();
        this.graceEndTime = now + graceMs;
        this.arcBlockEndTime = now + arcMs;
    }
    
    public void update() {
        // Spawn new asteroids if needed
        long currentTime = System.currentTimeMillis();
        if (currentTime >= graceEndTime && asteroids.size() < maxAsteroids && currentTime - lastSpawnTime > spawnInterval) {
            spawnRandomAsteroidWithMinDistance(safeSpawnDistance); // regular spawn uses safeSpawnDistance
            lastSpawnTime = currentTime;
        }
        
        // Remove destroyed asteroids
        asteroids.removeIf(asteroid -> asteroid.isDestroyed());
    }
    
    public void spawnRandomAsteroid() {
        spawnRandomAsteroidWithMinDistance(safeSpawnDistance);
    }
    
    private void spawnRandomAsteroidWithMinDistance(float minDistanceFromPlayer) {
        float effectiveMin = Math.max(minDistanceFromPlayer, dynamicMinDistance());
        Vector2 position = getRandomEdgePositionOutsideSafeZone(effectiveMin);
        // Random velocity towards center of screen
        Vector2 velocity = getRandomVelocityTowardsCenter(position);
        int[] sizes = {32, 48, 64};
        int size = sizes[random.nextInt(sizes.length)];
        Asteroid asteroid = new Asteroid(position, velocity, size);
        asteroids.add(asteroid);
    }
    
    private Vector2 getRandomEdgePosition() {
        int edge = random.nextInt(4); // 0=top, 1=right, 2=bottom, 3=left
        Vector2 position = new Vector2();
        
        switch (edge) {
            case 0: // top
                position.x = random.nextFloat() * screenWidth;
                position.y = -64; // spawn just off screen
                break;
            case 1: // right
                position.x = screenWidth + 64;
                position.y = random.nextFloat() * screenHeight;
                break;
            case 2: // bottom
                position.x = random.nextFloat() * screenWidth;
                position.y = screenHeight + 64;
                break;
            case 3: // left
                position.x = -64;
                position.y = random.nextFloat() * screenHeight;
                break;
        }
        
        return position;
    }
    
    private Vector2 getRandomEdgePositionOutsideSafeZone(float minDistanceFromPlayer) {
        if (player == null) {
            return getRandomEdgePosition();
        }
        Vector2 pos;
        int attempts = 0;
        do {
            pos = getRandomEdgePosition();
            attempts++;
            if (attempts > 40) break; // fail-safe
        } while (distance(pos, player.getCenter()) < minDistanceFromPlayer || isInPlayerForwardArc(pos));
        return pos;
    }
    
    private Vector2 getRandomVelocityTowardsCenter(Vector2 position) {
        // Calculate direction towards center of screen
        Vector2 center = new Vector2(screenWidth / 2f, screenHeight / 2f);
        Vector2 direction = new Vector2(center.x - position.x, center.y - position.y);
        
        // Normalize and apply random speed
        float length = (float) Math.sqrt(direction.x * direction.x + direction.y * direction.y);
        if (length > 0) {
            direction.x /= length;
            direction.y /= length;
        }
        
        // Base speed for fairness, scaled by difficulty multiplier
        float speed = (0.8f + random.nextFloat() * 1.7f) * speedMultiplier;
        
        // Add some randomness to direction
        float angleVariation = (random.nextFloat() - 0.5f) * 60; // ±30 degrees
        double angle = Math.atan2(direction.y, direction.x) + Math.toRadians(angleVariation);
        
        // Avoid directly targeting player when spawning close
        if (player != null) {
            Vector2 toPlayer = new Vector2(player.getCenter().x - position.x, player.getCenter().y - position.y);
            double aToPlayer = Math.atan2(toPlayer.y, toPlayer.x);
            double diff = smallestAngleBetween(angle, aToPlayer);
            if (distance(position, player.getCenter()) < 420 && Math.abs(diff) < Math.toRadians(20)) {
                // push angle away a bit
                angle += Math.toRadians(20) * (random.nextBoolean() ? 1 : -1);
            }
        }
        
        return new Vector2(
            (float) Math.cos(angle) * speed,
            (float) Math.sin(angle) * speed
        );
    }
    
    public void spawnInitialAsteroids(int count) {
        for (int i = 0; i < count; i++) {
            spawnRandomAsteroidWithMinDistance(initialSpawnDistance);
        }
    }
    
    public List<Asteroid> getAsteroids() {
        return asteroids;
    }
    
    public void removeAsteroid(Asteroid asteroid) {
        asteroid.destroy();
    }
    
    public int getAsteroidCount() {
        return asteroids.size();
    }
    
    private float distance(Vector2 a, Vector2 b) {
        float dx = a.x - b.x; float dy = a.y - b.y; return (float)Math.sqrt(dx*dx + dy*dy);
    }
    
    private double smallestAngleBetween(double a, double b) {
        double diff = a - b;
        while (diff > Math.PI) diff -= 2*Math.PI;
        while (diff < -Math.PI) diff += 2*Math.PI;
        return diff;
    }
    
    private boolean isInPlayerForwardArc(Vector2 pos) {
        if (player == null) return false;
        long now = System.currentTimeMillis();
        if (now > arcBlockEndTime) return false;
        Vector2 pc = player.getCenter();
        double toPos = Math.atan2(pos.y - pc.y, pos.x - pc.x);
        double playerAngle = Math.toRadians(player.getAngle());
        double diff = Math.abs(smallestAngleBetween(toPos, playerAngle));
        return Math.toDegrees(diff) < (arcBlockWidthDeg / 2.0);
    }
    
    private float dynamicMinDistance() {
        if (player == null) return safeSpawnDistance;
        Vector2 v = player.getVelocity();
        float speed = (float)Math.sqrt(v.x*v.x + v.y*v.y);
        // Map speed (0..?) to extra distance (adds up to ~80px when slow, less when fast)
        float extra = 80f * (1f / (1f + speed * 0.6f));
        return safeSpawnDistance + extra;
    }
    
    public void drawTelegraphs(Graphics2D g2) {
        // Telemetry disabled per design change
    }

    public void splitAsteroid(Asteroid parent) {
        int parentSize = parent.getSize();
        int childSize;
        if (parentSize >= 64) childSize = 32;
        else if (parentSize >= 48) childSize = 32;
        else return; // 32px: do not split further

        int pieces = 2 + random.nextInt(2); // 2-3 pieces
        Vector2 center = parent.getCenter();
        for (int i = 0; i < pieces; i++) {
            if (asteroids.size() >= maxAsteroids) break; // respect cap
            float angleDeg = random.nextFloat() * 360f;
            double angle = Math.toRadians(angleDeg);
            float speed = (0.7f + random.nextFloat() * 1.0f) * Math.max(0.9f, speedMultiplier * 0.95f); // children slightly slower than parents
            Vector2 pos = new Vector2(center.x + (float)Math.cos(angle) * (parentSize/4f),
                                       center.y + (float)Math.sin(angle) * (parentSize/4f));
            Vector2 vel = new Vector2((float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed);
            Asteroid child = new Asteroid(pos, vel, childSize);
            asteroids.add(child);
        }
    }
}
