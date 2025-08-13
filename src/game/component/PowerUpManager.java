package game.component;

import game.object.Asteroid;
import game.object.PowerUp;
import game.object.PowerUp.PowerUpType;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class PowerUpManager {
    private List<PowerUp> powerUps;
    private Random random;
    private long lastSpawnTime;
    private final long SPAWN_INTERVAL = 15000; // 15 seconds between natural spawns
    private final float ASTEROID_SPAWN_CHANCE = 0.3f; // 30% chance to spawn from destroyed asteroid
    
    public PowerUpManager() {
        powerUps = new ArrayList<>();
        random = new Random();
        lastSpawnTime = System.currentTimeMillis();
    }
    
    public void update() {
        // Check for natural spawns
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSpawnTime > SPAWN_INTERVAL) {
            spawnRandomPowerUp();
            lastSpawnTime = currentTime;
        }
        
        // Remove expired or collected power-ups
        Iterator<PowerUp> iterator = powerUps.iterator();
        while (iterator.hasNext()) {
            PowerUp powerUp = iterator.next();
            if (powerUp.isCollected() || powerUp.isExpired()) {
                iterator.remove();
            }
        }
    }
    
    public void checkAsteroidDestroyed(Asteroid asteroid) {
        // Chance to spawn power-up when asteroid is destroyed
        if (random.nextFloat() < ASTEROID_SPAWN_CHANCE) {
            spawnPowerUpAtPosition(asteroid.getCenter());
        }
    }
    
    private void spawnRandomPowerUp() {
        // Spawn at random position
        int screenWidth = (int) GameCore.screenSize.x;
        int screenHeight = (int) GameCore.screenSize.y;
        
        Vector2 position = new Vector2(
            random.nextInt(screenWidth - 100) + 50, // Keep away from edges
            random.nextInt(screenHeight - 100) + 50
        );
        
        spawnPowerUpAtPosition(position);
    }
    
    private void spawnPowerUpAtPosition(Vector2 position) {
        // Choose random power-up type
        PowerUpType[] types = PowerUpType.values();
        PowerUpType type = types[random.nextInt(types.length)];
        
        PowerUp powerUp = new PowerUp(position, type);
        powerUps.add(powerUp);
    }
    
    public void draw(Graphics2D g2) {
        for (PowerUp powerUp : powerUps) {
            powerUp.draw(g2);
        }
    }
    
    public List<PowerUp> getPowerUps() {
        return powerUps;
    }
    
    public void clear() {
        powerUps.clear();
    }
}
