package game.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;


public class PlayerInput {

    HashMap<String, Boolean> keys = new HashMap<>();

    public PlayerInput() {
        keys.put("key_right", false);
        keys.put("key_left", false);
        keys.put("key_up", false);
        keys.put("key_down", false);
        keys.put("key_space", false);
        //Add mouse input
        keys.put("mouse_left", false);
        keys.put("mouse_right", false);

    }


    public boolean isKey_right() {
        return keys.get("key_right");
    }

    public void setKey_right(boolean key_right) {
        this.keys.put("key_right", key_right);
    }

    public boolean isKey_left() {
        return keys.get("key_left");
    }

    public void setKey_left(boolean key_left) {
        this.keys.put("key_left", key_left);
    }

    public boolean isKey_up() {
        return keys.get("key_up");
    }

    public void setKey_up(boolean key_up) {
        this.keys.put("key_up", key_up);
    }

    public boolean isKey_down() {
        return keys.get("key_down");
    }

    public void setKey_down(boolean key_down) {
        this.keys.put("key_down", key_down);
    }

    public boolean isKey_space() {
        return keys.get("key_space");
    }

    public void setKey_space(boolean key_space) {
        this.keys.put("key_space", key_space);
    }

    public boolean isMouse_left() { return keys.get("mouse_left"); }
    public void setMouse_left(boolean mouse_left) { this.keys.put("mouse_left", mouse_left); }
    public boolean isMouse_right() { return keys.get("mouse_right"); }
    public void setMouse_right(boolean mouse_right) { this.keys.put("mouse_right", mouse_right); }




    public Vector2 getMousePositionInGame(JFrame window) {
        return new Vector2 (
                MouseInfo.getPointerInfo().getLocation().x - window.getLocationOnScreen().x,
                MouseInfo.getPointerInfo().getLocation().y - window.getLocationOnScreen().y - 30
        ); //-30 accounts for the title bar
    }

    public void keyPressUpdate(InputEventTypes eventType, int keyCode) {
        if (eventType == InputEventTypes.KEY_PRESSED) {
            if (keyCode == KeyEvent.VK_LEFT) {
                setKey_left(true);
            } else if (keyCode == KeyEvent.VK_RIGHT) {
                setKey_right(true);
            } else if (keyCode == KeyEvent.VK_UP) {
                setKey_up(true);
            } else if (keyCode == KeyEvent.VK_DOWN) {
                setKey_down(true);
            } else if (keyCode == KeyEvent.VK_SPACE) {
                setKey_space(true);
            }
        } else if (eventType == InputEventTypes.KEY_RELEASED) {
            if (keyCode == KeyEvent.VK_LEFT) {
                setKey_left(false);
            } else if (keyCode == KeyEvent.VK_RIGHT) {
                setKey_right(false);
            } else if (keyCode == KeyEvent.VK_UP) {
                setKey_up(false);
            } else if (keyCode == KeyEvent.VK_DOWN) {
                setKey_down(false);
            } else if (keyCode == KeyEvent.VK_SPACE) {
                setKey_space(false);
            }
        } else if (eventType == InputEventTypes.MOUSE_PRESSED) {
            System.out.println("Mouse pressed. Button: " + keyCode);
        } else if(eventType == InputEventTypes.MOUSE_RELEASED) {
            System.out.println("Mouse released. Button: " + keyCode);
        }
    }

}
