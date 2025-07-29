package game.component;

import game.component.Asteroid;
import game.object.Player;
import game.object.projectiles.Projectile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
// ... (rest of the imports)
public class GameCore extends JPanel {
    private Main main;
    private List<Asteroid> asteroids = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private boolean gameOver = false;

    // Add ship variables
    private double shipX = 640, shipY = 360;
    private double shipAngle = 0;
    private boolean leftPressed = false, rightPressed = false;

    public GameCore(Main main) {
        this.main = main;
        // Initialize game
        spawnAsteroids(5);
        new Timer(16, e -> updateGame()).start(); // ~60 FPS
        // Add keyboard listeners
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = true;
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = true;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) leftPressed = false;
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) rightPressed = false;
            }
        });
        setFocusable(true);
    }

    private void spawnAsteroids(int count) {
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double speed = rand.nextDouble() * 2 + 1;
            asteroids.add(new Asteroid(
                rand.nextDouble() * 1280,
                rand.nextDouble() * 720,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                rand.nextInt(3) + 1
            ));
        }
    }

    private void updateGame() {
        if (gameOver) return;

        // In updateGame() method
        if (leftPressed) shipAngle -= 0.05;
        if (rightPressed) shipAngle += 0.05;

        // Add ship movement
        shipX += Math.cos(shipAngle) * 5;
        shipY += Math.sin(shipAngle) * 5;

        // Update game objects
        for (Asteroid asteroid : asteroids) {
            asteroid.update();
        }

        for (Bullet bullet : bullets) {
            bullet.update();
        }

        // Collision detection
        checkCollisions();

        // Rendering
        repaint();
    }

    private void checkCollisions() {
        // Bullet-Asteroid collisions
        for (int b = bullets.size()-1; b >= 0; b--) {
            Bullet bullet = bullets.get(b);
            for (int a = asteroids.size()-1; a >= 0; a--) {
                Asteroid asteroid = asteroids.get(a);
                if (bullet.getBounds().intersects(asteroid.getBounds())) {
                    // Destroy asteroid and bullet
                    asteroids.remove(a);
                    bullets.remove(b);
                    break;
                }
            }
        }

        // Ship-Asteroid collisions
        for (Asteroid asteroid : asteroids) {
            if (new Rectangle2D.Double(shipX, shipY, 16, 16).intersects(asteroid.getBounds())) {
                // Game over
                gameOver = true;
                // Show game over message
                main.setTitle("Game Over - Press R to Restart");
                // Add restart logic
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw ship
        g.setColor(Color.WHITE);
        g.fillOval((int)shipX, (int)shipY, 16, 16);

        // Draw asteroids
        for (Asteroid asteroid : asteroids) {
            g.setColor(Color.GRAY);
            g.fillOval((int)asteroid.x, (int)asteroid.y, asteroid.size*16, asteroid.size*16);
        }

        // Draw bullets
        for (Bullet bullet : bullets) {
            g.setColor(Color.YELLOW);
            g.fillRect((int)bullet.x, (int)bullet.y, 4, 4);
        }
    }
}
