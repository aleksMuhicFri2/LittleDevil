package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class BigAltar {

    // CONFIG
    private static final int FRAME_WIDTH = 80;
    private static final int FRAME_HEIGHT = 64;
    private static final int TOTAL_FRAMES = 10;
    private static final float FRAME_DURATION = 0.15f;

    // STATE
    private int x, y;
    private float animationTimer = 0f;
    private int frameIndex = 0;
    private boolean wasOnAltar = false; // Tracks state from previous frame

    // RESOURCES
    private final Texture spriteSheet;
    private final TextureRegion[] frames;
    private TextureRegion currentFrame;
    private final Sound candleLightSound;

    // HITBOX
    public CollisionObject interactionBox;

    public BigAltar(int x, int y, String spriteSheetPath) {
        this.x = x;
        this.y = y;

        // Texture & Sound
        spriteSheet = new Texture(spriteSheetPath);
        spriteSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        candleLightSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/candleStartSound.mp3"));

        // Slice Frames
        TextureRegion[][] tmp = TextureRegion.split(spriteSheet, FRAME_WIDTH, FRAME_HEIGHT);
        frames = new TextureRegion[TOTAL_FRAMES];
        System.arraycopy(tmp[0], 0, frames, 0, TOTAL_FRAMES);
        currentFrame = frames[0];

        // Create Hitbox relative to Altar position (centered)
        this.interactionBox = new CollisionObject(
            "BigAltarInteractionBox",
            x + 30, y + 16, 16, 16,
            0, 0, 0, 0, 3 // Type 3 = ALTAR
        );
    }

    public void update(float delta, Player player, GameWorld gameWorld) {
        boolean onAltar = player.isOnAltar(gameWorld);

        // STATE CHANGES
        if (onAltar && !wasOnAltar) {
            // Player just stepped ON
            candleLightSound.play(0.12f);
        }
        else if (!onAltar && wasOnAltar) {
            // Player just stepped OFF
            gameWorld.gameScreen.uiManager.closeUpgradePage();
            gameWorld.gameScreen.uiManager.closeAugmentPage();
        }

        wasOnAltar = onAltar;

        // VISUALS
        animationTimer += delta;
        if (animationTimer >= FRAME_DURATION) {
            animationTimer = 0f;

            if (onAltar) {
                // Opening
                if (frameIndex < TOTAL_FRAMES - 1) frameIndex++;
            } else {
                // Closing
                if (frameIndex > 0) frameIndex--;
            }

            currentFrame = frames[frameIndex];
        }
    }

    public boolean isFullyOpen() {
        // Considered open if nearly finished animation
        return frameIndex >= TOTAL_FRAMES - 2;
    }

    public void render(SpriteBatch batch) {
        batch.draw(currentFrame, x, y);
    }

    public void dispose() {
        if (spriteSheet != null) spriteSheet.dispose();
        if (candleLightSound != null) candleLightSound.dispose();
    }
}
