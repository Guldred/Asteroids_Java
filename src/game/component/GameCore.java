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

    // Game Fps
    private final int FPS = 67;
    private final int TARGET_TIME = 1000000000 / FPS;

    // Game Objects
    private Player player;
    private PlayerInput playerInput;

    public GameCore(JFrame window) { this.window = window;}

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
        //Capture Inputs and send them to player (TODO: make this Observer Pattern)
        requestFocus();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                inputListenerEvent(InputEventTypes.KEY_PRESSED, e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                inputListenerEvent(InputEventTypes.KEY_RELEASED, e.getKeyCode());
            }
        });
    }

    public void inputListenerEvent(InputEventTypes eventType, int keyCode) {
        player.setInputToMap(eventType, keyCode);
    }



    private void initGameObjects() {
        player = new Player(window);
        player.setPosition(new Point(100, 100));
    }


    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }
}
