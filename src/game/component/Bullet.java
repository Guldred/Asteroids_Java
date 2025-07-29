package game.component;

import java.awt.geom.Rectangle2D;

public class Bullet {
    public double x, y; // Position
    public double dx, dy; // Velocity

    public Bullet(double x, double y, double dx, double dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    public void update() {
        x += dx;
        y += dy;
        // Remove bullets that go off-screen
        if (x < 0 || x > 1280 || y < 0 || y > 720) {
            // Mark for removal
        }
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x, y, 4, 4);
    }
}