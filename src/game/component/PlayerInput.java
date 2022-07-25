package game.component;

public class PlayerInput {
    private boolean key_right;
    private boolean key_left;
    private boolean key_up;
    private boolean key_down;
    private boolean key_space;


    public boolean isKey_right() {
        return key_right;
    }

    public void setKey_right(boolean key_right) {
        this.key_right = key_right;
    }

    public boolean isKey_left() {
        return key_left;
    }

    public void setKey_left(boolean key_left) {
        this.key_left = key_left;
    }

    public boolean isKey_up() {
        return key_up;
    }

    public void setKey_up(boolean key_up) {
        this.key_up = key_up;
    }

    public boolean isKey_down() {
        return key_down;
    }

    public void setKey_down(boolean key_down) {
        this.key_down = key_down;
    }

    public boolean isKey_space() {
        return key_space;
    }

    public void setKey_space(boolean key_space) {
        this.key_space = key_space;
    }


}
