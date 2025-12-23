package com.littleDevil.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.*;

public class GameScreen implements Screen {
    private final Main game;

    public UIManager uiManager;

    // Textures
    private Texture pixel;
    private Texture playerUI;
    private Texture wavePanel;
    private Texture barsBackground;
    private Texture healthBar;
    private Texture energyBar;
    private Texture xpBar;
    private Texture darknessTexture;

    // Cameras and viewports
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private ExtendViewport viewport;
    private OrthographicCamera UICamera;
    private ExtendViewport UIViewport;

    // Fonts and layouts
    private BitmapFont scoreFont, waveFont, comboFont, levelFont, skillFont;
    private GlyphLayout scoreLayout, waveLayout, comboLayout, levelLayout;

    // Game world and UI
    private GameWorld gameWorld;

    // Mouse tracking
    private final Vector2 mouseWorldPos = new Vector2();
    private final Vector2 mouseDir = new Vector2();
    public static float mouseAngle = 0f;

    // Music
    private Music backgroundMusic;

    // Effects
    private float cameraShakeTimer = 0f;
    private float darknessAlpha = 0.55f;
    private static final float CAMERA_SHAKE_INTENSITY = 1f;

    // Time pause
    private float timePauseTimer = 0f;
    private boolean timePaused = false;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // Load assets
        pixel = new Texture("whitePixel.png");
        playerUI = new Texture("GameUI/playerUI.png");
        wavePanel = new Texture("GameUI/wavePanel.png");
        barsBackground = new Texture("GameUI/barsBackground.png");
        healthBar = new Texture("GameUI/healthBar.png");
        energyBar = new Texture("GameUI/energyBar.png");
        xpBar = new Texture("GameUI/xpBar.png");

        // Fonts
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("pixelon.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();

        param.size = (int)(Gdx.graphics.getHeight() * 0.06f);
        param.color = new Color(0.85f, 0.85f, 0.85f, 0.5f);
        scoreFont = generator.generateFont(param);

        param.size = (int)(Gdx.graphics.getHeight() * 0.05f);
        param.color = new Color(0f, 0f, 0f, 1f);
        param.borderWidth = 0.5f;
        waveFont = generator.generateFont(param);

        param.color = new Color(1f, 1f, 1f, 0.4f);
        skillFont = generator.generateFont(param);

        param.color = new Color(1f, 0.1f, 0f, 1f);
        comboFont = generator.generateFont(param);

        param.color = Color.SKY;
        param.borderWidth = 0f;
        levelFont = generator.generateFont(param);

        generator.dispose();

        scoreLayout = new GlyphLayout();
        waveLayout = new GlyphLayout();
        comboLayout = new GlyphLayout();
        levelLayout = new GlyphLayout();

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("Sounds/gameBackgroundMusic.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.1f);
        backgroundMusic.play();

        gameWorld = new GameWorld(600, 400, 4, this);
        gameWorld.initialize();

        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0f, 0f, 0f, 1f);
        pix.fill();
        darknessTexture = new Texture(pix);
        pix.dispose();

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(gameWorld.mapWidth / 2f, gameWorld.mapHeight / 2f, camera);
        viewport.apply();
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        camera.update();

        UICamera = new OrthographicCamera();
        UIViewport = new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), UICamera);
        UIViewport.apply();
        UICamera.position.set(UIViewport.getWorldWidth() / 2f, UIViewport.getWorldHeight() / 2f, 0);
        UICamera.update();

        uiManager = new UIManager(
            gameWorld,
            gameWorld.player,
            batch,
            UICamera,
            UIViewport,
            scoreFont,
            waveFont,
            comboFont,
            levelFont,
            skillFont,
            playerUI,
            wavePanel,
            barsBackground,
            healthBar,
            energyBar,
            xpBar
        );
    }

    @Override
    public void render(float delta) {
        if (timePaused) delta = 0f;
        float worldDelta = timePaused ? 0f : delta;

        gameWorld.update(worldDelta, this);
        updateCamera(delta);
        updateMouse();

        ScreenUtils.clear(Color.BLACK);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        gameWorld.render(batch);
        renderLighting();
        batch.end();

        batch.setProjectionMatrix(UICamera.combined);
        uiManager.render();
    }

    private void renderLighting() {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, darknessAlpha);
        batch.draw(darknessTexture, 0, 0, gameWorld.mapWidth, gameWorld.mapHeight);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (Light light : LightData.lightObjects) {
            float drawX = light.x - light.width / 2f;
            float drawY = light.y - light.height / 2f;
            batch.setColor(1f, 1f, 1f, light.alpha);
            batch.draw(light.texture, drawX, drawY, light.width, light.height);
        }
        batch.setColor(1f,1f,1f,1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void updateCamera(float delta) {
        Player player = gameWorld.player;
        if (player == null) return;
        float lerp = 3f * delta;
        camera.position.lerp(new Vector3(player.x, player.y, 0), lerp);
        if (cameraShakeTimer > 0f) {
            camera.position.x += MathUtils.random(-0.5f, 0.5f) * CAMERA_SHAKE_INTENSITY;
            camera.position.y += MathUtils.random(-0.5f, 0.5f) * CAMERA_SHAKE_INTENSITY;
            cameraShakeTimer -= delta;
        }
        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;
        camera.position.x = MathUtils.clamp(camera.position.x, halfW, gameWorld.mapWidth - halfW);
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, gameWorld.mapHeight - halfH);

        if (timePaused) {
            timePauseTimer -= Gdx.graphics.getDeltaTime();
            if (timePauseTimer <= 0f) timePaused = false;
        }

        camera.update();
    }

    private void updateMouse() {
        Vector3 mouseScreen = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouseScreen);
        mouseWorldPos.set(mouseScreen.x, mouseScreen.y);

        Player player = gameWorld.player;
        if (player != null) {
            mouseDir.set(mouseWorldPos).sub(player.x, player.y).nor();
            mouseAngle = mouseDir.angleDeg();
        }
    }

    public static float getMouseAngle() {
        return mouseAngle;
    }

    public void triggerTimePause(float freezeDuration, float cameraShakeDuration) {
        if (!timePaused) {
            timePaused = true;
            timePauseTimer = freezeDuration;
            cameraShakeTimer = cameraShakeDuration;
        }
    }

    @Override
    public void resize(int width, int height) {
        // World viewport
        viewport.update(width, height);

        // UI viewport
        UIViewport.update(width, height, true);
        UICamera.position.set(UIViewport.getWorldWidth() / 2f, UIViewport.getWorldHeight() / 2f, 0);
        UICamera.update();

        // Forward resize to UI manager
        uiManager.resize(width, height);
    }


    @Override public void pause() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) backgroundMusic.pause();
    }
    @Override public void resume() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) backgroundMusic.play();
    }
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        pixel.dispose();
        playerUI.dispose();
        gameWorld.dispose();
        scoreFont.dispose();
        waveFont.dispose();
        comboFont.dispose();
        barsBackground.dispose();
        healthBar.dispose();
        energyBar.dispose();
        xpBar.dispose();
        levelFont.dispose();
    }
}
