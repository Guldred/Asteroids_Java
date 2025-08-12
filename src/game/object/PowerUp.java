package game.object;

import game.component.GameCore;
import game.component.Updateable;
import game.component.Vector2;

import javax.swing.*;
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
    private final int SIZE = 32; // Increased size for better visibility
    private final PowerUpType type;
    private final Color color; // Keep color for particle effects
    private boolean collected = false;
    private long creationTime;
    private final long LIFETIME = 10000; // 10 seconds lifetime
    private float angle = 0;
    private float rotationSpeed;
    private Image image;
    
    public PowerUp(Vector2 position, PowerUpType type) {
        super();
        this.position = new Vector2(position);
        this.type = type;
        
        // Set color based on type (for particles and effects)
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
        
        // Load appropriate image based on type
        loadImage();
        
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
    
    private void loadImage() {
        String imagePath;
        
        // Select appropriate image based on power-up type
        switch (type) {
            case HEALTH:
                imagePath = "src/game/resource/img/powerups/Item_Powerup_Heart_2.png"; // Heart for health
                break;
            case SHIELD:
                imagePath = "src/game/resource/img/powerups/Box_Item_3.png"; // Blue shield box
                break;
            case RAPID_FIRE:
                imagePath = "src/game/resource/img/powerups/Box_Item_11.png"; // Yellow speed box
                break;
            case TRIPLE_SHOT:
                imagePath = "src/game/resource/img/powerups/Item_Powerup_Weapon_4.png"; // Weapon icon for multi-shot
                break;
            default:
                imagePath = "src/game/resource/img/powerups/Box_Item_0.png"; // Default box
        }
        
        this.image = new ImageIcon(imagePath).getImage();
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
            // Clamp alpha to valid range [0.0, 1.0]
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        }
        
        // Pulsate size based on time
        float pulseFactor = (float) (1.0 + 0.1 * Math.sin(elapsedTime / 200.0));
        int pulseSize = (int) (SIZE * pulseFactor);
        
        // Save original transform
        AffineTransform oldTransform = g2.getTransform();
        
        // Apply transform for rotation
        g2.translate(position.x, position.y);
        g2.rotate(Math.toRadians(angle), SIZE / 2, SIZE / 2);
        
        // Set alpha composite for fading effect
        Composite oldComposite = g2.getComposite();
        if (alpha < 1.0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        
        // Draw the power-up image
        g2.drawImage(image, 0, 0, pulseSize, pulseSize, null);
        
        // Restore original composite
        g2.setComposite(oldComposite);
        
        // Restore original transform
        g2.setTransform(oldTransform);
        
        // Draw a subtle glow effect around the power-up
        drawGlowEffect(g2, alpha);
    }
    
    private void drawGlowEffect(Graphics2D g2, float alpha) {
        // Create a soft glow around the power-up
        int glowSize = SIZE + 10;
        int glowX = (int)position.x - 5;
        int glowY = (int)position.y - 5;
        
        // Ensure alpha is in valid range [0.0, 1.0]
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        
        // Set a radial gradient paint for the glow
        RadialGradientPaint paint = new RadialGradientPaint(
            position.x + SIZE/2, position.y + SIZE/2, glowSize/2,
            new float[] {0.0f, 1.0f},
            new Color[] {
                new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(100 * alpha)),
                new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
            }
        );
        
        // Draw the glow
        Composite oldComposite = g2.getComposite();
        // Ensure alpha is in valid range [0.0, 1.0]
        float glowAlpha = 0.5f * alpha;
        glowAlpha = Math.max(0.0f, Math.min(1.0f, glowAlpha));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, glowAlpha));
        g2.setPaint(paint);
        g2.fillOval(glowX, glowY, glowSize, glowSize);
        g2.setComposite(oldComposite);
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
    
    public Vector2 getPosition() {
        return position;
    }
    
    public Color getColor() {
        return color;
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
