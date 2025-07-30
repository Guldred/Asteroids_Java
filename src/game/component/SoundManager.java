package game.component;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static SoundManager instance;
    private Map<String, Clip> soundClips;
    private boolean soundEnabled = true;
    
    private SoundManager() {
        soundClips = new HashMap<>();
        loadSounds();
    }
    
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    
    private void loadSounds() {
        // Create sounds directory if it doesn't exist
        File soundsDir = new File("src/game/resource/sounds");
        if (!soundsDir.exists()) {
            soundsDir.mkdirs();
        }
        
        // Load all sound effects
        loadSound("shoot", "src/game/resource/sounds/shoot.wav");
        loadSound("explosion", "src/game/resource/sounds/explosion.wav");
        loadSound("hit", "src/game/resource/sounds/hit.wav");
        loadSound("powerup", "src/game/resource/sounds/powerup.wav");
        loadSound("levelup", "src/game/resource/sounds/levelup.wav");
        loadSound("gameover", "src/game/resource/sounds/gameover.wav");
    }
    
    private void loadSound(String name, String path) {
        try {
            File soundFile = new File(path);
            
            // If the sound file doesn't exist, create a placeholder
            if (!soundFile.exists()) {
                System.out.println("Sound file not found: " + path);
                return;
            }
            
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            soundClips.put(name, clip);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading sound: " + name + " - " + e.getMessage());
        }
    }
    
    public void playSound(String name) {
        if (!soundEnabled) return;
        
        Clip clip = soundClips.get(name);
        if (clip != null) {
            // Stop the clip if it's already playing
            if (clip.isRunning()) {
                clip.stop();
            }
            
            // Reset to beginning and play
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    public void playSoundWithVolume(String name, float volume) {
        if (!soundEnabled) return;
        
        Clip clip = soundClips.get(name);
        if (clip != null) {
            // Stop the clip if it's already playing
            if (clip.isRunning()) {
                clip.stop();
            }
            
            // Set volume
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
            
            // Reset to beginning and play
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    public void toggleSound() {
        soundEnabled = !soundEnabled;
    }
    
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }
    
    public void stopAllSounds() {
        for (Clip clip : soundClips.values()) {
            if (clip.isRunning()) {
                clip.stop();
            }
        }
    }
}
