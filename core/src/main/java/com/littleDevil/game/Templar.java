package com.littleDevil.game;

public class Templar extends Enemy {

    public Templar(float x, float y, GameWorld world) {
        super(x, y, "Spritesheets/templarSpritesheet.png", world);
        height = 32f;
        width = 32f;

        moveSpeed = 30f;
        knockbackDecay = 150f;

        hitboxOffsetX = -12;
        hitboxOffsetY = -16;
        hitboxWidth = 24;
        hitboxHeight = 20;
    }

    @Override
    public void update(float delta, Player player, GameWorld gameWorld, GameScreen  gameScreen) {
        if (!isAlive) return;

        if (hitFlashTime > 0) hitFlashTime -= delta;
        applySeparationForce(gameWorld);
        applyKnockback(delta, gameWorld);
        followPath(gameWorld, delta);
        handleAttack(player, gameScreen);
    }

    /*
    @Override
    public void attack(){}
     */
}
