package com.littleDevil.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
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

    // Mouse tracking
    private final Vector2 mouseWorldPos = new Vector2();
    private final Vector2 mouseDir = new Vector2();
    public static float mouseAngle = 0f;

    // Game world
    private GameWorld gameWorld;

    private Texture darknessTexture;
    private float darknessAlpha = 0.45f;

    private Music backgroundMusic;

    // Camera shake / pause
    private float cameraShakeTimer = 0f;
    private final float CAMERA_SHAKE_DURATION = 0.15f; // seconds
    private final float CAMERA_SHAKE_INTENSITY = 1f;

    // Time
    private final float TIME_PAUSE_DURATION = 0.1f; // seconds
    private float timePauseTimer = 0f;
    private boolean timePaused = false;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        pixel = new Texture("whitePixel.png");
        playerUI = new Texture("playerUITest.png");

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("Sounds/gameBackgroundMusic.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.1f);
        backgroundMusic.play();

        // --- Initialize GameWorld ---
        gameWorld = new GameWorld(600, 400, 4);
        gameWorld.initialize();

        // --- Darkness ---
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0f, 0f, 0f, 1f); // fully black pixel
        pix.fill();
        darknessTexture = new Texture(pix);
        pix.dispose();

        // --- World camera + viewport ---
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(gameWorld.mapWidth / 2f, gameWorld.mapHeight / 2f, camera);
        viewport.apply();
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        camera.update();

        // --- UI camera + viewport ---
        UICamera = new OrthographicCamera();
        UIViewport = new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), UICamera);
        UIViewport.apply();
        UICamera.position.set(UIViewport.getWorldWidth() / 2f, UIViewport.getWorldHeight() / 2f, 0);
        UICamera.update();
    }

    @Override
    public void render(float delta) {
        if (timePaused) delta = 0f; // freeze world
        float worldDelta = timePaused ? 0f : delta;
        float cameraDelta = delta; // camera still moves even if world is frozen

        // Update world
        gameWorld.update(worldDelta, this);
        updateCamera(cameraDelta);
        updateMouse();

        // --- Draw world ---
        ScreenUtils.clear(Color.BLACK);
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        gameWorld.render(batch);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, darknessAlpha);
        batch.draw(darknessTexture, 0, 0, gameWorld.mapWidth, gameWorld.mapHeight);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE); // additive blending

        for (Light light : LightData.lightObjects) {
            float drawX = light.x - light.width / 2f;
            float drawY = light.y - light.height / 2f;
            batch.setColor(1f, 1f, 1f, light.alpha);
            batch.draw(light.texture, drawX, drawY, light.width, light.height);
        }

        batch.setColor(1f,1f,1f,1f); // reset color
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); // reset blending

        batch.end();

        // --- Draw UI ---
        batch.setProjectionMatrix(UICamera.combined);
        batch.begin();
        drawPlayerUI();
        batch.end();
    }

    private void updateCamera(float delta) {
        Player player = gameWorld.player;
        if (player == null) return;

        // Only move if not paused
        float lerp = 5f * delta;
        float targetX = player.x;
        float targetY = player.y;

        camera.position.x += (targetX - camera.position.x) * lerp;
        camera.position.y += (targetY - camera.position.y) * lerp;

        // Apply shake
        if (cameraShakeTimer > 0f) {
            camera.position.x += (float)(Math.random() - 0.5f) * CAMERA_SHAKE_INTENSITY;
            camera.position.y += (float)(Math.random() - 0.5f) * CAMERA_SHAKE_INTENSITY;
            cameraShakeTimer -= delta;
        }

        // Clamp inside map
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
        camera.unproject(mouseScreen); // unproject after camera updated
        mouseWorldPos.set(mouseScreen.x, mouseScreen.y);

        Player player = gameWorld.player;
        if (player != null) {
            mouseDir.set(mouseWorldPos).sub(player.x, player.y).nor();
            mouseAngle = mouseDir.angleDeg();
        }
    }


    private void drawPlayerUI() {
        float referenceWidth = 1920f;
        float referenceHeight = 1080f;

        float baseWidth = 112f;
        float baseHeight = 32f;

        // Scale relative to smaller dimension
        float scale = Math.min(
            Gdx.graphics.getWidth() / referenceWidth,
            Gdx.graphics.getHeight() / referenceHeight
        );

        // Cap the scale so it never becomes gigantic
        scale = Math.min(scale * 8f, 4f); // <-- tweak 6f to whatever feels right

        float uiWidth = baseWidth * scale;
        float uiHeight = baseHeight * scale;

        // Center horizontally, near bottom
        float uiX = (UIViewport.getWorldWidth() - uiWidth) / 2f;
        float uiY = 0 - uiHeight * 0.5f;

        batch.draw(playerUI, uiX, uiY, uiWidth, uiHeight);
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
    }

    public static float getMouseAngle() {
        return mouseAngle;
    }

    public void triggerTimePause() {
        if (!timePaused) {
            timePaused = true;
            timePauseTimer = TIME_PAUSE_DURATION;
            cameraShakeTimer = CAMERA_SHAKE_DURATION; // keep shake too
        }
    }
}
