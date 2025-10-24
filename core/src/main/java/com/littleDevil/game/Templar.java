package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Templar extends Enemy {

    private Texture shieldSpritesheet;
    private TextureRegion[] templarFrames, shieldFrames;
    public enum TemplarState {
        CHASING,
        CHANNELING,
        BASHING
    }

    private TemplarState state = TemplarState.CHASING;
    private float stateTimer = 0f;

    private final float ATTACK_RANGE = 30f;
    private final float CHANNEL_TIME = 1f;
    private final float BASH_DURATION = 0.3f;
    private final float BASH_SPEED = moveSpeed * 3f;
    private final float BASH_COOLDOWN = 2f;
    private float bashCooldownTimer = 0f;

    private Vector2 bashDir = new Vector2();
    private boolean facingLeft = false; // add this as a class field

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

        templarFrames = new TextureRegion[9];
        for (int i = 0; i < 3; i++) templarFrames[i] = new TextureRegion(spriteSheet, i * 32, 0, 32, 32);
        currentFrame = templarFrames[0];

        shieldFrames = new TextureRegion[8];
        for (int i = 0; i < 8; i++) templarFrames[i] = new TextureRegion(spriteSheet, i * 32, 0, 32, 32);
        currentFrame = templarFrames[0];
    }

    // Overrides the method in abstract Enemy class
    @Override
    public void update(float delta, Player player, GameWorld gameWorld, GameScreen  gameScreen) {
        if (!isAlive) return;
        if (bashCooldownTimer > 0) bashCooldownTimer -= delta;

        stateTimer -= delta;
        switch (state) {
            case CHASING -> {
                followPath(gameWorld, delta);
                if (distanceToPlayer(player) < ATTACK_RANGE && bashCooldownTimer <= 0f) {
                    state = TemplarState.CHANNELING;
                    stateTimer = CHANNEL_TIME;
                    bashDir.set(player.x - x, player.y - y).nor();
                }
            }

            case CHANNELING -> {
                // Stand still dramatically
                if (stateTimer <= 0f) {
                    state = TemplarState.BASHING;
                    stateTimer = BASH_DURATION;
                }
            }

            case BASHING -> {
                float moveX = bashDir.x * BASH_SPEED * delta;
                float moveY = bashDir.y * BASH_SPEED * delta;

                // move with collision check
                moveWithCollision(moveX, moveY, gameWorld);

                if (stateTimer <= 0f) {
                    bashCooldownTimer = BASH_COOLDOWN;
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
            hitFlashTime -= delta;
            return;
        }

        // Determine if the templar should face left
        boolean shouldFaceLeft = (player.x + 32 / 2f) < (x + width / 2f);
        if (shouldFaceLeft != facingLeft) {
            // Only flip when changing direction
            facingLeft = shouldFaceLeft;
            templarFrames[0].flip(true, false);
            templarFrames[1].flip(true, false);
            templarFrames[2].flip(true, false); // optional: flip hurt frame too
        }

        // Animate only when moving (chasing or bashing)
        if (state == TemplarState.CHASING || state == TemplarState.BASHING) {
            // Basic 2-frame walk cycle between base(0) and walk(1)
            float frameTime = (float) (Math.sin((System.currentTimeMillis() % 400) / 400f * Math.PI * 2) * 0.5f + 0.5f);
            currentFrame = frameTime > 0.5f ? templarFrames[1] : templarFrames[0];
        }

        // CHANNELING stays idle frame
    }


    /*
    @Override
    public void attack(){}
     */
}
