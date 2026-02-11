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
    private float pullTime = 0f;

    public float lifetime = 15f;
    public OrbType type;
    public boolean isPulled = false;

    private final float PULL_RADIUS = 50f;
    private final float PULL_SPEED = 40f;
    private final float MAX_PULL_SPEED = 1000f;
    private final float MAX_RANGE;
    private final float ORB_SPEED;

    private TextureRegion[] orbFrames;
    private TextureRegion currentFrame;

    private float spawnTime;
    private final float PULL_DELAY = 0.6f;

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

        this.velocity = initialVelocity;
        ORB_SPEED = velocity.len();

        MAX_RANGE = 20f + rand.nextFloat() * 10f;
        spawnTime = 0f;
    }

    public void update(float delta, Player player, GameWorld world) {
        if (lifetime <= 0f) return;
        lifetime -= delta;
        spawnTime += delta;
        pulseTime += delta;

        // --- EMERGENCY UNSTUCK LOGIC ---
        if (isBlocked(x, y, world)) {
            Vector2 escapeDir = new Vector2(player.x - x, player.y - y).nor();
            // Move slowly towards player to escape wall
            x += escapeDir.x * 60f * delta;
            y += escapeDir.y * 60f * delta;
            return; // Skip normal physics this frame
        }
        // -------------------------------

        float playerLeft = player.x + player.collisionOffsetX;
        float playerRight = playerLeft + player.collisionWidth;
        float playerBottom = player.y + player.collisionOffsetY;
        float playerTop = playerBottom + player.collisionHeight;

        float closestX = Math.max(playerLeft, Math.min(x, playerRight));
        float closestY = Math.max(playerBottom, Math.min(y, playerTop));

        Vector2 dirToPlayer = new Vector2(closestX - x, closestY - y);
        float distanceToPlayer = dirToPlayer.len();

        // Pull logic
        if (spawnTime >= PULL_DELAY && distanceToPlayer < PULL_RADIUS) {
            pullTime += delta;
            isPulled = true;

            float gravityFactor = Math.min(pullTime, 2f);
            float pullSpeed = Math.min(PULL_SPEED + gravityFactor * 100f, MAX_PULL_SPEED);

            dirToPlayer.nor();
            velocity.set(dirToPlayer.scl(pullSpeed));
        } else {
            pullTime = 0f;
            isPulled = false;
        }

        // --- BOUNCE PHYSICS ---
        // Predict X movement
        float nextX = x + velocity.x * delta;
        if (isBlocked(nextX, y, world)) {
            velocity.x = -velocity.x * 0.8f; // Bounce X
        } else {
            x = nextX;
        }

        // Predict Y movement
        float nextY = y + velocity.y * delta;
        if (isBlocked(x, nextY, world)) {
            velocity.y = -velocity.y * 0.8f; // Bounce Y
        } else {
            y = nextY;
        }

        // Deceleration
        if (!isPulled) {
            distanceTraveled += velocity.len() * delta;
            if (distanceTraveled >= MAX_RANGE) velocity.scl(0.95f);
        } else {
            if (distanceToPlayer < 6f) { // collected
                player.addXP(getXP());
                world.playOrbSound();
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
        // Safety check for map bounds
        if (testX < 0 || testX >= world.mapWidth || testY < 0 || testY >= world.mapHeight) return true;

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
