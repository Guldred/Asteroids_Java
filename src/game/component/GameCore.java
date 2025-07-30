package game.component;

import game.object.Asteroid;
import game.object.Player;
import game.object.PowerUp;
import game.object.projectiles.Projectile;
import game.object.projectiles.TripleShot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
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

    private List<Projectile> projectiles;
    private List<Asteroid> asteroids;
    private AsteroidManager asteroidManager;
    private ParticleSystem particleSystem;
    private PowerUpManager powerUpManager;
    private SoundManager soundManager;
    private BackgroundManager backgroundManager;

    public static Vector2 screenSize;

    // Game Fps
    private final int FPS = 67;
    private final int TARGET_TIME = 1000000000 / FPS;

    // Game Objects
    private Player player;
    private PlayerInput playerInput;
    
    // Game settings
    private final int INITIAL_ASTEROID_COUNT = 5;
    private final int MAX_ASTEROIDS = 10;
    
    // Game state
    private int score = 0;
    private boolean gameOver = false;
    private long gameOverTime = 0;
    private final long RESTART_DELAY = 3000; // 3 seconds before restart option appears
    
    // Screen shake effect
    private boolean screenShaking = false;
    private long screenShakeStartTime = 0;
    private final long SCREEN_SHAKE_DURATION = 500; // 0.5 seconds
    private final int SCREEN_SHAKE_INTENSITY = 10; // pixels
    
    // Level system
    private int level = 1;
    private int asteroidsToNextLevel = 10;
    private int destroyedAsteroidsCount = 0;
    
    // Game states
    public enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }
    
    private GameState currentState = GameState.MENU;
    
    // Background stars (kept for fallback)
    private static final int NUM_STARS = 100;
    private Vector2[] starPositions;
    private int[] starSizes;
    private Color[] starColors;
    
    // Arcade-style UI
    private Font arcadeFont;
    private Font arcadeFontLarge;
    private Font arcadeFontSmall;

    public GameCore(JFrame window) {
        this.window = window;
        screenSize = new Vector2(1280, 720);
        soundManager = SoundManager.getInstance();
        
        // Load arcade-style fonts
        try {
            // Use a default font with BOLD style to simulate arcade font
            arcadeFont = new Font("Arial", Font.BOLD, 20);
            arcadeFontLarge = new Font("Arial", Font.BOLD, 48);
            arcadeFontSmall = new Font("Arial", Font.BOLD, 16);
        } catch (Exception e) {
            System.err.println("Error loading fonts: " + e.getMessage());
            // Fallback to default fonts
            arcadeFont = new Font("Arial", Font.BOLD, 20);
            arcadeFontLarge = new Font("Arial", Font.BOLD, 48);
            arcadeFontSmall = new Font("Arial", Font.BOLD, 16);
        }
    }

    public void start() {
        width = getWidth();
        height = getHeight();

        initGFX();
        initGameObjects();
        initInput();
        initProjectiles();
        initStars(); // Keep for fallback
        
        // Initialize background manager
        backgroundManager = new BackgroundManager(width, height);
        // Start with a random background theme
        BackgroundManager.BackgroundTheme[] themes = BackgroundManager.BackgroundTheme.values();
        backgroundManager.setTheme(themes[new Random().nextInt(themes.length)]);

        thread = new Thread(() -> {
            long frameStartTime = 0;
            long frameRenderTime = 0;
            while (start) {
                frameStartTime = System.nanoTime();
                update();
                draw();
                frameRenderTime = System.nanoTime() - frameStartTime;
                if (frameRenderTime < TARGET_TIME) {
                    sleep((TARGET_TIME - frameRenderTime) / 1000000);
                }
            }
        });
        thread.start();
    }
    
    private void initStars() {
        Random random = new Random();
        starPositions = new Vector2[NUM_STARS];
        starSizes = new int[NUM_STARS];
        starColors = new Color[NUM_STARS];
        
        for (int i = 0; i < NUM_STARS; i++) {
            starPositions[i] = new Vector2(
                random.nextFloat() * width,
                random.nextFloat() * height
            );
            
            // Vary star sizes (1-3 pixels)
            starSizes[i] = random.nextInt(3) + 1;
            
            // Vary star brightness
            int brightness = 150 + random.nextInt(106); // 150-255
            starColors[i] = new Color(brightness, brightness, brightness);
        }
    }
    
    private void update() {
        switch (currentState) {
            case MENU:
                // Menu logic will be handled by input events
                updateBackground(0, 0); // Slow background movement in menu
                break;
                
            case PLAYING:
                updateGameplay();
                break;
                
            case PAUSED:
                // Paused logic will be handled by input events
                break;
                
            case GAME_OVER:
                // Check for restart
                if (System.currentTimeMillis() - gameOverTime > RESTART_DELAY && playerInput.isKey_space()) {
                    restartGame();
                }
                updateBackground(0, 0); // Slow background movement in game over
                break;
        }
    }
    
    private void updateBackground(float playerVelocityX, float playerVelocityY) {
        // Update background with parallax effect
        backgroundManager.update(playerVelocityX, playerVelocityY);
    }
    
    private void updateGameplay() {
        // Check if player is dead
        if (!player.isAlive()) {
            currentState = GameState.GAME_OVER;
            gameOverTime = System.currentTimeMillis();
            soundManager.playSound("gameover");
            return;
        }
        
        // Update asteroid manager
        asteroidManager.update();
        
        // Update asteroids list from manager
        asteroids = asteroidManager.getAsteroids();
        
        // Update particle system
        particleSystem.update();
        
        // Update power-up manager
        powerUpManager.update();
        
        // Update background with parallax effect based on player velocity
        updateBackground(player.getVelocity().x * 0.01f, player.getVelocity().y * 0.01f);
        
        // Handle collisions
        int destroyedAsteroids = CollisionDetector.handleProjectileAsteroidCollisions(projectiles, asteroids);
        
        // Update level progress
        if (destroyedAsteroids > 0) {
            destroyedAsteroidsCount += destroyedAsteroids;
            
            // Play explosion sound for each destroyed asteroid
            for (int i = 0; i < destroyedAsteroids; i++) {
                // Vary the volume slightly for each explosion to create a more natural sound
                float volume = 0.7f + (float)(Math.random() * 0.3f);
                soundManager.playSoundWithVolume("explosion", volume);
            }
            
            // Check for level up
            if (destroyedAsteroidsCount >= asteroidsToNextLevel) {
                levelUp();
            }
            
            // Create explosion particles for destroyed asteroids
            for (Asteroid asteroid : asteroids) {
                if (asteroid.isDestroyed()) {
                    // Create explosion at asteroid position
                    Color[] explosionColors = {Color.RED, Color.ORANGE, Color.YELLOW};
                    Color explosionColor = explosionColors[(int)(Math.random() * explosionColors.length)];
                    particleSystem.createExplosion(
                        asteroid.getCenter(), 
                        explosionColor, 
                        20 + asteroid.getSize() / 2, // More particles for larger asteroids
                        asteroid.getSize() / 4
                    );
                    
                    // Chance to spawn power-up
                    powerUpManager.checkAsteroidDestroyed(asteroid);
                }
            }
        }
        
        score += destroyedAsteroids * 100; // 100 points per asteroid
        
        // Check for player-asteroid collisions
        if (CollisionDetector.handlePlayerAsteroidCollisions(player, asteroids)) {
            player.takeDamage(10); // Player takes damage when hit by asteroid
            
            // Play hit sound
            soundManager.playSound("hit");
            
            // Start screen shake
            screenShaking = true;
            screenShakeStartTime = System.currentTimeMillis();
        }
        
        // Check for player-powerup collisions
        checkPlayerPowerUpCollisions();
        
        // Update screen shake
        if (screenShaking && System.currentTimeMillis() - screenShakeStartTime > SCREEN_SHAKE_DURATION) {
            screenShaking = false;
        }
    }
    
    private void checkPlayerPowerUpCollisions() {
        Rectangle2D playerBounds = player.getCollisionBounds();
        List<PowerUp> powerUps = powerUpManager.getPowerUps();
        
        for (PowerUp powerUp : powerUps) {
            if (!powerUp.isCollected() && powerUp.getCollisionShape().intersects(playerBounds)) {
                // Collect power-up
                powerUp.collect();
                
                // Apply power-up effect
                player.activatePowerUp(powerUp.getType());
                
                // Play power-up sound
                soundManager.playSound("powerup");
                
                // Create particles
                particleSystem.createExplosion(
                    new Vector2(
                        (float)(playerBounds.getX() + playerBounds.getWidth() / 2),
                        (float)(playerBounds.getY() + playerBounds.getHeight() / 2)
                    ),
                    Color.WHITE,
                    15,
                    5
                );
                
                // Add score
                score += 50;
            }
        }
    }
    
    private void levelUp() {
        level++;
        destroyedAsteroidsCount = 0;
        asteroidsToNextLevel = 10 + (level * 5); // Increase required asteroids for next level
        
        // Play level up sound
        soundManager.playSound("levelup");
        
        // Increase max asteroids
        int newMaxAsteroids = Math.min(MAX_ASTEROIDS + level, 20); // Cap at 20
        asteroidManager = new AsteroidManager(width, height, newMaxAsteroids);
        
        // Spawn initial asteroids for new level
        asteroidManager.spawnInitialAsteroids(Math.min(INITIAL_ASTEROID_COUNT + level, 10)); // Cap at 10 initial
        asteroids = asteroidManager.getAsteroids();
        
        // Heal player a bit
        player.heal(10);
        
        // Create level up effect
        for (int i = 0; i < 5; i++) {
            particleSystem.createExplosion(
                new Vector2(
                    (float)(Math.random() * width),
                    (float)(Math.random() * height)
                ),
                Color.GREEN,
                30,
                10
            );
        }
    }

    private void draw() {
        drawBackground();
        
        switch (currentState) {
            case MENU:
                drawMenu();
                break;
                
            case PLAYING:
                drawGameplay();
                drawUI();
                break;
                
            case PAUSED:
                drawGameplay(); // Draw the game in the background
                drawPauseScreen();
                break;
                
            case GAME_OVER:
                drawGameplay(); // Draw the game in the background
                drawGameOver();
                break;
        }
        
        render();
    }
    
    private void drawGameplay() {
        // Apply screen shake transform if active
        AffineTransform originalTransform = g2.getTransform();
        if (screenShaking) {
            int shakeX = (int) (Math.random() * SCREEN_SHAKE_INTENSITY * 2 - SCREEN_SHAKE_INTENSITY);
            int shakeY = (int) (Math.random() * SCREEN_SHAKE_INTENSITY * 2 - SCREEN_SHAKE_INTENSITY);
            g2.translate(shakeX, shakeY);
        }
        
        drawGame();
        
        // Reset transform after drawing game elements
        g2.setTransform(originalTransform);
        
        // Draw power-ups (not affected by screen shake)
        powerUpManager.draw(g2);
        
        // Draw particles (not affected by screen shake)
        particleSystem.draw(g2);
    }
    
    private void drawMenu() {
        // Draw title with arcade style
        g2.setColor(Color.WHITE);
        g2.setFont(arcadeFontLarge);
        String title = "ASTEROIDS";
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(title);
        
        // Draw title with glow effect
        drawTextWithGlow(g2, title, width / 2 - textWidth / 2, height / 3, Color.WHITE, Color.BLUE);
        
        // Draw instructions
        g2.setFont(arcadeFont);
        String[] instructions = {
            "Arrow Keys: Move Ship",
            "Mouse: Aim Ship",
            "Left Click: Shoot",
            "P: Pause Game",
            "M: Toggle Sound",
            "B: Change Background",
            "",
            "Press SPACE to Start"
        };
        
        int y = height / 2;
        for (String instruction : instructions) {
            textWidth = g2.getFontMetrics().stringWidth(instruction);
            g2.drawString(instruction, width / 2 - textWidth / 2, y);
            y += 30;
        }
        
        // Draw animated asteroids in the background
        long time = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            int x = (int)(width * 0.2 + width * 0.6 * Math.sin((time + i * 1000) / 3000.0));
            int y2 = (int)(height * 0.2 + height * 0.6 * Math.cos((time + i * 1000) / 2000.0));
            int size = 20 + i * 10;
            g2.setColor(new Color(200, 200, 200, 100));
            g2.fillOval(x - size/2, y2 - size/2, size, size);
        }
        
        // Make the "Press SPACE to Start" text pulse
        String startText = "Press SPACE to Start";
        textWidth = g2.getFontMetrics().stringWidth(startText);
        float pulse = (float)Math.sin(time / 200.0) * 0.2f + 0.8f;
        Color pulseColor = new Color(pulse, pulse, 1.0f);
        
        // Draw at the bottom with a glow effect
        drawTextWithGlow(g2, startText, width / 2 - textWidth / 2, height - 100, pulseColor, Color.BLUE);
    }
    
    private void drawPauseScreen() {
        // Semi-transparent overlay
        g2.setColor(new Color(0, 0, 0, 150)); // Semi-transparent black
        g2.fillRect(0, 0, width, height);
        
        // Draw pause text with arcade style
        g2.setFont(arcadeFontLarge);
        String pauseText = "PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(pauseText);
        
        // Draw with glow effect
        drawTextWithGlow(g2, pauseText, width / 2 - textWidth / 2, height / 2, Color.WHITE, Color.CYAN);
        
        // Draw instructions
        g2.setFont(arcadeFont);
        
        // Make the text pulse
        long time = System.currentTimeMillis();
        float pulse = (float)Math.sin(time / 200.0) * 0.2f + 0.8f;
        Color pulseColor = new Color(pulse, pulse, 1.0f);
        
        String resumeText = "Press P to Resume";
        textWidth = g2.getFontMetrics().stringWidth(resumeText);
        drawTextWithGlow(g2, resumeText, width / 2 - textWidth / 2, height / 2 + 50, pulseColor, Color.CYAN);
        
        // Draw additional options
        String[] options = {
            "M: Toggle Sound",
            "B: Change Background"
        };
        
        int y = height / 2 + 100;
        for (String option : options) {
            textWidth = g2.getFontMetrics().stringWidth(option);
            g2.drawString(option, width / 2 - textWidth / 2, y);
            y += 30;
        }
    }

    private void render() {
        Graphics g = getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
    }

    private void drawBackground() {
        // Clear background
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);
        
        // Draw the background using BackgroundManager
        backgroundManager.draw(g2);
    }
    
    private void drawTextWithGlow(Graphics2D g2, String text, int x, int y, Color textColor, Color glowColor) {
        // Draw glow
        g2.setColor(glowColor);
        for (int i = 1; i <= 3; i++) {
            g2.drawString(text, x - i, y - i);
            g2.drawString(text, x + i, y - i);
            g2.drawString(text, x - i, y + i);
            g2.drawString(text, x + i, y + i);
        }
        
        // Draw text
        g2.setColor(textColor);
        g2.drawString(text, x, y);
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
        // Draw arcade-style UI with neon glow effect
        
        // Draw player health bar
        int healthBarWidth = 150;
        int healthBarHeight = 15;
        int healthBarX = 20;
        int healthBarY = 20;
        
        // Health bar background
        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
        
        // Health bar fill
        float healthPercent = (float)player.getHealth() / 100;
        int fillWidth = (int)(healthBarWidth * healthPercent);
        
        // Health color gradient (green to red)
        Color healthColor = new Color(
            (int)(255 * (1 - healthPercent)),
            (int)(255 * healthPercent),
            0
        );
        
        g2.setColor(healthColor);
        g2.fillRect(healthBarX, healthBarY, fillWidth, healthBarHeight);
        
        // Health bar border
        g2.setColor(Color.WHITE);
        g2.drawRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight);
        
        // Draw health text
        g2.setFont(arcadeFontSmall);
        g2.drawString("HEALTH: " + player.getHealth(), healthBarX + healthBarWidth + 10, healthBarY + healthBarHeight);
        
        // Draw score with glow effect
        g2.setFont(arcadeFont);
        String scoreText = "SCORE: " + score;
        drawTextWithGlow(g2, scoreText, width - 200, 30, Color.WHITE, Color.ORANGE);
        
        // Draw level with glow effect
        String levelText = "LEVEL: " + level;
        drawTextWithGlow(g2, levelText, width / 2 - 50, 30, Color.WHITE, Color.GREEN);
        
        // Draw level progress bar
        int progressBarWidth = 200;
        int progressBarHeight = 10;
        int progressBarX = width / 2 - progressBarWidth / 2;
        int progressBarY = 40;
        
        // Progress bar background
        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRect(progressBarX, progressBarY, progressBarWidth, progressBarHeight);
        
        // Progress bar fill
        float progressPercent = (float)destroyedAsteroidsCount / asteroidsToNextLevel;
        int progressFillWidth = (int)(progressBarWidth * progressPercent);
        
        g2.setColor(new Color(0, 200, 100));
        g2.fillRect(progressBarX, progressBarY, progressFillWidth, progressBarHeight);
        
        // Progress bar border
        g2.setColor(Color.WHITE);
        g2.drawRect(progressBarX, progressBarY, progressBarWidth, progressBarHeight);
        
        // Draw active power-ups indicator
        drawActivePowerUps();
    }
    
    private void drawActivePowerUps() {
        // Draw active power-ups at the bottom of the screen
        int iconSize = 32;
        int spacing = 10;
        int startX = 20;
        int startY = height - iconSize - 20;
        
        g2.setFont(arcadeFontSmall);
        g2.setColor(Color.WHITE);
        g2.drawString("ACTIVE POWER-UPS:", startX, startY - 5);
        
        // Draw power-up icons based on player's active power-ups
        int x = startX;
        
        if (player.hasActiveShield()) {
            g2.drawImage(new ImageIcon("src/game/resource/img/powerups/Box_Item_3.png").getImage(), 
                        x, startY, iconSize, iconSize, null);
            x += iconSize + spacing;
        }
        
        if (player.hasRapidFire()) {
            g2.drawImage(new ImageIcon("src/game/resource/img/powerups/Box_Item_11.png").getImage(), 
                        x, startY, iconSize, iconSize, null);
            x += iconSize + spacing;
        }
        
        if (player.hasTripleShot()) {
            g2.drawImage(new ImageIcon("src/game/resource/img/powerups/Box_Item_8.png").getImage(), 
                        x, startY, iconSize, iconSize, null);
        }
    }

    private void drawGameOver() {
        g2.setColor(new Color(0, 0, 0, 180)); // Semi-transparent black
        g2.fillRect(0, 0, width, height);
        
        // Draw game over text with arcade style and glow
        g2.setFont(arcadeFontLarge);
        String gameOverText = "GAME OVER";
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(gameOverText);
        
        drawTextWithGlow(g2, gameOverText, width / 2 - textWidth / 2, height / 2 - 50, Color.WHITE, Color.RED);
        
        // Draw score and level
        g2.setFont(arcadeFont);
        String scoreText = "FINAL SCORE: " + score;
        textWidth = g2.getFontMetrics().stringWidth(scoreText);
        g2.drawString(scoreText, width / 2 - textWidth / 2, height / 2);
        
        String levelText = "LEVEL REACHED: " + level;
        textWidth = g2.getFontMetrics().stringWidth(levelText);
        g2.drawString(levelText, width / 2 - textWidth / 2, height / 2 + 30);
        
        // Show restart option after delay
        if (System.currentTimeMillis() - gameOverTime > RESTART_DELAY) {
            g2.setFont(arcadeFont);
            String restartText = "PRESS SPACE TO RESTART";
            textWidth = g2.getFontMetrics().stringWidth(restartText);
            
            // Make the text pulse
            long time = System.currentTimeMillis();
            float pulse = (float)Math.sin(time / 200.0) * 0.2f + 0.8f;
            Color pulseColor = new Color(1.0f, pulse, pulse);
            
            drawTextWithGlow(g2, restartText, width / 2 - textWidth / 2, height / 2 + 80, pulseColor, Color.RED);
        }
    }

    private void restartGame() {
        // Reset game state
        score = 0;
        gameOver = false;
        level = 1;
        destroyedAsteroidsCount = 0;
        asteroidsToNextLevel = 10;
        currentState = GameState.PLAYING;
        
        // Reset player
        player.reset();
        player.setPosition(new Vector2(width / 2 - (float)Player.PLAYER_DIMENSIONS / 2, 
                                      height / 2 - (float)Player.PLAYER_DIMENSIONS / 2));
        
        // Clear projectiles
        projectiles.clear();
        
        // Reset asteroid manager
        asteroidManager = new AsteroidManager(width, height, MAX_ASTEROIDS);
        asteroidManager.spawnInitialAsteroids(INITIAL_ASTEROID_COUNT);
        asteroids = asteroidManager.getAsteroids();
        
        // Reset power-ups
        powerUpManager.clear();
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
                // Handle game state transitions
                if (e.getKeyCode() == KeyEvent.VK_P) {
                    if (currentState == GameState.PLAYING) {
                        currentState = GameState.PAUSED;
                    } else if (currentState == GameState.PAUSED) {
                        currentState = GameState.PLAYING;
                    }
                }
                
                // Toggle sound with M key
                if (e.getKeyCode() == KeyEvent.VK_M) {
                    soundManager.toggleSound();
                }
                
                // Start game from menu
                if (e.getKeyCode() == KeyEvent.VK_SPACE && currentState == GameState.MENU) {
                    currentState = GameState.PLAYING;
                }
                
                // Forward input to player if playing
                if (currentState == GameState.PLAYING) {
                    inputListenerEvent(InputEventTypes.KEY_PRESSED, e.getKeyCode());
                }
                
                // Handle space key for restart in game over state
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    playerInput.setKey_space(true);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Forward input to player if playing
                if (currentState == GameState.PLAYING) {
                    inputListenerEvent(InputEventTypes.KEY_RELEASED, e.getKeyCode());
                }
                
                // Handle space key for restart
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    playerInput.setKey_space(false);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentState == GameState.PLAYING) {
                    playerShoot(1);
                    soundManager.playSound("shoot");
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                //playerShoot(2);
            }
        });
    }

    public void inputListenerEvent(InputEventTypes eventType, int keyCode) {
        player.setInputToMap(eventType, keyCode);
    }

    private void initGameObjects() {
        player = new Player(window);
        player.setPosition(new Vector2(width / 2 - (float)Player.PLAYER_DIMENSIONS / 2, 
                                      height / 2 - (float)Player.PLAYER_DIMENSIONS / 2));
        
        // Initialize asteroid manager
        asteroidManager = new AsteroidManager(width, height, MAX_ASTEROIDS);
        asteroidManager.spawnInitialAsteroids(INITIAL_ASTEROID_COUNT);
        asteroids = asteroidManager.getAsteroids();
        
        // Initialize particle system
        particleSystem = new ParticleSystem();
        
        // Initialize power-up manager
        powerUpManager = new PowerUpManager();
        
        // Initialize player input
        playerInput = new PlayerInput();
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

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }
}
