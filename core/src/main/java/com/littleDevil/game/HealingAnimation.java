package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class HealingAnimation {

    private static final float FRAME_INTERVAL = 0.1f;

    private Texture texture;
    private TextureRegion[] frames;
    private int frameCount = 5;

    private Player player;   // <-- track the player
    private float offsetY;   // vertical offset below the player

    private float timer = 0f;
    private int frameIndex = 0;

    public boolean done = false;

    public HealingAnimation(Player player, float offsetY, Texture texture) {
        this.player = player;
        this.offsetY = offsetY;

        this.texture = texture;
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = new TextureRegion(texture, i * 32, 0, 32, 16);
        }
    }

    public void update(float delta) {
        if (done) return;

        timer += delta;
        if (timer >= FRAME_INTERVAL) {
            timer = 0f;
            frameIndex++;

            if (frameIndex >= frameCount) done = true;
        }
    }

    public void render(SpriteBatch batch) {
        if (done) return;

        float frameWidth = frames[frameIndex].getRegionWidth();

        // Center animation using player's real sprite width
        float drawX = player.x - frameWidth * 0.5f;

        batch.draw(
            frames[frameIndex],
            drawX,
            player.y + offsetY
        );
    }

    public void dispose() {
        texture.dispose();
    }
}
