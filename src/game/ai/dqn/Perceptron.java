package game.ai.dqn;

import java.util.Random;

/**
 * A single Perceptron - the fundamental building block of neural networks.
 * 
 * Structure:
 * inputs[] → [weights] → activation_function → output
 * 
 * This represents a single "neuron" that:
 * 1. Takes multiple inputs
 * 2. Multiplies each by its weight  
 * 3. Adds bias
 * 4. Applies activation function
 * 5. Produces single output
 */
public class Perceptron {
    private float[] weights;
    private float bias;
    private final int inputSize;
    private final Random random;
    
    // For backpropagation
    private float lastOutput;
    private float[] lastInputs;
    private float gradient;
    
    public Perceptron(int inputSize) {
        this.inputSize = inputSize;
        this.weights = new float[inputSize];
        this.random = new Random();
        this.lastInputs = new float[inputSize];
        
        // Xavier/Glorot initialization for better training
        float scale = (float) Math.sqrt(2.0 / inputSize);
        for (int i = 0; i < inputSize; i++) {
            weights[i] = (float) (random.nextGaussian() * scale);
        }
        bias = (float) (random.nextGaussian() * scale);
    }
    
    /**
     * Forward pass: compute output from inputs
     * output = activation(sum(input[i] * weight[i]) + bias)
     */
    public float forward(float[] inputs) {
        if (inputs.length != inputSize) {
            throw new IllegalArgumentException("Expected " + inputSize + " inputs, got " + inputs.length);
        }
        
        // Store inputs for backpropagation
        System.arraycopy(inputs, 0, lastInputs, 0, inputSize);
        
        // Weighted sum + bias
        float sum = bias;
        for (int i = 0; i < inputSize; i++) {
            sum += inputs[i] * weights[i];
        }
        
        // Apply activation function (ReLU)
        lastOutput = Math.max(0, sum);
        return lastOutput;
    }
    
    /**
     * Backward pass: compute gradients for backpropagation
     */
    public void backward(float errorGradient, float learningRate) {
        // ReLU derivative: 1 if output > 0, else 0
        float activationDerivative = lastOutput > 0 ? 1.0f : 0.0f;
        
        // Compute this neuron's gradient
        gradient = errorGradient * activationDerivative;
        
        // Update weights: weight -= learningRate * gradient * input
        for (int i = 0; i < inputSize; i++) {
            weights[i] -= learningRate * gradient * lastInputs[i];
        }
        
        // Update bias: bias -= learningRate * gradient
        bias -= learningRate * gradient;
    }
    
    /**
     * Get error to propagate to previous layer
     */
    public float[] getInputGradients() {
        float[] inputGradients = new float[inputSize];
        for (int i = 0; i < inputSize; i++) {
            inputGradients[i] = gradient * weights[i];
        }
        return inputGradients;
    }
    
    // Getters for visualization/debugging
    public float[] getWeights() { return weights.clone(); }
    public float getBias() { return bias; }
    public float getLastOutput() { return lastOutput; }
    public int getInputSize() { return inputSize; }
    
    // For genetic algorithm compatibility
    public void setWeights(float[] newWeights, float newBias) {
        System.arraycopy(newWeights, 0, weights, 0, inputSize);
        this.bias = newBias;
    }
    
    @Override
    public String toString() {
        return String.format("Perceptron[inputs=%d, bias=%.3f, lastOutput=%.3f]", 
                           inputSize, bias, lastOutput);
    }
}
