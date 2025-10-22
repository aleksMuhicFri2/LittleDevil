package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;
import java.util.ArrayList;

public class Enemy {

    public float x, y; // world position
    public float width = 32f;
    public float height = 32f;

    // Collision
    public int collisionOffsetX = -4, collisionOffsetY = -16, collisionWidth = 8, collisionHeight = 4;

    // Hitbox for player's sword
    private int hitboxOffsetX = -12, hitboxOffsetY = -16, hitboxWidth = 24, hitboxHeight = 20;

    private Texture spriteSheet;
    private TextureRegion currentFrame;

    public boolean isAlive = true;

    // Hit flash
    private float hitFlashTime = 0f;
    private final float hitFlashDuration = 0.2f;
    private boolean hitThisAttack = false;

    // Knockback
    private float knockbackX = 0f;
    private float knockbackY = 0f;
    private float knockbackDecay = 140f; // higher = stops faster

    private Sound hitSound;

    // Pathfinding
    public Pathfinder pathfinder;
    public List<Node> currentPath = new ArrayList<>();
    public int currentTargetIndex = 0;
    public float pathUpdateOffset = 0f;
    public float pathTimer = 0f;

    public Enemy(float enemyX, float enemyY, String spriteSheetPath, GameWorld gameWorld) {
        this.x = enemyX;
        this.y = enemyY;
        this.pathfinder = new Pathfinder(gameWorld);

        spriteSheet = new Texture(spriteSheetPath);
        currentFrame = new TextureRegion(spriteSheet, 0, 0, 32, 32);
        hitSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/hitSound.mp3"));
        this.pathUpdateOffset = (float)Math.random() * 2f; // random offset for timing

    }

    public void update(float delta, Player player, GameWorld gameWorld) {
        if (!isAlive) return;

        // --- Hit flash timer ---
        if (hitFlashTime > 0) hitFlashTime -= delta;

        applySeparationForce(gameWorld);

        // --- Knockback movement ---
        float nextX = x + knockbackX * delta;
        float nextY = y + knockbackY * delta;

        if (!checkCollision(nextX, y, gameWorld)) x = nextX;
        else knockbackX = 0;

        if (!checkCollision(x, nextY, gameWorld)) y = nextY;
        else knockbackY = 0;

        knockbackX = approachZero(knockbackX, knockbackDecay * delta);
        knockbackY = approachZero(knockbackY, knockbackDecay * delta);

        // --- Pathfinding logic ---
        float collisionCenterX = x + collisionOffsetX + collisionWidth / 2f;
        float collisionCenterY = y + collisionOffsetY + collisionHeight / 2f;

        followPath(gameWorld, collisionCenterX, collisionCenterY, delta);
        handleAttack(player);
    }

    // Follow the path thats created by the Pathfinding algorithm quite smoothly
    private void followPath(GameWorld gameWorld, float testX, float testY, float delta) {
        // --- Move along path ---
        if (currentPath != null && !currentPath.isEmpty() && currentTargetIndex < currentPath.size()) {
            Node targetNode = currentPath.get(currentTargetIndex);
            float targetWorldX = targetNode.x * gameWorld.tileSize + gameWorld.tileSize / 2f;
            float targetWorldY = targetNode.y * gameWorld.tileSize + gameWorld.tileSize / 2f;

            float dx = targetWorldX - testX;
            float dy = targetWorldY - testY;
            float dist = (float)Math.sqrt(dx*dx + dy*dy);

            if (dist > 1f) { // small threshold
                float moveSpeed = 30f;
                float moveX = (dx / dist) * moveSpeed * delta;
                float moveY = (dy / dist) * moveSpeed * delta;

                if (!checkCollision(x + moveX, y, gameWorld)) x += moveX;
                if (!checkCollision(x, y + moveY, gameWorld)) y += moveY;
            } else {
                currentTargetIndex++;
            }
        } else {

        }
    }

    // if in range when player is attacking, they get hit
    private void handleAttack(Player player) {
        // --- Player attack detection ---
        if (!player.isAttacking) hitThisAttack = false;

        if (player.isAttacking && !hitThisAttack) {
            float dx = x - player.x;
            float dy = y - player.y;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);

            if (distance <= player.range) {
                dx /= distance;
                dy /= distance;

                float dot = player.attackDirX * dx + player.attackDirY * dy;

                if (dot > 0.3f) { // in front of player
                    hitFlashTime = hitFlashDuration;
                    hitThisAttack = true;

                    float knockbackStrength = 80f;
                    knockbackX = dx * knockbackStrength;
                    knockbackY = dy * knockbackStrength;

                    if (hitSound != null)
                        hitSound.play(0.1f, 0.8f + (float)(Math.random() * 0.4f), 0f);
                }
            }
        }
    }

    // function that helps with being pushed away when hit by player
    private float approachZero(float value, float amount) {
        if (value > 0) { value -= amount; if (value < 0) value = 0; }
        else if (value < 0) { value += amount; if (value > 0) value = 0; }
        return value;
    }

    private boolean checkCollision(float testX, float testY, GameWorld world) {
        int left = (int)((testX + collisionOffsetX) / world.tileSize);
        int right = (int)((testX + collisionOffsetX + collisionWidth) / world.tileSize);
        int bottom = (int)((testY + collisionOffsetY) / world.tileSize);
        int top = (int)((testY + collisionOffsetY + collisionHeight) / world.tileSize);

        for (int y = bottom; y <= top; y++)
            for (int x = left; x <= right; x++)
                if (world.isTileType(x, y, GameWorld.TileType.BLOCK)) return true;

        return false;
    }

    // keeps the enemies from becoming one -> if they get too close to each other, they repel
    public void applySeparationForce(GameWorld world) {
        float repelStrength = 6f;    // how strongly they push away
        float desiredDistance = 15f;   // minimum distance between enemies

        float moveX = 0f, moveY = 0f;

        for (Enemy other : world.enemies) {
            if (other == this) continue;

            float dx = this.x - other.x;
            float dy = this.y - other.y;
            float dist2 = dx * dx + dy * dy;

            // Only repel if too close
            if (dist2 < desiredDistance * desiredDistance && dist2 > 0.0001f) {
                float dist = (float)Math.sqrt(dist2);
                float push = (desiredDistance - dist) / desiredDistance; // strength 0..1
                moveX += (dx / dist) * push;
                moveY += (dy / dist) * push;
            }
        }

        float len = (float)Math.sqrt(moveX * moveX + moveY * moveY);
        if (len > 0.001f) {
            moveX = (moveX / len) * repelStrength * Gdx.graphics.getDeltaTime();
            moveY = (moveY / len) * repelStrength * Gdx.graphics.getDeltaTime();

            // Apply the separation
            this.x += moveX;
            this.y += moveY;
        }
    }

    public TextureRegion getCurrentFrame() {
        return currentFrame;
    }


    // HELPER FUNCTIONS FOR RENDERING IN DEBUG MODE
    public void renderHitbox(SpriteBatch batch, Texture pixel) {
        batch.setColor(1f, 0f, 0f, 0.3f);
        batch.draw(pixel, x + hitboxOffsetX, y + hitboxOffsetY, hitboxWidth, hitboxHeight);
        batch.setColor(Color.WHITE);
    }

    public void renderCollisionBox(SpriteBatch batch, Texture pixel) {
        batch.setColor(0f, 1f, 0f, 0.3f); // semi-transparent green for collision
        batch.draw(pixel, x + collisionOffsetX, y + collisionOffsetY, collisionWidth, collisionHeight);
        batch.setColor(Color.WHITE);
    }

    public void renderPath(SpriteBatch batch, Texture pixel, GameWorld gameWorld) {
        if (currentPath == null || currentPath.isEmpty()) return;
        batch.setColor(Color.BLUE);

        for (Node node : currentPath) {
            float worldX = node.x * gameWorld.tileSize;
            float worldY = node.y * gameWorld.tileSize;
            batch.draw(pixel, worldX, worldY, gameWorld.tileSize, gameWorld.tileSize);
        }
        batch.setColor(Color.WHITE);
    }

    public void dispose() {
        spriteSheet.dispose();
    }
}
