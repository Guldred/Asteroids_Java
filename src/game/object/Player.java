package game.object;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.swing.JComponent;

public class Player extends JComponent {
    public static final double PLAYER_SIZE = 64;
    private Point position;
    private float angle = 0f;
    private final Image image;
    private final Image image_speed;


    public Player() {
        this.image = new ImageIcon("src/game/resource/img/spaceship_brown_default_turned.png").getImage();
        this.image_speed = new ImageIcon("src/game/resource/img/plane_speed.png").getImage();
    }

    public void changeAngle(float angle) {
        angle = (angle > 360) ? angle - 360 : (angle < 0) ? angle + 360 : angle;
        this.angle = angle;
    }

    public void draw(Graphics2D g2) {
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(position.x, position.y);
        AffineTransform t = g2.getTransform();
        t.rotate(Math.toRadians(angle), PLAYER_SIZE / 2, PLAYER_SIZE / 2);
        g2.drawImage(image, t, null);
        g2.setTransform(oldTransform);
    }

    public Point getPos() {
        return position;
    }

    public Point getCenter() {
        return new Point( (int) (position.x + PLAYER_SIZE / 2), (int)(position.y + PLAYER_SIZE / 2));
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
}
