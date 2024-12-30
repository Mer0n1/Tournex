package io.github.capoints;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.capoints.Server.Server;
import io.github.capoints.Server.WaitingWindow;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;

    private Simulator simulator;
    private WaitingWindow waitingWindow;
    private SaveRedactor.TheSave save;

    private FirstMenu firstMenu;
    private Stage stage;

    @Override
    public void create() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        firstMenu = new FirstMenu(stage);

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(Controller.getInstance());
        Gdx.input.setInputProcessor(multiplexer);
        //Gdx.input.setInputProcessor(Controller.getInstance());

        firstMenu.addListenerSinglePlayer(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                firstMenu.setState(false);
                simulator.loadMap(save, save.teams.get(0));
            }
        });

        firstMenu.addListenerMultiPlayer(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                firstMenu.setState(false);
                startServer();
            }
        });

        ///
        simulator = new Simulator();
        try {
            save = SaveRedactor.getInstance().loadSave("map");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        waitingWindow = WaitingWindow.getInstance();

    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        if (firstMenu.isOpen())
            firstMenu.render();
        else {
            if (waitingWindow.isActive())
                waitingWindow.render(batch);
            else {
                simulator.update();
                simulator.render(batch);
            }
        }
    }

    public void startServer() {
        waitingWindow.init(simulator, save);
        waitingWindow.setActive(true);

        Server server = Server.getInstance();
        server.init(save);

        try {
            server.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        waitingWindow.dispose();
    }

    /*public void stop() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Этот код будет выполнен при завершении приложения
            scheduler.shutdown(); // Освобождение ресурсов
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow(); // Принудительная остановка, если не завершились
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow(); // Принудительная остановка при прерывании
            }
        }));

    }*/
}


