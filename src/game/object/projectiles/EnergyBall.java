package game.object.projectiles;

import game.component.Vector2;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class EnergyBall extends BaseProjectile {
    public EnergyBall(Vector2 position, Vector2 velocity, float angle, float speed) {
        super(position, velocity, angle, speed);
        this.size = 10;
        this.shape = new Ellipse2D.Float(0, 0, size, size);
        this.color = Color.RED;
        startUpdate();
    }
}
