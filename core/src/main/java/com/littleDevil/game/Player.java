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

    public float baseEnergy = 100f, currentEnergy = 80f;
    public boolean isSuperActive = false;
    public float superDurationTimer = 0f;
    private float totalSuperDuration = 0f;

    public float armor = 10f;
    public float baseSpeed = 70f, speed = baseSpeed;
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

    // --- COMBAT STATE --
    public boolean isAttacking = false;
    public boolean isDashing = false;
    private boolean facingRight = true;

    // Dash Logic
    private float dashTime = 0f;
    private final float dashDuration = 0.25f, dashCooldown = 0.8f;
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

    // --- AUGMENT FLAGS ---
    // These match the flags set in AugmentManager
    public boolean hasVampiric = false;
    public boolean hasBloodthirsty = false;
    public boolean hasLuckyThrees = false;
    public boolean hasVeteran = false;
    public boolean hasSlowGrowth = false;
    public boolean hasTheFlash = false;
    public boolean hasParkour = false;
    public boolean hasScaredyCat = false;
    public boolean hasSlowDangerous = false;
    public boolean hasGraveLooter = false;
    public boolean hasMasochist = false;
    public boolean hasLastStand = false;
    public boolean hasBoostedAnimal = false;
    public boolean hasComboGod = false;

    public int attackCount = 0; // For Lucky Threes

    public float bloodthirstyTimer = 0f;
    public float bloodthirstyDuration = 5f;
    public float bloodThirstyBoost = 1f;

    public int luckyThreesDmgBonus = (int)(baseDamage * 0.3f);
    public int luckyThreesHeal = (int)(baseDamage * 0.15f);

    public int veteranBoost = 2;
    public int veteranStacks = 0;
    public float veteranTimer = 0f;
    public final int MAX_VETERAN_STACKS = 10;
    public final float VETERAN_DURATION = 3.0f;

    public int slowGrowthBonus = 4;

    public float flashTimer = 0f;
    public float maxFlashTime = 6f;
    public float currentFlashDamageBonus = 1f;

    public int nearbyEnemies = 0;
    public float nearbyDistance = 50f;
    public float scaredyCatBoostPerEnemy = 15f;

    public float lastStandMaxBoost = 1.3f;
    public float lastStandMaxLifesteal = 0.1f;

    public float boostedAnimalBoostPermanent = 0.1f;
    public float boostedAnimalBoostLength = 15f;

    public float comboGodBoostPerCombo = 0.03f;

    private final float[][] LIGHT_POSITIONS = {
        {122f, 204f}, // LightCenterLeft
        {296f, 316f}, // LightCenterTop
        {456f, 204f}, // LightCenterRight
        {296f, 80f},  // LightCenterBot
        {20f, 352f},  // LampTopLeft
        {560f, 352f}, // LampTopRight
        {560f, 32f},  // LampBotRight
        {20f, 32f}    // LampBotLeft
    };


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
        updateNearbyEnemies(gameWorld);

        float comboMultiplier = 1f;
        if (hasComboGod && gameWorld.combo > 0) {
            comboMultiplier = (float) Math.pow(1f + comboGodBoostPerCombo, gameWorld.combo);
        }

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

        // --- THE FLASH LOGIC ---
        float flashSpeedMult = 1f;

        if (hasTheFlash) {
            boolean isMoving = (moveX != 0 || moveY != 0);

            if (isMoving && !isAttacking && !isDashing) {
                // Ramp up timer
                flashTimer += delta;
                if (flashTimer > maxFlashTime) flashTimer = maxFlashTime;
            } else if (!isAttacking) {
                // Reset if stopped (but don't reset here if attacking, handle that in performAttack)
                flashTimer = 0f;
            }

            // Calculate Speed Multiplier (Linear: 1x at 0s, 3x at 8s)
            // Formula: 1 + (Progress * 2)
            float progress = flashTimer / maxFlashTime;
            flashSpeedMult = 1f + (progress * 2f);
        }

        // 2. Handle Actions (Dash / Attack)
        handleActions(moveX, moveY);

        // 3. Apply Physics & Position
        applyMovement(delta, moveX, moveY, gameWorld, flashSpeedMult);

        if (isDashing) {
            checkDashCollisions(gameWorld);
        }

        // 4. Resolve Environment (Collision/Knockback)
        resolveCollisions(gameWorld);
        updateKnockback(delta, gameWorld);

        // 5. Audio & Visuals
        updateAudio(delta, moveX, moveY);
        updateAnimation(delta, (moveX != 0 || moveY != 0));

        updateSuperLogic(delta);
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
        if (bloodthirstyTimer > 0) {
            bloodthirstyTimer -= delta;
            if (bloodthirstyTimer <= 0) bloodThirstyBoost = 1f;
        }

        if (veteranTimer > 0) {
            veteranTimer -= delta;
            if (veteranTimer <= 0) {
                float armorToRemove = veteranStacks * veteranBoost;
                armor -= armorToRemove;
                veteranStacks = 0;
            }
        }
    }

    private void handleActions(float moveX, float moveY) {
        // Trigger Super (Manual Activation)
        if (!isSuperActive && currentEnergy >= baseEnergy && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            activateSuper();
        }

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

    private void applyMovement(float delta, float moveX, float moveY, GameWorld gameWorld, float flashMult) {
        // Priority 1: Dashing
        if (isDashing) {
            dashTime -= delta;
            x += dashDirX * dashSpeed * delta;
            y += dashDirY * dashSpeed * delta;
            if (dashTime <= 0) {
                isDashing = false;
                float cooldownToSet = dashCooldown;
                if (isSuperActive) {
                    cooldownToSet = dashCooldown / 3f; // Half cooldown
                }
                dashTimer = cooldownToSet;
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
        // Combine base multipliers with Flash multiplier
        float currentSpeed = baseSpeed * speedMultiplier * flashMult;

        if (hasScaredyCat) {
            currentSpeed += (nearbyEnemies * scaredyCatBoostPerEnemy);
        }

        if (hasComboGod) {
            float comboMult = (float) Math.pow(1f + comboGodBoostPerCombo, gameWorld.combo);
            currentSpeed *= comboMult;
        }

        if (isOnStairs(gameWorld)) currentSpeed *= (2f / 3f);

        if (isSuperActive) currentSpeed *= 1.7f;

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

        if (moveX == 0 && moveY == 0) {
            dashDirX = facingRight ? 1 : -1;
            dashDirY = 0;
        } else {
            dashDirX = moveX;
            dashDirY = moveY;
        }
    }

    private void checkDashCollisions(GameWorld gameWorld) {
        boolean shouldReset = false;
        if (isBlocked(x, y, gameWorld)) {
            // 1. Stop the dash physics
            isDashing = false;
            dashTime = 0f;

            // 2. Mark for reset
            shouldReset = true;
            float cooldownToSet = dashCooldown;
            if (isSuperActive) {
                cooldownToSet = dashCooldown / 3f; // Half cooldown
            }
            dashTimer = cooldownToSet;

            gameWorld.spawnText(x, y + height - 10f, "BONK!", Color.WHITE, 0.5f);
        }

        if (hasParkour && shouldReset) {
            if (dashTimer > 0) {
                gameWorld.spawnText(x, y + height - 5f, "RESET!", Color.CYAN, 0.9f);
            }
            dashTimer = 0f;
        }
    }

    private void performAttack() {
        attackCount++;
        isAttacking = true;

        // 1. Calculate Speed Multiplier
        float speedMult = 1.0f;
        if (isSuperActive) {
            speedMult = 0.5f; // 50% multiplier = 2x Speed
        }

        // 2. Set Cooldown (When can I click again?)
        attackCooldownTimer = baseAttackSpeed * speedMult;

        // 3. Set Animation Duration (How fast does the sword visually swing?)
        // FIX: We scale this too, so the animation matches the fast cooldown!
        attackTimer = attackDuration * speedMult;

        // --- THE FLASH LOGIC ---
        if (hasTheFlash) {
            float progress = flashTimer / maxFlashTime;
            currentFlashDamageBonus = 1f + progress; // Ranges from 1.0 to 2.0
            flashTimer = 0f;
            if (currentFlashDamageBonus > 1.5f) {
                gameWorld.spawnText(x, y + height, "FLASH ATTACK!", Color.YELLOW, 0.8f);
            }
        } else {
            currentFlashDamageBonus = 1f;
        }

        // Calculate attack direction based on mouse
        attackAngle = gameWorld.gameScreen.getMouseAngle();
        float rad = MathUtils.degreesToRadians * attackAngle;
        attackDirX = MathUtils.cos(rad);
        attackDirY = MathUtils.sin(rad);

        // Update facing direction based on attack
        facingRight = !(attackAngle > 90 && attackAngle < 270);

        // Audio: Shift pitch up slightly during Super for "faster" sound
        float randomPitch = 0.8f + MathUtils.random(0.4f);
        if (isSuperActive) randomPitch += 0.2f;

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
            // NEW: Check if this new level is a milestone level

            if (level % 5 == 4 && level <= 25) {
                gameWorld.pendingAugments++;
            }
        }
    }

    private void applyPassiveLevelUpStats() {
        baseHP += 3f;
        currentHP += 3f;
        armor += 0.75f;

        baseSpeed += 1f;

        baseDamage += 1.5f;
        damage = baseDamage;

        baseLifesteal += 0.001f;
        lifesteal = baseLifesteal;

        luck += 0.0025f;
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
        float comboMult = 1f;
        if (hasComboGod && gameWorld.combo > 0) {
            comboMult = (float) Math.pow(1f + comboGodBoostPerCombo, gameWorld.combo);
        }

        float effectiveLuck = luck * comboMult;
        if (MathUtils.random() < effectiveLuck) {
            gameWorld.spawnText(x, y + height, "DODGE!", Color.CYAN, 0.6f);
            return;
        }

        if (hasMasochist) {
            if (MathUtils.random() < effectiveLuck) {
                heal((int)(amount / 3), gameWorld);
                return;
            }
        }

        // --- 2. VETERAN STACKING ---
        if (hasVeteran) {
            if (veteranStacks < MAX_VETERAN_STACKS) {
                veteranStacks++;
                armor += veteranBoost;
            }
            veteranTimer = VETERAN_DURATION;
        }

        float effectiveArmor = armor * comboMult;

        float reductionPercentage = Math.min(effectiveArmor, 90f);
        float actualDamage = amount * (1f - (reductionPercentage / 100f));

        // --- 4. APPLY DAMAGE ---
        currentHP -= actualDamage;

        // --- 5. DEATH CHECK ---
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
        float hpPercent = currentHP / baseHP;
        float currentLifesteal = lifesteal * lifestealMultiplier;

        // Last Stand Lifesteal: Scales from 0 (at 100% HP) to 0.2 (at 25% HP)
        if (hasLastStand) {
            currentLifesteal += MathUtils.clamp((1f - hpPercent) / 3.75f, 0f, lastStandMaxLifesteal);
        }

        float stealAmount = damageDealt * currentLifesteal;

        if (hasLuckyThrees) {
            stealAmount += (attackCount % 3 == 0) ? luckyThreesHeal : 0;
        }

        if (stealAmount >= 1f) {
            heal(stealAmount, gameWorld);
            gameWorld.spawnDamage(x - 5, y + height - 5, (int)stealAmount, Color.GREEN, 0.8f);
        }
    }

    // AUGMENTS

    public float getNearestLightDistance() {
        float minDistance = Float.MAX_VALUE;

        // Calculate center of player for more accuracy
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;

        for (float[] light : LIGHT_POSITIONS) {
            float lightX = light[0];
            float lightY = light[1];

            // Standard distance formula: sqrt((x2-x1)^2 + (y2-y1)^2)
            float dist = (float) Math.sqrt(
                Math.pow(centerX - lightX, 2) + Math.pow(centerY - lightY, 2)
            );

            if (dist < minDistance) {
                minDistance = dist;
            }
        }
        return minDistance;
    }

    private void updateNearbyEnemies(GameWorld gameWorld) {
        nearbyEnemies = 0;
        for (Enemy e : gameWorld.enemies) {
            if (!e.isAlive) continue;

            // Simple distance check
            float dx = x - e.x;
            float dy = y - e.y;
            float dist = (float) Math.sqrt(dx*dx + dy*dy);

            if (dist < nearbyDistance) {
                nearbyEnemies++;
            }
        }
    }

    // Inside Player.java

    public int calculateAttackDamage(GameWorld gameWorld, float specificMultiplier) {
        float totalMultiplier = this.damageMultiplier * specificMultiplier;

        // 1. Last Stand (HP based)
        if (hasLastStand) {
            totalMultiplier *= MathUtils.clamp(1f + (1f - (currentHP / baseHP)) / 0.75f, 1f, lastStandMaxBoost);
        }

        // 2. The Flash (Charge based)
        if (hasTheFlash) {
            totalMultiplier *= currentFlashDamageBonus;
        }

        // 3. Vampiric (Light based)
        if (hasVampiric) {
            totalMultiplier *= MathUtils.clamp(0.5f + (getNearestLightDistance() / 150f), 0.4f, 1.7f);
        }

        // 4. Combo God (Stacking)
        if (hasComboGod && gameWorld.combo > 0) {
            totalMultiplier *= (float) Math.pow(1f + comboGodBoostPerCombo, gameWorld.combo);
        }

        // 5. Bloodthirsty (Timed)
        if (hasBloodthirsty) {
            totalMultiplier *= bloodThirstyBoost;
        }

        // 6. Lucky Threes (Flat bonus)
        int luckyThreesDamage = 0;
        if (hasLuckyThrees) {
            luckyThreesDamage = (attackCount % 3 == 0) ? luckyThreesDmgBonus : 0;
        }

        if (isSuperActive) {
            totalMultiplier *= 1.5f; // Damage Boost
        }

        return (int)(baseDamage * totalMultiplier + luckyThreesDamage);
    }

    // --- COLLISION CHECKS (Kept mostly as-is) ---

    private boolean checkCollision(float testX, float testY, GameWorld.TileType type, GameWorld gameWorld) {
        int left = (int)((testX + collisionOffsetX) / gameWorld.tileSize);
        int right = (int)((testX + collisionOffsetX + collisionWidth) / gameWorld.tileSize);
        int bottom = (int)((testY + collisionOffsetY) / gameWorld.tileSize);
        int top = (int)((testY + collisionOffsetY + collisionHeight) / gameWorld.tileSize);

        if (left < 0 || bottom < 0) return true;

        for (int y = bottom; y <= top; y++) {
            for (int x = left; x <= right; x++) {
                try {
                    if (gameWorld.isTileType(x, y, type)) return true;
                } catch (Exception e) {
                    return true;
                }
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

    public void boostSpeed(float time, float multiplier){
        if (hasBoostedAnimal) {
            time = boostedAnimalBoostLength;
            baseSpeed += (baseSpeed * multiplier - baseSpeed) * boostedAnimalBoostPermanent;
        }
        speedBoostTimer = time;
        speedMultiplier = multiplier;
    }

    public void boostDamage(float time, float multiplier){
        if (hasBoostedAnimal) {
            time = boostedAnimalBoostLength;
            baseDamage += (baseDamage * multiplier - baseDamage) * boostedAnimalBoostPermanent;
            damage = baseDamage;
            baseLifesteal += (baseLifesteal * multiplier - baseLifesteal) * boostedAnimalBoostPermanent;
            lifesteal = baseLifesteal;
        }
        damageBoostTimer = time;
        damageMultiplier = multiplier;
        lifestealBoostTimer = time;
        lifestealMultiplier = multiplier;
    }

    public void boostHP(float amount) {
        if (hasBoostedAnimal) {
            baseHP += amount * boostedAnimalBoostPermanent;
            heal(amount + amount * boostedAnimalBoostPermanent, gameWorld);
        } else {
            heal(amount, gameWorld);
        }
    }

    public void gainEnergy(float luck) {
        if (isSuperActive) return;

        int amount = MathUtils.random(1, 5);
        if (MathUtils.random() < (luck * 0.5f)) {
            amount += 1;
        }

        currentEnergy += amount;

        // Clamp to max, but DON'T auto-activate anymore
        if (currentEnergy >= baseEnergy) {
            currentEnergy = baseEnergy;
        }
    }

    private void activateSuper() {
        isSuperActive = true;

        // Duration: 5s base + 1s per superLevel (Max 12s)
        totalSuperDuration = 7f + (superLevel * 1.0f);
        superDurationTimer = totalSuperDuration;

        // Visual Pop
        gameWorld.spawnText(x, y + height + 10, "SUPER ACTIVE!", Color.CYAN, 1.2f);
    }

    private void endSuper() {
        isSuperActive = false;
        currentEnergy = 0f; // Reset energy to 0
        gameWorld.spawnText(x, y + height + 10, "SUPER ENDED", Color.GRAY, 0.8f);
    }

    private void updateSuperLogic(float delta) {
        if (isSuperActive) {
            superDurationTimer -= delta;

            currentEnergy = (superDurationTimer / totalSuperDuration) * baseEnergy;

            if (superDurationTimer <= 0f) {
                endSuper();
            }
        }
    }

    public void upgradeStat(StatType stat) {
        if (skillPoints <= 0) return;

        boolean success = false;
        switch (stat) {
            case ATTACK:
                if (attackLevel < 7) {
                    attackLevel++;
                    baseDamage += 4f;
                    damage = baseDamage;

                    baseLifesteal += 0.01f;
                    lifesteal = baseLifesteal;
                    success = true;
                }
                break;
            case AGILITY:
                if (agilityLevel < 7) {
                    agilityLevel++;
                    baseSpeed += 5f;
                    success = true;
                }
                break;
            case DEFENSE:
                if (defenseLevel < 7) {
                    defenseLevel++;
                    baseHP += 20f;
                    currentHP += 20f;
                    armor += 3f;
                    success = true;
                }
                break;
            case LUCK:
                if (luckLevel < 7) {
                    luckLevel++;
                    luck += 0.05f;
                    critChance = luck;
                    success = true;
                }
                break;
            case SUPER:
                if (superLevel < 7) {
                    superLevel++;
                    baseEnergy -= 8;
                    success = true;
                }
                break;
        }
        if (success) {
            skillPoints--;
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
