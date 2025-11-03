package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.ArrayList;
import java.util.List;

public abstract class Enemy {

    public float x, y;
    public float width = 32f, height = 32f;

    // Stats that all Enemies have
    public float BASE_HP = 100f;
    public float BASE_DAMAGE = 20f;
    protected float HP;
    protected float damage;

    protected float moveSpeed = 30f;
    protected float intelligence = 1f;

    protected float[] guaranteedOrbsCounts = new float[3];
    protected float[] firstExtraChances =  new float[3];
    protected float[] orbProbabilityDecays =  new float[3];

    // Applied on death
    public float alpha = 1f;

    // Collision Box
    public int collisionOffsetX = -4, collisionOffsetY = -16, collisionWidth = 8, collisionHeight = 4;

    // Hitbox
    protected int hitboxOffsetX = -12, hitboxOffsetY = -16, hitboxWidth = 24, hitboxHeight = 20;

    // Spritesheets
    protected Texture spriteSheet;
    protected TextureRegion currentFrame;

    public boolean isAlive = true;

    // Hit FX
    protected float hitFlashTime = 0f;
    protected final float hitFlashDuration = 0.1f;
    protected boolean hitThisAttack = false;
    protected Sound hitSound;

    // Knockback
    protected float knockbackX = 0f, knockbackY = 0f;
    protected float knockbackDecay = 100f;
    protected float knockbackStrength = 100f;

    // Pathfinding
    public Pathfinder pathfinder;
    public List<Node> currentPath = new ArrayList<>();
    public int currentTargetIndex = 0;
    public float pathUpdateOffset, pathTimer = 0f;

    public Enemy(float x, float y, String spriteSheetPath, GameWorld gameWorld) {
        this.x = x;
        this.y = y;
        this.pathfinder = new Pathfinder(gameWorld);

        spriteSheet = new Texture(spriteSheetPath);
        currentFrame = new TextureRegion(spriteSheet, 0, 0, 32, 32);
        hitSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/hitSound.mp3"));
        pathUpdateOffset = (float) Math.random() * 2f;

        HP = BASE_HP + (gameWorld.wave - 1) * 20f;
        damage = BASE_DAMAGE +  (gameWorld.wave - 1) * 2f;
    }

    // Abstract function every Enemy needs to implement
    public abstract void update(float delta, Player player, GameWorld gameWorld, GameScreen gameScreen);

    // Updates the path for enemies or reduces time until its updated
    public void updatePathsForEnemy(float delta, Player player, GameWorld gameWorld, float PATH_UPDATE_INTERVAL) {

        float playerCenterX = player.x + player.collisionOffsetX + player.collisionWidth / 2f;
        float playerCenterY = player.y + player.collisionOffsetY + player.collisionHeight / 2f;

        pathTimer += delta;
        boolean timeToUpdate = pathTimer >= PATH_UPDATE_INTERVAL + pathUpdateOffset;
        boolean pathEmpty = (currentPath == null || currentPath.isEmpty());
        boolean pathEnded = (!pathEmpty && currentTargetIndex >= currentPath.size());

        // Update when timer expires, or path is empty/finished
        if (!timeToUpdate && !pathEmpty && !pathEnded) return;

        pathTimer = 0f;

        float enemyCenterX = x + collisionOffsetX + collisionWidth / 2f;
        float enemyCenterY = y + collisionOffsetY + collisionHeight / 2f;

        float dx = enemyCenterX - playerCenterX;
        float dy = enemyCenterY - playerCenterY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        float distanceFactor = Math.min(dist / 300f, 1f);
        pathUpdateOffset = 0.2f + distanceFactor * (float) Math.random() * 2f;
        int startX = (int) (enemyCenterX / gameWorld.tileSize);
        int startY = (int) (enemyCenterY / gameWorld.tileSize);
        int targetX = (int) (playerCenterX / gameWorld.tileSize);
        int targetY = (int) (playerCenterY / gameWorld.tileSize);

        Node oldNextNode = null;
        if (currentPath != null && !currentPath.isEmpty()
            && currentTargetIndex < currentPath.size()) {
            oldNextNode = currentPath.get(currentTargetIndex);
        }

        currentPath = pathfinder.findPath(startX, startY, targetX, targetY);

        if (currentPath != null && !currentPath.isEmpty()) {
            currentTargetIndex = getClosestIndex(oldNextNode, gameWorld);
        } else {
            currentTargetIndex = 0;
        }
    }

    // Gets the second-closest node so the player doesn't stop
    private int getClosestIndex(Node oldNextNode, GameWorld gameWorld) {
        int closestIndex = 0;
        float minDist = Float.MAX_VALUE;

        for (int i = 0; i < currentPath.size(); i++) {
            Node n = currentPath.get(i);
            float dx, dy;
            if (oldNextNode != null) {
                dx = n.x - oldNextNode.x;
                dy = n.y - oldNextNode.y;
            } else {
                dx = n.x - x / gameWorld.tileSize;
                dy = n.y - y / gameWorld.tileSize;
            }
            float d = dx*dx + dy*dy;
            if (d < minDist) {
                minDist = d;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    // Knocks the Enemy back
    protected void applyKnockback(float delta, GameWorld world) {
        float nextX = x + knockbackX * delta;
        float nextY = y + knockbackY * delta;

        if (!checkCollision(nextX, y, world)) x = nextX; else knockbackX = 0;
        if (!checkCollision(x, nextY, world)) y = nextY; else knockbackY = 0;

        knockbackX = approachZero(knockbackX, knockbackDecay * delta);
        knockbackY = approachZero(knockbackY, knockbackDecay * delta);
    }

    // Enemy Follows path generated by its Pathfinder
    protected void followPath(GameWorld world, float delta) {
        if (currentPath == null || currentPath.isEmpty() || currentTargetIndex >= currentPath.size()) return;

        Node target = currentPath.get(currentTargetIndex);
        float targetX = target.x * world.tileSize + world.tileSize / 2f;
        float targetY = target.y * world.tileSize + world.tileSize / 2f;

        float dx = targetX - (x + collisionOffsetX + collisionWidth / 2f);
        float dy = targetY - (y + collisionOffsetY + collisionHeight / 2f);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 1f) { // small offset so it doesn't jitter (Don't change)
            float moveX = (dx / dist) * moveSpeed * delta;
            float moveY = (dy / dist) * moveSpeed * delta;
            moveWithCollision(moveX, moveY, world);
        } else {
            currentTargetIndex++;
        }
    }

    protected void handleAttack(Player player, GameScreen gameScreen, GameWorld gameWorld) {
        if (!player.isAttacking) hitThisAttack = false;
        if (player.isAttacking && !hitThisAttack) {
            float dx = x - player.x;
            float dy = y - player.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance <= player.range) {
                dx /= distance;
                dy /= distance;
                float dot = player.attackDirX * dx + player.attackDirY * dy;
                if (dot > 0.3f) {
                    hitFlashTime = hitFlashDuration;
                    hitThisAttack = true;
                    applyHitKnockback(dx, dy);
                    playHitSound();
                    gameScreen.triggerTimePause(0.1f, 0.15f);
                }
            }
        }
    }

    // Function which prevents moving enemies into walls
    protected void moveWithCollision(float moveX, float moveY, GameWorld world) {
        float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        if (length > 0) {
            float factor = Math.min(1f, length / moveSpeed);
            moveX = moveX / length * moveSpeed * factor;
            moveY = moveY / length * moveSpeed * factor;
        }

        // Step safely
        int steps = (int) (Math.max(Math.abs(moveX), Math.abs(moveY)) / 2f) + 1;
        float stepX = moveX / steps;
        float stepY = moveY / steps;

        for (int i = 0; i < steps; i++) {
            if (!checkCollision(x + stepX, y, world)) x += stepX;
            else stepX = 0;
            if (!checkCollision(x, y + stepY, world)) y += stepY;
            else stepY = 0;
            if (stepX == 0 && stepY == 0) break; // fully blocked
        }
    }

    // Actually knocks Enemy Back
    protected void applyHitKnockback(float dx, float dy) {
        knockbackX = dx * knockbackStrength;
        knockbackY = dy * knockbackStrength;
    }

    // plays sound when enemy is hit
    protected void playHitSound() {
        if (hitSound != null)
            hitSound.play(0.1f, 0.8f + (float) (Math.random() * 0.4f), 0f);
    }

    // Reduces knockback force over time
    protected float approachZero(float value, float amount) {
        if (value > 0) { value -= amount; if (value < 0) value = 0; }
        else if (value < 0) { value += amount; if (value > 0) value = 0; }
        return value;
    }

    protected float distanceToPlayer(Player player) {
        float dx = x - player.x;
        float dy = y - player.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    protected boolean checkCollision(float testX, float testY, GameWorld world) {
        int left = (int) ((testX + collisionOffsetX) / world.tileSize);
        int right = (int) ((testX + collisionOffsetX + collisionWidth) / world.tileSize);
        int bottom = (int) ((testY + collisionOffsetY) / world.tileSize);
        int top = (int) ((testY + collisionOffsetY + collisionHeight) / world.tileSize);

        for (int y = bottom; y <= top; y++)
            for (int x = left; x <= right; x++)
                if (world.isTileType(x, y, GameWorld.TileType.BLOCK) || world.isTileType(x, y, GameWorld.TileType.BLOCKENEMY)) return true;
        return false;
    }

    // Function which prevents Enemies grouping in a single spot
    public void applySeparationForce(GameWorld world) {
        float repelStrength = 5f;
        float desiredDistance = 50f;

        float moveX = 0f, moveY = 0f;

        for (Enemy other : world.enemies) {
            if (other == this) continue;
            float dx = this.x - other.x;
            float dy = this.y - other.y;
            float dist2 = dx * dx + dy * dy;
            if (dist2 < desiredDistance * desiredDistance && dist2 > 0.0001f) {
                float dist = (float) Math.sqrt(dist2);
                float push = (desiredDistance - dist) / desiredDistance;
                moveX += (dx / dist) * push;
                moveY += (dy / dist) * push;
            }
        }

        float len = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        if (len > 0.001f) {
            moveX = (moveX / len) * repelStrength * Gdx.graphics.getDeltaTime();
            moveY = (moveY / len) * repelStrength * Gdx.graphics.getDeltaTime();

            // collision-safe movement
            moveWithCollision(moveX, moveY, world);
        }
    }


    // Functions which help render stuff in debug mode
    public void renderHitbox(SpriteBatch batch, Texture pixel) {
        batch.setColor(1f, 0f, 0f, 0.3f);
        batch.draw(pixel, x + hitboxOffsetX, y + hitboxOffsetY, hitboxWidth, hitboxHeight);
        batch.setColor(Color.WHITE);
    }

    public void renderCollisionBox(SpriteBatch batch, Texture pixel) {
        batch.setColor(0f, 1f, 0f, 0.3f);
        batch.draw(pixel, x + collisionOffsetX, y + collisionOffsetY, collisionWidth, collisionHeight);
        batch.setColor(Color.WHITE);
    }

    public void renderPath(SpriteBatch batch, Texture pixel, GameWorld world) {
        if (currentPath == null || currentPath.isEmpty()) return;
        batch.setColor(Color.BLUE);
        for (Node node : currentPath) {
            float worldX = node.x * world.tileSize;
            float worldY = node.y * world.tileSize;
            batch.draw(pixel, worldX, worldY, world.tileSize, world.tileSize);
        }
        batch.setColor(Color.WHITE);
    }

    // For RenderEntity objects creation
    public TextureRegion getCurrentFrame() {
        return currentFrame;
    }

    public void dispose() {
        spriteSheet.dispose();
    }
}
