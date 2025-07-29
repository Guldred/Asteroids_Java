package game.object;

import game.component.GameCore;
import game.component.Updateable;
import game.component.Vector2;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

public class Asteroid extends Updateable {

    private Vector2 position;
    private Vector2 velocity;
    private Vector2 frameVelocity;
    private float angle;
    private float rotationSpeed;
    private final int SIZE;
    private final Image image;
    private final Area shape;
    private float wallBounceFactor;
    private final float MAX_SPEED;

    public Asteroid(Vector2 position, Vector2 velocity, int size) {
        super();
        this.position = position;
        this.velocity = velocity;
        this.SIZE = size;
        this.rotationSpeed = (float) (Math.random() * 2 - 1);
        this.angle = 0;
        this.image = new ImageIcon("src/game/resource/img/asteroids/asteroid_" + size + ".png").getImage();
        this.shape = createShape();
        this.wallBounceFactor = 1.2f;
        this.MAX_SPEED = (float) (Math.random() * 10 + 1);

        startUpdate();
    }

    private Area createShape() {
        return new Area(new Ellipse2D.Double(0, 0, SIZE, SIZE));
    }

    public Area getShape() {
        AffineTransform t = new AffineTransform();
        t.translate(position.x, position.y);
        t.rotate(Math.toRadians(angle), SIZE / 2, SIZE / 2);
        return new Area(t.createTransformedShape(shape));
    }


    @Override
    public void onUpdate(float deltaTime) {
        this.UpdatePosition(deltaTime);
    }

    protected void UpdatePosition(float deltaTime) {
        checkOutOfBounds();
        frameVelocity = new Vector2(velocity.x * deltaTime, velocity.y * deltaTime);
        position.add(frameVelocity);
        angle += (rotationSpeed * deltaTime);
    }

    public void draw(Graphics2D g2) {
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(position.x, position.y);
        AffineTransform t = new AffineTransform();
        t.rotate(Math.toRadians(angle), (double) SIZE / 2, (double) SIZE / 2);
        g2.drawImage(image, t, null);

        //Test
        Shape shape = getShape();


        g2.setTransform(oldTransform);

        //Test
        g2.setColor(Color.RED);
        g2.draw(shape);
        g2.draw(shape.getBounds());
    }

    public Vector2 getCenter() {
        return new Vector2( (float) (position.x + SIZE / 2), (float)(position.y + SIZE / 2));
    }

    public void checkOutOfBounds() {
        if (position.x < 0) {
            velocity.x = Math.min((Math.abs(velocity.x) * wallBounceFactor), MAX_SPEED);
        } else if (position.x + SIZE > GameCore.screenSize.x) {
            velocity.x = Math.max((Math.abs(velocity.x) * -1 * wallBounceFactor), -MAX_SPEED);
        } else if (position.y < 0) {
            velocity.y = Math.min((Math.abs(velocity.y) * wallBounceFactor), MAX_SPEED);
        } else if (position.y + SIZE + 30 > GameCore.screenSize.y) {
            velocity.y = Math.max((Math.abs(velocity.y) * -1 * wallBounceFactor), -MAX_SPEED);
        }
    }
}
