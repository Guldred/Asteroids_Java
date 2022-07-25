package game.component;

public class DeltaTimer {
    float frameTime = 0;
    float lastFrameTime = 0;
    float deltaTime = 0;

    long frame = 0;

    public float get(long fNum) {
        if (this.frame != fNum) {
            this.frame = fNum;
            this.lastFrameTime = this.frameTime;
            this.frameTime = System.nanoTime();
            this.deltaTime = (this.frameTime - this.lastFrameTime) / 10000000f;
        }
        return this.deltaTime;
    }
}
