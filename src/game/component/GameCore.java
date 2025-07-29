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

    private boolean playerAlive = true;

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
        updateGame();
        drawBackground();
        drawGame();
        drawUI();
        render();
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
        if (playerAlive) {
            player.draw(g2);
        }
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
        // TODO
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
        player.setInputToMap(eventType, keyCode);
    }



    private void initGameObjects() {
        player = new Player(window);
        player.setPosition(new Vector2(100, 100));
        asteroids = new ArrayList<>();
        int asteroidCount = 7;
        for (int i = 0; i < asteroidCount; i++) {
            Vector2 pos = new Vector2((float)(Math.random() * (screenSize.x - 100)), (float)(Math.random() * (screenSize.y - 100)));
            Vector2 vel = new Vector2((float)(Math.random() * 6 - 3), (float)(Math.random() * 6 - 3));
            int[] sizes = {16, 32, 64, 128};
            int size = sizes[(int)(Math.random() * sizes.length)];
            asteroids.add(new Asteroid(pos, vel, size));
        }
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

    private void updateGame() {
        if (!playerAlive) return;
        player.onUpdate(1f / FPS);
        for (Asteroid asteroid : asteroids) {
            asteroid.onUpdate(1f / FPS);
        }
        for (Projectile proj : projectiles) {
            if (proj instanceof Updateable) {
                ((Updateable)proj).onUpdate(1f / FPS);
            }
        }
        // Kollisionen: Projektile vs Asteroiden
        List<Asteroid> destroyed = new ArrayList<>();
        List<Projectile> usedProjectiles = new ArrayList<>();
        for (Asteroid asteroid : asteroids) {
            for (Projectile proj : projectiles) {
                Rectangle projRect = new Rectangle((int)proj.getPosition().x, (int)proj.getPosition().y, (int)proj.getSize(), (int)proj.getSize());
                if (asteroid.getShape().intersects(projRect)) {
                    destroyed.add(asteroid);
                    usedProjectiles.add(proj);
                }
            }
        }
        asteroids.removeAll(destroyed);
        projectiles.removeAll(usedProjectiles);
        // Kollisionen: Spieler vs Asteroiden
        for (Asteroid asteroid : asteroids) {
            double px = player.getPosition().x;
            double py = player.getPosition().y;
            double pr = Player.PLAYER_DIMENSIONS / 2.0;
            Rectangle playerRect = new Rectangle((int)(px - pr), (int)(py - pr), (int)(pr * 2), (int)(pr * 2));
            if (asteroid.getShape().intersects(playerRect)) {
                playerAlive = false;
            }
        }
    }

}
