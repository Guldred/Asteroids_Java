package game.ai;

import game.nn.Network;

public class NNAgent implements Agent {
    private final Network net;
    private final float maxTurnDeg;

    public NNAgent(Network net, float maxTurnDeg) {
        this.net = net;
        this.maxTurnDeg = maxTurnDeg;
    }

    @Override
    public AgentInput decide(float[] state) {
        float[] out = net.forward(state);
        AgentInput a = new AgentInput();
        // Expecting out dims >= 6: [angleDeltaRaw, thrustF, thrustB, strafeL, strafeR, shoot]
        float angleRaw = out[0];
        a.angleDelta = tanh(angleRaw) * maxTurnDeg;
        a.thrustForward = sigmoid(out[1]) > 0.5f;
        a.thrustBack = sigmoid(out[2]) > 0.5f;
        a.strafeLeft = sigmoid(out[3]) > 0.5f;
        a.strafeRight = sigmoid(out[4]) > 0.5f;
        a.shoot = sigmoid(out[5]) > 0.5f;
        return a;
    }

    public void setParams(float[] params) {
        net.setParams(params);
    }

    public int paramCount() {
        return net.paramCount();
    }

    private float sigmoid(float x) {
        return 1f / (1f + (float)Math.exp(-x));
    }
    private float tanh(float x) {
        return (float)Math.tanh(x);
    }
}
