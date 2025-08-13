package game.component;

import game.object.Asteroid;
import game.object.Player;
import game.object.PowerUp;
import game.object.projectiles.Projectile;
import game.object.projectiles.TripleShot;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class CollisionDetector {
    
    public static boolean checkCollision(Projectile projectile, Asteroid asteroid) {
        if (asteroid.isDestroyed()) {
            return false;
        }
        
        // Simple bounding box collision for projectiles (they're small)
        Rectangle2D projectileBounds = new Rectangle2D.Float(
            projectile.getPosition().x - 2,
            projectile.getPosition().y - 2,
            4, 4
        );
        
        Area asteroidShape = asteroid.getShape();
        return asteroidShape.intersects(projectileBounds);
    }
    
    public static boolean checkCollision(Player player, Asteroid asteroid) {
        if (asteroid.isDestroyed()) {
            return false;
        }
        
        // Player collision using bounding box (simplified)
        Rectangle2D playerBounds = new Rectangle2D.Float(
            player.getPosition().x,
            player.getPosition().y,
            (float) Player.PLAYER_DIMENSIONS,
            (float) Player.PLAYER_DIMENSIONS
        );
        
        Area asteroidShape = asteroid.getShape();
        return asteroidShape.intersects(playerBounds);
    }
    
    public static int handleProjectileAsteroidCollisions(List<Projectile> projectiles, List<Asteroid> asteroids, AsteroidManager asteroidManager) {
        int destroyedCount = 0;
        
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            // Check if index is still valid (projectiles list might have been modified)
            if (i >= projectiles.size()) continue;
            
            Projectile projectile = projectiles.get(i);
            if (projectile == null) continue;
            
            // Special handling for TripleShot
            if (projectile instanceof TripleShot) {
                TripleShot tripleShot = (TripleShot) projectile;
                List<Projectile> subProjectiles = tripleShot.getSubProjectiles();
                
                boolean hitDetected = false;
                
                // Check each sub-projectile for collision
                for (Projectile subProjectile : subProjectiles) {
                    for (Asteroid asteroid : asteroids) {
                        if (!asteroid.isDestroyed() && checkCollision(subProjectile, asteroid)) {
                            // Split larger asteroids once, then destroy
                            if (!asteroid.isSplitProcessed() && asteroid.getSize() > 32) {
                                asteroid.markSplitProcessed();
                                if (asteroidManager != null) {
                                    asteroidManager.splitAsteroid(asteroid);
                                }
                            }
                            asteroid.destroy();
                            destroyedCount++;
                            
                            hitDetected = true;
                            break;
                        }
                    }
                    
                    if (hitDetected) break;
                }
                
                if (hitDetected) {
                    // Remove the entire triple shot if any part hits
                    projectile.stop();
                    // Check if index is still valid before removing
                    if (i < projectiles.size()) {
                        projectiles.remove(i);
                    }
                }
            } else {
                // Normal projectile handling
                for (Asteroid asteroid : asteroids) {
                    if (checkCollision(projectile, asteroid)) {
                        // Split larger asteroids once, then destroy
                        if (!asteroid.isSplitProcessed() && asteroid.getSize() > 32) {
                            asteroid.markSplitProcessed();
                            if (asteroidManager != null) {
                                asteroidManager.splitAsteroid(asteroid);
                            }
                        }
                        asteroid.destroy();
                        destroyedCount++;
                        
                        // Remove projectile
                        projectile.stop();
                        // Check if index is still valid before removing
                        if (i < projectiles.size()) {
                            projectiles.remove(i);
                        }
                        break; // Projectile can only hit one asteroid
                    }
                }
            }
        }
        
        return destroyedCount;
    }
    
    public static boolean handlePlayerAsteroidCollisions(Player player, List<Asteroid> asteroids, AsteroidManager asteroidManager) {
        // If player is invulnerable, ignore collisions entirely
        if (player.isInvulnerable()) {
            return false;
        }
        for (Asteroid asteroid : asteroids) {
            if (checkCollision(player, asteroid)) {
                // Make asteroid behave like it was shot: split (if applicable) and destroy
                if (!asteroid.isSplitProcessed() && asteroid.getSize() > 32) {
                    asteroid.markSplitProcessed();
                    if (asteroidManager != null) {
                        asteroidManager.splitAsteroid(asteroid);
                    }
                }
                asteroid.destroy();
                return true; // Player hit
            }
        }
        return false;
    }
    
    public static boolean checkCollision(Player player, PowerUp powerUp) {
        Rectangle2D playerBounds = new Rectangle2D.Float(
            player.getPosition().x,
            player.getPosition().y,
            (float) Player.PLAYER_DIMENSIONS,
            (float) Player.PLAYER_DIMENSIONS
        );
        
        return powerUp.getCollisionShape().intersects(playerBounds);
    }
}
