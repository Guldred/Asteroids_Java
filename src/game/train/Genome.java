package game.train;

public class Genome {
    public final float[] params;
    public float fitness;

    public Genome(int size) {
        this.params = new float[size];
        this.fitness = Float.NEGATIVE_INFINITY;
    }

    public Genome copy() {
        Genome g = new Genome(params.length);
        System.arraycopy(this.params, 0, g.params, 0, this.params.length);
        g.fitness = this.fitness;
        return g;
    }
}
