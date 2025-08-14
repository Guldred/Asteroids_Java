package game.ai.dqn;

/**
 * Deep Q-Network - a complete neural network built from Layers.
 * 
 * Structure:
 * Input → Hidden Layer 1 → Hidden Layer 2 → ... → Output Layer
 * 
 * For Asteroids DQN:
 * - Input: Game state (player pos, velocity, asteroids, etc.)
 * - Hidden: Feature extraction and decision making
 * - Output: Q-values for each possible action
 * 
 * Educational demonstration of how:
 * Perceptrons → Layers → Complete Network
 */
public class DQNNetwork {
    private final Layer[] layers;
    private final int inputSize;
    private final int outputSize;
    private final int[] layerSizes;
    
    // For backpropagation
    private float[][] layerOutputs; // Store outputs from each layer
    
    /**
     * Create a Deep Q-Network with specified architecture
     * 
     * @param inputSize - size of input vector (game state)
     * @param hiddenSizes - array of hidden layer sizes [32, 32] means 2 hidden layers of 32 neurons each
     * @param outputSize - number of possible actions (output Q-values)
     */
    public DQNNetwork(int inputSize, int[] hiddenSizes, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        
        // Build layer architecture: input → hidden1 → hidden2 → ... → output
        int numLayers = hiddenSizes.length + 1; // +1 for output layer
        this.layers = new Layer[numLayers];
        this.layerSizes = new int[numLayers + 1]; // +1 to include input size
        this.layerOutputs = new float[numLayers + 1][]; // +1 for input layer
        
        // Track layer sizes for visualization
        layerSizes[0] = inputSize;
        System.arraycopy(hiddenSizes, 0, layerSizes, 1, hiddenSizes.length);
        layerSizes[numLayers] = outputSize;
        
        // Create layers
        int currentInputSize = inputSize;
        
        // Hidden layers
        for (int i = 0; i < hiddenSizes.length; i++) {
            layers[i] = new Layer(currentInputSize, hiddenSizes[i]);
            currentInputSize = hiddenSizes[i];
        }
        
        // Output layer
        layers[numLayers - 1] = new Layer(currentInputSize, outputSize);
        
        // Initialize layer output storage
        for (int i = 0; i <= numLayers; i++) {
            layerOutputs[i] = new float[layerSizes[i]];
        }
    }
    
    /**
     * Forward pass: compute Q-values from game state
     * 
     * gameState → layer1 → layer2 → ... → Q-values
     */
    public float[] forward(float[] inputs) {
        if (inputs.length != inputSize) {
            throw new IllegalArgumentException("Expected " + inputSize + " inputs, got " + inputs.length);
        }
        
        // Store input as first "layer output"
        System.arraycopy(inputs, 0, layerOutputs[0], 0, inputSize);
        
        // Forward through each layer
        float[] currentOutput = inputs;
        for (int i = 0; i < layers.length; i++) {
            currentOutput = layers[i].forward(currentOutput);
            
            // Store this layer's output
            System.arraycopy(currentOutput, 0, layerOutputs[i + 1], 0, currentOutput.length);
        }
        
        return currentOutput; // Final layer output (Q-values)
    }
    
    /**
     * Backward pass: train network using Q-learning loss
     * 
     * @param targetQValues - what the Q-values should have been
     * @param learningRate - how fast to learn
     */
    public void backward(float[] targetQValues, float learningRate) {
        if (targetQValues.length != outputSize) {
            throw new IllegalArgumentException("Expected " + outputSize + " target values, got " + targetQValues.length);
        }
        
        // Compute output layer error (Mean Squared Error derivative)
        float[] outputGradients = new float[outputSize];
        float[] actualOutputs = layerOutputs[layers.length]; // Last layer's outputs
        
        for (int i = 0; i < outputSize; i++) {
            // MSE derivative: 2 * (predicted - target) / n
            outputGradients[i] = 2.0f * (actualOutputs[i] - targetQValues[i]) / outputSize;
        }
        
        // Backpropagate through layers (reverse order)
        float[] currentGradients = outputGradients;
        for (int i = layers.length - 1; i >= 0; i--) {
            currentGradients = layers[i].backward(currentGradients, learningRate);
        }
    }
    
    /**
     * Select best action using epsilon-greedy policy
     * 
     * @param qValues - Q-values from forward pass
     * @param epsilon - exploration rate (0.0 = always exploit, 1.0 = always explore)
     * @return action index
     */
    public int selectAction(float[] qValues, float epsilon) {
        if (Math.random() < epsilon) {
            // Explore: random action
            return (int) (Math.random() * qValues.length);
        } else {
            // Exploit: best action
            int bestAction = 0;
            float bestValue = qValues[0];
            for (int i = 1; i < qValues.length; i++) {
                if (qValues[i] > bestValue) {
                    bestValue = qValues[i];
                    bestAction = i;
                }
            }
            return bestAction;
        }
    }
    
    // Getters for visualization and debugging
    public int getInputSize() { return inputSize; }
    public int getOutputSize() { return outputSize; }
    public int getNumLayers() { return layers.length; }
    public Layer[] getLayers() { return layers; }
    public int[] getLayerSizes() { return layerSizes.clone(); }
    public float[][] getLayerOutputs() { return layerOutputs; }
    
    /**
     * Get the input state size of this network
     */
    public int getStateSize() {
        return inputSize;
    }
    
    /**
     * Get total number of parameters in the network
     */
    public int getParameterCount() {
        int count = 0;
        for (Layer layer : layers) {
            count += layer.getOutputSize() * (layer.getInputSize() + 1); // +1 for bias
        }
        return count;
    }
    
    /**
     * Get all network parameters as flat array (for genetic algorithm compatibility)
     */
    public float[] getAllParameters() {
        int totalParams = getParameterCount();
        float[] params = new float[totalParams];
        
        int idx = 0;
        for (Layer layer : layers) {
            float[] layerWeights = layer.getAllWeights();
            System.arraycopy(layerWeights, 0, params, idx, layerWeights.length);
            idx += layerWeights.length;
        }
        
        return params;
    }
    
    /**
     * Set all network parameters from flat array (for genetic algorithm compatibility)
     */
    public void setAllParameters(float[] params) {
        if (params.length != getParameterCount()) {
            throw new IllegalArgumentException("Expected " + getParameterCount() + " parameters, got " + params.length);
        }
        
        int idx = 0;
        for (Layer layer : layers) {
            int layerParamCount = layer.getOutputSize() * (layer.getInputSize() + 1);
            float[] layerParams = new float[layerParamCount];
            System.arraycopy(params, idx, layerParams, 0, layerParamCount);
            layer.setAllWeights(layerParams);
            idx += layerParamCount;
        }
    }
    
    /**
     * Create a copy of this network (for target networks in DQN)
     */
    public DQNNetwork copy() {
        // Create network with same architecture
        int[] hiddenSizes = new int[layers.length - 1];
        for (int i = 0; i < hiddenSizes.length; i++) {
            hiddenSizes[i] = layers[i].getOutputSize();
        }
        
        DQNNetwork copy = new DQNNetwork(inputSize, hiddenSizes, outputSize);
        
        // Copy all parameters
        copy.setAllParameters(this.getAllParameters());
        
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DQNNetwork Architecture:\n");
        sb.append(String.format("Input: %d neurons\n", inputSize));
        
        for (int i = 0; i < layers.length - 1; i++) {
            sb.append(String.format("Hidden[%d]: %d neurons\n", i + 1, layers[i].getOutputSize()));
        }
        
        sb.append(String.format("Output: %d neurons\n", outputSize));
        sb.append(String.format("Total parameters: %d", getParameterCount()));
        
        return sb.toString();
    }
    
    /**
     * Detailed visualization showing layer-by-layer structure
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(toString()).append("\n\n");
        
        for (int i = 0; i < layers.length; i++) {
            sb.append(String.format("Layer %d:\n", i + 1));
            sb.append(layers[i].toDetailedString()).append("\n");
        }
        
        return sb.toString();
    }
}
