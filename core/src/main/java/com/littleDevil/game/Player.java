package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class Player {

    private final GameWorld gameWorld;

    // --- POSITIONS & DIMENSIONS ---
    public float x, y, prevX, prevY;
    public int width = 32, height = 32;
    // Collision offsets
    public int collisionOffsetX = -4, collisionOffsetY = -16;
    public int collisionWidth = 8, collisionHeight = 4;
    public int hitBoxWidth = 8, hitBoxHeight = 8;

    // --- STATS ---
    public float baseHP = 200f, currentHP = baseHP;
    public float baseEnergy = 100f, currentEnergy = 0f;
    public float armor = 10f;
    public float baseSpeed = 50f, speed = baseSpeed;
    public float baseDamage = 60f, damage = baseDamage;
    public float luck = 0.05f, critChance = luck, critMultiplier = 1.5f;

    public float baseLifesteal = 0f;
    public float lifesteal = baseLifesteal;
    private float lifestealMultiplier = 1f, lifestealBoostTimer = 0f;

    // Leveling
    public float level = 1f, currentXp = 0f, neededXp = 15f, previousNeededXp = 15f;
    public int skillPoints = 0;

    // Upgrade Levels
    public int attackLevel = 0, defenseLevel = 0, luckLevel = 0, agilityLevel = 0, superLevel = 0;

    // --- TIMERS & MULTIPLIERS ---
    private float speedMultiplier = 1f, speedBoostTimer = 0f;
    public float damageMultiplier = 1f, damageBoostTimer = 0f;


    // (Removed unused lifesteal variables to clean up, unless you plan to use them soon)

    // --- COMBAT STATE ---
    public boolean isAttacking = false;
    public boolean isDashing = false;
    private boolean facingRight = true;

    // Dash Logic
    private float dashTime = 0f;
    private final float dashDuration = 0.3f, dashCooldown = 0.8f;
    private float dashTimer = 0f;
    private final float dashSpeed;
    private float dashDirX = 0, dashDirY = 0;

    // Attack Logic
    private float attackTimer = 0f;
    private final float attackDuration = 0.2f;
    private float baseAttackSpeed = 0.5f, attackCooldownTimer = 0f;
    public float attackDirX = 0, attackDirY = 0;
    private float attackAngle = 0f;
    public float range = 30f;

    // Knockback
    private float knockbackX = 0f, knockbackY = 0f;
    private final float knockbackDecay = 150f;
    private float hitFlashTimer = 0f;
    private final float hitFlashDuration = 0.1f;

    // --- RENDERING ---
    private final Texture spriteSheet, swordSheet;
    private final TextureRegion[] frames, swordFrames;
    private TextureRegion currentFrame, currentSwordFrame;
    private float animationTimer = 0f, dashAnimTimer = 0f;
    private int frameIndex = 0;

    // UI Helpers
    public float displayHP, displayEnergy, displayXp;
    public boolean levelUp = false;
    public boolean xpOverflowAnimating = false;

    // --- AUDIO ---
    private final Sound walkSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/walk.mp3"));
    private final Sound attackSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/swordAttack.mp3"));
    private float walkStepTimer = 0f;
    private final float walkStepInterval = 0.3f;

    public enum StatType { ATTACK, AGILITY, DEFENSE, LUCK, SUPER }

    public Player(float startX, float startY, String spriteSheetPath, GameWorld gameWorld) {
        this.gameWorld = gameWorld;
        this.x = startX;
        this.y = startY;

        // Initialize Textures
        spriteSheet = new Texture(spriteSheetPath);
        frames = new TextureRegion[10];
        for (int i = 0; i < 10; i++) frames[i] = new TextureRegion(spriteSheet, i * 32, 0, 32, 32);
        currentFrame = frames[0];

        swordSheet = new Texture("Spritesheets/swordSpritesheet.png");
        swordFrames = new TextureRegion[2];
        for (int i = 0; i < 2; i++) swordFrames[i] = new TextureRegion(swordSheet, i * 64, 0, 64, 64);
        currentSwordFrame = swordFrames[0];

        dashSpeed = baseSpeed * 4f;

        // Init UI values
        displayHP = currentHP;
        displayEnergy = currentEnergy;
        displayXp = currentXp;
    }

    public void update(float delta, GameWorld gameWorld) {
        updateTimers(delta);

        // Save position for collision resolution
        prevX = x;
        prevY = y;

        // 1. Calculate Movement Input
        float moveX = 0, moveY = 0;
        if (hitFlashTimer <= 0) {
            if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1;
        }

        // Normalize vector
        if (moveX != 0 || moveY != 0) {
            float len = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            moveX /= len;
            moveY /= len;
            // Update facing direction
            if (moveX > 0) facingRight = true;
            else if (moveX < 0) facingRight = false;
        }

        // 2. Handle Actions (Dash / Attack)
        handleActions(moveX, moveY);

        // 3. Apply Physics & Position
        applyMovement(delta, moveX, moveY, gameWorld);

        // 4. Resolve Environment (Collision/Knockback)
        resolveCollisions(gameWorld);
        updateKnockback(delta, gameWorld);

        // 5. Audio & Visuals
        updateAudio(delta, moveX, moveY);
        updateAnimation(delta, (moveX != 0 || moveY != 0));
    }

    private void updateTimers(float delta) {
        if (attackCooldownTimer > 0f) attackCooldownTimer -= delta;
        if (dashTimer > 0) dashTimer -= delta;
        if (hitFlashTimer > 0f) hitFlashTimer -= delta;

        // Stat Boosts
        if (speedBoostTimer > 0) {
            speedBoostTimer -= delta;
            if (speedBoostTimer <= 0) speedMultiplier = 1f;
        }
        if (damageBoostTimer > 0) {
            damageBoostTimer -= delta;
            if (damageBoostTimer <= 0) damageMultiplier = 1f;
        }
        if (lifestealBoostTimer > 0) {
            lifestealBoostTimer -= delta;
            if (lifestealBoostTimer <= 0) lifestealMultiplier = 1f;
        }
    }

    private void handleActions(float moveX, float moveY) {
        // Trigger Dash
        if (!isDashing && dashTimer <= 0 && (moveX != 0 || moveY != 0) && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            performDash(moveX, moveY);
        }

        // Trigger Attack
        if (canAttack()) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                performAttack();
            }
        }
    }

    private boolean canAttack() {
        return !isDashing && !isAttacking
            && !isOnStairs(gameWorld)
            && !isOnAltar(gameWorld)
            && !isUnreachable(gameWorld)
            && attackCooldownTimer <= 0;
    }

    private void applyMovement(float delta, float moveX, float moveY, GameWorld gameWorld) {
        // Priority 1: Dashing
        if (isDashing) {
            dashTime -= delta;
            x += dashDirX * dashSpeed * delta;
            y += dashDirY * dashSpeed * delta;
            if (dashTime <= 0) {
                isDashing = false;
                dashTimer = dashCooldown;
            }
            return;
        }

        // Priority 2: Attacking (Lunging logic)
        if (isAttacking) {
            attackTimer -= delta;
            x -= attackDirX * 10f * delta; // Small backward recoil or lunge adjustment
            y -= attackDirY * 10f * delta;
            if (attackTimer <= 0) isAttacking = false;
            updateAttackAnimation();
            return;
        }

        // Priority 3: Normal Walking
        float currentSpeed = baseSpeed * speedMultiplier;
        if (isOnStairs(gameWorld)) currentSpeed *= (2f / 3f); // Slow down on stairs

        x += moveX * currentSpeed * delta;
        y += moveY * currentSpeed * delta;
    }

    private void resolveCollisions(GameWorld gameWorld) {
        if (isBlocked(x, prevY, gameWorld)) x = prevX;
        if (isBlocked(prevX, y, gameWorld)) y = prevY;
    }

    private void updateAudio(float delta, float moveX, float moveY) {
        boolean moving = (moveX != 0 || moveY != 0);
        if (moving) {
            walkStepTimer -= delta;
            if (walkStepTimer <= 0f) {
                float randomVolume = 0.05f + MathUtils.random(0.02f);
                float randomPitch = 0.8f + MathUtils.random(0.5f);
                walkSound.play(randomVolume, randomPitch, 0f);
                walkStepTimer = walkStepInterval;
            }
        } else {
            walkStepTimer = 0f;
        }
    }

    // --- ACTIONS ---

    private void performDash(float moveX, float moveY) {
        isDashing = true;
        dashTime = dashDuration;

        // Ensure we dash in the moving direction
        if (moveX == 0 && moveY == 0) {
            dashDirX = facingRight ? 1 : -1;
            dashDirY = 0;
        } else {
            dashDirX = moveX;
            dashDirY = moveY;
        }
    }

    private void performAttack() {
        isAttacking = true;
        attackTimer = attackDuration;
        attackCooldownTimer = baseAttackSpeed; // Use the stored attack speed var

        // Calculate attack direction based on mouse
        attackAngle = gameWorld.gameScreen.getMouseAngle();
        float rad = MathUtils.degreesToRadians * attackAngle;
        attackDirX = MathUtils.cos(rad);
        attackDirY = MathUtils.sin(rad);

        // Update facing direction based on attack
        facingRight = !(attackAngle > 90 && attackAngle < 270);

        float randomPitch = 0.8f + MathUtils.random(0.4f);
        attackSound.play(0.5f, randomPitch, 0);
    }

    // --- ANIMATION ---

    private void updateAnimation(float delta, boolean moving) {
        if (hitFlashTimer > 0) {
            setFrame(9);
            return;
        }

        animationTimer += delta;

        if (isDashing) {
            dashAnimTimer += delta;
            if (dashAnimTimer < dashDuration) setFrame(6);
            else dashAnimTimer = 0f;
        } else if (moving) {
            if (animationTimer > 0.1f) {
                frameIndex++;
                if (frameIndex < 2) frameIndex = 2;
                if (frameIndex > 4) frameIndex = 3;
                setFrame(frameIndex);
                animationTimer = 0;
            }
        } else if (animationTimer > 0.4f) {
            // Idle animation
            frameIndex = (frameIndex == 0) ? 1 : 0;
            setFrame(frameIndex);
            animationTimer = 0;
        }
    }

    private void updateAttackAnimation() {
        float progress = attackDuration - attackTimer;
        if (progress < attackDuration / 2f) {
            setFrame(7);
            currentSwordFrame = swordFrames[0];
        } else {
            setFrame(8);
            currentSwordFrame = swordFrames[1];
        }
        flipSwordIfNeeded();
    }

    // Helper to set frame and handle flipping automatically
    private void setFrame(int index) {
        currentFrame = frames[index];
        if ((facingRight && currentFrame.isFlipX()) || (!facingRight && !currentFrame.isFlipX())) {
            currentFrame.flip(true, false);
        }
    }

    private void flipSwordIfNeeded() {
        if (currentSwordFrame == null) return;
        if ((facingRight && currentSwordFrame.isFlipX()) || (!facingRight && !currentSwordFrame.isFlipX())) {
            currentSwordFrame.flip(true, false);
        }
    }

    // --- KNOCKBACK & UTILS ---

    private void updateKnockback(float delta, GameWorld gameWorld) {
        if (knockbackX == 0 && knockbackY == 0) return;

        float nextX = x + knockbackX * delta;
        float nextY = y + knockbackY * delta;

        // Apply knockback only if not blocked
        if (!isBlocked(nextX, y, gameWorld)) x = nextX;
        else knockbackX = 0;

        if (!isBlocked(x, nextY, gameWorld)) y = nextY;
        else knockbackY = 0;

        // Decay
        knockbackX = approachZero(knockbackX, knockbackDecay * delta);
        knockbackY = approachZero(knockbackY, knockbackDecay * delta);
    }

    private float approachZero(float value, float amount) {
        if (value > 0) return Math.max(0, value - amount);
        if (value < 0) return Math.min(0, value + amount);
        return 0;
    }

    public void applyKnockBack(float dirX, float dirY, float strength) {
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0) return;
        knockbackX = (dirX / len) * strength;
        knockbackY = (dirY / len) * strength;
        hitFlashTimer = hitFlashDuration;
    }

    // --- GAME LOGIC ---

    public void addXP(float value) {
        float XP_INCREASE_RATIO = 1.15f;
        currentXp += value;

        if (currentXp >= neededXp) {
            currentXp -= neededXp;
            previousNeededXp = neededXp;
            neededXp *= XP_INCREASE_RATIO;

            skillPoints++;
            applyPassiveLevelUpStats();

            levelUp = true;
            xpOverflowAnimating = true;
        }
    }

    private void applyPassiveLevelUpStats() {
        baseHP += 7.5f;
        currentHP += 7.5f;
        armor += 1f;

        baseSpeed += 2.0f;

        baseDamage += 2f;
        damage = baseDamage;

        baseLifesteal += 0.004f;
        lifesteal = baseLifesteal;

        luck += 0.006f;
        critChance = luck;


        if (baseEnergy > 30f) {
            baseEnergy -= 1f;
        }
    }

    public void heal(float amount, GameWorld gameWorld) {
        currentHP = Math.min(baseHP, currentHP + amount);
        gameWorld.healAnimations.add(new HealingAnimation(this, 2f, gameWorld.healingTexture));
    }

    public void loseHP(float amount) {
        // 1. DODGE CHECK
        if (MathUtils.random() < luck) {
            // Use our new spawnText function
            gameWorld.spawnText(x, y + height, "DODGE!", Color.CYAN, 0.6f);
            return;
        }

        float effectiveArmor = Math.min(armor, 90f); // Cap at 90% reduction
        float actualDamage = amount * (1f - (effectiveArmor / 100f));

        // 3. APPLY DAMAGE
        currentHP -= actualDamage;

        // 4. DEATH CHECK
        if (currentHP <= 0) {
            currentHP = 0;
            die(gameWorld);
        }
    }

    public void die(GameWorld gameWorld) {
        Preferences prefs = Gdx.app.getPreferences("MyGameInfo");
        int currentHighscore = prefs.getInteger("highscore", 0);
        int myScore = (int) gameWorld.score;

        if (myScore > currentHighscore) {
            prefs.putInteger("highscore", myScore);
            prefs.flush();
        }
        gameWorld.gameScreen.game.setScreen(new StartScreen(gameWorld.gameScreen.game));
    }

    public void onDealDamage(float damageDealt) {
        float stealAmount = damageDealt * (lifesteal * lifestealMultiplier);

        if (stealAmount >= 1f) {
            heal(stealAmount, gameWorld);
            gameWorld.spawnDamage(x - 5, y + height - 5, (int)stealAmount, Color.GREEN, 0.6f);
        }
    }

    // --- COLLISION CHECKS (Kept mostly as-is) ---

    private boolean checkCollision(float testX, float testY, GameWorld.TileType type, GameWorld gameWorld) {
        int left = (int)((testX + collisionOffsetX) / gameWorld.tileSize);
        int right = (int)((testX + collisionOffsetX + collisionWidth) / gameWorld.tileSize);
        int bottom = (int)((testY + collisionOffsetY) / gameWorld.tileSize);
        int top = (int)((testY + collisionOffsetY + collisionHeight) / gameWorld.tileSize);

        for (int y = bottom; y <= top; y++) {
            for (int x = left; x <= right; x++) {
                if (gameWorld.isTileType(x, y, type)) return true;
            }
        }
        return false;
    }

    public boolean isBlocked(float x, float y, GameWorld gameWorld) { return checkCollision(x, y, GameWorld.TileType.BLOCK, gameWorld); }
    public boolean isOnStairs(GameWorld gameWorld) { return checkCollision(x, y, GameWorld.TileType.STAIRS, gameWorld); }
    public boolean isUnreachable(GameWorld gameWorld) { return checkCollision(x, y, GameWorld.TileType.BLOCKENEMY, gameWorld); }
    public boolean isOnAltar(GameWorld gameWorld) { return checkCollision(x, y, GameWorld.TileType.ALTAR, gameWorld); }

    public boolean isOnBoost(Boost boost) {
        if (boost == null || boost.pickedUp) return false;
        // Simple AABB check
        return (x + hitBoxWidth > boost.getX()) && (x < boost.getX() + 16) &&
            (y + hitBoxHeight > boost.getY()) && (y < boost.getY() + 16);
    }

    // --- BOOSTS & UPGRADES ---

    public void boostSpeed(float time, float multiplier){ speedBoostTimer = time; speedMultiplier = multiplier;}
    public void boostDamage(float time, float multiplier){
        damageBoostTimer = time;
        damageMultiplier = multiplier;
        lifestealBoostTimer = time;
        lifestealMultiplier = multiplier;
    }

    public void upgradeStat(StatType stat) {
        if (skillPoints <= 0) return;

        boolean success = false;
        switch (stat) {
            case ATTACK:
                if (attackLevel < 7) {
                    attackLevel++;
                    baseDamage += 10f;
                    damage = baseDamage;

                    baseLifesteal += 0.02f;
                    lifesteal = baseLifesteal;
                    success = true;
                }
                break;
            case AGILITY:
                if (agilityLevel < 7) {
                    agilityLevel++;
                    baseSpeed += 8f;
                    success = true;
                }
                break;
            case DEFENSE:
                if (defenseLevel < 7) {
                    defenseLevel++;
                    baseHP += 30f;
                    currentHP += 30f;
                    armor += 4f;
                    success = true;
                }
                break;
            case LUCK:
                if (luckLevel < 7) {
                    luckLevel++;
                    luck += 0.08f;
                    critChance = luck;
                    success = true;
                }
                break;
            case SUPER:
                if (superLevel < 7) {
                    superLevel++;
                    baseEnergy -= 10;
                    success = true;
                }
                break;
        }
        if (success) {
            skillPoints--;
            System.out.println("Upgraded " + stat + "! Points remaining: " + skillPoints);
        }
    }

    // --- DRAWING ---

    public void renderSword(SpriteBatch batch) {
        if (isAttacking && currentSwordFrame != null) {
            float angleOffset = facingRight ? 40f : 140f;
            float originX = currentSwordFrame.getRegionWidth() / 2f;
            float originY = currentSwordFrame.getRegionHeight() / 2f;
            batch.draw(currentSwordFrame, x - originX, y - originY - 10f, originX, originY,
                currentSwordFrame.getRegionWidth(), currentSwordFrame.getRegionHeight(), 1f, 1f, attackAngle + angleOffset);
        }
    }

    public TextureRegion getCurrentFrame() { return currentFrame; }

    public void dispose() {
        spriteSheet.dispose();
        swordSheet.dispose();
        attackSound.dispose();
        walkSound.dispose();
    }
}
