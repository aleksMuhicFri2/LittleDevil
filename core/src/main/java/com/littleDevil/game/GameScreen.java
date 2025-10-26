package com.littleDevil.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.*;

public class GameScreen implements Screen {

    private final Main game;

    // Rendering
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private ExtendViewport viewport;

    private OrthographicCamera UICamera;
    private ExtendViewport UIViewport;

    // white pixel texture
    private Texture pixel;

    // UI
    private Texture playerUI;
    private Texture wavePanel;

    // Mouse tracking
    private final Vector2 mouseWorldPos = new Vector2();
    private final Vector2 mouseDir = new Vector2();
    public static float mouseAngle = 0f;

    // Game world
    private GameWorld gameWorld;

    private Texture darknessTexture;
    private float darknessAlpha = 0.55f;

    // Font + layout
    private BitmapFont scoreFont;
    private GlyphLayout scoreLayout;
    private BitmapFont waveFont;
    private GlyphLayout waveLayout;
    private BitmapFont comboFont;
    private GlyphLayout comboLayout;

    private Music backgroundMusic;

    // Camera shake / pause
    private float cameraShakeTimer = 0f;
    private final float CAMERA_SHAKE_INTENSITY = 1f;

    // Time
    private float timePauseTimer = 0f;
    private boolean timePaused = false;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        pixel = new Texture("whitePixel.png");
        playerUI = new Texture("GameUI/playerUI.png");
        wavePanel = new Texture("GameUI/wavePanel.png");

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("pixelon.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();

        // --- Score font ---
        param.size = (int)(Gdx.graphics.getHeight() * 0.06f);
        param.color = new Color(0.85f, 0.85f, 0.85f, 0.5f);
        scoreFont = generator.generateFont(param);

        // --- Wave font ---
        param.size = (int)(Gdx.graphics.getHeight() * 0.05f);
        param.color = new Color(0f, 0f, 0f, 1f);
        param.borderWidth = 0.5f;
        waveFont = generator.generateFont(param);

        // --- Combo font ---
        param.size = (int)(Gdx.graphics.getHeight() * 0.05f);
        param.color = new Color(1f, 0.5f, 0f, 1f); // orange
        comboFont = generator.generateFont(param);

        generator.dispose();

        scoreLayout = new GlyphLayout();
        waveLayout = new GlyphLayout();
        comboLayout = new GlyphLayout();

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("Sounds/gameBackgroundMusic.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.1f);
        backgroundMusic.play();

        gameWorld = new GameWorld(600, 400, 4);
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
    }

    @Override
    public void render(float delta) {
        if (timePaused) delta = 0f;
        float worldDelta = timePaused ? 0f : delta;
        float cameraDelta = delta;

        gameWorld.update(worldDelta, this);
        updateCamera(cameraDelta);
        updateMouse();

        ScreenUtils.clear(Color.BLACK);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        gameWorld.render(batch);

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
        batch.end();

        batch.setProjectionMatrix(UICamera.combined);
        batch.begin();
        drawPlayerUI();
        drawWavePanelUI();
        drawWaveUI();
        drawScoreUI();
        drawComboUI(); // new
        batch.end();
    }

    private void updateCamera(float delta) {
        Player player = gameWorld.player;
        if (player == null) return;

        float lerp = 5f * delta;
        float targetX = player.x;
        float targetY = player.y;

        camera.position.x += (targetX - camera.position.x) * lerp;
        camera.position.y += (targetY - camera.position.y) * lerp;

        if (cameraShakeTimer > 0f) {
            camera.position.x += (float)(Math.random() - 0.5f) * CAMERA_SHAKE_INTENSITY;
            camera.position.y += (float)(Math.random() - 0.5f) * CAMERA_SHAKE_INTENSITY;
            cameraShakeTimer -= delta;
        }

        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;
        camera.position.x = Math.max(halfW, Math.min(gameWorld.mapWidth - halfW, camera.position.x));
        camera.position.y = Math.max(halfH, Math.min(gameWorld.mapHeight - halfH, camera.position.y));

        if (timePaused) {
            timePauseTimer -= Gdx.graphics.getDeltaTime();
            if (timePauseTimer <= 0f) {
                timePaused = false;
            }
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

    private float getScale(float maxScale) {
        float referenceWidth = 1920f;
        float referenceHeight = 1080f;
        float scale = Math.min(
            Gdx.graphics.getWidth() / referenceWidth,
            Gdx.graphics.getHeight() / referenceHeight
        );
        return Math.min(scale * 8f, maxScale);
    }

    private void drawWavePanelUI() {
        float baseWidth = 64f;
        float baseHeight = 64f;
        float scale = getScale(3f);
        float uiWidth = baseWidth * scale;
        float uiHeight = baseHeight * scale;
        float uiX = (UIViewport.getWorldWidth() - uiWidth) / 2f;
        float uiY = UIViewport.getWorldHeight() - (uiHeight / 1.5f);
        batch.draw(wavePanel, uiX, uiY, uiWidth, uiHeight);
    }

    private void drawPlayerUI() {
        float baseWidth = 112f;
        float baseHeight = 32f;
        float scale = getScale(3.5f);
        float uiWidth = baseWidth * scale;
        float uiHeight = baseHeight * scale;
        float uiX = (UIViewport.getWorldWidth() - uiWidth) / 2f;
        float uiY = 0 - uiHeight * 0.5f;
        batch.draw(playerUI, uiX, uiY, uiWidth, uiHeight);
    }

    private void drawScoreUI() {
        String label = "Score:";
        String value = String.valueOf(gameWorld.getScore());

        scoreLayout.setText(scoreFont, label);
        float labelX = 20f;
        float y = UIViewport.getWorldHeight() - 20f;

        scoreFont.draw(batch, label, labelX, y);

        float numberX = labelX + scoreLayout.width + 10f;
        scoreLayout.setText(scoreFont, value);
        scoreFont.draw(batch, value, numberX, y);
    }

    private void drawComboUI() {
        int combo = gameWorld.getCombo();
        if (combo <= 0) return; // don't display zero combos

        String label = "Combo:";
        String value = String.valueOf(combo) + "x";

        comboLayout.setText(comboFont, label);
        float labelX = 20f;
        float y = UIViewport.getWorldHeight() - 60f; // below score

        comboFont.draw(batch, label, labelX, y);

        float numberX = labelX + comboLayout.width + 10f;
        comboLayout.setText(comboFont, value);
        comboFont.draw(batch, value, numberX, y);
    }

    private void drawWaveUI() {
        String label = "Wave";
        String value = String.valueOf(gameWorld.getWave());

        waveLayout.setText(waveFont, label);
        float labelWidth = waveLayout.width;
        waveLayout.setText(waveFont, value);
        float valueWidth = waveLayout.width;

        float totalWidth = labelWidth + 10f + valueWidth;
        float startX = (UIViewport.getWorldWidth() - totalWidth) / 2f;
        float y = UIViewport.getWorldHeight() - 43f;

        waveFont.draw(batch, label, startX, y);
        waveFont.draw(batch, value, startX + labelWidth + 10f, y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        UIViewport.update(width, height);
        UICamera.position.set(UIViewport.getWorldWidth() / 2f, UIViewport.getWorldHeight() / 2f, 0);
        UICamera.update();
    }

    @Override public void pause() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }
    @Override public void resume() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
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
}
