package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Boost {
    // CLASS USED TO DESCRIBE BOOSTS
    public enum Type { SPEED, DAMAGE, REGEN, SUPER }

    private Sound pickupSound;

    private final Type type;
    private float x, y;
    private Texture texture;

    public boolean pickedUp = false;
    private float floatTimer = 0f;
    private final float baseY;

    public Boost(Type type, float x, float y, Texture texture) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.baseY = y;
        this.texture = texture;

        pickupSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/boostSound.mp3"));
    }

    public void update(float delta) {
        if (pickedUp) return;

        // Floating motion
        float floatRange = 2f;
        floatTimer += delta;
        y = baseY + MathUtils.sin(floatTimer * 3f) * floatRange; // smooth float up/down
    }

    public void render(SpriteBatch batch) {
        if (!pickedUp)
            batch.draw(texture, x - 6, y - 8, 16, 16);
    }

    public void applyEffect(Player player, GameWorld gameWorld) {
        pickedUp = true;

        // Play pickup sound with random pitch variation
        float pitch = 0.9f + MathUtils.random() * 0.15f;
        pickupSound.play(0.05f, pitch, 0f);

        switch (type) {
            case SPEED:
                player.boostSpeed(10f, 1.5f);
                break;
            case DAMAGE:
                player.boostDamage(10f, 1.5f);
                break;
            case REGEN:
                player.boostHP(player.baseHP / 4);
                break;
            case SUPER:
                player.currentEnergy = Math.min(player.baseEnergy, player.currentEnergy + player.baseEnergy / 4);
                break;
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public Type getType() { return type; }

    public void dispose() {
        texture.dispose();
        pickupSound.dispose();
    }
}
