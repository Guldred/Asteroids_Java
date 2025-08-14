package game.ai.dqn;

/**
 * A Layer - collection of Perceptrons working in parallel.
 * 
 * Structure:
 * input[] → [Perceptron1, Perceptron2, ..., PerceptronN] → output[]
 * 
 * Each perceptron in the layer:
 * - Receives the SAME input vector
 * - Produces ONE output value
 * - Layer combines all outputs into output vector
 * 
 * Example: Input layer [x1, x2, x3] → Hidden layer [h1, h2] → Output layer [y1]
 */
public class Layer {
    private final Perceptron[] perceptrons;
    private final int inputSize;
    private final int outputSize;
    
    // For backpropagation
    private float[] lastInputs;
    private float[] lastOutputs;
    
    public Layer(int inputSize, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.perceptrons = new Perceptron[outputSize];
        this.lastInputs = new float[inputSize];
        this.lastOutputs = new float[outputSize];
        
        // Create perceptrons - each takes same input size
        for (int i = 0; i < outputSize; i++) {
            perceptrons[i] = new Perceptron(inputSize);
        }
    }
    
    /**
     * Forward pass: run inputs through all perceptrons in parallel
     * 
     * Input:  [x1, x2, x3]
     * Output: [perceptron1(inputs), perceptron2(inputs), perceptron3(inputs)]
     */
    public float[] forward(float[] inputs) {
        if (inputs.length != inputSize) {
            throw new IllegalArgumentException("Expected " + inputSize + " inputs, got " + inputs.length);
        }
        
        // Store inputs for backpropagation
        System.arraycopy(inputs, 0, lastInputs, 0, inputSize);
        
        // Run each perceptron with the same inputs
        for (int i = 0; i < outputSize; i++) {
            lastOutputs[i] = perceptrons[i].forward(inputs);
        }
        
        return lastOutputs.clone();
    }
    
    /**
     * Backward pass: backpropagate error through each perceptron
     * 
     * @param outputGradients - gradient for each perceptron's output
     * @param learningRate - how fast to learn
     * @return inputGradients - gradients to pass to previous layer
     */
    public float[] backward(float[] outputGradients, float learningRate) {
        if (outputGradients.length != outputSize) {
            throw new IllegalArgumentException("Expected " + outputSize + " gradients, got " + outputGradients.length);
        }
        
        // Accumulate input gradients from all perceptrons
        float[] totalInputGradients = new float[inputSize];
        
        // Backpropagate through each perceptron
        for (int i = 0; i < outputSize; i++) {
            // Update this perceptron's weights
            perceptrons[i].backward(outputGradients[i], learningRate);
            
            // Get gradients to pass back to inputs
            float[] inputGrads = perceptrons[i].getInputGradients();
            
            // Accumulate gradients (multiple perceptrons contribute to each input)
            for (int j = 0; j < inputSize; j++) {
                totalInputGradients[j] += inputGrads[j];
            }
        }
        
        return totalInputGradients;
    }
    
    // Getters for visualization and debugging
    public int getInputSize() { return inputSize; }
    public int getOutputSize() { return outputSize; }
    public Perceptron[] getPerceptrons() { return perceptrons; }
    public float[] getLastInputs() { return lastInputs.clone(); }
    public float[] getLastOutputs() { return lastOutputs.clone(); }
    
    /**
     * Get all weights as flat array (for genetic algorithm compatibility)
     */
    public float[] getAllWeights() {
        // Count total parameters: each perceptron has (inputSize + 1) parameters
        int totalParams = outputSize * (inputSize + 1);
        float[] weights = new float[totalParams];
        
        int idx = 0;
        for (int i = 0; i < outputSize; i++) {
            Perceptron p = perceptrons[i];
            
            // Copy weights
            float[] perceptronWeights = p.getWeights();
            System.arraycopy(perceptronWeights, 0, weights, idx, inputSize);
            idx += inputSize;
            
            // Copy bias
            weights[idx++] = p.getBias();
        }
        
        return weights;
    }
    
    /**
     * Set all weights from flat array (for genetic algorithm compatibility)
     */
    public void setAllWeights(float[] weights) {
        int expectedParams = outputSize * (inputSize + 1);
        if (weights.length != expectedParams) {
            throw new IllegalArgumentException("Expected " + expectedParams + " weights, got " + weights.length);
        }
        
        int idx = 0;
        for (int i = 0; i < outputSize; i++) {
            // Extract weights for this perceptron
            float[] perceptronWeights = new float[inputSize];
            System.arraycopy(weights, idx, perceptronWeights, 0, inputSize);
            idx += inputSize;
            
            // Extract bias
            float bias = weights[idx++];
            
            // Set perceptron parameters
            perceptrons[i].setWeights(perceptronWeights, bias);
        }
    }
    
    @Override
    public String toString() {
        return String.format("Layer[%d→%d] with %d perceptrons", 
                           inputSize, outputSize, perceptrons.length);
    }
    
    /**
     * Detailed string showing each perceptron
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Layer[%d→%d]:\n", inputSize, outputSize));
        for (int i = 0; i < perceptrons.length; i++) {
            sb.append(String.format("  Perceptron[%d]: %s\n", i, perceptrons[i]));
        }
        return sb.toString();
    }
}
