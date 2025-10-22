package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class BigAltar {

    private Texture spriteSheet;
    private TextureRegion[] frames;
    private TextureRegion currentFrame;

    private float x, y;
    private float animationTimer = 0f;
    private float frameDuration = 0.2f; // seconds per frame
    private int frameIndex = 0;
    private int totalFrames = 10;

    private boolean reversing = false;

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

        // 10 frames horizontally, each 80×64 px
        frames = new TextureRegion[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            frames[i] = new TextureRegion(spriteSheet, i * 80, 0, 80, 64);
        }

        currentFrame = frames[0];
    }

    public void update(float delta, Player player, GameWorld gameWorld) {
        animationTimer += delta;

        if (animationTimer < frameDuration) return; // wait for next frame
        animationTimer = 0f;

        // --- Handle entering or staying inside ---
        if (player.isOnAltar(gameWorld)) {
            if (!reversing) {
                frameIndex++;
                if (frameIndex >= 8) {
                    // TODO gameWorld.openUpgrades()
                }
                // Loop between 8–9–10 when fully activated
                if (frameIndex > 9) {
                    frameIndex = 8;
                }
            } else {
                // If reversing but player re-enters, resume forward direction
                reversing = false;
            }
        }
        // --- Handle stepping out ---
        else {
            if (frameIndex > 0) {
                reversing = true;
                frameIndex--;
            } else {
                reversing = false;
            }
        }

        // Clamp just in case
        frameIndex = Math.max(0, Math.min(frameIndex, totalFrames - 1));
        currentFrame = frames[frameIndex];
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, x, y);
    }

    public void dispose() {
        spriteSheet.dispose();
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
