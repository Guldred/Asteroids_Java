package game.object.projectiles;

import game.component.Updateable;
import game.component.Vector2;
import game.object.Asteroid;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class BaseProjectile extends Updateable implements Projectile {
    protected Vector2 position;
    protected Vector2 velocity;
    protected Vector2 frameVelocity;
    protected float angle;
    protected Shape shape;
    protected Color color;
    protected float size;
    protected float speed;



    public BaseProjectile(Vector2 position, Vector2 velocity, float angle, float speed) {
        this.position = new Vector2(position);
        this.speed = speed;
        this.velocity = new Vector2((float) Math.cos(Math.toRadians(angle)) * speed, (float) Math.sin(Math.toRadians(angle)) * speed);
        this.angle = angle;
    }

    @Override
    protected void onUpdate(float deltaTime) {
        this.UpdatePosition(deltaTime);
    }



    @Override
    public void draw(Graphics2D g2D) {
        AffineTransform at = g2D.getTransform();
        g2D.setColor(color);
        g2D.translate(position.x, position.y);
        g2D.fill(shape);
        g2D.setTransform(at);
    }

    @Override
    public Vector2 getPosition() {
        return position;
    }

    @Override
    public Vector2 getVelocity() {
        return velocity;
    }

    @Override
    public float getSize() {
        return size;
    }

    @Override
    public Vector2 getCenter() {
        return new Vector2( (float) (position.x + size / 2), (float)(position.y + size / 2));
    }

    @Override
    public boolean outOfBounds(int width, int height) {
        return (position.x < 0 || position.x > width || position.y < 0 || position.y > height);
    }

    @Override
    public void stop() {
        start = false;
    }

    protected void UpdatePosition(float deltaTime) {
        frameVelocity = new Vector2(velocity.x * deltaTime, velocity.y * deltaTime);
        position.add(frameVelocity);
    }

    @Override
    public boolean collidesWith(Asteroid asteroid) {
        Vector2 projectileCenter = getCenter();
        Vector2 asteroidCenter = asteroid.getCenter();
        float distance = projectileCenter.distance(asteroidCenter);
        float collisionDistance = (size / 2) + (asteroid.getSize() / 2);
        return distance < collisionDistance;
    }
}
