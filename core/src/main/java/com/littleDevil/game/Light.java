package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Light {
    public float x, y;          // world position
    public float width, height; // size of the light
    public TextureRegion texture; // white-to-transparent radial gradient
    public float alpha = 1f;    // intensity
    public Texture tex;          // keep reference to dispose later

    // Flicker data
    public float flickerSpeed = 0.5f + (float)Math.random() * 1.0f; // Hz
    public float flickerPhase = (float)Math.random() * 10f;
    public float flickerAmplitude = 0.25f + (float)Math.random() * 0.3f; // how strong the flicker is

    public Light(float x, float y, float width, float height, String texturePath, float alpha) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.tex = new Texture(texturePath);  // load texture from path
        this.texture = new TextureRegion(tex);

        this.alpha = alpha;
    }

    public void dispose() {
        if (tex != null) tex.dispose();
    }
}
