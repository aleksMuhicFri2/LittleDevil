package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Templar extends Enemy {

    private Texture shieldSpritesheet;
    public TextureRegion currentShieldFrame;
    private TextureRegion[] templarFrames, shieldFrames;

    public float BASE_HP = 200f;
    public float WAVE_HP_INCREMENT = 20f;

    public float BASE_DAMAGE = 20f;
    public float WAVE_DAMAGE_INCREMENT = 2f;

    public enum TemplarState {
        CHASING,
        CHANNELING,
        BASHING,
        POST_HIT_PAUSE
    }

    private TemplarState state = TemplarState.CHASING;
    private float stateTimer = 0f;

    private final float ATTACK_RANGE = 40f;
    private final float CHANNEL_TIME = 0.7f;
    private final float BASH_DURATION = 0.4f;
    private final float BASH_SPEED = moveSpeed * 6f;
    private final float BASH_COOLDOWN = 3f;
    private float bashCooldownTimer = 0f;
    private final float POST_HIT_PAUSE = 1f;

    private float frozenShieldRotation = 0f;

    private Vector2 bashDir = new Vector2();
    private boolean facingLeft = false;
    private boolean recentFacing;

    private float shieldHitWidth = 20f;
    private float shieldHitHeight = 20f;
    private boolean hitPlayerThisBash = false;

    private final Sound bashSound;

    // For Death Animation
    private float helmetY;
    private float helmetVelocity = 0f;
    private final float GRAVITY = 300f; // tweak for drop speed
    private final float GROUND_OFFSET = 16f;

    public Templar(float x, float y, GameWorld world) {
        super(x, y, "Spritesheets/templarSpritesheet.png", world);
        shieldSpritesheet = new Texture("Spritesheets/shieldSpritesheet.png");
        bashSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/shieldBash.mp3"));

        HP = BASE_HP + (world.wave - 1) * WAVE_HP_INCREMENT;
        damage = BASE_DAMAGE +  (world.wave - 1) * WAVE_DAMAGE_INCREMENT;

        moveSpeed = 30f;
        knockbackDecay = 150f;

        hitboxOffsetX = -12;
        hitboxOffsetY = -16;
        hitboxWidth = 24;
        hitboxHeight = 20;

        guaranteedOrbsCounts = new float[] {3f, 0f, 0f};
        firstExtraChances = new float[] {0.5f, 0.4f, 0.05f};
        orbProbabilityDecays = new float[] {0.3f, 0.2f, 0.1f};

        templarFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) templarFrames[i] = new TextureRegion(spriteSheet, i * 32, 0, 32, 32);
        currentFrame = templarFrames[0];

        shieldFrames = new TextureRegion[9];
        for (int i = 0; i < 9; i++) shieldFrames[i] = new TextureRegion(shieldSpritesheet, i * 32, 0, 32, 32);
        currentShieldFrame = shieldFrames[0];
    }

    @Override
    public void update(float delta, Player player, GameWorld gameWorld, GameScreen gameScreen) {

        if (isAlive) {

            if (player.isUnreachable(gameWorld)) {

                state = TemplarState.CHASING;
                stateTimer = 0f;

                // Prevent an instant bash the millisecond player walks out
                bashCooldownTimer = Math.max(bashCooldownTimer, 1f);

                // Move normally
                followPath(gameWorld, delta);
                applySeparationForce(gameWorld);
                applyKnockback(delta, gameWorld);
                updateAnimation(delta, player);

                return; // Skip ALL attack logic
            }

            if (bashCooldownTimer > 0) bashCooldownTimer -= delta;

            checkBashHit(player, gameScreen);
            stateTimer -= delta;

            switch (state) {

                case CHASING -> {
                    followPath(gameWorld, delta);
                    currentShieldFrame = shieldFrames[0];

                    if (distanceToPlayer(player) < ATTACK_RANGE && bashCooldownTimer <= 0f) {
                        state = TemplarState.CHANNELING;
                        stateTimer = CHANNEL_TIME;
                    }
                }

                case CHANNELING -> {
                    float progress = 1f - stateTimer / CHANNEL_TIME;
                    int frameIndex = Math.min(4, (int)(progress * 5f));
                    currentShieldFrame = shieldFrames[frameIndex];

                    // smarter direction prediction
                    if (stateTimer >= CHANNEL_TIME - CHANNEL_TIME * (Math.min(intelligence, 10) / 10)) {
                        bashDir.set(player.x - x, player.y - y).nor();
                    }

                    if (stateTimer <= 0f) {
                        state = TemplarState.BASHING;
                        stateTimer = BASH_DURATION;
                    }
                }

                case BASHING -> {
                    float progress = 1f - stateTimer / BASH_DURATION;
                    float decel = 1f - (progress * progress);

                    float moveX = bashDir.x * BASH_SPEED * decel * delta;
                    float moveY = bashDir.y * BASH_SPEED * decel * delta;

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
                    hitPlayerThisBash = false;
                    if (stateTimer <= 0f) state = TemplarState.CHASING;
                }
            }

            applySeparationForce(gameWorld);
            applyKnockback(delta, gameWorld);
            handleAttack(player, gameScreen, gameWorld);
            updateAnimation(delta, player);

        } else {
            // DEATH ANIMATION
            if (helmetY == 0f) helmetY = y;
            float groundY = helmetY - GROUND_OFFSET;

            helmetVelocity += GRAVITY * delta;
            y -= helmetVelocity * delta;

            if (y <= groundY) {
                y = groundY;
                helmetVelocity = 0f;
                alpha -= 0.7f * delta;
            }

            if (alpha <= 0f) {
                alpha = 0f;
                gameWorld.removeEnemy(this);
            }
        }
    }


    // Handles what happens when the enemy is hit
    @Override
    public void handleAttack(Player player, GameScreen gameScreen, GameWorld gameWorld) {
        if (!player.isAttacking) hitThisAttack = false;
        if (player.isAttacking && !hitThisAttack) {
            float dx = x - player.x;
            float dy = y - player.y;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);
            if (distance <= player.range) {
                dx /= distance;
                dy /= distance;
                float dot = player.attackDirX * dx + player.attackDirY * dy;
                if (dot > 0.3f) {
                    hitFlashTime = hitFlashDuration;
                    hitThisAttack = true;
                    boolean applyKnockbackFlag = state != TemplarState.CHANNELING && state != TemplarState.BASHING;
                    templarHit(dx, dy, applyKnockbackFlag, 0.1f, 0.15f, player, gameScreen, gameWorld);
                    gameWorld.combo += 1;
                }
            }
        }
    }

    // Applies damage suffered from player, spawns damageText and handles death if HP <= 0
    private void templarHit(float dx, float dy, boolean applyKnockback, float freezeDuration, float cameraShakeDuration,
                            Player player, GameScreen gameScreen, GameWorld gameWorld) {
        if (applyKnockback) applyHitKnockback(dx, dy);
        playHitSound();
        gameScreen.triggerTimePause(freezeDuration, cameraShakeDuration);

        float damageMultiplier = player.damageMultiplier;

        // gets correct direction of templar
        boolean playerHitsFront = recentFacing ? player.x < x : player.x > x;

        boolean crit = Math.random() <= player.critChance;
        Color textColor = new Color(1f, 0f, 0f, 1f);
        float scale;

        // If player hits shield
        if (playerHitsFront) {
            damageMultiplier *= 0.7f;
            textColor.set(0.8f, 0.8f, 1f, 1f);
            scale = 1f;
        } else { // If player hits from the back
            damageMultiplier *= 1.3f;
            textColor.set(1f, 0f, 0f, 1f);
            scale = 1.15f;
        }
        if (crit) { // Additional damage on crit
            damageMultiplier *= player.critMultiplier;
            textColor.set(1f, 0.4f, 0.2f, 1f);
            scale = 1.3f;
        }

        int damage = (int)(player.damage * damageMultiplier);
        gameWorld.spawnDamage(x, y + height / 1.5f, damage, textColor, scale);
        HP -= damage;
        if (HP <= 0 && isAlive) die(gameWorld, player);
    }

    // Handles event when templar hits player with shield
    private void checkBashHit(Player player, GameScreen gameScreen) {
        if (state != TemplarState.BASHING || hitPlayerThisBash) return;

        float offsetX = recentFacing ? -5f : 5f;
        float centerX = x + offsetX;
        float centerY = y;

        float hitboxX = centerX - shieldHitWidth / 2f;
        float hitboxY = centerY - shieldHitHeight / 2f;

        float playerLeft = player.x;
        float playerRight = player.x + player.hitBoxWidth;
        float playerBottom = player.y;
        float playerTop = player.y + player.hitBoxHeight;

        boolean overlapX = playerRight > hitboxX && playerLeft < hitboxX + shieldHitWidth;
        boolean overlapY = playerTop > hitboxY && playerBottom < hitboxY + shieldHitHeight;

        if (overlapX && overlapY) {
            hitPlayerThisBash = true;
            player.currentHP -= damage;
            bashSound.play(0.2f);
            gameScreen.triggerTimePause(0.2f, 0.2f);

            float dx = player.x - x;
            float dy = player.y - y;
            player.applyKnockBack(dx, dy, 140f);
        }
    }

    // Updates Templar animation based on his state and if hit by player
    public void updateAnimation(float delta, Player player) {
        if(!isAlive) return;
        currentFrame = templarFrames[0];

        if (hitFlashTime > 0) {
            currentFrame = templarFrames[2];
            currentShieldFrame = shieldFrames[7];
            hitFlashTime -= delta;
            return;
        }

        boolean shouldFaceLeft = (player.x + 32 / 2f) < (x + width / 2f);
        if (shouldFaceLeft != facingLeft) {
            facingLeft = shouldFaceLeft;
            for (int i = 0; i <= 2; i++) templarFrames[i].flip(true, false);
        }

        if (state == TemplarState.CHASING || state == TemplarState.BASHING) {
            float frameTime = (float)(Math.sin((System.currentTimeMillis() % 400) / 400f * Math.PI * 2) * 0.5f + 0.5f);
            currentFrame = frameTime > 0.5f ? templarFrames[1] : templarFrames[0];
        }
    }

    // Function for rendering shield and rotating it based on player
    public void renderShield(SpriteBatch batch, Player player) {
        TextureRegion shieldFrame = currentShieldFrame;
        if (state == TemplarState.CHASING) {
            recentFacing = (player.x + 16f) < (x + width / 2f);
        }

        float scaleY = recentFacing ? -1f : 1f;
        float offsetX = recentFacing ? -3f : 3f;

        float rotation = frozenShieldRotation;
        if (state == TemplarState.CHASING) {
            rotation = (float)Math.toDegrees(Math.atan2(
                (player.y + 16f) - (y + height / 2f),
                (player.x + 16f) - (x + width / 2f)
            ));
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

    public void renderShieldHitbox(SpriteBatch batch, Texture pixel) {
        if (state != TemplarState.BASHING) return;
        float offsetX = recentFacing ? -3f : 3f;
        float centerX = x + offsetX;
        float centerY = y;
        float hitboxX = centerX - shieldHitWidth / 2f;
        float hitboxY = centerY - shieldHitHeight / 2f;

        batch.setColor(1f, 1f, 0f, 0.3f);
        batch.draw(pixel, hitboxX, hitboxY, shieldHitWidth, shieldHitHeight);
        batch.setColor(Color.WHITE);
    }

    // Function that handles templar death and removal from scene
    private void die(GameWorld gameWorld, Player player) {
        if (!isAlive) return;
        isAlive = false;

        currentFrame = templarFrames[3];
        currentShieldFrame = shieldFrames[8];

        helmetY = y;
        helmetVelocity = 10f;
        gameWorld.spawnOrbs(this, player);
    }


}
