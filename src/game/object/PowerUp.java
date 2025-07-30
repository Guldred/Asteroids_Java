package game.object;

import game.component.GameCore;
import game.component.Updateable;
import game.component.Vector2;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Random;

public class PowerUp extends Updateable {
    
    public enum PowerUpType {
        HEALTH,
        SHIELD,
        RAPID_FIRE,
        TRIPLE_SHOT
    }
    
    private Vector2 position;
    private Vector2 velocity;
    private final int SIZE = 20;
    private final PowerUpType type;
    private final Color color;
    private boolean collected = false;
    private long creationTime;
    private final long LIFETIME = 10000; // 10 seconds lifetime
    private float angle = 0;
    private float rotationSpeed;
    
    public PowerUp(Vector2 position, PowerUpType type) {
        super();
        this.position = new Vector2(position);
        this.type = type;
        
        // Set color based on type
        switch (type) {
            case HEALTH:
                this.color = Color.GREEN;
                break;
            case SHIELD:
                this.color = Color.BLUE;
                break;
            case RAPID_FIRE:
                this.color = Color.YELLOW;
                break;
            case TRIPLE_SHOT:
                this.color = Color.MAGENTA;
                break;
            default:
                this.color = Color.WHITE;
        }
        
        // Random slow movement
        Random random = new Random();
        this.velocity = new Vector2(
            (random.nextFloat() - 0.5f) * 0.5f,
            (random.nextFloat() - 0.5f) * 0.5f
        );
        
        this.rotationSpeed = (random.nextFloat() - 0.5f) * 2;
        this.creationTime = System.currentTimeMillis();
        
        startUpdate();
    }
    
    @Override
    protected void onUpdate(float deltaTime) {
        // Move the power-up
        position.add(new Vector2(velocity.x * deltaTime, velocity.y * deltaTime));
        
        // Rotate the power-up
        angle += rotationSpeed * deltaTime;
        
        // Check if out of bounds and bounce
        checkOutOfBounds();
    }
    
    public void draw(Graphics2D g2) {
        if (collected) return;
        
        // Calculate alpha based on lifetime (fade out near end of life)
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - creationTime;
        float alpha = 1.0f;
        
        if (elapsedTime > LIFETIME * 0.7f) {
            alpha = 1.0f - ((elapsedTime - LIFETIME * 0.7f) / (LIFETIME * 0.3f));
        }
        
        // Pulsate size based on time
        float pulseFactor = (float) (1.0 + 0.2 * Math.sin(elapsedTime / 200.0));
        int pulseSize = (int) (SIZE * pulseFactor);
        
        // Save original transform
        AffineTransform oldTransform = g2.getTransform();
        
        // Apply transform for rotation
        g2.translate(position.x, position.y);
        g2.rotate(Math.toRadians(angle), SIZE / 2, SIZE / 2);
        
        // Draw power-up
        Color drawColor = new Color(
            color.getRed(), 
            color.getGreen(), 
            color.getBlue(), 
            (int) (255 * alpha)
        );
        g2.setColor(drawColor);
        
        // Draw outer circle
        g2.fillOval(0, 0, pulseSize, pulseSize);
        
        // Draw inner symbol based on type
        g2.setColor(Color.WHITE);
        switch (type) {
            case HEALTH:
                // Draw plus sign
                g2.fillRect(pulseSize / 4, pulseSize / 2 - pulseSize / 10, pulseSize / 2, pulseSize / 5);
                g2.fillRect(pulseSize / 2 - pulseSize / 10, pulseSize / 4, pulseSize / 5, pulseSize / 2);
                break;
            case SHIELD:
                // Draw shield symbol
                g2.drawOval(pulseSize / 4, pulseSize / 4, pulseSize / 2, pulseSize / 2);
                break;
            case RAPID_FIRE:
                // Draw lightning bolt
                int[] xPoints = {pulseSize / 4, pulseSize / 2, pulseSize / 3, pulseSize * 3 / 4};
                int[] yPoints = {pulseSize / 4, pulseSize / 2, pulseSize / 2, pulseSize * 3 / 4};
                g2.fillPolygon(xPoints, yPoints, 4);
                break;
            case TRIPLE_SHOT:
                // Draw three dots
                int dotSize = pulseSize / 6;
                g2.fillOval(pulseSize / 2 - dotSize / 2, pulseSize / 4, dotSize, dotSize);
                g2.fillOval(pulseSize / 3 - dotSize / 2, pulseSize * 2 / 3, dotSize, dotSize);
                g2.fillOval(pulseSize * 2 / 3 - dotSize / 2, pulseSize * 2 / 3, dotSize, dotSize);
                break;
        }
        
        // Restore original transform
        g2.setTransform(oldTransform);
    }
    
    public Area getCollisionShape() {
        AffineTransform t = new AffineTransform();
        t.translate(position.x, position.y);
        return new Area(t.createTransformedShape(new Ellipse2D.Float(0, 0, SIZE, SIZE)));
    }
    
    public boolean isCollected() {
        return collected;
    }
    
    public void collect() {
        collected = true;
        // Stop the update loop by setting start to false
        this.start = false;
    }
    
    public PowerUpType getType() {
        return type;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > LIFETIME;
    }
    
    private void checkOutOfBounds() {
        if (position.x < 0) {
            position.x = 0;
            velocity.x *= -1;
        } else if (position.x + SIZE > GameCore.screenSize.x) {
            position.x = GameCore.screenSize.x - SIZE;
            velocity.x *= -1;
        }
        
        if (position.y < 0) {
            position.y = 0;
            velocity.y *= -1;
        } else if (position.y + SIZE > GameCore.screenSize.y) {
            position.y = GameCore.screenSize.y - SIZE;
            velocity.y *= -1;
        }
    }
}
