package game.component;

import game.object.Asteroid;
import game.object.Player;
import game.object.projectiles.Projectile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameCore extends JComponent{

    private JFrame window;
    private Graphics2D g2;
    private BufferedImage image;

    private int width;
    private int height;
    private Thread thread;
    private boolean start = true;
    private boolean gameOver = false;

    private List<Projectile> projectiles;
    private List<Asteroid> asteroids;
    private Random random = new Random();

    public static Vector2 screenSize;

    // Game Fps
    private final int FPS = 67;
    private final int TARGET_TIME = 1000000000 / FPS;

    // Asteroid spawning
    private final int MAX_ASTEROIDS = 10;
    private final int ASTEROID_SPAWN_DELAY = 3000; // milliseconds
    private long lastAsteroidSpawnTime;

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
        updateGame();
        drawGame();
        drawUI();
        render();
    }

    private void updateGame() {
        if (gameOver) {
            return;
        }

        // Spawn new asteroids periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAsteroidSpawnTime > ASTEROID_SPAWN_DELAY) {
            spawnAsteroid();
            lastAsteroidSpawnTime = currentTime;
        }

        // Check for projectile-asteroid collisions
        Iterator<Projectile> projIterator = projectiles.iterator();
        while (projIterator.hasNext()) {
            Projectile projectile = projIterator.next();

            Iterator<Asteroid> astIterator = asteroids.iterator();
            boolean hitDetected = false;

            while (astIterator.hasNext() && !hitDetected) {
                Asteroid asteroid = astIterator.next();

                // Simple distance-based collision detection
                float distance = projectile.getCenter().distance(asteroid.getCenter());
                if (distance < (projectile.getSize() / 2) + (asteroid.getSize() / 2)) {
                    // Collision detected!
                    astIterator.remove();
                    projectile.stop();
                    projIterator.remove();
                    hitDetected = true;
                }
            }
        }

        // Check for player-asteroid collisions
        if (player != null && !player.isInvulnerable()) {
            for (Asteroid asteroid : asteroids) {
                // Using Area intersection for precise collision detection
                Area playerArea = player.getShape();
                Area asteroidArea = asteroid.getShape();

                // Create a copy of the asteroid area
                Area intersection = new Area(asteroidArea);
                // Get the intersection between player and asteroid
                intersection.intersect(playerArea);

                if (!intersection.isEmpty()) {
                    // Collision detected!
                    player.takeDamage(1);

                    if (player.getHealth() <= 0) {
                        gameOver = true;
                    }
                    break;
                }
            }
        }
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
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));

        // Display player health
        if (player != null) {
            g2.drawString("Health: " + player.getHealth(), 20, 30);
        }

        // Display number of asteroids
        g2.drawString("Asteroids: " + asteroids.size(), 20, 60);

        // Game over message
        if (gameOver) {
            g2.setFont(new Font("Arial", Font.BOLD, 50));
            String gameOverText = "GAME OVER";

            FontMetrics metrics = g2.getFontMetrics();
            int textWidth = metrics.stringWidth(gameOverText);

            g2.drawString(gameOverText, (width - textWidth) / 2, height / 2);

            g2.setFont(new Font("Arial", Font.BOLD, 25));
            String restartText = "Press SPACE to restart";
            textWidth = g2.getFontMetrics().stringWidth(restartText);
            g2.drawString(restartText, (width - textWidth) / 2, height / 2 + 50);
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
            public void keyPressed(KeyEvent e) { 
                inputListenerEvent(InputEventTypes.KEY_PRESSED, e.getKeyCode());

                // Restart game when space is pressed and game is over
                if (gameOver && e.getKeyCode() == KeyEvent.VK_SPACE) {
                    restartGame();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                inputListenerEvent(InputEventTypes.KEY_RELEASED, e.getKeyCode());
            }

        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!gameOver) {
                    playerShoot(1);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                //playerShoot(2);
            }
        });
    }

    private void restartGame() {
        // Reset game state
        gameOver = false;

        // Clear existing objects
        projectiles.clear();
        asteroids.clear();

        // Reinitialize player
        player = new Player(window);
        player.setPosition(new Vector2(screenSize.x / 2 - (float)Player.PLAYER_DIMENSIONS / 2, 
                                    screenSize.y / 2 - (float)Player.PLAYER_DIMENSIONS / 2));

        // Reset asteroid spawn timer
        lastAsteroidSpawnTime = System.currentTimeMillis();
        spawnAsteroid(); // Spawn initial asteroid
    }

    public void inputListenerEvent(InputEventTypes eventType, int keyCode) {
        player.setInputToMap(eventType, keyCode);
    }



    private void initGameObjects() {
        player = new Player(window);
        player.setPosition(new Vector2(screenSize.x / 2 - (float)Player.PLAYER_DIMENSIONS / 2, 
                                    screenSize.y / 2 - (float)Player.PLAYER_DIMENSIONS / 2));
        asteroids = new ArrayList<>();
        lastAsteroidSpawnTime = System.currentTimeMillis();
        spawnAsteroid(); // Spawn initial asteroid
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

    public void playerShoot(int weapon) {
        projectiles.add(player.shoot(weapon));
    }

    private void spawnAsteroid() {
        if (asteroids.size() >= MAX_ASTEROIDS) {
            return;
        }

        // Random size between 32, 64, and 96
        int[] possibleSizes = {32, 64, 96};
        int size = possibleSizes[random.nextInt(possibleSizes.length)];

        // Random position outside the screen but close to the edges
        Vector2 position = new Vector2();
        int side = random.nextInt(4); // 0: top, 1: right, 2: bottom, 3: left

        switch (side) {
            case 0: // top
                position.x = random.nextInt((int)screenSize.x);
                position.y = -size;
                break;
            case 1: // right
                position.x = screenSize.x;
                position.y = random.nextInt((int)screenSize.y);
                break;
            case 2: // bottom
                position.x = random.nextInt((int)screenSize.x);
                position.y = screenSize.y;
                break;
            case 3: // left
                position.x = -size;
                position.y = random.nextInt((int)screenSize.y);
                break;
        }

        // Random velocity toward center of screen
        float centerX = screenSize.x / 2;
        float centerY = screenSize.y / 2;

        // Calculate direction toward center with some randomness
        float dirX = centerX - position.x;
        float dirY = centerY - position.y;

        // Add some randomness to direction
        dirX += (random.nextFloat() - 0.5f) * 200;
        dirY += (random.nextFloat() - 0.5f) * 200;

        // Normalize and scale
        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        float speed = 1.0f + random.nextFloat() * 3.0f; // Random speed between 1 and 4

        Vector2 velocity = new Vector2(
            (dirX / length) * speed,
            (dirY / length) * speed
        );

        asteroids.add(new Asteroid(position, velocity, size));
    }

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }

}
