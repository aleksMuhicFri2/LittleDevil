package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class SmallAltar {

    private Texture spriteSheet;
    private TextureRegion[] frames;
    private TextureRegion currentFrame;

    private float x, y;
    private float animationTimer = 0f;
    private float setupFrameDuration; // variable time between setup frames
    private float cycleFrameDuration = 0.15f; // 200ms for looping animation
    private int frameIndex = 0;
    private int totalFrames = 8;
    public CollisionObject interactionBox;

    private boolean isLoaded = false; // altar fully charged
    private boolean cycling = false;  // cycling between frames 4–8

    private Boost boost = null;
    private boolean boostSpawned = false;

    public SmallAltar(float x, float y, String spriteSheetPath, float setupSpeed, CollisionObject interactionBox) {
        this.x = x;
        this.y = y;
        this.setupFrameDuration = setupSpeed;
        this.interactionBox = interactionBox;

        spriteSheet = new Texture(spriteSheetPath);
        spriteSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // 8 frames horizontally, each 32x32
        frames = new TextureRegion[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            frames[i] = new TextureRegion(spriteSheet, i * 32, 0, 32, 32);
        }

        currentFrame = frames[0];
    }

    public void update(float delta, Player player, GameWorld gameWorld) {
        animationTimer += delta;

        // --- Phase 1: Charging up to frame 4 ---
        if (!isLoaded && !cycling) {
            if (animationTimer >= setupFrameDuration) {
                animationTimer = 0f;
                frameIndex++;

                // When reaching frame 4 (index 3)
                if (frameIndex == 3) {
                    isLoaded = true;
                    cycling = true; // start cycling
                }

                if (frameIndex > 3) frameIndex = 3;
            }
        }

        // --- Phase 2: Active cycling (frames 4–8) ---
        else if (cycling) {
            if (!boostSpawned) {
                boost = new Boost(Boost.Type.SPEED, x + 14, y + 26, new Texture("movementBoost.png"));
                boostSpawned = true;
            }

            if (boost != null && !boost.pickedUp) {
                boost.update(delta);
            }

            if (animationTimer >= cycleFrameDuration) {
                animationTimer = 0f;
                frameIndex++;
                if (frameIndex > 7) frameIndex = 3; // loop between 4–8
            }

            // If player steps on it while loaded
            if (player.isOnBoost(gameWorld)) {
                boost.applyEffect(player);
                resetAltar();
            }
        }

        currentFrame = frames[frameIndex];
    }

    private void resetAltar() {
        frameIndex = 0;
        isLoaded = false;
        cycling = false;
        animationTimer = 0f;
        boostSpawned = false;
        boost = null;
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, x, y);
        if (boost != null && !boost.pickedUp) {
            boost.render(batch);
        }
    }

    public void dispose() {
        spriteSheet.dispose();
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
