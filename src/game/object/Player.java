package game.object;

import game.component.*;
import game.object.projectiles.*;
import game.ai.AgentInput;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class Player extends Updateable {
    public static final double PLAYER_DIMENSIONS = 64;
    private final float MAX_SPEED = 1;
    private Vector2 velocity = new Vector2(0, 0);
    private float playerViewAngle = 0f;
    private boolean start = true;
    private Vector2 position;
    private final Image playerImage;
    private float wallBounceFactor;
    PlayerInput playerInput;
    JFrame window;
    
    // Agent control
    private boolean agentControl = false;
    private volatile AgentInput agentInput;
    private boolean updateStarted = false;
    
    // Health system
    private int maxHealth = 100;
    private int health = maxHealth;
    private boolean invulnerable = false;
    private long invulnerabilityTime = 0;
    private final long INVULNERABILITY_DURATION = 1500; // 1.5 seconds of invulnerability after being hit
    
    // Power-up system
    private boolean shieldActive = false;
    private long shieldEndTime = 0;
    private final long SHIELD_DURATION = 10000; // 10 seconds
    
    private boolean rapidFireActive = false;
    private long rapidFireEndTime = 0;
    private final long RAPID_FIRE_DURATION = 8000; // 8 seconds
    
    private boolean tripleShotActive = false;
    private long tripleShotEndTime = 0;
    private final long TRIPLE_SHOT_DURATION = 12000; // 12 seconds
    
    // Visual effects for power-ups
    private Color shieldColor = new Color(0, 100, 255, 100);



    public Player(JFrame window) {
        super();
        this.window = window;
        this.playerImage = new ImageIcon("src/game/resource/img/spaceship_brown_default_turned.png").getImage();
        this.playerInput = new PlayerInput();
        this.wallBounceFactor = 0.6f;
        // startUpdate will be called after initial position is set by GameCore
    }

    private void applyAgentControls(float deltaTime) {
        AgentInput ai = this.agentInput;
        if (ai != null) {
            // Rotation via angle delta (degrees per tick)
            this.playerViewAngle += ai.angleDelta;
            if (this.playerViewAngle < 0) this.playerViewAngle += 360f;
            if (this.playerViewAngle >= 360f) this.playerViewAngle -= 360f;

            // Movement mapped to existing accelerate directions
            if (ai.thrustForward) {
                accelerate(0, deltaTime);
            }
            if (ai.strafeRight) {
                accelerate(60, deltaTime);
            }
            if (ai.thrustBack) {
                accelerate(180, deltaTime);
            }
            if (ai.strafeLeft) {
                accelerate(300, deltaTime);
            }
        }
    }

    public void setAgentControl(boolean enabled) {
        this.agentControl = enabled;
    }

    public void setAgentInput(AgentInput input) {
        this.agentInput = input;
    }

    // Call after initial position is set
    public void begin() {
        if (!updateStarted) {
            updateStarted = true;
            super.startUpdate();
        }
    }

    // Stop the internal update loop (used by headless trainer cleanup)
    public void stopUpdates() {
        this.updateStarted = false;
        this.start = false;
    }


    @Override
    public void onUpdate(float deltaTime) {
        if (agentControl) {
            applyAgentControls(deltaTime);
        } else {
            this.getInput(deltaTime);
            this.turnToCursor();
        }
        this.updatePos(deltaTime);
        
        // Update invulnerability
        if (invulnerable && System.currentTimeMillis() - invulnerabilityTime > INVULNERABILITY_DURATION) {
            invulnerable = false;
        }
        
        // Update power-up timers
        updatePowerUpTimers();
    }
    
    private void updatePowerUpTimers() {
        long currentTime = System.currentTimeMillis();
        
        // Check shield timer
        if (shieldActive && currentTime > shieldEndTime) {
            shieldActive = false;
        }
        
        // Check rapid fire timer
        if (rapidFireActive && currentTime > rapidFireEndTime) {
            rapidFireActive = false;
        }
        
        // Check triple shot timer
        if (tripleShotActive && currentTime > tripleShotEndTime) {
            tripleShotActive = false;
        }
    }



    private void getInput(float deltaTime) {
        if (playerInput.isKey_up()) {
            accelerate(0, deltaTime);
        }
        if (playerInput.isKey_right()) {
            accelerate(60, deltaTime);
        }
        if (playerInput.isKey_down()) {
            accelerate(180, deltaTime);
        }
        if (playerInput.isKey_left()) {
            accelerate(300, deltaTime);
        }
    }

    public void setInputToMap(InputEventTypes eventType, int keyCode) {
        playerInput.keyPressUpdate(eventType, keyCode);
    }

    private void sleep(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted: " + e.getMessage());
        }
    }

    public void draw(Graphics2D g2) {
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(position.x, position.y);
        
        // Flash the player if invulnerable
        if (invulnerable && System.currentTimeMillis() % 300 < 150) {
            // Skip drawing to create flashing effect
            g2.setTransform(oldTransform);
            return;
        }
        
        // Draw shield if active
        if (shieldActive) {
            g2.setColor(shieldColor);
            int shieldSize = (int)(PLAYER_DIMENSIONS * 1.3);
            int offset = (int)((shieldSize - PLAYER_DIMENSIONS) / 2);
            g2.fillOval(-offset, -offset, shieldSize, shieldSize);
        }
        
        AffineTransform t = new AffineTransform();
        t.rotate(Math.toRadians(playerViewAngle), PLAYER_DIMENSIONS / 2, PLAYER_DIMENSIONS / 2);
        g2.drawImage(playerImage, t, null);
        
        // Draw power-up indicators
        drawPowerUpIndicators(g2);
        
        g2.setTransform(oldTransform);
    }
    
    private void drawPowerUpIndicators(Graphics2D g2) {
        int indicatorSize = 8;
        int spacing = 4;
        int y = (int)PLAYER_DIMENSIONS - indicatorSize - 2;
        int x = 2;
        
        // Rapid fire indicator
        if (rapidFireActive) {
            g2.setColor(Color.YELLOW);
            g2.fillRect(x, y, indicatorSize, indicatorSize);
            x += indicatorSize + spacing;
        }
        
        // Triple shot indicator
        if (tripleShotActive) {
            g2.setColor(Color.MAGENTA);
            g2.fillRect(x, y, indicatorSize, indicatorSize);
        }
    }

    public Vector2 getPos() {
        return position;
    }
    
    public Vector2 getPosition() {
        return position;
    }
    
    public Vector2 getVelocity() {
        return velocity;
    }

    public Vector2 getCenter() {
        return new Vector2( (float) (position.x + PLAYER_DIMENSIONS / 2), (float)(position.y + PLAYER_DIMENSIONS / 2));
    }

    public float getAngle() {
        return playerViewAngle;
    }

    public void setPosition(Vector2 pos) {
        this.position = pos;
    }

    public void setAngle(float playerViewAngle) {
        this.playerViewAngle = playerViewAngle;
    }

    public void turnToCursor() {
        if (window == null || !window.isShowing()) {
            return;
        }
        setAngle(calcAngleFromPoints(getCenter(), playerInput.getMousePositionInGame(window)));
    }

    public float calcAngleFromPoints(Vector2 player, Vector2 target) {
        float angle = (float) Math.toDegrees(Math.atan2(target.y - player.y, target.x - player.x));

        if(angle < 0){
            angle += 360;
        }
        return angle;
    }

    public void accelerate(float inputDirection, float deltaTime) {
        float force = deltaTime/10 * 1;
        velocity.addForce(playerViewAngle + inputDirection, force);
    }

    public void updatePos(float deltaTime) {
        if (position == null) return;
        checkOutOfBounds();
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;
    }

    public Projectile shoot(int weapon) {
        if (tripleShotActive) {
            return new TripleShot(getCenter(), velocity, this.playerViewAngle, rapidFireActive ? 15f : 10f);
        } else {
            return new EnergyBall(getCenter(), velocity, this.playerViewAngle, rapidFireActive ? 15f : 10f);
        }
    }
    
    public Rectangle2D getCollisionBounds() {
        return new Rectangle2D.Float(
            position.x,
            position.y,
            (float) PLAYER_DIMENSIONS,
            (float) PLAYER_DIMENSIONS
        );
    }

    public void checkOutOfBounds() {
        if (position == null) return;
        if (position.x < 0) {
            velocity.x = Math.abs(velocity.x) * wallBounceFactor;
        } else if (position.x + PLAYER_DIMENSIONS > GameCore.screenSize.x) {
            velocity.x = Math.abs(velocity.x) * -1 * wallBounceFactor;
        } else if (position.y < 0) {
            velocity.y = Math.abs(velocity.y) * wallBounceFactor;
        } else if (position.y + PLAYER_DIMENSIONS + 30 > GameCore.screenSize.y) {
            velocity.y = Math.abs(velocity.y) * -1 * wallBounceFactor;
        }
    }
    
    public int getHealth() {
        return health;
    }
    
    public void takeDamage(int damage) {
        if (invulnerable) return;
        
        // Shield absorbs damage
        if (shieldActive) {
            // Shield reduces damage by 50%
            damage = damage / 2;
        }
        
        health -= damage;
        if (health < 0) health = 0;
        
        // Make player invulnerable for a short time after being hit
        invulnerable = true;
        invulnerabilityTime = System.currentTimeMillis();
    }
    
    public boolean isAlive() {
        return health > 0;
    }
    
    public void heal(int amount) {
        health += amount;
        if (health > maxHealth) health = maxHealth;
    }
    
    public void reset() {
        health = maxHealth;
        invulnerable = false;
        
        // Reset all power-ups
        shieldActive = false;
        rapidFireActive = false;
        tripleShotActive = false;
    }
    
    public boolean isInvulnerable() {
        return invulnerable;
    }
    
    public void activatePowerUp(PowerUp.PowerUpType type) {
        long currentTime = System.currentTimeMillis();
        
        switch (type) {
            case HEALTH:
                heal(25); // Heal 25 health points
                break;
                
            case SHIELD:
                shieldActive = true;
                shieldEndTime = currentTime + SHIELD_DURATION;
                break;
                
            case RAPID_FIRE:
                rapidFireActive = true;
                rapidFireEndTime = currentTime + RAPID_FIRE_DURATION;
                break;
                
            case TRIPLE_SHOT:
                tripleShotActive = true;
                tripleShotEndTime = currentTime + TRIPLE_SHOT_DURATION;
                break;
        }
    }
    
    public boolean hasActiveShield() {
        return shieldActive;
    }
    
    public boolean hasRapidFire() {
        return rapidFireActive;
    }
    
    public boolean hasTripleShot() {
        return tripleShotActive;
    }
}
