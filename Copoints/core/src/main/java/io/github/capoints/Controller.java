package io.github.capoints;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

import java.util.HashMap;
import java.util.Map;

public class Controller implements InputProcessor {

    private static Controller controller;

    private int mouseX, mouseY;
    private boolean isTouchLEFTDown;
    private boolean isTouchRIGHTDown;
    private boolean isTouchMIDDLEDown;

    private Map<Integer, StandardCallback> callbackMap;
    private StandardCallback callbackLeftDown;

    private Controller() {
        callbackMap = new HashMap<>();
    }

    public static Controller getInstance() {
        if (controller == null)
            controller = new Controller();
        return controller;
    }


    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        if (button == Input.Buttons.LEFT) {
            isTouchLEFTDown = true;
            callbackLeftDown.callback();
        }
        if (button == Input.Buttons.RIGHT)
            isTouchRIGHTDown = true;
        if (button == Input.Buttons.MIDDLE)
            isTouchMIDDLEDown = true;


        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.LEFT)
            isTouchLEFTDown  = false;
        if (button == Input.Buttons.RIGHT)
            isTouchRIGHTDown = false;
        if (button == Input.Buttons.MIDDLE)
            isTouchMIDDLEDown = false;

        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        mouseX = screenX;
        mouseY = Gdx.graphics.getHeight() - screenY;
        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        notifyListener(keycode);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {return false;}

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mouseX = screenX;
        mouseY = Gdx.graphics.getHeight() - screenY;
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    public void addListener(int keycode, StandardCallback callback) {
        callbackMap.put(keycode, callback);
    }

    public void notifyListener(Integer keycode) {
        try {
            callbackMap.get(keycode).callback();
        } catch (Exception e) {}
    }

    public void setCallbackLeftDown(StandardCallback callbackLeftDown) {
        this.callbackLeftDown = callbackLeftDown;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public boolean isTouchLEFTDown() {
        return isTouchLEFTDown;
    }

    public boolean isTouchRIGHTDown() {
        return isTouchRIGHTDown;
    }

    public boolean isTouchMIDDLEDown() {
        return isTouchMIDDLEDown;
    }
}

