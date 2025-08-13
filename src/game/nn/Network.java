package game.nn;

import java.util.Random;

public class Network {
    private final DenseLayer[] layers;

    public Network(DenseLayer[] layers) {
        this.layers = layers;
    }

    public static Network mlp(int input, int[] hidden, int output, long seed) {
        Random rng = new Random(seed);
        DenseLayer[] arr = new DenseLayer[hidden.length + 1];
        int in = input;
        for (int i = 0; i < hidden.length; i++) {
            arr[i] = new DenseLayer(in, hidden[i], Activation.RELU, rng);
            in = hidden[i];
        }
        // Output layer linear, post-processed by agent
        arr[hidden.length] = new DenseLayer(in, output, Activation.LINEAR, rng);
        return new Network(arr);
    }

    public float[] forward(float[] x) {
        float[] a = x;
        for (int i = 0; i < layers.length; i++) {
            DenseLayer L = layers[i];
            float[] y = new float[L.outSize];
            L.forward(a, y);
            a = y;
        }
        return a;
    }

    public int paramCount() {
        int n = 0;
        for (DenseLayer L : layers) {
            n += L.outSize * L.inSize; // weights
            n += L.outSize;            // biases
        }
        return n;
    }

    public void getParams(float[] dest) {
        int idx = 0;
        for (DenseLayer L : layers) {
            for (int o = 0; o < L.outSize; o++) {
                for (int i = 0; i < L.inSize; i++) {
                    dest[idx++] = L.w[o][i];
                }
            }
            for (int o = 0; o < L.outSize; o++) {
                dest[idx++] = L.b[o];
            }
        }
    }

    public void setParams(float[] src) {
        int idx = 0;
        for (DenseLayer L : layers) {
            for (int o = 0; o < L.outSize; o++) {
                for (int i = 0; i < L.inSize; i++) {
                    L.w[o][i] = src[idx++];
                }
            }
            for (int o = 0; o < L.outSize; o++) {
                L.b[o] = src[idx++];
            }
        }
    }
}
