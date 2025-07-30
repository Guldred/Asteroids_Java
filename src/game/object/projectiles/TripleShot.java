package game.object.projectiles;

import game.component.Vector2;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

public class TripleShot extends BaseProjectile {
    private List<Projectile> subProjectiles;
    private final float SPREAD_ANGLE = 15f; // Angle between shots
    
    public TripleShot(Vector2 position, Vector2 velocity, float angle, float speed) {
        super(position, velocity, angle, speed);
        this.size = 10;
        this.shape = new Ellipse2D.Float(0, 0, size, size);
        this.color = Color.MAGENTA;
        
        // Create the three projectiles
        subProjectiles = new ArrayList<>();
        
        // Center projectile
        subProjectiles.add(new EnergyBall(position, velocity, angle, speed));
        
        // Left projectile
        subProjectiles.add(new EnergyBall(position, velocity, angle - SPREAD_ANGLE, speed * 0.9f));
        
        // Right projectile
        subProjectiles.add(new EnergyBall(position, velocity, angle + SPREAD_ANGLE, speed * 0.9f));
        
        startUpdate();
    }
    
    @Override
    public void draw(Graphics2D g2D) {
        for (Projectile projectile : subProjectiles) {
            projectile.draw(g2D);
        }
    }
    
    @Override
    protected void onUpdate(float deltaTime) {
        for (Projectile projectile : subProjectiles) {
            if (projectile instanceof BaseProjectile) {
                ((BaseProjectile) projectile).onUpdate(deltaTime);
            }
        }
    }
    
    @Override
    public boolean outOfBounds(int width, int height) {
        // Check if all sub-projectiles are out of bounds
        for (Projectile projectile : subProjectiles) {
            if (!projectile.outOfBounds(width, height)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void stop() {
        super.stop();
        for (Projectile projectile : subProjectiles) {
            if (projectile instanceof BaseProjectile) {
                ((BaseProjectile) projectile).stop();
            }
        }
    }
    
    // For collision detection, we'll use the center projectile's position
    @Override
    public Vector2 getPosition() {
        return subProjectiles.get(0).getPosition();
    }
    
    // Return all three projectiles for collision detection
    public List<Projectile> getSubProjectiles() {
        return subProjectiles;
    }
}
