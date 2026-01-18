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

        boolean onAltar = player.isOnAltar(gameWorld);

        // --- Player on altar ---
        if (onAltar) {

            // Animate altar opening (Visual feedback always happens)
            if (!reversing) {
                frameIndex++;
                if (frameIndex > 9) frameIndex = 9;
            } else {
                reversing = false;
            }

            // Only open UI if animation is ready AND tutorial is finished
            if (frameIndex >= 6) {
                if (gameWorld.basicTutorialDone) {
                    if (gameWorld.augmentSelectionPending) {
                        gameWorld.gameScreen.uiManager.openAugmentPage();
                    } else {
                        gameWorld.gameScreen.uiManager.openUpgradePage();
                    }
                }
                // If tutorial isn't done, we simply do nothing here
                // The altar glows, but the menu refuses to open
            }

        }
        // --- Player left altar ---
        else {

            // Close UI if it was open
            gameWorld.gameScreen.uiManager.closeUpgradePage();

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
