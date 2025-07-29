package game.object.projectiles;

import game.component.Vector2;
import game.object.Asteroid;

import java.awt.*;

public interface Projectile {

    public void draw(Graphics2D g2d);

    public Vector2 getPosition();

    public Vector2 getVelocity();

    public float getSize();

    public Vector2 getCenter();

    public void stop();

    public boolean outOfBounds(int width, int height);

    public boolean collidesWith(Asteroid asteroid);

}
