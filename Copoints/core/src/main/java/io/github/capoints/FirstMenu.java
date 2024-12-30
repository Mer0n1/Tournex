package io.github.capoints;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import io.github.capoints.view.ViewObject;

public class FirstMenu {
    //UI
    private Stage stage;
    private Texture singleButtonTexture;
    private Texture multiplayerButtonTexture;

    private ImageButton singleButton;
    private ImageButton multiplayerButton;

    private boolean isOpen;

    public FirstMenu(Stage stage) {
        this.stage = stage;
        isOpen = true;

        //------------------UI--------------------
        singleButtonTexture = new Texture("start_button.png");
        multiplayerButtonTexture = new Texture("multiplayer.png");

        singleButton = new ImageButton(new TextureRegionDrawable(new TextureRegion(singleButtonTexture)));
        multiplayerButton = new ImageButton(new TextureRegionDrawable(new TextureRegion(multiplayerButtonTexture)));

        singleButton.setPosition(Gdx.graphics.getWidth() / 2f - singleButton.getWidth() / 2f, 300);
        multiplayerButton.setPosition(Gdx.graphics.getWidth() / 2f - multiplayerButton.getWidth() / 2f, 200);

        stage.addActor(singleButton);
        stage.addActor(multiplayerButton);

    }

    public void addListenerSinglePlayer(ClickListener clickListener) {
        singleButton.addListener(clickListener);
    }

    public void addListenerMultiPlayer(ClickListener clickListener) {
        multiplayerButton.addListener(clickListener);
    }

    public void render() {
        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    public void setState(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public boolean isOpen() {
        return isOpen;
    }
}
