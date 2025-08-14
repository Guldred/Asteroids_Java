package game.ai;

import game.component.Vector2;
import game.object.Player;

import javax.swing.*;
import java.awt.*;

public class AIPlayer extends Player {
    
    private QLearningAgent aiAgent;
    private QLearningAgent.AIAction currentAction;
    private boolean aiControlled = true;
    private long lastActionTime = System.currentTimeMillis();
    private final long ACTION_UPDATE_INTERVAL = 100; // Update AI decision every 100ms
    private boolean wantsToFire = false;
    
    public AIPlayer(JFrame window, int screenWidth, int screenHeight) {
        super(window);
        this.aiAgent = new QLearningAgent(screenWidth, screenHeight);
        this.currentAction = new QLearningAgent.AIAction(0, 0, 0, false);
    }
    
    @Override
    public void onUpdate(float deltaTime) {
        // Update AI decision periodically
        long currentTime = System.currentTimeMillis();
        if (aiControlled && currentTime - lastActionTime > ACTION_UPDATE_INTERVAL) {
            currentAction = aiAgent.getAction();
            lastActionTime = currentTime;
        }
        
        // Apply AI input instead of human input if AI controlled
        if (aiControlled) {
            getAIInput(deltaTime);
        } else {
            // Fall back to human input
            super.getInput(deltaTime);
        }
        
        // Continue with normal player update
        this.updatePos(deltaTime);
        this.turnToCursor(); // We'll override this for AI
        
        // Update invulnerability
        if (isInvulnerable() && System.currentTimeMillis() - getInvulnerabilityStartTime() > 1500) {
            setInvulnerable(false);
        }
        
        // Update power-up timers (inline implementation)
        updateAIPowerUpTimers();
    }
    
    private void getAIInput(float deltaTime) {
        if (currentAction == null) return;
        
        // Apply forward/backward thrust
        if (Math.abs(currentAction.forwardThrust) > 0.1) {
            if (currentAction.forwardThrust > 0) {
                accelerate(0, deltaTime * (float)Math.abs(currentAction.forwardThrust)); // Forward
            } else {
                accelerate(180, deltaTime * (float)Math.abs(currentAction.forwardThrust)); // Backward
            }
        }
        
        // Apply side thrust
        if (Math.abs(currentAction.sideThrust) > 0.1) {
            if (currentAction.sideThrust > 0) {
                accelerate(90, deltaTime * (float)Math.abs(currentAction.sideThrust)); // Right
            } else {
                accelerate(270, deltaTime * (float)Math.abs(currentAction.sideThrust)); // Left
            }
        }
        
        // Set turn angle directly
        setPlayerViewAngle((float)currentAction.turnAngle);
        
        // Handle firing - we'll need to communicate this to GameCore
        setWantsToFire(currentAction.fire);
    }
    
    // Override turnToCursor to use AI angle instead
    @Override
    protected void turnToCursor() {
        // AI sets angle directly, so we don't need to track cursor
        if (!aiControlled) {
            super.turnToCursor();
        }
    }
    
    public void setGameEnvironment(java.util.List<game.object.Asteroid> asteroids,
                                  java.util.List<game.object.PowerUp> powerUps,
                                  java.util.List<game.object.projectiles.Projectile> projectiles) {
        if (aiAgent != null) {
            aiAgent.setGameEnvironment(this, asteroids, powerUps, projectiles);
        }
    }
    
    public QLearningAgent getAIAgent() {
        return aiAgent;
    }
    
    public boolean isAIControlled() {
        return aiControlled;
    }
    
    public void setAIControlled(boolean aiControlled) {
        this.aiControlled = aiControlled;
    }
    
    public QLearningAgent.AIAction getCurrentAction() {
        return currentAction;
    }
    
    public void setWantsToFire(boolean fire) {
        this.wantsToFire = fire;
    }
    
    public boolean wantsToFire() {
        return wantsToFire;
    }
    
    @Override
    public void draw(Graphics2D g2) {
        super.draw(g2);
        
        // AI visualization completely disabled per user request
        // The spaceship itself is sufficient visual indicator that AI is controlling
    }
    
    // Episode management for AI learning lifecycle
    public void onEpisodeStart() {
        if (aiAgent != null) {
            // Reset AI decision timing
            lastActionTime = System.currentTimeMillis();
            currentAction = new QLearningAgent.AIAction(0, 0, 0, false);
            // AI agent will handle its own episode start logic
        }
    }
    
    public void onEpisodeEnd() {
        if (aiAgent != null) {
            // AI agent handles episode end logic (reward calculation, experience storage, etc.)
            // This is called from GameCore when player dies
        }
    }
    
    private void updateAIPowerUpTimers() {
        // Simple power-up timer logic using available methods from parent class
        // The parent class handles power-up timers automatically in its update cycle
        // This is just a placeholder for future enhancements
    }
}
