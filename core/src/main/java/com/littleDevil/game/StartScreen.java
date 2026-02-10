package com.littleDevil.game;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StartScreen implements Screen {

    private final Main game;
    private SpriteBatch batch;

    // Camera and viewport
    private OrthographicCamera camera;
    private Viewport viewport;
    private final float VIRTUAL_WIDTH = 1920f;
    private final float VIRTUAL_HEIGHT = 1080f;

    // Textures
    private Texture backgroundTexture;
    private Texture titleTexture;
    private Texture playButtonTexture;
    private Texture candleSheet;
    private Texture particleSheet;

    // Sounds
    private Sound difficultySound;
    private Sound playSound;

    // Music
    private Music introMusic;

    // Font
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    // Difficulty
    private final String[] difficulties = {"Easy", "Hard", "Crazy", "HELL"};
    private int currentDifficulty = 0;

    // Buttons
    private Rectangle lessDiffButton, moreDiffButton, playButton;

    // Mouse
    private float mouseX, mouseY;

    // Animation states
    private float lessScale = 1f;
    private float moreScale = 1f;
    private float playScale = 1f;
    private final Color playTextColor = new Color(0.85f, 0.85f, 0.85f, 1f);

    // Candles
    private Candle candleLeft;
    private Candle candleRight;

    // Particles
    private final List<Particle> particles = new ArrayList<>();
    private TextureRegion[] particleFrames;
    private float particleSpawnTimer = 0f;
    private float particleSpawnInterval = 1f;

    // States
    private boolean playPressed = false;
    private float fadeAlpha = 0f;

    // Highscore
    private int highscoreVal = 0;
    private float highscoreTimer = 0f;
    private final Color redColor = new Color(1f, 0.24f, 0.24f, 1f); // Red
    private final Color whiteColor = new Color(1f, 1f, 1f, 1f);   // White
    private final Color currentHighscoreColor = new Color();

    // --- NEW: Completion Display ---
    private boolean isCurrentDifficultyCompleted = false;
    private float completionPulseTimer = 0f;
    private final Color completionGold = new Color(1f, 0.84f, 0.0f, 1f); // Gold

    public StartScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        playPressed = false;
        fadeAlpha = 0f;

        batch = new SpriteBatch();

        Preferences prefs = Gdx.app.getPreferences("MyGameInfo");
        currentDifficulty = prefs.getInteger("difficulty", 0);
        checkCompletionStatus(); // Check immediately on load

        highscoreVal = prefs.getInteger("highscore", 0);

        // Camera and viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);

        // Load textures
        backgroundTexture = new Texture("IntroUI/background.jpg");
        titleTexture = new Texture("IntroUI/title.png");
        playButtonTexture = new Texture("IntroUI/playButton.png");
        candleSheet = new Texture("Spritesheets/candleSpritesheet.png");
        particleSheet = new Texture("Spritesheets/particlesSpritesheet.png");

        // Load sounds
        difficultySound = Gdx.audio.newSound(Gdx.files.internal("Sounds/difficultyButtonSound.mp3"));
        playSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/playButtonSound.mp3"));

        // Load and loop intro music
        introMusic = Gdx.audio.newMusic(Gdx.files.internal("Sounds/introBackgroundMusic.mp3"));
        introMusic.setLooping(true);
        introMusic.setVolume(0.2f);
        introMusic.play();

        // Particle spritesheet
        TextureRegion[][] tmp = TextureRegion.split(particleSheet, 16, 16);
        particleFrames = new TextureRegion[tmp[0].length];
        System.arraycopy(tmp[0], 0, particleFrames, 0, tmp[0].length);

        // Font setup
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("pixelon.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = (int)(VIRTUAL_HEIGHT * 0.045f);
        param.color = new Color(0.85f, 0.85f, 0.85f, 1f);
        font = generator.generateFont(param);
        generator.dispose();

        // Buttons
        float btnSize = VIRTUAL_HEIGHT * 0.06f;
        lessDiffButton = new Rectangle(VIRTUAL_WIDTH * 0.45f - btnSize, VIRTUAL_HEIGHT * 0.4f, btnSize, btnSize);
        moreDiffButton = new Rectangle(VIRTUAL_WIDTH * 0.55f, VIRTUAL_HEIGHT * 0.4f, btnSize, btnSize);
        playButton = new Rectangle(VIRTUAL_WIDTH * 0.4f, VIRTUAL_HEIGHT * 0.2f, VIRTUAL_WIDTH * 0.2f, VIRTUAL_HEIGHT * 0.1f);

        // Candles
        float candleY = VIRTUAL_HEIGHT * 0.27f;
        float candleOffsetX = VIRTUAL_WIDTH * 0.35f;
        candleLeft = new Candle(candleSheet, VIRTUAL_WIDTH / 2f - candleOffsetX, candleY);
        candleRight = new Candle(candleSheet, VIRTUAL_WIDTH / 2f + candleOffsetX - VIRTUAL_WIDTH * 0.06f, candleY);

        // Initial particles
        for (int i = 0; i < 8; i++) {
            Particle particle = new Particle(particleFrames);
            particle.randomizeUpperHalfStart();
            particles.add(particle);
        }

        // Input handling
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (playPressed) return false;

                Vector3 touchPos = new Vector3(screenX, screenY, 0);
                camera.unproject(touchPos);
                float tx = touchPos.x;
                float ty = touchPos.y;

                Preferences prefs = Gdx.app.getPreferences("MyGameInfo");

                if (lessDiffButton.contains(tx, ty)) {
                    currentDifficulty = (currentDifficulty + difficulties.length - 1) % difficulties.length;
                    difficultySound.play(0.75f);
                    prefs.putInteger("difficulty", currentDifficulty);
                    prefs.flush();
                    checkCompletionStatus(); // Check new difficulty status
                } else if (moreDiffButton.contains(tx, ty)) {
                    currentDifficulty = (currentDifficulty + 1) % difficulties.length;
                    difficultySound.play(0.75f);
                    prefs.putInteger("difficulty", currentDifficulty);
                    prefs.flush();
                    checkCompletionStatus(); // Check new difficulty status
                } else if (playButton.contains(tx, ty)) {
                    playPressed = true;
                    candleLeft.extinguish();
                    candleRight.extinguish();
                    playSound.play(0.1f);
                    introMusic.stop();
                }
                return true;
            }
        });
    }

    private void checkCompletionStatus() {
        Preferences prefs = Gdx.app.getPreferences("MyGameInfo");
        String key = "difficulty_" + currentDifficulty + "_completed";
        isCurrentDifficultyCompleted = prefs.getBoolean(key, false);
    }

    @Override
    public void render(float delta) {
        checkMousePos();
        ScreenUtils.clear(Color.BLACK);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float screenW = viewport.getWorldWidth();
        float screenH = viewport.getWorldHeight();

        // Background
        batch.draw(backgroundTexture, 0, 0, screenW, screenH);

        // Update and draw particles
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update(delta);
            particle.draw(batch);
            if (particle.isOffScreen()) iterator.remove();
        }

        particleSpawnTimer += delta;
        if (particleSpawnTimer >= particleSpawnInterval) {
            particles.add(new Particle(particleFrames));
            particleSpawnTimer = 0f;
            particleSpawnInterval = MathUtils.random(1.5f, 3f);
        }

        // Title
        float titleScale = screenH / 175f;
        float titleWidth = titleTexture.getWidth() * titleScale;
        float titleHeight = titleTexture.getHeight() * titleScale;
        float titleX = (screenW - titleWidth) / 2f;
        float titleY = screenH * 0.57f;
        batch.draw(titleTexture, titleX, titleY, titleWidth, titleHeight);

        // Candles
        candleLeft.update(delta);
        candleRight.update(delta);
        candleLeft.draw(batch);
        candleRight.draw(batch);

        // Difficulty Label
        String labelText = "Difficulty:";
        layout.setText(font, labelText);
        float labelX = (screenW - layout.width) / 2f;
        float labelY = screenH * 0.51f;
        font.draw(batch, layout, labelX, labelY);

        // Difficulty Value
        String difficulty = difficulties[currentDifficulty];
        layout.setText(font, difficulty);
        float diffX = (screenW - layout.width) / 2f;
        float diffY = screenH * 0.45f;
        font.draw(batch, layout, diffX, diffY);

        // --- NEW: COMPLETED PULSING TEXT ---
        if (isCurrentDifficultyCompleted) {
            completionPulseTimer += delta * 3f;
            float pulseScale = 1f + MathUtils.sin(completionPulseTimer) * 0.07f;

            // Draw slightly to the right of the difficulty text
            // Or below it, depending on your preference.
            // Here: Right side
            String completedText = "COMPLETED!";
            font.setColor(completionGold);
            font.getData().setScale(0.7f * pulseScale); // Smaller font

            layout.setText(font, completedText);

            // Position: To the right of the arrow button + padding
            float compX = moreDiffButton.x + moreDiffButton.width + 30f;
            float compY = diffY - 10f; // Align with difficulty text vertically

            font.draw(batch, completedText, compX, compY);

            // Reset
            font.setColor(Color.WHITE);
            font.getData().setScale(1f);
        }
        // -----------------------------------

        // Buttons hover
        boolean hoverLess = lessDiffButton.contains(mouseX, mouseY);
        lessScale += ((hoverLess ? 1.2f : 1f) - lessScale) * 10f * delta;
        drawCenteredText(batch, "<", lessDiffButton, lessScale);

        boolean hoverMore = moreDiffButton.contains(mouseX, mouseY);
        moreScale += ((hoverMore ? 1.2f : 1f) - moreScale) * 10f * delta;
        drawCenteredText(batch, ">", moreDiffButton, moreScale);

        boolean hoverPlay = playButton.contains(mouseX, mouseY);
        float targetPlayScale = hoverPlay ? 1.1f : 1f;
        playScale += (targetPlayScale - playScale) * 8f * delta;
        Color targetColor = hoverPlay ? Color.BLACK : new Color(0.85f, 0.85f, 0.85f, 1f);
        playTextColor.lerp(targetColor, 8f * delta);

        float centerX = playButton.x + playButton.width / 2f;
        float centerY = playButton.y + playButton.height / 2f;
        float scaledWidth = playButton.width * playScale;
        float scaledHeight = playButton.height * playScale;
        float drawX = centerX - scaledWidth / 2f;
        float drawY = centerY - scaledHeight / 2f;

        batch.draw(playButtonTexture, drawX, drawY, scaledWidth, scaledHeight);
        font.getData().setScale(playScale);
        font.setColor(playTextColor);
        layout.setText(font, "Play");
        font.draw(batch, layout, centerX - layout.width / 2f, centerY + layout.height / 2f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);


        // Highscore Logic
        highscoreTimer += delta * 4f;
        float pulseScale = 1f + MathUtils.sin(highscoreTimer) * 0.05f;
        float colorSine = (MathUtils.sin(highscoreTimer) + 1f) / 2f;
        currentHighscoreColor.set(redColor).lerp(whiteColor, colorSine);

        String highscoreText = "Highscore: " + highscoreVal;
        font.getData().setScale(pulseScale * 0.8f);
        font.setColor(currentHighscoreColor);
        layout.setText(font, highscoreText);

        float padding = 200f;
        float scoreX = screenW - layout.width - padding;
        float scoreY = padding + layout.height;

        font.draw(batch, layout, scoreX, scoreY);
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);

        // Fade after play pressed
        if (playPressed) {
            if ((candleLeft.isSmoking() || candleRight.isSmoking() ||
                candleLeft.isFinished() || candleRight.isFinished())) {
                fadeAlpha += delta;
                fadeAlpha = Math.min(fadeAlpha, 1f);

                batch.setColor(0, 0, 0, fadeAlpha);
                batch.draw(backgroundTexture, 0, 0, screenW, screenH);
                batch.setColor(Color.WHITE);

                if (fadeAlpha >= 1f) {
                    Preferences prefs = Gdx.app.getPreferences("MyGameInfo");
                    boolean storySeen = prefs.getBoolean("storySeen", false);

                    if (storySeen) {
                        game.setScreen(new GameScreen(game));
                    } else {
                        game.setScreen(new StoryScreen(game, StoryData.STORY_PARTS));
                    }
                }
            }
        }

        batch.end();
    }

    private void drawCenteredText(SpriteBatch batch, String text, Rectangle rect, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        float x = rect.x + (rect.width - layout.width) / 2f;
        float y = rect.y + (rect.height + layout.height) / 2f;
        font.draw(batch, layout, x, y);
        font.getData().setScale(1f);
    }

    private void checkMousePos() {
        if (Gdx.input.getX() >= 0 && Gdx.input.getX() <= Gdx.graphics.getWidth() &&
            Gdx.input.getY() >= 0 && Gdx.input.getY() <= Gdx.graphics.getHeight()) {

            Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mousePos);
            mouseX = mousePos.x;
            mouseY = mousePos.y;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override public void pause() {
        if (introMusic != null && introMusic.isPlaying()) {
            introMusic.pause();
        }
    }
    @Override public void resume() {
        if (introMusic != null && !introMusic.isPlaying() && !playPressed) {
            introMusic.play();
        }
    }
    @Override public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        batch.dispose();
        backgroundTexture.dispose();
        titleTexture.dispose();
        playButtonTexture.dispose();
        candleSheet.dispose();
        particleSheet.dispose();
        font.dispose();
        difficultySound.dispose();
        playSound.dispose();
        introMusic.dispose();
    }

    // =======================
    //      PARTICLE CLASS
    // =======================
    private static class Particle {
        private final TextureRegion frame;
        private float x, y, speedX, swaySpeed, scale, rotation;
        private float time;

        public Particle(TextureRegion[] frames) {
            this.frame = frames[MathUtils.random(frames.length - 1)];
            resetPosition();
            this.swaySpeed = MathUtils.random(2f, 3f);
            this.rotation = MathUtils.random(0f, 360f);
            this.time = MathUtils.random(0f, 100f);
            this.scale = MathUtils.random(1.6f, 2.3f) * (1080f / 240f);
        }

        public void resetPosition() {
            this.x = -MathUtils.random(50, 150);
            this.y = MathUtils.random(1080f * 0.3f, 1080f * 0.95f);
            this.speedX = MathUtils.random(40f, 60f);
        }

        public void randomizeUpperHalfStart() {
            this.x = MathUtils.random(0, 1920f);
            this.y = MathUtils.random(1080f * 0.3f, 1080f * 0.95f);
            this.speedX = MathUtils.random(40f, 60f);
        }

        public void update(float delta) {
            time += delta;
            x += speedX * delta;
            y += MathUtils.sin(time * swaySpeed) * 15f * delta;
            rotation += delta * 25f;
        }

        public void draw(SpriteBatch batch) {
            batch.draw(frame, x, y, frame.getRegionWidth()/2f, frame.getRegionHeight()/2f,
                frame.getRegionWidth(), frame.getRegionHeight(), scale, scale, rotation);
        }

        public boolean isOffScreen() {
            return x > 1920f + 50;
        }
    }

    // =======================
    //       CANDLE CLASS
    // =======================
    private static class Candle {
        private final TextureRegion[] frames;
        private float frameTime;
        private int frameIndex;
        private float x, y;
        private CandleState state = CandleState.BURNING;
        private float timer = 0f;
        private float candleScale = 1080f / 240f;

        private enum CandleState {BURNING, DIMMING, OUT, SMOKE, FINISHED}

        public Candle(Texture sheet, float x, float y) {
            TextureRegion[][] tmp = TextureRegion.split(sheet, 32, 96);
            frames = new TextureRegion[tmp[0].length];
            System.arraycopy(tmp[0], 0, frames, 0, tmp[0].length);
            this.x = x;
            this.y = y;
        }

        public void update(float delta) {
            timer += delta;
            switch (state) {
                case BURNING:
                    frameTime = 0.1f;
                    frameIndex = (int)((timer / frameTime) % 3);
                    break;
                case DIMMING:
                    int dimFrame = (int)(timer / 0.1f);
                    if (dimFrame < 3) frameIndex = 3 + dimFrame;
                    else { frameIndex = 6; state = CandleState.OUT; timer = 0; }
                    break;
                case OUT:
                    frameIndex = 6;
                    if (timer > 0.3f) { state = CandleState.SMOKE; timer = 0; }
                    break;
                case SMOKE:
                    int smokeFrame = (int)(timer / 0.22f);
                    frameIndex = 7 + Math.min(smokeFrame, 3);
                    if (smokeFrame > 3) state = CandleState.FINISHED;
                    break;
                case FINISHED:
                    frameIndex = 10;
                    break;
            }
        }

        public void extinguish() { if (state == CandleState.BURNING) { state = CandleState.DIMMING; timer = 0; } }
        public boolean isSmoking() { return state == CandleState.SMOKE; }
        public boolean isFinished() { return state == CandleState.FINISHED; }
        public void draw(SpriteBatch batch) {
            batch.draw(frames[frameIndex], x, y, frames[frameIndex].getRegionWidth()*candleScale, frames[frameIndex].getRegionHeight()*candleScale);
        }
    }
}
