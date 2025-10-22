package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Player {

    // Position
    public float x, y, prevX, prevY;

    // Stats
    public float baseHP = 200f, currentHP = baseHP;
    public float armor = 10f;
    public float baseSpeed = 60f, speed = baseSpeed;
    private float speedMultiplier = 1f, speedBoostTimer = 0f;
    private float baseAttackSpeed = 0.5f, attackSpeed = baseAttackSpeed, attackCooldownTimer = 0f;
    private float baseDamage = 60f, damage = baseDamage, damageMultiplier = 1f, damageBoostTimer = 0f;
    private float baseLifesteal = 0f, lifesteal = baseLifesteal, lifestealMultiplier = 1f, lifestealBoostTimer = 0f;

    public float baseEnergy = 100f, currentEnergy = baseEnergy;

    // Collision
    public int collisionOffsetX = -4, collisionOffsetY = -16, collisionWidth = 8, collisionHeight = 4;

    // Dash
    private boolean isDashing = false;
    private float dashTime = 0f, dashDuration = 0.25f, dashCooldown = 0.8f, dashTimer = 0f, dashSpeed;
    private float dashDirX = 0, dashDirY = 0;

    // Attack
    public boolean isAttacking = false;
    private float attackTimer = 0f, attackDuration = 0.2f;
    public float attackDirX = 0, attackDirY = 0;
    private float attackAngle = 0f;
    public float range = 28f;

    // Animation
    private Texture spriteSheet, swordSheet;
    private TextureRegion[] frames, swordFrames;
    private TextureRegion currentFrame, currentSwordFrame;
    private boolean facingRight = true;
    private float animationTimer = 0f, dashAnimTimer = 0f;
    private int frameIndex = 0;

    // Sounds
    private Sound walkSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/walk.mp3"));
    private Sound attackSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/swordAttack.mp3"));
    private float walkStepTimer = 0f;
    private final float walkStepInterval = 0.3f;

    public Player(float startX, float startY, String spriteSheetPath) {
        this.x = startX;
        this.y = startY;

        // Load player frames
        spriteSheet = new Texture(spriteSheetPath);
        frames = new TextureRegion[9];
        for (int i = 0; i < 9; i++) frames[i] = new TextureRegion(spriteSheet, i * 32, 0, 32, 32);
        currentFrame = frames[7];  // start at attack frame 7
        currentSwordFrame = null;

        // Load sword frames
        swordSheet = new Texture("Spritesheets/swordSpritesheet.png");
        swordFrames = new TextureRegion[2];
        for (int i = 0; i < 2; i++) swordFrames[i] = new TextureRegion(swordSheet, i * 64, 0, 64, 64);
        currentSwordFrame = swordFrames[0];

        dashSpeed = baseSpeed * 3;

    }

    public void update(float delta, GameWorld world) {
        // Update timers
        if (attackCooldownTimer > 0f) attackCooldownTimer -= delta;
        if (dashTimer > 0) dashTimer -= delta;
        if (speedBoostTimer > 0) { speedBoostTimer -= delta; if (speedBoostTimer <= 0) speedMultiplier = 1f; }
        if (damageBoostTimer > 0) { damageBoostTimer -= delta; if (damageBoostTimer <= 0) damageMultiplier = 1f; }
        if (lifestealBoostTimer > 0) { lifestealBoostTimer -= delta; if (lifestealBoostTimer <= 0) lifestealMultiplier = 1f; }

        // save position in case of collision
        prevX = x;
        prevY = y;

        // Movement input
        float moveX = 0, moveY = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1;

        if (moveX > 0) facingRight = true; else if (moveX < 0) facingRight = false;

        float len = (float)Math.sqrt(moveX * moveX + moveY * moveY);
        if (len > 0) { moveX /= len; moveY /= len; }

        // Stairs check
        boolean onStairs = isOnStairs(world);
        speed = onStairs ? baseSpeed * speedMultiplier * 2f / 3f : baseSpeed * speedMultiplier;

        // Dash
        if (!isDashing && dashTimer <= 0 && (moveX != 0 || moveY != 0) && Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            performDash(moveX, moveY);

        // Attack
        if (!isDashing && !isAttacking && attackCooldownTimer <= 0 && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))
            performAttack();

        // Apply movement
        if (isDashing) {
            dashTime -= delta;
            x += dashDirX * dashSpeed * delta;
            y += dashDirY * dashSpeed * delta;
            if (dashTime <= 0) { isDashing = false; dashTimer = dashCooldown; }
        } else if (isAttacking) {
            attackTimer -= delta;
            x -= attackDirX * 10f * delta;
            y -= attackDirY * 10f * delta;
            if (attackTimer <= 0) isAttacking = false;
            updateAttackAnimation();
        } else {
            x += moveX * speed * delta;
            y += moveY * speed * delta;
        }

        // Collision sliding
        if (isBlocked(x, prevY, world)) x = prevX;
        if (isBlocked(prevX, y, world)) y = prevY;

        // Walking sound
        boolean moving = moveX != 0 || moveY != 0;
        if (moving) {
            walkStepTimer -= delta;
            if (walkStepTimer <= 0f) {
                float randomVolume = 0.05f + (float)Math.random() * 0.02f;
                float randomPitch = 0.8f + (float)Math.random() * 0.5f;
                walkSound.play(randomVolume, randomPitch, 0f);
                walkStepTimer = walkStepInterval;
            }
        } else {
            walkStepTimer = 0f;
        }

        updateAnimation(delta, moving);
    }

    // --- Collision helpers ---
    private boolean checkCollision(float testX, float testY, GameWorld.TileType type, GameWorld world) {
        int left = (int)((testX + collisionOffsetX) / world.tileSize);
        int right = (int)((testX + collisionOffsetX + collisionWidth) / world.tileSize);
        int bottom = (int)((testY + collisionOffsetY) / world.tileSize);
        int top = (int)((testY + collisionOffsetY + collisionHeight) / world.tileSize);

        for (int y = bottom; y <= top; y++)
            for (int x = left; x <= right; x++)
                if (world.isTileType(x, y, type)) return true;
        return false;
    }

    // helper functions that return if player is interacting with the environment
    public boolean isBlocked(float x, float y, GameWorld world) { return checkCollision(x, y, GameWorld.TileType.BLOCK, world); }
    public boolean isOnStairs(GameWorld world) { return checkCollision(x, y, GameWorld.TileType.STAIRS, world); }
    public boolean isOnBoost(GameWorld world) { return checkCollision(x, y, GameWorld.TileType.BOOST, world); }
    public boolean isOnAltar(GameWorld world) { return checkCollision(x, y, GameWorld.TileType.ALTAR, world); }

    // --- Animation of player ---
    private void updateAnimation(float delta, boolean moving) {
        animationTimer += delta;
        if (isDashing) {
            dashAnimTimer += delta;
            if (dashAnimTimer < dashDuration) currentFrame = frames[6];
            else dashAnimTimer = 0f;
        } else if (moving) {
            if (animationTimer > 0.1f) {
                frameIndex++; if (frameIndex < 2) frameIndex = 2; if (frameIndex > 4) frameIndex = 3;
                currentFrame = frames[frameIndex]; animationTimer = 0;
            }
        } else if (animationTimer > 0.4f) {
            frameIndex = frameIndex == 0 ? 1 : 0; currentFrame = frames[frameIndex]; animationTimer = 0;
        }
        if ((facingRight && currentFrame.isFlipX()) || (!facingRight && !currentFrame.isFlipX())) currentFrame.flip(true, false);
    }

    // --- Dash ---
    private void performDash(float moveX, float moveY) {
        isDashing = true; dashTime = dashDuration;
        float len = (float)Math.sqrt(moveX*moveX + moveY*moveY);
        dashDirX = len != 0 ? moveX / len : 0;
        dashDirY = len != 0 ? moveY / len : 0;
    }

    // player attack
    private void performAttack() {
        isAttacking = true;
        attackTimer = attackDuration;
        attackCooldownTimer = attackSpeed;

        // Lock the angle at attack start
        attackAngle = GameScreen.getMouseAngle();

        float rad = (float)Math.toRadians(attackAngle);
        attackDirX = (float)Math.cos(rad);
        attackDirY = (float)Math.sin(rad);

        facingRight = !(attackAngle > 90 && attackAngle < 270);

        // Play attack sound
        float randomPitch = 0.8f + (float)Math.random() * 0.4f;
        attackSound.play(0.5f, randomPitch, 0);
    }

    private void updateAttackAnimation() {
        float progress = attackDuration - attackTimer;
        if (progress < attackDuration / 2f) { currentFrame = frames[7]; currentSwordFrame = swordFrames[0]; }
        else { currentFrame = frames[8]; currentSwordFrame = swordFrames[1]; }

        if ((facingRight && currentFrame.isFlipX()) || (!facingRight && !currentFrame.isFlipX())) currentFrame.flip(true, false);
        if ((facingRight && currentSwordFrame.isFlipX()) || (!facingRight && !currentSwordFrame.isFlipX())) currentSwordFrame.flip(true, false);
    }


    // --- Boosts ---
    public void boostSpeed(float time, float multiplier){ speedBoostTimer = time; speedMultiplier = multiplier; }
    public void boostDamage(float time, float multiplier){ damageBoostTimer = time; damageMultiplier = multiplier; }

    // Draw sword
    private void drawSword(SpriteBatch batch, TextureRegion swordFrame) {
        float angleOffset = facingRight ? 40f : 140f;
        float originX = swordFrame.getRegionWidth() / 2f;
        float originY = swordFrame.getRegionHeight() / 2f;
        float drawX = x - originX;
        float drawY = y - originY - 10f;

        batch.draw(
            swordFrame,
            drawX, drawY,
            originX, originY,
            swordFrame.getRegionWidth(), swordFrame.getRegionHeight(),
            1f, 1f,
            attackAngle + angleOffset
        );
    }

    public void renderSword(SpriteBatch batch) {
        if (isAttacking && currentSwordFrame != null) drawSword(batch, currentSwordFrame);
    }

    // --- Getters ---
    public TextureRegion getCurrentFrame() { return currentFrame; }

    public void dispose() {
        spriteSheet.dispose();
        swordSheet.dispose();
        attackSound.dispose();
        walkSound.dispose();
    }
}
