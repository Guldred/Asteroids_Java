package game.component;

import game.object.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class GameCore extends JComponent {

    private JFrame window;
    private Graphics2D g2;
    private BufferedImage image;

    private int width;
    private int height;
    private Thread thread;
    private boolean start = true;
    private PlayerInput playerInput;

    // Game Fps
    private final int FPS = 60;
    private final int TARGET_TIME = 1000000000 / FPS;

    // Game Objects
    private Player player;

    public GameCore(JFrame window) {
        this.window = window;
    }

    public void start() {
        width = getWidth();
        height = getHeight();

        initGFX();
        initGameObjects();
        initInput();

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

        playerInput = new PlayerInput();

        requestFocus();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    playerInput.setKey_left(true);
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    playerInput.setKey_right(true);
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    playerInput.setKey_up(true);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    playerInput.setKey_down(true);
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    playerInput.setKey_space(true);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    playerInput.setKey_left(false);
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    playerInput.setKey_right(false);
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    playerInput.setKey_up(false);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    playerInput.setKey_down(false);
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    playerInput.setKey_space(false);
                }
            }
        });
        /*
        new Thread(() -> {
            float s = 1f;
            float angle = 0;
            float frameTime = 0;
            float lastFrameTime = 0;
            float inputFactor = 0;
            while (start) {
                lastFrameTime = frameTime;
                frameTime = System.nanoTime();
                inputFactor = (frameTime - lastFrameTime) / 10000000f;
                angle = player.getAngle();

                System.out.println("Factor: " + inputFactor);
                if (playerInput.isKey_left()) {
                    angle -= s * inputFactor;
                } else if (playerInput.isKey_right()) {
                    angle += s * inputFactor;
                }
                player.setAngle(angle);
                sleep(10);
            }
        }).start();

         */

        new Thread(() -> {
            float s = 1f;
            float frameTime = 0;
            float lastFrameTime = 0;
            float inputFactor = 0;
            Point playerPos;
            Point mousePos;

            while (start) {
                lastFrameTime = frameTime;
                frameTime = System.nanoTime();
                inputFactor = (frameTime - lastFrameTime) / 10000000f;

                playerPos = player.getCenter();

                mousePos = playerInput.getMousePositionInGame(window);

                //player.setPosition(250, 250);
                player.setAngle(calcAngleFromPoints(playerPos, mousePos));

                System.out.println("PlayerPos: " + playerPos.x + "/" + playerPos.y + " MousePos: " + mousePos.x + "/" + mousePos.y);

                sleep(10);
            }
        }).start();


    }

    public float calcAngleFromPoints(Point player, Point target) {
        float angle = (float) Math.toDegrees(Math.atan2(target.y - player.y, target.x - player.x));

        if(angle < 0){
            angle += 360;
        }

        return angle;
    }

    private void initGameObjects() {
        player = new Player();
        player.setPosition(new Point(100, 100));
    }





    private void sleep(long t) {
        //System.out.println("Sleeping for " + t + " ms");
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }
}
