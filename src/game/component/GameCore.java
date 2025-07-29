package game.component;

import game.object.Asteroid;
import game.object.Player;
import game.object.projectiles.Projectile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GameCore extends JComponent{

    private JFrame window;
    private Graphics2D g2;
    private BufferedImage image;

    private int width;
    private int height;
    private Thread thread;
    private boolean start = true;

    private List<Projectile> projectiles;
    private List<Asteroid> asteroids;

    public static Vector2 screenSize;

    // Game Fps
    private final int FPS = 67;
    private final int TARGET_TIME = 1000000000 / FPS;

    // Game Objects
    private Player player;
    private PlayerInput playerInput;

    public GameCore(JFrame window) {
        this.window = window;
        screenSize = new Vector2(1280, 720);
    }

    public void start() {
        width = getWidth();
        height = getHeight();


        initGFX();
        initGameObjects();
        initInput();
        initProjectiles();



        thread = new Thread(() -> {
            long frameStartTime = 0;
            long frameRenderTime = 0;
            while (start) {
                frameStartTime = System.nanoTime();
                draw();
                frameRenderTime = System.nanoTime() - frameStartTime;
                if (frameRenderTime < TARGET_TIME) {
                    sleep((TARGET_TIME - frameRenderTime) / 1000000);
                }

            }
        });
        thread.start();
    }

    private void draw() {
        drawBackground();
        drawGame();
        drawUI();
        
        // Check for player-asteroid collisions
        checkPlayerAsteroidCollisions();
        
        render();
    }
    
    private void checkPlayerAsteroidCollisions() {
        // Skip if player is dead
        if (player.isDead()) {
            return;
        }
        
        // Get player center and radius
        Vector2 playerCenter = player.getCenter();
        float playerRadius = (float) (Player.PLAYER_DIMENSIONS / 2);
        
        // Check each asteroid for collision with player
        for (Asteroid asteroid : asteroids) {
            // Get asteroid center and radius
            Vector2 asteroidCenter = asteroid.getCenter();
            float asteroidRadius = asteroid.getSize() / 2;
            
            // Calculate distance between centers
            float distance = playerCenter.distance(asteroidCenter);
            
            // If the distance is less than the sum of their radii, they collide
            if (distance < (playerRadius + asteroidRadius)) {
                // Apply damage to player (20 damage per hit)
                boolean tookDamage = player.takeDamage(20);
                
                // If player took damage and is now dead, handle game over
                if (tookDamage && player.isDead()) {
                    handleGameOver();
                }
                
                // No need to check other asteroids if player took damage
                if (tookDamage) {
                    break;
                }
            }
        }
    }
    
    private void handleGameOver() {
        // For now, just print a message
        System.out.println("Game Over! Player destroyed!");
        
        // Could add more game over logic here later
    }

    private void render() {
        Graphics g = getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
    }

    private void drawBackground() {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);
    }

    private void drawGame() {
        player.draw(g2);
        for (int i = 0; i < projectiles.size(); i++) {
            if (projectiles.get(i) != null) {
                projectiles.get(i).draw(g2);
            }
        }
        for (int i = 0; i < asteroids.size(); i++) {
            if (asteroids.get(i) != null) {
                asteroids.get(i).draw(g2);
            }
        }

    }

    private void drawUI() {
        // Draw player health
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Health: " + player.getHealth(), 20, 30);
        
        // If player is invulnerable, show indicator
        if (player.isInvulnerable()) {
            g2.setColor(Color.YELLOW);
            g2.drawString("SHIELD ACTIVE", 20, 50);
        }
        
        // If player is dead, show game over message
        if (player.isDead()) {
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 36));
            String gameOverText = "GAME OVER";
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(gameOverText);
            g2.drawString(gameOverText, (width - textWidth) / 2, height / 2);
            
            // Show restart instruction
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            String restartText = "Press 'R' to restart";
            fm = g2.getFontMetrics();
            textWidth = fm.stringWidth(restartText);
            g2.drawString(restartText, (width - textWidth) / 2, height / 2 + 40);
        }
    }

    private void initGFX() {
        //Background
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);


    }

    private void initInput() {
        //Capture Inputs and send them to player (TODO: make this Observer Pattern)
        requestFocus();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { inputListenerEvent(InputEventTypes.KEY_PRESSED, e.getKeyCode());}

            @Override
            public void keyReleased(KeyEvent e) {
                inputListenerEvent(InputEventTypes.KEY_RELEASED, e.getKeyCode());
            }

        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                playerShoot(1);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                //playerShoot(2);
            }
        });
    }

    public void inputListenerEvent(InputEventTypes eventType, int keyCode) {
        // Handle restart key (R) when player is dead
        if (eventType == InputEventTypes.KEY_PRESSED && keyCode == KeyEvent.VK_R && player.isDead()) {
            resetGame();
            return;
        }
        
        // Forward other inputs to player
        player.setInputToMap(eventType, keyCode);
    }
    
    private void resetGame() {
        // Reset player
        player = new Player(window);
        player.setPosition(new Vector2(100, 100));
        
        // Clear and reset asteroids
        asteroids.clear();
        for (int i = 0; i < 3; i++) {
            spawnRandomAsteroid();
        }
        
        // Clear projectiles
        projectiles.clear();
        
        System.out.println("Game Reset!");
    }



    private void initGameObjects() {
        player = new Player(window);
        player.setPosition(new Vector2(100, 100));
        asteroids = new ArrayList<>();
        
        // Start asteroid spawning thread
        startAsteroidSpawner();
    }
    
    private void startAsteroidSpawner() {
        // Spawn initial asteroids
        for (int i = 0; i < 3; i++) {
            spawnRandomAsteroid();
        }
        
        // Start thread to spawn asteroids periodically
        new Thread(() -> {
            while (start) {
                try {
                    // Wait 2-5 seconds between spawns
                    Thread.sleep((long) (Math.random() * 3000 + 2000));
                    spawnRandomAsteroid();
                } catch (InterruptedException e) {
                    System.err.println("Asteroid spawner interrupted: " + e.getMessage());
                }
            }
        }).start();
    }
    
    private void spawnRandomAsteroid() {
        // Random position (outside the center area to avoid spawning on player)
        Vector2 position;
        do {
            position = new Vector2(
                (float) (Math.random() * (width - 128)),
                (float) (Math.random() * (height - 128))
            );
        } while (position.distance(player.getPos()) < 200); // Keep away from player
        
        // Random velocity
        Vector2 velocity = new Vector2(
            (float) (Math.random() * 6 - 3),
            (float) (Math.random() * 6 - 3)
        );
        
        // Random size (16, 32, 64, or 128)
        int[] sizes = {16, 32, 64, 128};
        int size = sizes[(int) (Math.random() * sizes.length)];
        
        asteroids.add(new Asteroid(position, velocity, size));
    }

    private void initProjectiles() {
        projectiles = new ArrayList<>();

        new Thread(() -> {
            long frameStartTime = 0;
            long frameRenderTime = 0;
            while (start) {
                frameStartTime = System.nanoTime();

                //use update on all projectiles
                for (int i = 0; i < projectiles.size(); i++) {
                    if (projectiles.get(i) != null) {
                        //projectiles.get(i).Update();
                        if (projectiles.get(i).outOfBounds(width, height)) {
                            projectiles.get(i).stop();
                            projectiles.remove(projectiles.get(i));
                        } else {
                            // Check for collisions with asteroids
                            checkProjectileAsteroidCollisions(projectiles.get(i));
                        }
                    } else {
                        projectiles.remove(projectiles.get(i));
                    }
                }

                frameRenderTime = System.nanoTime() - frameStartTime;
                if (frameRenderTime < TARGET_TIME) {
                    sleep((TARGET_TIME - frameRenderTime) / 1000000);
                }

            }
        }).start();
    }
    
    private void checkProjectileAsteroidCollisions(Projectile projectile) {
        // Create a list to store asteroids that need to be removed
        List<Asteroid> asteroidsToRemove = new ArrayList<>();
        
        // Check each asteroid for collision with the projectile
        for (Asteroid asteroid : asteroids) {
            // Get the center of the projectile and asteroid
            Vector2 projectileCenter = projectile.getCenter();
            Vector2 asteroidCenter = asteroid.getCenter();
            
            // Calculate the distance between centers
            float distance = projectileCenter.distance(asteroidCenter);
            
            // If the distance is less than the sum of their radii, they collide
            if (distance < (projectile.getSize() / 2 + asteroid.getSize() / 2)) {
                // Mark asteroid for removal
                asteroidsToRemove.add(asteroid);
                
                // Remove the projectile
                projectile.stop();
                projectiles.remove(projectile);
                
                // Break since this projectile can't hit any more asteroids
                break;
            }
        }
        
        // Remove all asteroids that were hit
        asteroids.removeAll(asteroidsToRemove);
        
        // If few asteroids remain, spawn more to keep the game challenging
        if (asteroids.size() < 2) {
            spawnRandomAsteroid();
        }
    }

    public void playerShoot(int weapon) {
        projectiles.add(player.shoot(weapon));
    }


    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }

}
