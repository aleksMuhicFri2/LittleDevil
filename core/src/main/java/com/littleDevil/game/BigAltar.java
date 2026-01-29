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
    private float frameDuration = 0.15f; // Snappier animation
    private int frameIndex = 0;
    private int totalFrames = 10;

    private boolean reversing = false;
    private Sound candleLightSound;

    public CollisionObject interactionBox = new CollisionObject(
        "BigAltarInteractionBox",
        292, 216, 16, 16, 0, 0, 0, 0, 3
    );

    public BigAltar(float x, float y, String spriteSheetPath) {
        this.x = x;
        this.y = y;

        spriteSheet = new Texture(spriteSheetPath);
        spriteSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        frames = new TextureRegion[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            frames[i] = new TextureRegion(spriteSheet, i * 80, 0, 80, 64);
        }

        currentFrame = frames[0];
        candleLightSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/candleStartSound.mp3"));
    }

    public void update(float delta, Player player, GameWorld gameWorld) {
        animationTimer += delta;
        if (animationTimer < frameDuration) return;
        animationTimer = 0f;

        boolean onAltar = player.isOnAltar(gameWorld);

        // --- VISUAL ANIMATION ONLY ---
        if (onAltar) {
            // Opening Animation
            if (frameIndex < totalFrames - 1) {
                if (frameIndex == 0) candleLightSound.play(0.3f); // Play sound once on start
                frameIndex++;
            }
        } else {
            // Closing Animation
            if (frameIndex > 0) {
                frameIndex--;
            }

            // CLEANUP: If the player walks away, ensure the menus close.
            // GameWorld handles opening, Altar handles closing when leaving.
            if (!gameWorld.gameScreen.uiManager.isAugmentPageOpen()) {
                gameWorld.gameScreen.uiManager.closeUpgradePage();
            }
        }

        currentFrame = frames[frameIndex];
    }

    public boolean isFullyOpen() {
        return frameIndex >= 8;
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, x, y);
    }

    public void dispose() {
        if (spriteSheet != null) spriteSheet.dispose();
        if (candleLightSound != null) candleLightSound.dispose();
    }
}
