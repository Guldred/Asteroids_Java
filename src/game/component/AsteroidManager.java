package game.component;

import game.object.Asteroid;
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
    
    public AsteroidManager(int screenWidth, int screenHeight, int maxAsteroids) {
        this.asteroids = new ArrayList<>();
        this.random = new Random();
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.maxAsteroids = maxAsteroids;
        this.lastSpawnTime = System.currentTimeMillis();
        this.spawnInterval = 3000; // spawn every 3 seconds
    }
    
    public void update() {
        // Spawn new asteroids if needed
        long currentTime = System.currentTimeMillis();
        if (asteroids.size() < maxAsteroids && currentTime - lastSpawnTime > spawnInterval) {
            spawnRandomAsteroid();
            lastSpawnTime = currentTime;
        }
        
        // Remove destroyed asteroids
        asteroids.removeIf(asteroid -> asteroid.isDestroyed());
    }
    
    public void spawnRandomAsteroid() {
        // Random position at screen edges
        Vector2 position = getRandomEdgePosition();
        
        // Random velocity towards center of screen
        Vector2 velocity = getRandomVelocityTowardsCenter(position);
        
        // Random size (32, 48, or 64)
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
        
        // Random speed between 1 and 4
        float speed = 1 + random.nextFloat() * 3;
        
        // Add some randomness to direction
        float angleVariation = (random.nextFloat() - 0.5f) * 60; // ±30 degrees
        double angle = Math.atan2(direction.y, direction.x) + Math.toRadians(angleVariation);
        
        return new Vector2(
            (float) Math.cos(angle) * speed,
            (float) Math.sin(angle) * speed
        );
    }
    
    public void spawnInitialAsteroids(int count) {
        for (int i = 0; i < count; i++) {
            spawnRandomAsteroid();
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
}
