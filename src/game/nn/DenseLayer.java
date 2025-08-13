package game.nn;

import java.util.Random;

public class DenseLayer {
    public final int inSize;
    public final int outSize;
    public final Activation activation;

    // weights[out][in]
    public final float[][] w;
    // biases[out]
    public final float[] b;

    public DenseLayer(int inSize, int outSize, Activation activation, Random rng) {
        this.inSize = inSize;
        this.outSize = outSize;
        this.activation = activation;
        this.w = new float[outSize][inSize];
        this.b = new float[outSize];
        initWeights(rng);
    }

    private void initWeights(Random rng) {
        // Xavier/Glorot uniform-ish
        float limit = (float) Math.sqrt(6.0 / (inSize + outSize));
        for (int o = 0; o < outSize; o++) {
            for (int i = 0; i < inSize; i++) {
                w[o][i] = (rng.nextFloat() * 2f - 1f) * limit;
            }
            b[o] = 0f;
        }
    }

    public void forward(float[] input, float[] output) {
        for (int o = 0; o < outSize; o++) {
            float sum = b[o];
            float[] row = w[o];
            for (int i = 0; i < inSize; i++) {
                sum += row[i] * input[i];
            }
            output[o] = Activation.activate(sum, activation);
        }
    }
}
