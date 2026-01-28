package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class ExplodingPriest extends Enemy {

    private enum PriestState {
        APPROACH,
        CHARGING,
        EXPLODING
    }

    private PriestState state = PriestState.APPROACH;

    private TextureRegion frame1, frame2, flashFrame;
    private TextureRegion currentPriestFrame;

    private boolean facingLeft = false;
    private float prevX, prevY;

    private float walkTimer = 0f;
    private final float WALK_INTERVAL = 0.20f;

    // Flash sequence
    private float chargeTimer = 0f;
    private int chargeStage = 0;
    // 0 = flash 1
    // 1 = return to normal
    // 2 = flash 2
    // 3 = explode

    private final float BLINK_INTERVAL_1 = 0.3f;   // first flash speed
    private final float BLINK_INTERVAL_2 = 0.50f;  // second flash (big one)
    private float currentBlinkInterval = 0f;

    // Radiuses
    private final float TRIGGER_DISTANCE = 20f;       // start charging
    private final float EXPLOSION_RADIUS = 50f;       // does damage
    private final float CHASE_RESET_DISTANCE = 35f;   // abort charging if player escapes THIS

    private boolean diedWhileCharging = false;

    private final Sound explodeSound;

    public ExplodingPriest(float x, float y, GameWorld gameWorld) {
        super(x, y, "Spritesheets/priestSpritesheet.png", gameWorld);

        spriteSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

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
        orbProbabilityDecays = new float [] {0.25f, 0.30f, 0.30f};

        explodeSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/explosionSound.mp3"));
    }

    @Override
    public void update(float delta, Player player, GameWorld gameWorld, GameScreen screen) {

        if (!isAlive) return;

        // Faster priest path updates
        updatePathsForEnemy(delta, player, gameWorld, 0.10f);

        prevX = x;
        prevY = y;

        float dist = distanceToPlayer(player);

        switch (state) {

            case APPROACH -> {

                if (dist <= TRIGGER_DISTANCE) {

                    state = PriestState.CHARGING;
                    chargeStage = 0;
                    chargeTimer = 0f;
                    currentBlinkInterval = BLINK_INTERVAL_1 / intelligence;
                    currentPriestFrame = flashFrame;

                } else {

                    followPath(gameWorld, delta);
                }
            }

            case CHARGING -> {

                // Abort only if player exits the BIG zone
                if (dist > CHASE_RESET_DISTANCE) {
                    state = PriestState.APPROACH;
                    currentPriestFrame = frame1;
                    chargeStage = 0;
                    chargeTimer = 0f;
                    break;
                }

                chargeTimer += delta;

                if (chargeTimer >= currentBlinkInterval) {
                    chargeTimer = 0f;

                    switch (chargeStage) {
                        case 0 -> { // flash → normal
                            currentPriestFrame = frame1;
                            chargeStage = 1;
                            currentBlinkInterval = BLINK_INTERVAL_1 / intelligence;
                        }
                        case 1 -> { // normal → big flash
                            currentPriestFrame = flashFrame;
                            chargeStage = 2;
                            currentBlinkInterval = BLINK_INTERVAL_2 / intelligence;
                        }
                        case 2 -> { // BOOM time
                            state = PriestState.EXPLODING;
                        }
                    }
                }
                break;
            }

            case EXPLODING -> {
                explode(gameWorld, player, screen);
                return;
            }
        }

        applySeparationForce(gameWorld);
        applyKnockback(delta, gameWorld);
        updateFacing(player);
        updateAnimation(delta);
        handlePriestDamageReaction(player, gameWorld);
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

        // explosion animation
        gameWorld.explosions.add(
            new Explosion(x, y, gameWorld.explosionTexture)
        );

        isAlive = false;

        gameWorld.removeEnemy(this);
    }


    private void handlePriestDamageReaction(Player player, GameWorld gameWorld) {

        if (!player.isAttacking) { hitThisAttack = false; return; }
        if (hitThisAttack) return;

        float dx = x - player.x;
        float dy = y - player.y;

        float dist = (float)Math.sqrt(dx*dx + dy*dy);
        if (dist > player.range) return;

        dx /= dist;
        dy /= dist;

        float dot = dx * player.attackDirX + dy * player.attackDirY;
        if (dot <= 0.3f) return;

        hitThisAttack = true;
        hitFlashTime = hitFlashDuration;

        float dmgMult = player.damageMultiplier;
        boolean crit = Math.random() <= player.critChance;

        Color color = new Color(1f, 0.2f, 0.2f, 1f);
        float scale = 1f;

        if (crit) {
            dmgMult *= player.critMultiplier;
            color.set(1f, 0.4f, 0.2f, 1f);
            scale = 1.25f;
        }

        int dmg = (int)(player.damage * dmgMult);
        gameWorld.spawnDamage(x, y + height / 1.5f, dmg, color, scale);

        HP -= dmg;
        gameWorld.combo += 1;
        gameWorld.timeSinceLastHit = 0f;

        player.onDealDamage(dmg);

        if (HP <= 0) {
            if (state == PriestState.CHARGING) diedWhileCharging = true;
            isAlive = false;
            gameWorld.removeEnemy(this);
            gameWorld.spawnOrbs(this, player);
        }
    }

    private void updateAnimation(float delta) {

        if (state == PriestState.CHARGING) {
            // Charging stage controls its own frames
            return;
        }

        if (hitFlashTime > 0) {
            currentPriestFrame = flashFrame;
            hitFlashTime -= delta;
            return;
        }

        float dx = x - prevX;
        float dy = y - prevY;
        boolean moving = dx*dx + dy*dy > 0.1f;

        if (!moving) {
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
    public TextureRegion getCurrentFrame() {
        return currentPriestFrame;
    }

    @Override
    public void dispose() {
        super.dispose();
        explodeSound.dispose();
    }
}
