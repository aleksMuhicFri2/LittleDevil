package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class BottleProjectile {

    public enum State {
        FLYING,
        BREAKING,
        DONE
    }

    private State state = State.FLYING;

    public float x, y;
    public float targetX, targetY;
    public float startX, startY;
    public float travelTime; // computed based on distance
    private final float flightSpeed = 100f; // tweak this
    private float timer = 0f;

    private TextureRegion[] frames; // 0 = flying, 1..4 breaking
    private int breakFrame = 1;
    private float breakTimer = 0f;

    private float stickTimer = 0f; // stays after breaking
    private float alpha = 1f;

    public BottleProjectile(Texture potionSheet,
                            float startX,
                            float startY,
                            float targetX,
                            float targetY) {

        // Load sprites exactly like the Orb class
        frames = new TextureRegion[5];
        for (int i = 0; i < 5; i++) {
            frames[i] = new TextureRegion(potionSheet, i * 32, 0, 32, 32);
        }

        this.startX = startX;
        this.startY = startY;

        this.x = startX;
        this.y = startY;

        this.targetX = targetX;
        this.targetY = targetY;

        // Compute distance and travel time
        float dx = targetX - startX;
        float dy = targetY - startY;
        float distance = (float)Math.sqrt(dx * dx + dy * dy);

        travelTime = distance / flightSpeed;
        if (travelTime < 0.25f) travelTime = 0.25f; // minimum flight time (optional)
    }

    public void update(float delta) {

        switch (state) {

            case FLYING -> {
                timer += delta;
                float t = timer / travelTime;   // 0 → 1

                if (t >= 1f) {
                    t = 1f;
                    state = State.BREAKING;
                }

                // Horizontal movement: perfect LERP
                x = startX + (targetX - startX) * t;
                y = startY + (targetY - startY) * t;

                float dx = targetX - startX;
                float dy = targetY - startY;
                float distance = (float)Math.sqrt(dx*dx + dy*dy);

                // Peak height proportional to distance
                float peakHeight = distance * 0.25f;  // tweak gameplay feel

                float arc = peakHeight * 4f * t * (1f - t);
                y += arc;
            }

            case BREAKING -> {
                breakTimer += delta;

                if (breakTimer > 0.08f && breakFrame < 4) {
                    breakTimer = 0f;
                    breakFrame++;
                }

                if (breakFrame == 4) {
                    stickTimer += delta;
                    if (stickTimer >= 2f) {
                        // start fadeout
                        alpha -= delta * 1f;
                        if (alpha <= 0f) {
                            alpha = 0f;
                            state = State.DONE;
                        }
                    }
                }
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (state == State.DONE) return;

        TextureRegion frame =
            (state == State.FLYING) ? frames[0] : frames[breakFrame];

        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(frame, x - 16, y - 16, 32, 32);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public boolean isDead() {
        return state == State.DONE;
    }
}
