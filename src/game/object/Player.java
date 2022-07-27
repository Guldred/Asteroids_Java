package game.object;

import game.component.*;
import game.object.projectiles.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class Player extends Updateable {
    public static final double PLAYER_DIMENSIONS = 64;
    private final float MAX_SPEED = 1;
    private Vector2 velocity = new Vector2(0, 0);
    private float playerViewAngle = 0f;
    private boolean start = true;
    private Vector2 position;
    private final Image playerImage;
    private float wallBounceFactor;
    PlayerInput playerInput;
    JFrame window;



    public Player(JFrame window) {
        super();
        this.window = window;
        this.playerImage = new ImageIcon("src/game/resource/img/spaceship_brown_default_turned.png").getImage();
        this.playerInput = new PlayerInput();
        this.wallBounceFactor = 0.6f;
        super.startUpdate();
    }


    @Override
    public void onUpdate(float deltaTime) {
        this.getInput(deltaTime);
        this.updatePos(deltaTime);
        this.turnToCursor();
    }



    private void getInput(float deltaTime) {
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
    }

    public void setInputToMap(InputEventTypes eventType, int keyCode) {
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
        t.rotate(Math.toRadians(playerViewAngle), PLAYER_DIMENSIONS / 2, PLAYER_DIMENSIONS / 2);
        g2.drawImage(playerImage, t, null);
        //g2.drawImage(accForward ? accelImage : image, t, null);
        g2.setTransform(oldTransform);
    }

    public Vector2 getPos() {
        return position;
    }

    public Vector2 getCenter() {
        return new Vector2( (float) (position.x + PLAYER_DIMENSIONS / 2), (float)(position.y + PLAYER_DIMENSIONS / 2));
    }

    public float getAngle() {
        return playerViewAngle;
    }

    public void setPosition(Vector2 pos) {
        this.position = pos;
    }

    public void setAngle(float playerViewAngle) {
        this.playerViewAngle = playerViewAngle;
    }

    public void turnToCursor() {
        setAngle(calcAngleFromPoints(getCenter(), playerInput.getMousePositionInGame(window)));
    }

    public float calcAngleFromPoints(Vector2 player, Vector2 target) {
        float angle = (float) Math.toDegrees(Math.atan2(target.y - player.y, target.x - player.x));

        if(angle < 0){
            angle += 360;
        }
        return angle;
    }

    public void accelerate(float inputDirection, float deltaTime) {
        float force = deltaTime/10 * 1;
        velocity.addForce(playerViewAngle + inputDirection, force);
    }

    public void updatePos(float deltaTime) {
        checkOutOfBounds();
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;
    }

    public Projectile shoot(int weapon) {
        return new EnergyBall(getCenter(), velocity, this.playerViewAngle, 10f);
    }

    public void checkOutOfBounds() {
        if (position.x < 0) {
            velocity.x = Math.abs(velocity.x) * wallBounceFactor;
        } else if (position.x + PLAYER_DIMENSIONS > GameCore.screenSize.x) {
            velocity.x = Math.abs(velocity.x) * -1 * wallBounceFactor;
        } else if (position.y < 0) {
            velocity.y = Math.abs(velocity.y) * wallBounceFactor;
        } else if (position.y + PLAYER_DIMENSIONS + 30 > GameCore.screenSize.y) {
            velocity.y = Math.abs(velocity.y) * -1 * wallBounceFactor;
        }
    }
}
