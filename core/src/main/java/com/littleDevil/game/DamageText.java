package com.littleDevil.game;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Color;

public class DamageText {
    public float x, y;
    public String text;
    private float lifetime; // total time before disappearing
    private float elapsed = 0f;
    private float riseSpeed = 10f; // pixels per second
    private BitmapFont font;
    private Color color;
    private float scale;

    public boolean finished = false;

    public DamageText(float x, float y, String text, float lifetime, BitmapFont font, Color color, float scale) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.lifetime = lifetime;
        this.font = font;
        this.color = color;
        this.scale = scale;
    }

    public void update(float delta) {
        elapsed += delta;
        y += riseSpeed * delta; // float upward
        if (elapsed >= lifetime) finished = true;
    }

    public void render(SpriteBatch batch) {
        if (finished) return;

        // Compute alpha
        float alpha = 1f - (elapsed / lifetime);

        // Apply color with fade
        font.setColor(color.r, color.g, color.b, alpha);
        font.getData().setScale(scale);

        // Draw text
        font.draw(batch, text, x, y);
    }
}
