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
        screenSize = new Vector2(1920, 1080);
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
        asteroids.add(new Asteroid(new Vector2(100, 100), new Vector2(3, 2), 64));
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
