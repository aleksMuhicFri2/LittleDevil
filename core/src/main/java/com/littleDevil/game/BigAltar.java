package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class BigAltar {

    private Texture spriteSheet;
    private TextureRegion[] frames;
    private TextureRegion currentFrame;

    private float x, y;
    private float animationTimer = 0f;
    private float frameDuration = 0.2f;
    private int frameIndex = 0;
    private int totalFrames = 10;

    private boolean reversing = false;

    // Sound when candle lights
    private Sound candleLightSound;

    // Track state so we don't replay sound
    private boolean[] candleLit = new boolean[5];

    public CollisionObject interactionBox = new CollisionObject(
        "BigAltarInteractionBox",
        292,
        216,
        16,
        16,
        0,
        0,
        0,
        0,
        3
    );

    public BigAltar(float x, float y, String spriteSheetPath) {
        this.x = x;
        this.y = y;

        spriteSheet = new Texture(spriteSheetPath);
        spriteSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Load animation frames
        frames = new TextureRegion[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            frames[i] = new TextureRegion(spriteSheet, i * 80, 0, 80, 64);
        }

        currentFrame = frames[0];

        // Load sound
        candleLightSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/candleStartSound.mp3"));
    }

    public void update(float delta, Player player, GameWorld gameWorld) {
        animationTimer += delta;

        if (animationTimer < frameDuration) return;
        animationTimer = 0f;

        // --- Player on altar ---
        if (player.isOnAltar(gameWorld)) {

            if (!reversing) {
                frameIndex++;

                if (frameIndex >= 8) {
                    // altar fully opened, loops 8-9-10
                }
                if (frameIndex > 9) frameIndex = 8;

            } else {
                reversing = false;
            }
        }
        // --- Player left altar ---
        else {
            if (frameIndex > 0) {
                reversing = true;
                frameIndex--;
            } else {
                reversing = false;
            }
        }

        // Clamp
        frameIndex = Math.max(0, Math.min(frameIndex, totalFrames - 1));
        currentFrame = frames[frameIndex];

        // --- Candle light transitions ---
        // Candles correspond to frameIndex 1–6
        for (int i = 0; i < 5; i++) {

            int lightIndex = 14 + i;
            float targetAlpha = (frameIndex >= i + 1) ? 0.7f : 0f;

            // Smooth interpolate
            LightData.lightObjects[lightIndex].alpha +=
                (targetAlpha - LightData.lightObjects[lightIndex].alpha) * 20f * delta;

            boolean shouldBeLit = frameIndex >= i + 1;

            // Play sound ONLY on the first candle (i == 0)
            if (i == 0 && shouldBeLit && !candleLit[0]) {
                float pitch = 0.85f + (float)Math.random() * 0.3f;
                candleLightSound.play(0.05f, pitch, 0f);
                candleLit[0] = true;
            }

            // Track lit state for all candles (but do not play sound for 1–4)
            if (shouldBeLit && !candleLit[i]) {
                candleLit[i] = true;
            }

            // Reset when turning off
            if (!shouldBeLit && candleLit[i]) {
                candleLit[i] = false;
            }
        }
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, x, y);
    }

    public void dispose() {
        spriteSheet.dispose();
        candleLightSound.dispose();
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
