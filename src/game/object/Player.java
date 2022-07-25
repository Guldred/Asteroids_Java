package game.object;

import game.component.PlayerInput;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import javax.swing.JComponent;

public class Player extends JComponent {
    public static final double PLAYER_SIZE = 64;
    private double x;
    private double y;
    private float angle = 0f;
    private final Image image;
    private final Image image_speed;


    public Player() {
        this.image = new ImageIcon("src/game/resource/img/plane.png").getImage();
        this.image_speed = new ImageIcon("src/game/resource/img/plane_speed.png").getImage();
    }

    public void changeAngle(float angle) {
        angle = (angle > 360) ? angle - 360 : (angle < 0) ? angle + 360 : angle;
        this.angle = angle;
    }

    public void draw(Graphics2D g2) {
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(x, y);
        AffineTransform t = g2.getTransform();
        t.rotate(Math.toRadians(angle+45), PLAYER_SIZE / 2, PLAYER_SIZE / 2);
        g2.drawImage(image, t, null);
        g2.setTransform(oldTransform);
    }

    public double getPosX() {
        return x;
    }

    public double getPosY() {
        return y;
    }

    public float getAngle() {
        return angle;
    }

    public void updateLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }
}
