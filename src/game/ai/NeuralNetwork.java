package game.ai;

import java.util.List;
import java.util.Random;

public class NeuralNetwork {
    private Layer[] layers;
    private double learningRate;
    private Random random;
    private static final double DISCOUNT_FACTOR = 0.99; // Assuming a discount factor
    
    public NeuralNetwork(int inputSize, int outputSize, double learningRate) {
        this.learningRate = learningRate;
        this.random = new Random();
        
        // Create a simple feedforward network: input -> hidden1 -> hidden2 -> output
        int hidden1Size = Math.max(64, inputSize * 2);
        int hidden2Size = Math.max(32, outputSize * 4);
        
        layers = new Layer[3];
        layers[0] = new Layer(hidden1Size, inputSize, true); // Hidden layer 1
        layers[1] = new Layer(hidden2Size, hidden1Size, true); // Hidden layer 2  
        layers[2] = new Layer(outputSize, hidden2Size, false); // Output layer (no activation for Q-values)
        
        initializeWeights();
    }
    
    private void initializeWeights() {
        // Xavier/Glorot initialization
        for (Layer layer : layers) {
            layer.initializeWeights(random);
        }
    }
    
    public double[] forward(double[] input) {
        double[] current = input.clone();
        
        for (Layer layer : layers) {
            current = layer.forward(current);
        }
        
        return current;
    }
    
    public void trainBatch(List<ExperienceReplay.Experience> batch) {
        for (ExperienceReplay.Experience exp : batch) {
            // Validate experience data to prevent crashes
            if (exp.state == null || exp.action == null || exp.nextState == null ||
                exp.state.length == 0 || exp.action.length == 0 || exp.nextState.length == 0) {
                continue; // Skip invalid experiences
            }
            
            // Calculate target using Bellman equation
            double[] nextQValues = forward(exp.nextState);
            double maxNextQ = 0;
            for (double q : nextQValues) {
                maxNextQ = Math.max(maxNextQ, q);
            }
            
            double target = exp.reward;
            if (!exp.done) {
                target += DISCOUNT_FACTOR * maxNextQ;
            }
            
            // Create proper target array with expected output size
            double[] currentOutput = forward(exp.state);
            if (currentOutput.length != exp.action.length) {
                continue; // Skip if dimensions don't match
            }
            
            double[] targets = currentOutput.clone(); // Start with current network output
            
            // Update all outputs toward the target - simplified continuous action training
            for (int i = 0; i < targets.length; i++) {
                targets[i] = target * 0.1 + exp.action[i] * 0.9; // Smooth update toward target
            }
            
            // Backpropagate
            backpropagate(exp.state, targets);
        }
    }
    
    private void backpropagate(double[] input, double[] targetOutput) {
        // Forward pass to get activations
        double[] current = input.clone();
        double[][] activations = new double[layers.length + 1][];
        activations[0] = current;
        
        for (int i = 0; i < layers.length; i++) {
            current = layers[i].forward(current);
            activations[i + 1] = current.clone();
        }
        
        // Calculate output error
        double[] outputError = new double[targetOutput.length];
        for (int i = 0; i < outputError.length; i++) {
            outputError[i] = targetOutput[i] - current[i];
        }
        
        // Backward pass
        double[] currentError = outputError;
        
        for (int i = layers.length - 1; i >= 0; i--) {
            currentError = layers[i].backward(activations[i], currentError, learningRate);
        }
    }
    
    public void copyWeightsFrom(NeuralNetwork other) {
        if (other.layers.length != this.layers.length) {
            throw new IllegalArgumentException("Network architectures don't match");
        }
        
        for (int i = 0; i < layers.length; i++) {
            layers[i].copyWeightsFrom(other.layers[i]);
        }
    }
    
    private double findMax(double[] values) {
        double max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }
    
    private int findMaxIndex(double[] values) {
        int maxIndex = 0;
        double max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }
    
    // Inner Layer class
    private static class Layer {
        private double[][] weights;
        private double[] biases;
        private boolean useActivation;
        
        public Layer(int outputSize, int inputSize, boolean useActivation) {
            this.weights = new double[outputSize][inputSize];
            this.biases = new double[outputSize];
            this.useActivation = useActivation;
        }
        
        public void initializeWeights(Random random) {
            double limit = Math.sqrt(6.0 / (weights[0].length + weights.length));
            
            for (int i = 0; i < weights.length; i++) {
                for (int j = 0; j < weights[i].length; j++) {
                    weights[i][j] = (random.nextDouble() * 2 - 1) * limit;
                }
                biases[i] = 0; // Initialize biases to zero
            }
        }
        
        public double[] forward(double[] input) {
            double[] output = new double[weights.length];
            
            // Linear transformation
            for (int i = 0; i < weights.length; i++) {
                double sum = biases[i];
                for (int j = 0; j < input.length; j++) {
                    sum += weights[i][j] * input[j];
                }
                output[i] = sum;
            }
            
            // Apply activation function (ReLU for hidden layers)
            if (useActivation) {
                for (int i = 0; i < output.length; i++) {
                    output[i] = Math.max(0, output[i]); // ReLU activation
                }
            }
            
            return output;
        }
        
        public double[] backward(double[] input, double[] outputError, double learningRate) {
            // Validate input arrays to prevent crashes
            if (input == null || outputError == null || input.length == 0 || outputError.length == 0 ||
                weights.length == 0 || weights[0].length == 0) {
                return new double[input != null ? input.length : 0]; // Return safe empty array
            }
            
            double[] inputError = new double[input.length];
            
            // Calculate input error
            for (int j = 0; j < input.length; j++) {
                double sum = 0;
                for (int i = 0; i < weights.length && i < outputError.length; i++) {
                    if (j < weights[i].length) {
                        sum += outputError[i] * weights[i][j];
                    }
                }
                inputError[j] = sum;
                
                // Apply derivative of activation function
                if (useActivation && input[j] <= 0) {
                    inputError[j] = 0; // Derivative of ReLU
                }
            }
            
            // Update weights and biases
            for (int i = 0; i < weights.length && i < outputError.length; i++) {
                // Update bias
                biases[i] += learningRate * outputError[i];
                
                // Update weights
                for (int j = 0; j < weights[i].length && j < input.length; j++) {
                    weights[i][j] += learningRate * outputError[i] * input[j];
                }
            }
            
            return inputError;
        }
        
        public void copyWeightsFrom(Layer other) {
            for (int i = 0; i < weights.length; i++) {
                System.arraycopy(other.weights[i], 0, this.weights[i], 0, weights[i].length);
                biases[i] = other.biases[i];
            }
        }
    }
}
