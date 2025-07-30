package game.object;

import game.component.GameCore;
import game.component.Updateable;
import game.component.Vector2;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class Asteroid extends Updateable {

    private Vector2 position;
    private Vector2 velocity;
    private Vector2 frameVelocity;
    private float angle;
    private float rotationSpeed;
    private final int SIZE;
    private final Image image;
    private final Area shape;
    private float wallBounceFactor;
    private final float MAX_SPEED;
    private boolean destroyed = false;

    public Asteroid(Vector2 position, Vector2 velocity, int size) {
        super();
        this.position = position;
        this.velocity = velocity;
        this.SIZE = size;
        this.rotationSpeed = (float) (Math.random() * 2 - 1);
        this.angle = 0;
        
        // Load asteroid image with error handling
        String imagePath = "src/game/resource/img/asteroids/asteroid_" + size + ".png";
        ImageIcon icon = new ImageIcon(imagePath);
        if (icon.getIconWidth() <= 0) {
            // Fallback to a different size if the specific size isn't available
            icon = new ImageIcon("src/game/resource/img/asteroids/asteroid_64.png");
            
            // If still no image, create a simple asteroid shape
            if (icon.getIconWidth() <= 0) {
                BufferedImage fallbackImage = createFallbackAsteroidImage(size);
                this.image = fallbackImage;
            } else {
                this.image = icon.getImage();
            }
        } else {
            this.image = icon.getImage();
        }
        
        this.shape = createShape();
        this.wallBounceFactor = 1.2f;
        this.MAX_SPEED = (float) (Math.random() * 10 + 1);

        startUpdate();
    }
    
    private BufferedImage createFallbackAsteroidImage(int size) {
        // Create a simple asteroid shape as fallback
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        
        // Use anti-aliasing for smoother edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw a rocky asteroid shape
        int points = 8 + (int)(Math.random() * 5); // 8-12 points
        int[] xPoints = new int[points];
        int[] yPoints = new int[points];
        
        // Generate random points around a circle
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double radius = size / 2.0 * (0.8 + Math.random() * 0.4); // Vary radius 80-120%
            xPoints[i] = (int)(size / 2 + radius * Math.cos(angle));
            yPoints[i] = (int)(size / 2 + radius * Math.sin(angle));
        }
        
        // Fill with a gray color
        g2.setColor(new Color(150, 150, 150));
        g2.fillPolygon(xPoints, yPoints, points);
        
        // Add some texture/craters
        g2.setColor(new Color(100, 100, 100));
        for (int i = 0; i < 5; i++) {
            int craterSize = (int)(size * (0.1 + Math.random() * 0.1));
            int x = (int)(Math.random() * (size - craterSize));
            int y = (int)(Math.random() * (size - craterSize));
            g2.fillOval(x, y, craterSize, craterSize);
        }
        
        g2.dispose();
        return img;
    }

    private Area createShape() {
        return new Area(new Ellipse2D.Double(0, 0, SIZE, SIZE));
    }

    public Area getShape() {
        AffineTransform t = new AffineTransform();
        t.translate(position.x, position.y);
        t.rotate(Math.toRadians(angle), SIZE / 2, SIZE / 2);
        return new Area(t.createTransformedShape(shape));
    }


    @Override
    protected void onUpdate(float deltaTime) {
        this.UpdatePosition(deltaTime);
    }

    protected void UpdatePosition(float deltaTime) {
        checkOutOfBounds();
        frameVelocity = new Vector2(velocity.x * deltaTime, velocity.y * deltaTime);
        position.add(frameVelocity);
        angle += (rotationSpeed * deltaTime);
    }

    public void draw(Graphics2D g2) {
        if (destroyed) return;
        
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(position.x, position.y);
        AffineTransform t = new AffineTransform();
        t.rotate(Math.toRadians(angle), (double) SIZE / 2, (double) SIZE / 2);
        g2.drawImage(image, 0, 0, SIZE, SIZE, null);

        g2.setTransform(oldTransform);
    }

    public Vector2 getCenter() {
        return new Vector2( (float) (position.x + SIZE / 2), (float)(position.y + SIZE / 2));
    }

    public void checkOutOfBounds() {
        if (position.x < 0) {
            velocity.x = Math.min((Math.abs(velocity.x) * wallBounceFactor), MAX_SPEED);
        } else if (position.x + SIZE > GameCore.screenSize.x) {
            velocity.x = Math.max((Math.abs(velocity.x) * -1 * wallBounceFactor), -MAX_SPEED);
        } else if (position.y < 0) {
            velocity.y = Math.min((Math.abs(velocity.y) * wallBounceFactor), MAX_SPEED);
        } else if (position.y + SIZE + 30 > GameCore.screenSize.y) {
            velocity.y = Math.max((Math.abs(velocity.y) * -1 * wallBounceFactor), -MAX_SPEED);
        }
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
    
    public void destroy() {
        this.destroyed = true;
        // Stop the update loop by setting start to false
        this.start = false;
    }
    
    public int getSize() {
        return SIZE;
    }
    
    public Vector2 getPosition() {
        return position;
    }
    
    public Vector2 getVelocity() {
        return velocity;
    }
}
