package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Boost {
    // CLASS USED TO DESCRIBE BOOSTS
    public enum Type { SPEED, DAMAGE, REGEN, SUPER }

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
            batch.draw(texture, x - 8, y - 8, 16, 16);
    }

    public void applyEffect(Player player) {
        pickedUp = true;
        switch (type) {
            case SPEED:
                player.boostSpeed(2f, 1.5f);
                break;
            case DAMAGE:
                player.boostDamage(2f, 1.5f);
                break;
            case REGEN:
                player.baseHP += player.currentHP / 4;
                break;
            case SUPER:
                player.baseEnergy += player.currentEnergy / 4;
                break;
        }
    }

    public void dispose() {
        texture.dispose();
    }
}
