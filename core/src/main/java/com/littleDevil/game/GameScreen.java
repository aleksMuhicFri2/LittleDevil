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

    // White pixel texture
    private Texture pixel;

    // Game Cameras
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private ExtendViewport viewport;

    // UI Cameras
    private OrthographicCamera UICamera;
    private ExtendViewport UIViewport;

    // UI
    private Texture playerUI;
    private Texture wavePanel;

    // Player UI bars
    private Texture barsBackground;
    private Texture healthBar;
    private Texture energyBar;
    private Texture xpBar;

    // Game world
    private GameWorld gameWorld;

    // Mouse tracking
    private final Vector2 mouseWorldPos = new Vector2();
    private final Vector2 mouseDir = new Vector2();
    public static float mouseAngle = 0f;

    // Dark Overlay
    private Texture darknessTexture;
    private float darknessAlpha = 0.55f;

    // Fonts + layouts
    private BitmapFont scoreFont;
    private GlyphLayout scoreLayout;
    private BitmapFont waveFont;
    private GlyphLayout waveLayout;
    private BitmapFont comboFont;
    private GlyphLayout comboLayout;
    private BitmapFont levelFont;
    private GlyphLayout levelLayout;

    // Music
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

        // Load textures
        pixel = new Texture("whitePixel.png");
        playerUI = new Texture("GameUI/playerUI.png");
        wavePanel = new Texture("GameUI/wavePanel.png");
        barsBackground = new Texture("GameUI/barsBackground.png");
        healthBar = new Texture("GameUI/healthBar.png");
        energyBar = new Texture("GameUI/energyBar.png");
        xpBar = new Texture("GameUI/xpBar.png");

        // Font generator setup
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("pixelon.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();

        // Score font
        param.size = (int)(Gdx.graphics.getHeight() * 0.06f);
        param.color = new Color(0.85f, 0.85f, 0.85f, 0.5f);
        scoreFont = generator.generateFont(param);

        // Wave font
        param.size = (int)(Gdx.graphics.getHeight() * 0.05f);
        param.color = new Color(0f, 0f, 0f, 1f);
        param.borderWidth = 0.5f;
        waveFont = generator.generateFont(param);

        // Combo font
        param.size = (int)(Gdx.graphics.getHeight() * 0.05f);
        param.color = new Color(1f, 0.1f, 0f, 1f);
        comboFont = generator.generateFont(param);

        // Level font
        param.size = (int)(Gdx.graphics.getHeight() * 0.05f);
        param.color = Color.SKY;
        param.borderWidth = 0f;
        levelFont = generator.generateFont(param);

        generator.dispose();

        // Define layouts
        scoreLayout = new GlyphLayout();
        waveLayout = new GlyphLayout();
        comboLayout = new GlyphLayout();
        levelLayout = new GlyphLayout();

        // Background music
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("Sounds/gameBackgroundMusic.mp3"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(0.1f);
        backgroundMusic.play();

        // Game world setup
        gameWorld = new GameWorld(600, 400, 4);
        gameWorld.initialize();

        // Darkness overlay
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0f, 0f, 0f, 1f);
        pix.fill();
        darknessTexture = new Texture(pix);
        pix.dispose();

        // Cameras init
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

        gameWorld.update(worldDelta, this);
        updateCamera(delta);
        updateMouse();

        ScreenUtils.clear(Color.BLACK);

        // Game world render
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        gameWorld.render(batch);

        // Darkness overlay
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, darknessAlpha);
        batch.draw(darknessTexture, 0, 0, gameWorld.mapWidth, gameWorld.mapHeight);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // Draw lights
        for (Light light : LightData.lightObjects) {
            float drawX = light.x - light.width / 2f;
            float drawY = light.y - light.height / 2f;
            batch.setColor(1f, 1f, 1f, light.alpha);
            batch.draw(light.texture, drawX, drawY, light.width, light.height);
        }

        batch.setColor(1f,1f,1f,1f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.end();

        // UI render
        batch.setProjectionMatrix(UICamera.combined);
        batch.begin();
        drawPlayerUI();
        drawWavePanelUI();
        drawWaveUI();
        drawScoreUI();
        drawComboUI();
        batch.end();
    }

    private void updateCamera(float delta) {
        Player player = gameWorld.player;
        if (player == null) return;

        float lerp = 3f * delta;
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

    // Balances scale of UI when resizing
    private float getScale(float maxScale) {
        float referenceWidth = 1920f;
        float referenceHeight = 1080f;
        float scale = Math.min(
            Gdx.graphics.getWidth() / referenceWidth,
            Gdx.graphics.getHeight() / referenceHeight
        );
        return Math.min(scale * 8f, maxScale);
    }

    // Draws wave panel
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

    // Draws Player UI
    private void drawPlayerUI() {
        float baseWidth = 112f;
        float baseHeight = 48f;
        float scale = getScale(3.5f);
        float uiWidth = baseWidth * scale;
        float uiHeight = baseHeight * scale;
        float uiX = (UIViewport.getWorldWidth() - uiWidth) / 2f;
        float uiY = -uiHeight * 0.4f;

        Player player = gameWorld.player;
        if (player == null) return;

        float lerpSpeed = 8f * Gdx.graphics.getDeltaTime();

        player.displayHP = MathUtils.lerp(player.displayHP, player.currentHP, lerpSpeed);
        player.displayEnergy = MathUtils.lerp(player.displayEnergy, player.currentEnergy, lerpSpeed);

        if (player.xpOverflowAnimating) {
            player.displayXp = MathUtils.lerp(player.displayXp, player.previousNeededXp, lerpSpeed);
            if (Math.abs(player.displayXp - player.previousNeededXp) < 1f) {
                player.displayXp = 0f;
                player.xpOverflowAnimating = false;
                player.levelUp = false;
            }
        } else {
            player.displayXp = MathUtils.lerp(player.displayXp, player.currentXp, lerpSpeed);
        }

        // Ratios
        float healthRatio = MathUtils.clamp(player.displayHP / player.baseHP, 0f, 1f);
        float energyRatio = MathUtils.clamp(player.displayEnergy / player.baseEnergy, 0f, 1f);
        float xpRatio = MathUtils.clamp(player.displayXp / player.neededXp, 0f, 1f);

        // Draw background
        batch.draw(barsBackground, uiX, uiY, uiWidth, uiHeight);

        // Bar dimensions
        float barScale = scale;
        float fullHealthWidth = 43f * barScale;
        float fullEnergyWidth = 43f * barScale;
        float fullXPWidth = 76f * barScale;
        float healthHeight = 5f * barScale;
        float energyHeight = 5f * barScale;
        float xpHeight = 2f * barScale;

        // Bar positions
        float healthX = uiX + 12f * barScale;
        float healthY = uiY + 23f * barScale;
        float energyX = uiX + 57f * barScale;
        float energyY = healthY;
        float xpX = uiX + 18f * barScale;
        float xpY = uiY + 32f * barScale;

        // Draw level text
        String levelText = "Lvl " + (int)player.level;
        levelLayout.setText(levelFont, levelText);
        float textX = (UIViewport.getWorldWidth() - levelLayout.width) / 2f;
        float textY = uiY + uiHeight - 5f * scale;
        levelFont.draw(batch, levelText, textX, textY);

        // Draw bars
        batch.draw(healthBar, healthX, healthY, fullHealthWidth * healthRatio, healthHeight);
        batch.draw(energyBar, energyX, energyY, fullEnergyWidth * energyRatio, energyHeight);
        batch.draw(xpBar, xpX, xpY, fullXPWidth * xpRatio, xpHeight);

        // Frame over everything
        batch.draw(playerUI, uiX, uiY, uiWidth, uiHeight);
    }

    // Draws Score UI
    private void drawScoreUI() {
        String label = "Score:";
        String value = String.valueOf(gameWorld.getScore());

        scoreLayout.setText(scoreFont, label);
        float labelX = 20f;
        float y = UIViewport.getWorldHeight() - 20f;

        scoreFont.draw(batch, label, labelX, y);
        scoreLayout.setText(scoreFont, value);
        scoreFont.draw(batch, value, labelX + scoreLayout.width + 10f, y);
    }

    // Draws Combo UI
    private void drawComboUI() {
        int combo = gameWorld.getCombo();
        if (combo <= 0) return;

        String label = "Combo:";
        String value = combo + "x";

        comboLayout.setText(comboFont, label);
        float labelX = 20f;
        float y = UIViewport.getWorldHeight() - 60f;

        comboFont.draw(batch, label, labelX, y);
        comboLayout.setText(comboFont, value);
        comboFont.draw(batch, value, labelX + comboLayout.width + 10f, y);
    }

    // Public functions
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
        viewport.update(width, height);
        UIViewport.update(width, height);
        UICamera.position.set(UIViewport.getWorldWidth() / 2f, UIViewport.getWorldHeight() / 2f, 0);
        UICamera.update();
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
