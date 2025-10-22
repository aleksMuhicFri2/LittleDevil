package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;

public class CollisionObject {

    public String name;           // optional identifier
    public int posX, posY;        // top-left corner in pixels
    public int width, height;     // visual size in pixels
    public int collisionWidth, collisionHeight; // size of collision box in pixels
    public int offsetX, offsetY;  // offset of collision box from top-left corner
    public Texture texture;       // optional texture for visualization
    public int type;              // 1 = world border, 2 = obstacle, 3 = powerup, etc.

    /**
     * Constructor without texture
     */
    public CollisionObject(String name, int posX, int posY, int collisionWidth, int collisionHeight, int offsetX, int offsetY, int width, int height, int type) {
        this.name = name;
        this.posX = posX;
        this.posY = posY;
        this.collisionWidth = collisionWidth;
        this.collisionHeight = collisionHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;
        this.type = type;
        this.texture = null;
    }

    /**
     * Constructor with texture
     */
    public CollisionObject(String name, int posX, int posY, int collisionWidth, int collisionHeight, int offsetX, int offsetY, int width, int height, int type, Texture texture) {
        this(name, posX, posY, collisionWidth, collisionHeight, offsetX, offsetY, width, height, type);
        this.texture = texture;
    }

    /**
     * Marks this object's collision area on the grid
     */
    public void markOnGrid(int[][] grid, int tileSize) {
        int startX = (posX + offsetX) / tileSize;
        int startY = (posY + offsetY) / tileSize;
        int endX = (posX + offsetX + collisionWidth) / tileSize;
        int endY = (posY + offsetY + collisionHeight) / tileSize;

        for (int y = startY; y <= endY && y < grid.length; y++) {
            for (int x = startX; x <= endX && x < grid[0].length; x++) {
                grid[y][x] = type;
            }
        }
    }
}
