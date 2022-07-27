package game.component;

public abstract class Updateable {
    private int fps;
    private int targetFrameTime;

    protected boolean start;

    public Updateable() {
        this.start = true;
        this.setFps(60);
    }

    protected void setFps(int fps) {
        this.fps = fps;
        this.targetFrameTime = 1000000000 / fps;
    }

    protected void startUpdate() {
        new Thread(() -> {
            long frameStartTime = System.nanoTime();
            long frameRenderTime = 0;
            float deltaTime = 0;
            while (this.start) {
                deltaTime = (System.nanoTime() - frameStartTime) / 10000000f;
                frameStartTime = System.nanoTime();

                this.onUpdate(deltaTime);

                frameRenderTime = System.nanoTime() - frameStartTime;
                if (frameRenderTime < this.targetFrameTime) {
                    sleep((this.targetFrameTime - frameRenderTime) / 1000000);
                }

            }
        }).start();
    }

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }

    protected void onUpdate(float deltaTime) {

    }
}
