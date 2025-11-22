package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.Color;

public class Nun extends Enemy {

    private enum NunState {
        APPROACH,
        WINDUP,
        THROWING,
        REPOSITION
    }

    private NunState state = NunState.APPROACH;
    private float stateTimer = 0f;

    private final float RANGE = 80f;
    private final float WINDUP_TIME = 0.4f;
    private final float COOLDOWN_TIME = 0.8f;
    private float throwCooldown = 0f;
    private final float THROW_COOLDOWN = 1.0f; // or whatever you want

    private TextureRegion idleFrame;
    private TextureRegion walkFrame;
    private TextureRegion flashFrame;

    private float walkTimer = 0f;
    private final float WALK_INTERVAL = 0.2f;

    private boolean facingLeft = false;
    private Texture fullSheet;

    public Nun(float x, float y, GameWorld world) {
        super(x, y, "Spritesheets/nunSpritesheet.png", world);

        fullSheet = new Texture("Spritesheets/nunSpritesheet.png");

        idleFrame  = new TextureRegion(fullSheet, 0, 0, 32, 32);
        walkFrame  = new TextureRegion(fullSheet, 32, 0, 32, 32);
        flashFrame = new TextureRegion(fullSheet, 64, 0, 32, 32);

        currentFrame = idleFrame;

        moveSpeed = 38f;

        BASE_HP = 75;
        HP = BASE_HP + (world.wave - 1) * 10;
        damage = 15f;

        intelligence = 1.0f;

        guaranteedOrbsCounts = new float[]{1f, 0f, 0f};
        firstExtraChances    = new float[]{0.10f, 0f, 0f};
        orbProbabilityDecays = new float[]{0.3f, 0.2f, 0.1f};
    }

    @Override
    public void update(float delta, Player player, GameWorld gameWorld, GameScreen gameScreen) {

        if (!isAlive) {
            handleDeathFade(delta, gameWorld);
            return;
        }

        stateTimer -= delta;
        walkTimer += delta;
        throwCooldown -= delta;

        updateFacing(player);
        updateAnimation(delta);

        switch (state) {

            case APPROACH -> {
                float dist = distanceToPlayer(player);

                if (dist > RANGE) {
                    followPath(gameWorld, delta);

                } else {
                    // in range but not allowed to throw yet
                    if (throwCooldown > 0f) {
                        // stay in APPROACH, keep moving/hovering at range
                        // (do nothing, she won't enter WINDUP)
                    } else {
                        // cooldown finished → begin throw sequence
                        changeState(NunState.WINDUP, WINDUP_TIME);
                    }
                }
            }

            case WINDUP -> {
                if (stateTimer <= 0f) {
                    changeState(NunState.THROWING, 0.1f);
                }
            }

            case THROWING -> {
                throwBottle(gameWorld, player);
                throwCooldown = THROW_COOLDOWN;      // start cooldown
                changeState(NunState.REPOSITION, COOLDOWN_TIME);
            }

            case REPOSITION -> {
                float dist = distanceToPlayer(player);

                if (dist < RANGE - 5f) {
                    Vector2 dir = new Vector2(x - player.x, y - player.y).nor();
                    moveWithCollision(dir.x * moveSpeed * delta,
                        dir.y * moveSpeed * delta,
                        gameWorld);
                }
                else if (dist > RANGE + 5f) {
                    followPath(gameWorld, delta);
                }

                if (stateTimer <= 0f) {
                    changeState(NunState.APPROACH, 0.1f);
                }
            }
        }

        applySeparationForce(gameWorld);
        applyKnockback(delta, gameWorld);
        handleAttack(player, gameScreen, gameWorld);
    }

    private void changeState(NunState newState, float duration) {
        state = newState;
        stateTimer = duration;
    }

    private void throwBottle(GameWorld world, Player player) {
        // Player movement vector per second
        float velX = (player.x - player.prevX) / Gdx.graphics.getDeltaTime();
        float velY = (player.y - player.prevY) / Gdx.graphics.getDeltaTime();

        // If player is basically not moving → no prediction at all
        float speedSquared = velX * velX + velY * velY;
        if (speedSquared < 1f) {
            float tx = player.x;
            float ty = player.y - player.height * 0.5f;
            world.spawnBottleToPoint(x, y, tx, ty);
            return;
        }

        // Estimate how long the bottle will fly (same as BottleProjectile uses)
        float distance = Vector2.dst(x, y, player.x, player.y);
        float flightSpeed = 150f; // same speed you set in the bottle code
        float travelTime = distance / flightSpeed;

        // Prediction: lead based on velocity * travelTime * intelligence
        float predictMultiplier = intelligence * 1.0f; // adjust if needed

        float targetX = player.x + velX * travelTime * predictMultiplier;
        float targetY = player.y + velY * travelTime * predictMultiplier - player.height * 0.5f;

        world.spawnBottleToPoint(x, y, targetX, targetY);
    }

    @Override
    public void handleAttack(Player player, GameScreen gameScreen, GameWorld gameWorld) {

        if (!player.isAttacking) {
            hitThisAttack = false;
            return;
        }

        if (hitThisAttack) return;

        float dx = x - player.x;
        float dy = y - player.y;
        float dist = (float)Math.sqrt(dx * dx + dy * dy);

        if (dist > player.range) return;

        dx /= dist;
        dy /= dist;

        float dot = player.attackDirX * dx + player.attackDirY * dy;
        if (dot <= 0.3f) return;

        hitFlashTime = hitFlashDuration;
        hitThisAttack = true;

        nunHit(dx, dy, player, gameScreen, gameWorld);
        gameWorld.combo++;
    }

    private void nunHit(float dx, float dy,
                        Player player,
                        GameScreen gameScreen,
                        GameWorld gameWorld) {

        applyHitKnockback(dx, dy);
        playHitSound();
        gameScreen.triggerTimePause(0.1f, 0.15f);

        // simple damage — nun has no front/back reduction
        float damageMultiplier = player.damageMultiplier;

        boolean crit = Math.random() <= player.critChance;

        Color textColor = new Color(1f, 0.2f, 0.2f, 1f);
        float scale = 1f;

        if (crit) {
            damageMultiplier *= player.critMultiplier;
            textColor.set(1f, 0.4f, 0.2f, 1f);
            scale = 1.3f;
        }

        int dmg = (int)(player.damage * damageMultiplier);

        gameWorld.spawnDamage(x, y + height / 1.5f, dmg, textColor, scale);

        HP -= dmg;

        if (HP <= 0 && isAlive) {
            die(gameWorld, player);
        }
    }

    private void die(GameWorld gameWorld, Player player) {
        isAlive = false;
        currentFrame = flashFrame;
        gameWorld.spawnOrbs(this, player);
    }

    private void updateFacing(Player player) {
        boolean shouldFaceLeft = player.x < x;

        if (shouldFaceLeft != facingLeft) {
            facingLeft = shouldFaceLeft;

            idleFrame.flip(true, false);
            walkFrame.flip(true, false);
            flashFrame.flip(true, false);
        }
    }

    private void updateAnimation(float delta) {
        if (hitFlashTime > 0) {
            currentFrame = flashFrame;
            hitFlashTime -= delta;
            return;
        }

        if (walkTimer > WALK_INTERVAL) {
            currentFrame = (currentFrame == idleFrame) ? walkFrame : idleFrame;
            walkTimer = 0f;
        }
    }

    private void handleDeathFade(float delta, GameWorld gameWorld) {
        alpha -= 0.6f * delta;
        if (alpha <= 0f) {
            gameWorld.removeEnemy(this);
        }
    }

    @Override
    public void dispose() {
        fullSheet.dispose();
        super.dispose();
    }
}
