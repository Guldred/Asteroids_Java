package game.object;

import game.component.DeltaTimer;
import game.component.InputEventTypes;
import game.component.PlayerInput;
import game.component.Vector2;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.swing.JComponent;

public class Player extends JComponent {
    public static final double PLAYER_DIMENSIONS = 64;
    private final float MAX_SPEED = 1;
    private float speed = 0f;
    private Vector2 velocity = new Vector2(0, 0);
    private boolean accForward;
    private float angle = 0f;
    private Point position;
    private final Image image;
    private final Image image_speed;
    PlayerInput playerInput;
    JFrame window;


    public Player(JFrame window) {
        this.window = window;
        this.image = new ImageIcon("src/game/resource/img/spaceship_brown_default_turned.png").getImage();
        this.image_speed = new ImageIcon("src/game/resource/img/plane_speed.png").getImage();

        initInput();
    }

    private void initInput() {

        playerInput = new PlayerInput();


        new Thread(() -> {
            long lastFrameTime = 0;
            long frameTime = 0;
            float deltaTime = 0;



            while (true) {
                lastFrameTime = frameTime;
                frameTime = System.nanoTime();
                deltaTime = (float)(frameTime - lastFrameTime) / 10000000f;

                update();
                turnToCursor();


                if (playerInput.isKey_up()) {
                    accelerate(0, deltaTime);
                }
                if (playerInput.isKey_right()) {
                    accelerate(60, deltaTime);
                }
                if (playerInput.isKey_down()) {
                    accelerate(180, deltaTime);
                }
                if (playerInput.isKey_left()) {
                    accelerate(300, deltaTime);
                }


                sleep(10);
            }
        }).start();


    }



    public void inputUpdate(InputEventTypes eventType, int keyCode) {
        playerInput.keyPressUpdate(eventType, keyCode);
    }

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }

    public void draw(Graphics2D g2) {
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(position.x, position.y);
        AffineTransform t = new AffineTransform();
        t.rotate(Math.toRadians(angle), PLAYER_DIMENSIONS / 2, PLAYER_DIMENSIONS / 2);
        g2.drawImage(image, t, null);
        //g2.drawImage(accForward ? accelImage : image, t, null);
        g2.setTransform(oldTransform);
    }

    public Point getPos() {
        return position;
    }

    public Point getCenter() {
        return new Point( (int) (position.x + PLAYER_DIMENSIONS / 2), (int)(position.y + PLAYER_DIMENSIONS / 2));
    }

    public float getAngle() {
        return angle;
    }

    public void setPosition(Point pos) {
        this.position = pos;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public void turnToCursor() {
        setAngle(calcAngleFromPoints(getCenter(), playerInput.getMousePositionInGame(window)));
    }

    public float calcAngleFromPoints(Point player, Point target) {
        float angle = (float) Math.toDegrees(Math.atan2(target.y - player.y, target.x - player.x));

        if(angle < 0){
            angle += 360;
        }
        return angle;
    }

    public void accelerate(float direction, float deltaTime) {
        //System.out.println("delta: " + deltaTime);
        accForward = true;
        velocity.x += Math.cos(Math.toRadians(angle + direction)) * deltaTime/10 * 1;
        velocity.y += Math.sin(Math.toRadians(angle + direction)) * deltaTime/10 * 1;
    }

    public void update() {
        position.x += velocity.x;
        position.y += velocity.y;
    }
}
