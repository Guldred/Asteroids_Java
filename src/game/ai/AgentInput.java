package game.ai;

public class AgentInput {
    public boolean thrustForward;
    public boolean thrustBack;
    public boolean strafeLeft;
    public boolean strafeRight;
    public boolean shoot;
    // Angle change in degrees for this tick (positive = clockwise)
    public float angleDelta;

    public AgentInput() {}

    public static AgentInput idle() {
        return new AgentInput();
    }
}
