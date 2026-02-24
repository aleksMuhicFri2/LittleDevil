package com.littleDevil.game;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Explosion {

    public float x, y;
    public boolean done = false;

    private TextureRegion[] frames;
    private int currentFrame = 0;

    private float frameTimer = 0f;
    private final float FRAME_INTERVAL = 0.07f;

    public float scale = 2.5f; // looks nicer than 1.8, tweak if needed

    public Explosion(float x, float y, Texture explosionSheet) {

        // Explosion sheet is 8 frames horizontally, 32×32 each
        frames = new TextureRegion[8];
        for (int i = 0; i < 8; i++) {
            frames[i] = new TextureRegion(explosionSheet, i * 32, 0, 32, 32);
        }

        // center explosion on priest
        this.x = x - 16;
        this.y = y - 16;
    }

    public void update(float delta) {
        frameTimer += delta;

        if (frameTimer >= FRAME_INTERVAL) {
            frameTimer = 0f;
            currentFrame++;

            if (currentFrame >= frames.length) {
                done = true;
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (done) return;

        TextureRegion f = frames[currentFrame];

        batch.draw(
            f,
            x,
            y,
            16, 16,      // origin for scaling
            32, 32,      // size
            scale, scale,
            0
        );
    }
}
