package game.ai;

import java.util.Random;

public class RandomAgent implements Agent {
    private final Random rng = new Random();

    @Override
    public AgentInput decide(float[] state) {
        AgentInput a = new AgentInput();
        // Small random rotation each tick
        a.angleDelta = (rng.nextFloat() - 0.5f) * 10f; // -5 to +5 degrees per frame
        // Random thrust behaviors
        a.thrustForward = rng.nextFloat() < 0.2f;
        a.thrustBack = false;
        a.strafeLeft = rng.nextFloat() < 0.1f;
        a.strafeRight = rng.nextFloat() < 0.1f;
        // Occasionally shoot
        a.shoot = rng.nextFloat() < 0.15f;
        return a;
    }
}
