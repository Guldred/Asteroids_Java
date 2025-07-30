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
    
    public static int handleProjectileAsteroidCollisions(List<Projectile> projectiles, List<Asteroid> asteroids) {
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
                            // Destroy asteroid
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
                        // Destroy asteroid
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
    
    public static boolean handlePlayerAsteroidCollisions(Player player, List<Asteroid> asteroids) {
        for (Asteroid asteroid : asteroids) {
            if (checkCollision(player, asteroid)) {
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
