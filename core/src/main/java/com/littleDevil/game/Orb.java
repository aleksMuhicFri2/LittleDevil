package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import java.util.Random;

public class Orb {

    public enum OrbType { COMMON, RARE, GOLD }

    public float x, y;
    private Vector2 velocity;
    private float distanceTraveled = 0f;
    private float pulseTime = 0f;
    private float pullTime = 0f; // time orb has been in pull range

    public float lifetime = 20f; // seconds
    public OrbType type;
    public boolean isPulled = false;

    private final float PULL_RADIUS = 18f;
    private final float PULL_SPEED = 12f;
    private final float MAX_PULL_SPEED = 250f;
    private final float MAX_RANGE;
    private final float ORB_SPEED;

    private TextureRegion[] orbFrames;
    private TextureRegion currentFrame;

    private float spawnTime; // time since orb spawned
    private final float PULL_DELAY = 0.3f; // 0.2 seconds before pull is allowed

    private Random rand = new Random();

    public Orb(float x, float y, OrbType type, Texture orbSheet, Vector2 initialVelocity) {
        this.x = x;
        this.y = y;
        this.type = type;

        orbFrames = new TextureRegion[3];
        for (int i = 0; i < 3; i++) {
            orbFrames[i] = new TextureRegion(orbSheet, i * 16, 0, 16, 16);
        }
        currentFrame = orbFrames[type.ordinal()];

        // Set the initial velocity as passed
        this.velocity = initialVelocity;
        ORB_SPEED = velocity.len();

        // Random max range for natural scatter deceleration
        MAX_RANGE = 20f + rand.nextFloat() * 10f;
        spawnTime = 0f; // starts at 0
    }

    public void update(float delta, Player player, GameWorld world) {
        if (lifetime <= 0f) return;
        lifetime -= delta;
        spawnTime += delta; // track time since spawn
        pulseTime += delta;

        float playerLeft = player.x + player.collisionOffsetX;
        float playerRight = playerLeft + player.collisionWidth;
        float playerBottom = player.y + player.collisionOffsetY;
        float playerTop = playerBottom + player.collisionHeight;

        float closestX = Math.max(playerLeft, Math.min(x, playerRight));
        float closestY = Math.max(playerBottom, Math.min(y, playerTop));

        Vector2 dirToPlayer = new Vector2(closestX - x, closestY - y);
        float distanceToPlayer = dirToPlayer.len();

        // Pull logic only after delay
        if (spawnTime >= PULL_DELAY && distanceToPlayer < PULL_RADIUS) {
            pullTime += delta; // accumulate pull time
            isPulled = true;

            // Gravity-like acceleration
            float gravityFactor = Math.min(pullTime, 2f); // clamp for safety
            float pullSpeed = Math.min(PULL_SPEED + gravityFactor * 100f, MAX_PULL_SPEED);

            dirToPlayer.nor();
            velocity.set(dirToPlayer.scl(pullSpeed));
        } else {
            pullTime = 0f;
            isPulled = false;
        }

        // Move orb with collision
        Vector2 newPos = new Vector2(x + velocity.x * delta, y + velocity.y * delta);
        if (!isBlocked(newPos.x, y, world)) x = newPos.x;
        if (!isBlocked(x, newPos.y, world)) y = newPos.y;

        // Natural scatter deceleration
        if (!isPulled) {
            distanceTraveled += velocity.len() * delta;
            if (distanceTraveled >= MAX_RANGE) velocity.scl(0.95f);
        } else {
            if (distanceToPlayer < 6f) { // collected
                player.addXP(getXP());
                lifetime = 0f;
            }
        }
    }


    public void render(SpriteBatch batch) {
        if (lifetime <= 0f) return;
        float scale = 1f + 0.1f * (float)Math.sin(pulseTime * 3f);
        batch.draw(currentFrame, x - 8f, y - 8f, 8f, 8f, 16f, 16f, scale, scale, 0f);
    }

    private boolean isBlocked(float testX, float testY, GameWorld world) {
        int tileX = (int)(testX / world.tileSize);
        int tileY = (int)(testY / world.tileSize);
        return world.isTileType(tileX, tileY, GameWorld.TileType.BLOCK);
    }

    private int getXP() {
        return switch(type) {
            case COMMON -> 1;
            case RARE -> 5;
            case GOLD -> 20;
        };
    }

    public boolean isAlive() {
        return lifetime > 0f;
    }
}
