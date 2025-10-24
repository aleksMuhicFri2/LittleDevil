package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Templar extends Enemy {

    private Texture shieldSpritesheet;
    public TextureRegion currentShieldFrame;
    private TextureRegion[] templarFrames, shieldFrames;

    public enum TemplarState {
        CHASING,
        CHANNELING,
        BASHING,
        POST_HIT_PAUSE
    }

    private TemplarState state = TemplarState.CHASING;
    private float stateTimer = 0f;

    private final float ATTACK_RANGE = 35f;
    private final float CHANNEL_TIME = 0.75f;
    private final float BASH_DURATION = 0.35f;
    private final float BASH_SPEED = moveSpeed * 6f;
    private final float BASH_COOLDOWN = 2f;
    private float bashCooldownTimer = 0f;
    private final float POST_HIT_PAUSE = 1f;

    private float frozenShieldRotation = 0f;

    private Vector2 bashDir = new Vector2();
    private boolean facingLeft = false;
    private boolean recentFacing;

    public Templar(float x, float y, GameWorld world) {
        super(x, y, "Spritesheets/templarSpritesheet.png", world);
        shieldSpritesheet = new Texture("Spritesheets/shieldSpritesheet.png");

        height = 32f;
        width = 32f;
        moveSpeed = 30f;
        knockbackDecay = 150f;

        hitboxOffsetX = -12;
        hitboxOffsetY = -16;
        hitboxWidth = 24;
        hitboxHeight = 20;

        // Templar frames
        templarFrames = new TextureRegion[9];
        for (int i = 0; i < 3; i++) templarFrames[i] = new TextureRegion(spriteSheet, i * 32, 0, 32, 32);
        currentFrame = templarFrames[0];

        // Shield frames
        shieldFrames = new TextureRegion[8];
        for (int i = 0; i < 8; i++) shieldFrames[i] = new TextureRegion(shieldSpritesheet, i * 32, 0, 32, 32);
        currentShieldFrame = shieldFrames[0];
    }

    @Override
    public void update(float delta, Player player, GameWorld gameWorld, GameScreen gameScreen) {
        if (!isAlive) return;
        if (bashCooldownTimer > 0) bashCooldownTimer -= delta;

        stateTimer -= delta;

        switch (state) {
            case CHASING -> {
                followPath(gameWorld, delta);
                currentShieldFrame = shieldFrames[0];
                if (distanceToPlayer(player) < ATTACK_RANGE && bashCooldownTimer <= 0f) {
                    state = TemplarState.CHANNELING;
                    stateTimer = CHANNEL_TIME;
                    bashDir.set(player.x - x, player.y - y).nor();
                }
            }

            case CHANNELING -> {
                // Animate shield frames 0 → 4 over CHANNEL_TIME
                float progress = 1f - stateTimer / CHANNEL_TIME; // 0 → 1
                int frameIndex = Math.min(4, (int)(progress * 5f));
                currentShieldFrame = shieldFrames[frameIndex];

                // Stay still
                if (stateTimer <= 0f) {
                    state = TemplarState.BASHING;
                    stateTimer = BASH_DURATION;
                }
            }

            case BASHING -> {
                float progress = 1f - stateTimer / BASH_DURATION;
                float decel = 1f - (progress * progress); // quadratic drop
                float moveX = bashDir.x * BASH_SPEED * decel * delta;
                float moveY = bashDir.y * BASH_SPEED * decel * delta;

                // Animate hit frames
                if (stateTimer <= BASH_DURATION / 2f) {
                    currentShieldFrame = shieldFrames[5];
                } else {
                    currentShieldFrame = shieldFrames[4];
                }

                moveWithCollision(moveX, moveY, gameWorld);

                if (stateTimer <= 0f) {
                    currentShieldFrame = shieldFrames[6];
                    bashCooldownTimer = BASH_COOLDOWN;
                    state = TemplarState.POST_HIT_PAUSE;
                    stateTimer = POST_HIT_PAUSE;
                }
            }

            case POST_HIT_PAUSE -> {
                currentShieldFrame = shieldFrames[0];
                if (stateTimer <= 0f) {
                    state = TemplarState.CHASING;
                }
            }
        }

        applySeparationForce(gameWorld);
        applyKnockback(delta, gameWorld);
        handleAttack(player, gameScreen);
        updateAnimation(delta, player);
    }

    public void updateAnimation(float delta, Player player) {
        // Default: idle
        currentFrame = templarFrames[0];

        // Hurt flash overrides everything
        if (hitFlashTime > 0) {
            currentFrame = templarFrames[2];
            currentShieldFrame = shieldFrames[7];
            hitFlashTime -= delta;
            return;
        }

        // Determine if the templar should face left
        boolean shouldFaceLeft = (player.x + 32 / 2f) < (x + width / 2f);
        if (shouldFaceLeft != facingLeft) {
            facingLeft = shouldFaceLeft;
            for (int i = 0; i <= 2; i++) templarFrames[i].flip(true, false);
        }

        // Walk animation
        if (state == TemplarState.CHASING || state == TemplarState.BASHING) {
            float frameTime = (float) (Math.sin((System.currentTimeMillis() % 400) / 400f * Math.PI * 2) * 0.5f + 0.5f);
            currentFrame = frameTime > 0.5f ? templarFrames[1] : templarFrames[0];
        }
    }

    public void renderShield(SpriteBatch batch, Player player) {
        TextureRegion shieldFrame = currentShieldFrame;

        // Update facing only while chasing
        if (state == TemplarState.CHASING) {
            recentFacing = (player.x + 16f) < (x + width / 2f);
        }

        // Use recent facing for scale and offset in all states
        float scaleY = recentFacing ? -1f : 1f;
        float offsetX = recentFacing ? -3f : 3f;

        // Calculate rotation only when chasing
        float rotation = frozenShieldRotation;
        if (state == TemplarState.CHASING) {
            rotation = (float) Math.toDegrees(Math.atan2(
                (player.y + 16f) - (y + height / 2f),
                (player.x + 16f) - (x + width / 2f)
            ));

            // Clamp angle to [-180, 180]
            if (rotation > 180f) rotation -= 360f;
            if (rotation < -180f) rotation += 360f;

            frozenShieldRotation = rotation;
        }

        batch.draw(
            shieldFrame,
            x + offsetX - shieldFrame.getRegionWidth() / 2f,
            y - shieldFrame.getRegionHeight() / 2f,
            shieldFrame.getRegionWidth() / 2f,
            shieldFrame.getRegionHeight() / 2f,
            shieldFrame.getRegionWidth(),
            shieldFrame.getRegionHeight(),
            1.1f,
            1.1f * scaleY,
            rotation
        );
    }

}
