package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class BottleProjectile {

    private GameWorld world;
    private Player player;

    public enum State {
        FLYING,
        BREAKING,
        DONE
    }

    private State state = State.FLYING;

    public float x, y;
    public float targetX, targetY;
    public float startX, startY;
    public float travelTime;
    private final float flightSpeed = 100f;
    private float timer = 0f;
    private float bottleDamage;

    private TextureRegion[] frames;
    private int breakFrame = 1;
    private float breakTimer = 0f;

    private float stickTimer = 0f;
    private float alpha = 1f;

    private final float hitboxWidth = 28f;   // tweak
    private final float hitboxHeight = 14f;  // tweak
    private boolean damageApplied = false;

    private final Sound breakSound;

    public BottleProjectile(Texture potionSheet,
                            float startX,
                            float startY,
                            float targetX,
                            float targetY,
                            GameWorld gameWorld, float damage) {

        this.world = gameWorld;
        this.player = gameWorld.player;

        // Load sprites
        frames = new TextureRegion[5];
        for (int i = 0; i < 5; i++) {
            frames[i] = new TextureRegion(potionSheet, i * 32, 0, 32, 32);
        }

        breakSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/bottleBreakSfx.mp3"));

        this.startX = startX;
        this.startY = startY;
        this.x = startX;
        this.y = startY;
        this.targetX = targetX;
        this.targetY = targetY;

        float dx = targetX - startX;
        float dy = targetY - startY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        travelTime = distance / flightSpeed;
        if (travelTime < 0.25f) travelTime = 0.25f;

        bottleDamage = damage;
    }

    public void update(float delta) {

        switch (state) {

            case FLYING -> {
                timer += delta;
                float t = timer / travelTime;

                if (t >= 1f) {
                    t = 1f;
                    enterBreakingState();
                }

                x = startX + (targetX - startX) * t;
                y = startY + (targetY - startY) * t;

                float dx = targetX - startX;
                float dy = targetY - startY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                float peakHeight = distance * 0.25f;
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

    private void enterBreakingState() {
        state = State.BREAKING;

        float randomPitch = MathUtils.random(0.8f, 1.3f);
        breakSound.play(0.12f, randomPitch, 0f);

        // Deal damage immediately
        if (!damageApplied) {
            checkDamageHit();
            damageApplied = true;
        }
    }

    private void checkDamageHit() {
        if (player == null) return;

        // Bottle hitbox
        float left = x - hitboxWidth / 2f;
        float right = x + hitboxWidth / 2f;
        float bottom = y - hitboxHeight / 2f;
        float top = y + hitboxHeight / 2f;

        // Player collision box (REAL one)
        float pLeft   = player.x + player.collisionOffsetX;
        float pRight  = pLeft + player.collisionWidth;
        float pBottom = player.y + player.collisionOffsetY;
        float pTop    = pBottom + player.collisionHeight;

        boolean overlapX = pRight > left && pLeft < right;
        boolean overlapY = pTop > bottom && pBottom < top;

        if (overlapX && overlapY) {
            player.currentHP -= bottleDamage;
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
