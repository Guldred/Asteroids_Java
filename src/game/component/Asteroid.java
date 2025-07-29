package game.component;

import java.awt.geom.Rectangle2D;

public class Asteroid {
    public double x, y; // Position
    public double dx, dy; // Velocity
    public int size; // Size (1-3)

    public Asteroid(double x, double y, double dx, double dy, int size) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.size = size;
    }

    public void update() {
        x += dx;
        y += dy;
        // Wrap around screen edges
        if (x < 0) x = 1280;
        if (x > 1280) x = 0;
        if (y < 0) y = 720;
        if (y > 720) y = 0;
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, size*16, size*16);
    }
}