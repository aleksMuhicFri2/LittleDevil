package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Minimap {

    private final GameWorld world;
    private final Player player;
    private final ShapeRenderer shapeRenderer;

    // --- CONFIGURATION ---
    private final float SIZE = 300f;       // Size of the box (200x200)
    private final float MARGIN = 40f;      // Distance from bottom-right corner
    private final float BORDER_THICKNESS = 8f;

    private final float PLAYER_DOT_SIZE = 8f;
    private final float ENEMY_DOT_SIZE = 8f;

    // Colors
    private final Color bgColor = new Color(0f, 0f, 0.4f, 0.6f); // Translucent Dark Blue
    private final Color borderColor = Color.BLACK;
    private final Color enemyColor = new Color(0.9f, 0.1f, 0.1f, 1f); // Red
    private final Color playerColor = new Color(0.2f, 0.8f, 1f, 1f);  // Cyan/Blue

    public Minimap(GameWorld world, Player player) {
        this.world = world;
        this.player = player;
        this.shapeRenderer = new ShapeRenderer();
    }

    public void render(SpriteBatch batch, Viewport viewport) {
        // 1. INTERRUPT THE BATCH
        // ShapeRenderer cannot run while SpriteBatch is active.
        // We pause the batch here and resume it at the end.
        batch.end();

        // 2. SETUP GL STATE (Enable Transparency)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // 3. SETUP SHAPE RENDERER
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // --- CALCULATE POSITION ---
        // Anchored to Bottom-Right
        float mapX = viewport.getWorldWidth() - SIZE - MARGIN;
        float mapY = MARGIN;

        // --- DRAW BACKGROUND ---
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(mapX, mapY, SIZE, SIZE);

        // --- DRAW BORDER (4 Rectangles technique for thickness) ---
        shapeRenderer.setColor(borderColor);
        // Bottom
        shapeRenderer.rect(mapX - BORDER_THICKNESS, mapY - BORDER_THICKNESS, SIZE + 2 * BORDER_THICKNESS, BORDER_THICKNESS);
        // Top
        shapeRenderer.rect(mapX - BORDER_THICKNESS, mapY + SIZE, SIZE + 2 * BORDER_THICKNESS, BORDER_THICKNESS);
        // Left
        shapeRenderer.rect(mapX - BORDER_THICKNESS, mapY, BORDER_THICKNESS, SIZE);
        // Right
        shapeRenderer.rect(mapX + SIZE, mapY, BORDER_THICKNESS, SIZE);

        // --- CALCULATE SCALING ---
        // This ratio converts "World Coordinates" to "Minimap Coordinates"
        float scaleX = SIZE / world.mapWidth;
        float scaleY = SIZE / world.mapHeight;

        // --- DRAW ENEMIES ---
        shapeRenderer.setColor(enemyColor);
        for (Enemy e : world.enemies) {
            // Optimization: Only draw if alive
            if (e.HP > 0) {
                float ex = mapX + (e.x * scaleX);
                float ey = mapY + (e.y * scaleY);
                // Draw centered square
                shapeRenderer.rect(ex - ENEMY_DOT_SIZE/2, ey - ENEMY_DOT_SIZE/2, ENEMY_DOT_SIZE, ENEMY_DOT_SIZE);
            }
        }

        // --- DRAW PLAYER ---
        shapeRenderer.setColor(playerColor);
        float px = mapX + (player.x * scaleX);
        float py = mapY + (player.y * scaleY);
        shapeRenderer.circle(px, py, PLAYER_DOT_SIZE);

        // 4. CLEANUP
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 5. RESUME BATCH
        batch.begin();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
