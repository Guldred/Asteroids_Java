package game.component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.geom.AffineTransform;

public class BackgroundManager {
    private List<ParallaxLayer> layers;
    private int width;
    private int height;
    private Random random;
    
    // Background themes
    public enum BackgroundTheme {
        BLUE_NEBULA,
        GREEN_NEBULA,
        PURPLE_NEBULA,
        STARFIELD
    }
    
    private BackgroundTheme currentTheme;
    
    public BackgroundManager(int width, int height) {
        this.width = width;
        this.height = height;
        this.layers = new ArrayList<>();
        this.random = new Random();
        
        // Default to blue nebula
        setTheme(BackgroundTheme.BLUE_NEBULA);
    }
    
    public void setTheme(BackgroundTheme theme) {
        this.currentTheme = theme;
        loadTheme();
    }
    
    private void loadTheme() {
        // Clear existing layers
        layers.clear();
        
        String basePath = "src/game/resource/img/backgrounds/";
        String themePath;
        
        switch (currentTheme) {
            case BLUE_NEBULA:
                themePath = basePath + "Blue Nebula/";
                break;
            case GREEN_NEBULA:
                themePath = basePath + "Green Nebula/";
                break;
            case PURPLE_NEBULA:
                themePath = basePath + "Purple Nebula/";
                break;
            case STARFIELD:
                themePath = basePath + "Starfields/";
                break;
            default:
                themePath = basePath + "Blue Nebula/";
        }
        
        // Load background images from the theme directory
        File directory = new File(themePath);
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        
        if (files != null && files.length > 0) {
            // Sort files to ensure consistent loading order
            java.util.Arrays.sort(files);
            
            // Load the main background (first file) as the base layer
            try {
                BufferedImage baseImage = ImageIO.read(files[0]);
                layers.add(new ParallaxLayer(baseImage, 0, 0, 0.0f, 0.0f)); // Static background
                
                // Add additional layers with stronger parallax effect for visibility
                if (files.length > 1) {
                    // Second image as slow-moving middle layer
                    BufferedImage midImage = ImageIO.read(files[1]);
                    layers.add(new ParallaxLayer(midImage, 0, 0, 0.35f, 0.20f));
                }
                
                if (files.length > 2) {
                    // Third image as faster-moving foreground layer
                    BufferedImage foreImage = ImageIO.read(files[2]);
                    layers.add(new ParallaxLayer(foreImage, 0, 0, 0.70f, 0.40f));
                }
                
            } catch (IOException e) {
                System.err.println("Error loading background images: " + e.getMessage());
                // Fall back to simple stars if images can't be loaded
                createStarLayers();
            }
        } else {
            System.err.println("No background images found in: " + themePath);
            // Fall back to simple stars if no images are found
            createStarLayers();
        }
    }
    
    private void createStarLayers() {
        // Create a simple starfield as fallback
        BufferedImage starfield = createStarfieldImage(width, height, 100);
        layers.add(new ParallaxLayer(starfield, 0, 0, 0.0f, 0.0f));
        
        BufferedImage distantStars = createStarfieldImage(width, height, 50);
        layers.add(new ParallaxLayer(distantStars, 0, 0, 0.05f, 0.02f));
        
        BufferedImage nearStars = createStarfieldImage(width, height, 30);
        layers.add(new ParallaxLayer(nearStars, 0, 0, 0.1f, 0.05f));
    }
    
    private BufferedImage createStarfieldImage(int width, int height, int starCount) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        
        g2.setColor(new Color(0, 0, 0, 0)); // Transparent background
        g2.fillRect(0, 0, width, height);
        
        for (int i = 0; i < starCount; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int size = random.nextInt(3) + 1;
            int brightness = 150 + random.nextInt(106); // 150-255
            
            g2.setColor(new Color(brightness, brightness, brightness));
            g2.fillRect(x, y, size, size);
        }
        
        g2.dispose();
        return image;
    }
    
    public void update(float playerVelocityX, float playerVelocityY) {
        // Update all layers based on player velocity
        for (ParallaxLayer layer : layers) {
            layer.update(playerVelocityX, playerVelocityY);
        }
    }
    
    public void draw(Graphics2D g2) {
        // Draw all layers from back to front
        for (ParallaxLayer layer : layers) {
            layer.draw(g2, width, height);
        }
    }
    
    public void nextTheme() {
        // Cycle to the next theme
        BackgroundTheme[] themes = BackgroundTheme.values();
        int nextIndex = (currentTheme.ordinal() + 1) % themes.length;
        setTheme(themes[nextIndex]);
    }
    
    // Inner class to handle parallax scrolling for each layer
    private class ParallaxLayer {
        private BufferedImage image;
        private float offsetX;
        private float offsetY;
        private float parallaxFactorX;
        private float parallaxFactorY;
        
        public ParallaxLayer(BufferedImage image, float offsetX, float offsetY, float parallaxFactorX, float parallaxFactorY) {
            this.image = image;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.parallaxFactorX = parallaxFactorX;
            this.parallaxFactorY = parallaxFactorY;
        }
        
        public void update(float playerVelocityX, float playerVelocityY) {
            // Move the layer based on player velocity and parallax factor
            offsetX -= playerVelocityX * parallaxFactorX;
            offsetY -= playerVelocityY * parallaxFactorY;
            
            // Wrap around for seamless scrolling
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();
            
            // Ensure offsets stay within image dimensions for proper tiling
            offsetX = (offsetX % imgWidth + imgWidth) % imgWidth;
            offsetY = (offsetY % imgHeight + imgHeight) % imgHeight;
        }
        
        public void draw(Graphics2D g2, int screenWidth, int screenHeight) {
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();
            
            // Calculate how many tiles we need to cover the screen
            int tilesX = (screenWidth / imgWidth) + 2; // +2 to ensure coverage during scrolling
            int tilesY = (screenHeight / imgHeight) + 2;
            
            // Draw the tiled background with proper offsets
            for (int y = -1; y < tilesY; y++) {
                for (int x = -1; x < tilesX; x++) {
                    float drawX = (float) (x * imgWidth - offsetX);
                    float drawY = (float) (y * imgHeight - offsetY);
                    AffineTransform at = AffineTransform.getTranslateInstance(drawX, drawY);
                    g2.drawImage(image, at, null);
                }
            }
        }
    }
}
