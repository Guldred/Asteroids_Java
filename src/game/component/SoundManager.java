package game.component;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static SoundManager instance;
    private Map<String, Clip> soundClips;
    private Map<String, String> soundPaths; // supports mp3 via afplay
    private Map<String, String> musicPaths;
    private boolean soundEnabled = false; // disabled by default for now
    private Process musicProcess = null;
    private String currentMusicKey = null;
    
    private SoundManager() {
        soundClips = new HashMap<>();
        soundPaths = new HashMap<>();
        musicPaths = new HashMap<>();
        loadSounds();
    }
    
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    
    private void loadSounds() {
        // New resource layout uses mp3 under src/game/resource/audio
        // Sound effects
        registerSound("laser", "src/game/resource/audio/sounds/laser.mp3");
        registerSound("shoot", "src/game/resource/audio/sounds/laser.mp3"); // alias for existing calls
        registerSound("explosion", "src/game/resource/audio/sounds/explosion.mp3");
        // Map other events to closest available sounds as placeholders
        registerSound("hit", "src/game/resource/audio/sounds/explosion.mp3");
        registerSound("powerup", "src/game/resource/audio/sounds/laser.mp3");
        registerSound("levelup", "src/game/resource/audio/sounds/laser.mp3");
        registerSound("gameover", "src/game/resource/audio/sounds/explosion.mp3");

        // Music
        registerMusic("title", "src/game/resource/audio/music/TitleMusic.mp3");
    }
    
    private void registerSound(String name, String path) {
        File soundFile = new File(path);
        if (!soundFile.exists()) {
            System.out.println("Sound file not found: " + path);
            return;
        }
        soundPaths.put(name, path);

        // Preload clip only for non-mp3 (e.g., wav) to use Java Sound directly
        if (!path.toLowerCase().endsWith(".mp3")) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                soundClips.put(name, clip);
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.err.println("Error loading sound: " + name + " - " + e.getMessage());
            }
        }
    }

    private void registerMusic(String name, String path) {
        File musicFile = new File(path);
        if (!musicFile.exists()) {
            System.out.println("Music file not found: " + path);
            return;
        }
        musicPaths.put(name, path);
    }
    
    public void playSound(String name) {
        if (!soundEnabled) return;
        String path = soundPaths.get(name);
        if (path == null) return;

        if (path.toLowerCase().endsWith(".mp3")) {
            playMp3Async(path, 0.3f);
            return;
        }

        Clip clip = soundClips.get(name);
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    public void playSoundWithVolume(String name, float volume) {
        if (!soundEnabled) return;
        String path = soundPaths.get(name);
        if (path == null) return;

        if (path.toLowerCase().endsWith(".mp3")) {
            playMp3Async(path, Math.max(0.0f, Math.min(1.0f, volume)));
            return;
        }

        Clip clip = soundClips.get(name);
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            // Set volume for Java Sound clip
            try {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log(Math.max(0.0001f, volume)) / Math.log(10.0) * 20.0);
                gainControl.setValue(dB);
            } catch (IllegalArgumentException ignored) {}
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    public void toggleSound() {
        setSoundEnabled(!soundEnabled);
    }
    
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
        if (!enabled) {
            // Immediately stop any currently playing audio
            try { stopMusic(); } catch (Exception ignored) {}
            try { stopAllSounds(); } catch (Exception ignored) {}
        }
    }
    
    public void stopAllSounds() {
        for (Clip clip : soundClips.values()) {
            if (clip.isRunning()) {
                clip.stop();
            }
        }
    }

    // --- Music control (uses macOS afplay for mp3) ---
    public void playMusic(String key, boolean loop, float volume) {
        if (!soundEnabled) return;
        String path = musicPaths.get(key);
        if (path == null) return;

        stopMusic();
        currentMusicKey = key;

        Thread t = new Thread(() -> {
            do {
                ProcessBuilder pb = new ProcessBuilder("afplay", "-v", String.valueOf(Math.max(0.0f, Math.min(1.0f, volume))), path);
                pb.redirectErrorStream(true);
                try {
                    musicProcess = pb.start();
                    // Wait for song to finish
                    musicProcess.waitFor();
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error playing music: " + e.getMessage());
                    break;
                }
            } while (loop && soundEnabled);
        }, "MusicPlayerThread");
        t.setDaemon(true);
        t.start();
    }

    public void stopMusic() {
        if (musicProcess != null) {
            musicProcess.destroy();
            musicProcess = null;
        }
        currentMusicKey = null;
    }

    private void playMp3Async(String path, float volume) {
        // macOS-specific playback using afplay for simplicity and zero deps
        new Thread(() -> {
            try {
                new ProcessBuilder("afplay", "-v", String.valueOf(Math.max(0.0f, Math.min(1.0f, volume))), path)
                        .redirectErrorStream(true)
                        .start();
            } catch (IOException e) {
                System.err.println("Error playing mp3: " + e.getMessage());
            }
        }, "SFXPlayerThread").start();
    }
}

