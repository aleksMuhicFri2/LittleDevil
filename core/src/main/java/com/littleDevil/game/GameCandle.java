package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class GameCandle {
    private final TextureRegion[] frames;
    private final float frameTime = 0.2f;
    private float timer = 0f;
    private int frameIndex = 1; // Start at frame[1] (the second frame)
    private final float x, y;
    private final float candleScale = 1f; // Optional: adjust if needed

    public GameCandle(Texture sheet, float x, float y) {
        TextureRegion[][] tmp = TextureRegion.split(sheet, 16, 16);
        frames = new TextureRegion[tmp[0].length];
        System.arraycopy(tmp[0], 0, frames, 0, tmp[0].length);
        this.x = x;
        this.y = y;
    }

    public void update(float delta) {
        timer += delta;
        if (timer >= frameTime) {
            timer = 0f;
            frameIndex++;
            if (frameIndex > 4) frameIndex = 1;
        }
    }

    public TextureRegion getCurrentFrame() {
        return frames[frameIndex];
    }

    public void draw(SpriteBatch batch) {
        batch.draw(
            frames[frameIndex],
            x,
            y,
            frames[frameIndex].getRegionWidth() * candleScale,
            frames[frameIndex].getRegionHeight() * candleScale
        );
    }
}
