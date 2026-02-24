package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Light {
    public float x, y;          // world position
    public float width, height; // size of the light
    public TextureRegion texture; // white-to-transparent radial gradient
    public float alpha = 1f;    // intensity
    public Texture tex;          // keep reference to dispose later

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
