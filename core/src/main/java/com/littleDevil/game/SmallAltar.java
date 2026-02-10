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
    private float setupFrameDuration;
    private float cycleFrameDuration = 0.15f;
    private int frameIndex = 0;
    private int totalFrames = 8;
    public CollisionObject interactionBox;

    private boolean isLoaded = false;
    private boolean cycling = false;

    private Boost boost = null;
    private boolean boostSpawned = false;

    public SmallAltar(float x, float y, String spriteSheetPath, float setupSpeed, CollisionObject interactionBox) {
        this.x = x;
        this.y = y;
        this.setupFrameDuration = setupSpeed;
        this.interactionBox = interactionBox;

        spriteSheet = new Texture(spriteSheetPath);
        spriteSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        frames = new TextureRegion[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            frames[i] = new TextureRegion(spriteSheet, i * 32, 0, 32, 32);
        }

        currentFrame = frames[0];
    }

    public void update(float delta, Player player, GameWorld gameWorld) {

        // --- Phase 1: Charging up to frame 4 ---
        if (!isLoaded && !cycling) {

            // LOGIC CHANGE: Only progress the charging animation if the wave is active!
            if (gameWorld.waveActive) {
                animationTimer += delta;

                if (animationTimer >= setupFrameDuration) {
                    animationTimer = 0f;
                    frameIndex++;

                    if (frameIndex == 3) {
                        isLoaded = true;
                        cycling = true;
                    }

                    if (frameIndex > 3) frameIndex = 3;
                }
            }
        }

        // --- Phase 2: Active cycling (frames 4–8) ---
        else if (cycling) {
            // We continue animating the "glow" (cycling frames) even if wave stops
            animationTimer += delta;

            if (!boostSpawned) {
                // LOGIC CHANGE: Only spawn the actual item if the wave is active
                if (gameWorld.waveActive) {

                    double r = Math.random();
                    Boost.Type type;

                    if (r < 0.25) type = Boost.Type.SPEED;
                    else if (r < 0.50) type = Boost.Type.DAMAGE;
                    else if (r < 0.75) type = Boost.Type.REGEN;
                    else type = Boost.Type.SUPER;

                    // WARNING: Creating new Textures in update() causes memory leaks!
                    // Ideally, load these 4 textures once in the constructor.
                    Texture texture;
                    switch (type) {
                        case SPEED -> texture = new Texture("MapAssets/movementBoost.png");
                        case DAMAGE -> texture = new Texture("MapAssets/attackBoost.png");
                        case REGEN -> texture = new Texture("MapAssets/regenBoost.png");
                        case SUPER -> texture = new Texture("MapAssets/superBoost.png");
                        default -> texture = new Texture("MapAssets/movementBoost.png");
                    }

                    boost = new Boost(type, x + 14, y + 26, texture);
                    manageLight(0.7f);
                    boostSpawned = true;
                }
            }

            // Update existing boost regardless of wave state (so player can pick it up after wave ends)
            if (boost != null && !boost.pickedUp) {
                boost.update(delta);
            }

            // Cycle Animation Logic
            if (animationTimer >= cycleFrameDuration) {
                animationTimer = 0f;
                frameIndex++;
                if (frameIndex > 7) frameIndex = 3;
            }

            // Player Pickup Logic
            if (boost != null && !boost.pickedUp && player.isOnBoost(boost)) {
                boost.applyEffect(player, gameWorld);
                manageLight(0f);
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

    private void manageLight(float alpha) {
        /*
        LightData.lightObjects[19].alpha = alpha;
        LightData.lightObjects[20].alpha = alpha;
        LightData.lightObjects[21].alpha = alpha;
        LightData.lightObjects[22].alpha = alpha;

         */
    }

    public void dispose() {
        spriteSheet.dispose();
    }

    public float getX() { return x; }
    public float getY() { return y; }
}
