package game.nn;

public enum Activation {
    LINEAR,
    RELU,
    TANH,
    SIGMOID;

    public static float activate(float x, Activation a) {
        switch (a) {
            case RELU: return x > 0 ? x : 0f;
            case TANH: return (float) Math.tanh(x);
            case SIGMOID: return 1f / (1f + (float)Math.exp(-x));
            case LINEAR:
            default: return x;
        }
    }
}
