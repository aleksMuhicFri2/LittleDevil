package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class StoryScreen implements Screen {

    private final Main game;
    private SpriteBatch batch;
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    // Viewport & camera
    private OrthographicCamera camera;
    private Viewport viewport;
    private final float WORLD_WIDTH = 800;
    private final float WORLD_HEIGHT = 480;

    // Music
    private Music storyMusic;

    // Story details
    private final String[] storyParts;
    private int currentPartIndex = 0;
    private String displayedText = "";
    private int charIndex = 0;
    private float charTimer = 0f;

    // Typewriter delays
    private final float charDelay = 0.055f;
    private final float newlinePause = 0.5f;
    private float partIdle = 0.5f;
    private final float partFade = 0.5f;
    private final float partBlack = 0.5f;

    // Typewriter states
    private enum State { TYPING, NEWLINE_PAUSE, PART_IDLE, PART_FADE, PART_BLACK }
    private State state = State.TYPING;
    private float stateTimer = 0f;
    private boolean skip = false;
    private boolean skipIdleLength = false;
    private float fadeAlpha = 1f;

    public StoryScreen(Main game, String[] storyParts) {
        this.game = game;
        this.storyParts = storyParts;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // Camera & viewport setup
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();
        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);

        // Font init
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("pixelon.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = (int)(WORLD_HEIGHT * 0.045f);
        param.color = Color.WHITE;
        font = generator.generateFont(param);
        generator.dispose();

        // Music
        storyMusic = Gdx.audio.newMusic(Gdx.files.internal("Sounds/storyBackgroundMusic.mp3"));
        storyMusic.setLooping(false);
        storyMusic.setVolume(0.3f);
        storyMusic.play();

        // Input to skip
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                skip = true;
                return true;
            }
        });
    }

    @Override
    public void render(float delta) {
        // Clear with black to cover letterbox areas
        ScreenUtils.clear(Color.BLACK);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // End of story logic with music fade-out
        if (currentPartIndex >= storyParts.length) {
            if (storyMusic.getVolume() > 0f) {
                float fadeSpeed = 0.8f;
                storyMusic.setVolume(Math.max(0f, storyMusic.getVolume() - fadeSpeed * delta));
            } else {
                storyMusic.stop();
                game.setScreen(new GameScreen(game)); // replace with your next screen
                batch.end();
                return;
            }
        }

        String currentPart = currentPartIndex < storyParts.length ? storyParts[currentPartIndex] : "";

        // Typewriter logic
        switch (state) {
            case TYPING:
                if (skip) {
                    displayedText = currentPart;
                    charIndex = currentPart.length();
                    state = State.PART_IDLE;
                    stateTimer = 0f;
                    skip = false;
                    skipIdleLength = true;
                } else {
                    charTimer += delta;
                    while (charTimer >= charDelay && charIndex < currentPart.length()) {
                        char c = currentPart.charAt(charIndex);
                        displayedText += c;
                        charIndex++;
                        charTimer -= charDelay;

                        if (c == '\n') {
                            state = State.NEWLINE_PAUSE;
                            stateTimer = 0f;
                            break;
                        }
                    }
                    if (charIndex >= currentPart.length()) {
                        state = State.PART_IDLE;
                        stateTimer = 0f;
                    }
                }
                break;

            case NEWLINE_PAUSE:
                stateTimer += delta;
                if (stateTimer >= newlinePause) state = State.TYPING;
                break;

            case PART_IDLE:
                if (currentPartIndex == storyParts.length - 1 || skipIdleLength) {
                    partIdle = 2f;
                    skipIdleLength = false;
                }
                stateTimer += delta;
                if (stateTimer >= partIdle || skip) {
                    state = State.PART_FADE;
                    stateTimer = 0f;
                    partIdle = 0.5f;
                }
                break;

            case PART_FADE:
                stateTimer += delta;
                fadeAlpha = 1f - (stateTimer / partFade);
                if (fadeAlpha <= 0f) {
                    fadeAlpha = 1f;
                    state = State.PART_BLACK;
                    stateTimer = 0f;
                    displayedText = "";
                    charIndex = 0;
                    currentPartIndex++;
                }
                break;

            case PART_BLACK:
                stateTimer += delta;
                if (stateTimer >= partBlack) {
                    state = State.TYPING;
                    stateTimer = 0f;
                }
                break;
        }

        // Last part colored red
        if (currentPartIndex == storyParts.length - 1) {
            font.setColor(1, 0, 0, fadeAlpha);
        } else {
            font.setColor(1, 1, 1, fadeAlpha);
        }

        layout.setText(font, displayedText);
        float x = (WORLD_WIDTH - layout.width) / 2f;
        float y = (WORLD_HEIGHT + layout.height) / 2f;
        font.draw(batch, layout, x, y);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        storyMusic.dispose();
    }
}
