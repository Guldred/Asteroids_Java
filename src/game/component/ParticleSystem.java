package game.component;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleSystem {
    private List<Particle> particles;
    private Random random;
    
    public ParticleSystem() {
        particles = new ArrayList<>();
        random = new Random();
    }
    
    public void createExplosion(Vector2 position, Color color, int count, int size) {
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * 360;
            float speed = 1 + random.nextFloat() * 3;
            
            Vector2 velocity = new Vector2(
                (float) Math.cos(Math.toRadians(angle)) * speed,
                (float) Math.sin(Math.toRadians(angle)) * speed
            );
            
            int particleSize = size / 2 + random.nextInt(size / 2);
            int lifetime = 500 + random.nextInt(1000); // 0.5 to 1.5 seconds
            
            particles.add(new Particle(position, velocity, color, particleSize, lifetime));
        }
    }
    
    public void update() {
        long currentTime = System.currentTimeMillis();
        Iterator<Particle> iterator = particles.iterator();
        
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update();
            
            if (currentTime - particle.getCreationTime() > particle.getLifetime()) {
                iterator.remove();
            }
        }
    }
    
    public void draw(Graphics2D g2) {
        for (Particle particle : particles) {
            particle.draw(g2);
        }
    }
    
    public int getParticleCount() {
        return particles.size();
    }
    
    private class Particle {
        private Vector2 position;
        private Vector2 velocity;
        private Color color;
        private int size;
        private long creationTime;
        private int lifetime;
        private float alpha = 1.0f;
        
        public Particle(Vector2 position, Vector2 velocity, Color color, int size, int lifetime) {
            this.position = new Vector2(position);
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.creationTime = System.currentTimeMillis();
            this.lifetime = lifetime;
        }
        
        public void update() {
            position.add(velocity);
            
            // Slow down over time
            velocity.x *= 0.98f;
            velocity.y *= 0.98f;
            
            // Fade out over time
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - creationTime;
            alpha = 1.0f - ((float) elapsedTime / lifetime);
            
            // Shrink over time
            if (size > 1) {
                size = (int) (size * (1.0f - ((float) elapsedTime / lifetime) * 0.5f));
            }
        }
        
        public void draw(Graphics2D g2) {
            Color particleColor = new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int) (255 * alpha)
            );
            
            g2.setColor(particleColor);
            g2.fillOval((int) position.x, (int) position.y, size, size);
        }
        
        public long getCreationTime() {
            return creationTime;
        }
        
        public int getLifetime() {
            return lifetime;
        }
    }
}
