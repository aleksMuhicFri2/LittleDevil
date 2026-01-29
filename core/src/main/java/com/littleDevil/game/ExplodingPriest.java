package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class ExplodingPriest extends Enemy {

    private enum PriestState { APPROACH, CHARGING, EXPLODING }
    private PriestState state = PriestState.APPROACH;

    private TextureRegion frame1, frame2, flashFrame;
    private TextureRegion currentPriestFrame;

    private boolean facingLeft = false;
    private float prevX, prevY;
    private float walkTimer = 0f;
    private final float WALK_INTERVAL = 0.20f;

    // Flash/Charge sequence
    private float chargeTimer = 0f;
    private int chargeStage = 0;
    private final float BLINK_INTERVAL_1 = 0.3f;
    private final float BLINK_INTERVAL_2 = 0.50f;
    private float currentBlinkInterval = 0f;

    // Radii
    private final float TRIGGER_DISTANCE = 20f;
    private final float EXPLOSION_RADIUS = 50f;
    private final float CHASE_RESET_DISTANCE = 35f;

    private boolean diedWhileCharging = false;
    private final Sound explodeSound;

    public ExplodingPriest(float x, float y, GameWorld gameWorld) {
        super(x, y, "Spritesheets/priestSpritesheet.png", gameWorld);

        frame1 = new TextureRegion(spriteSheet, 0, 0, 32, 32);
        frame2 = new TextureRegion(spriteSheet, 32, 0, 32, 32);
        flashFrame = new TextureRegion(spriteSheet, 64, 0, 32, 32);
        currentPriestFrame = frame1;

        UnitStats.StatsResult stats = UnitStats.get(UnitStats.UnitType.PRIEST, gameWorld.wave, gameWorld.difficulty);
        this.HP = stats.maxHp;
        this.damage = stats.damage;
        this.moveSpeed = stats.speed;
        this.intelligence = stats.intelligence;
        this.scoreValue = stats.score;

        guaranteedOrbsCounts = new float [] {2, 1, 1};
        firstExtraChances = new float [] {0.70f, 0.40f, 0.15f};

        explodeSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/explosionSound.mp3"));
    }

    @Override
    public void update(float delta, Player player, GameWorld gameWorld, GameScreen screen) {
        if (!isAlive) return;

        updatePathsForEnemy(delta, player, gameWorld, 0.10f);
        prevX = x; prevY = y;
        float dist = distanceToPlayer(player);

        switch (state) {
            case APPROACH -> {
                if (dist <= TRIGGER_DISTANCE) startCharging();
                else followPath(gameWorld, delta);
            }
            case CHARGING -> updateCharging(delta, dist);
            case EXPLODING -> { explode(gameWorld, player, screen); return; }
        }

        applySeparationForce(gameWorld);
        applyKnockback(delta, gameWorld);
        updateFacing(player);
        updateAnimation(delta);

        // Use the standard attack handler we refined
        handleAttack(player, screen, gameWorld);
    }

    private void startCharging() {
        state = PriestState.CHARGING;
        chargeStage = 0;
        chargeTimer = 0f;
        currentBlinkInterval = BLINK_INTERVAL_1 / intelligence;
        currentPriestFrame = flashFrame;
    }

    private void updateCharging(float delta, float dist) {
        if (dist > CHASE_RESET_DISTANCE) {
            state = PriestState.APPROACH;
            currentPriestFrame = frame1;
            return;
        }

        chargeTimer += delta;
        if (chargeTimer >= currentBlinkInterval) {
            chargeTimer = 0f;
            chargeStage++;
            if (chargeStage == 1) {
                currentPriestFrame = frame1;
                currentBlinkInterval = BLINK_INTERVAL_1 / intelligence;
            } else if (chargeStage == 2) {
                currentPriestFrame = flashFrame;
                currentBlinkInterval = BLINK_INTERVAL_2 / intelligence;
            } else {
                state = PriestState.EXPLODING;
            }
        }
    }

    private void explode(GameWorld gameWorld, Player player, GameScreen screen) {
        float dx = (player.x + player.hitBoxWidth / 2f) - (x + width / 2f);
        float dy = (player.y + player.hitBoxHeight / 2f) - (y + height / 2f);
        float dist = (float)Math.sqrt(dx*dx + dy*dy);

        explodeSound.play(0.15f);
        if (dist <= EXPLOSION_RADIUS) {
            player.loseHP(damage);
            screen.triggerTimePause(0.15f, 0.35f);
            player.applyKnockBack(dx, dy, 100f);
        }

        gameWorld.explosions.add(new Explosion(x, y, gameWorld.explosionTexture));
        isAlive = false;
        gameWorld.removeEnemy(this);
    }

    @Override
    public void handleAttack(Player player, GameScreen screen, GameWorld gameWorld) {
        if (!isHitBy(player)) return;

        boolean crit = Math.random() <= player.critChance;
        Color color = crit ? new Color(0.8f, 0.4f, 0.2f, 1f) : new Color(1f, 0.2f, 0.2f, 1f);
        float scale = crit ? 1.4f : 1.0f;

        int damageAmount = player.calculateAttackDamage(gameWorld, crit ? player.critMultiplier : 1.0f);
        applyStandardHitFeedback(gameWorld, damageAmount, color, scale);

        playHitSound();

        HP -= damageAmount;
        player.onDealDamage(damageAmount);

        if (HP <= 0 && isAlive) die(player, gameWorld);
    }

    private void die(Player player, GameWorld gameWorld) {
        if (state == PriestState.CHARGING) diedWhileCharging = true;

        gameWorld.removeEnemy(this); // FIX: Instant disappearance
        onDeath(player, gameWorld);
    }

    private void updateAnimation(float delta) {
        if (state == PriestState.CHARGING || hitFlashTime > 0) {
            if (hitFlashTime > 0) {
                currentPriestFrame = flashFrame;
                hitFlashTime -= delta;
            }
            return;
        }

        float movement = (float)Math.sqrt(Math.pow(x - prevX, 2) + Math.pow(y - prevY, 2));
        if (movement < 0.1f) {
            currentPriestFrame = frame1;
            return;
        }

        walkTimer += delta;
        if (walkTimer > WALK_INTERVAL) {
            currentPriestFrame = (currentPriestFrame == frame1) ? frame2 : frame1;
            walkTimer = 0f;
        }
    }

    private void updateFacing(Player player) {
        boolean shouldFaceLeft = player.x < x;
        if (shouldFaceLeft != facingLeft) {
            facingLeft = shouldFaceLeft;
            frame1.flip(true, false);
            frame2.flip(true, false);
            flashFrame.flip(true, false);
        }
    }

    @Override
    public TextureRegion getCurrentFrame() { return currentPriestFrame; }

    @Override
    public void dispose() {
        super.dispose();
        explodeSound.dispose();
    }
}
