package com.littleDevil.game;

import com.badlogic.gdx.math.MathUtils;
import java.util.Random;

public class Spawner {

    private final GameWorld gameWorld;

    // Current Wave State
    private int templarsRemaining;
    private int nunsRemaining;
    private int priestsRemaining;

    // Timers
    private float templarTimer;
    private float nunTimer;
    private float priestTimer;

    // Calculated Intervals (Dynamic per wave)
    private float templarInterval;
    private float nunInterval;
    private float priestInterval;

    public Spawner(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    /**
     * Calculates spawn intervals so all enemies finish spawning roughly at the same time.
     */
    public void startWave(int waveNumber, Wave waveData) {
        this.templarsRemaining = waveData.templars;
        this.nunsRemaining = waveData.nuns;
        this.priestsRemaining = waveData.priests;

        // 1. Determine how long the "Spawning Phase" of this wave should last.
        // Base 25s + 6s per wave level. (Wave 1 = 31s, Wave 10 = 85s)
        float targetWaveDuration = 25f + (waveNumber * 6f);

        // 2. Calculate intervals based on count.
        // If 10 templars in 60s -> spawn every 6s.
        // If 2 priests in 60s -> spawn every 30s.

        if (templarsRemaining > 0) {
            templarInterval = targetWaveDuration / templarsRemaining;
            templarTimer = 0f;
        }

        if (nunsRemaining > 0) {
            nunInterval = targetWaveDuration / nunsRemaining;
            nunTimer = 0f;
        }

        if (priestsRemaining > 0) {
            priestInterval = targetWaveDuration / priestsRemaining;
            priestTimer = 0f;
        }
    }

    public void update(float delta) {
        // Templars
        if (templarsRemaining > 0) {
            templarTimer += delta;
            if (templarTimer >= templarInterval) {
                templarTimer -= templarInterval;
                spawnEnemy(GameWorld.EnemyType.TEMPLAR);
                templarsRemaining--;
            }
        }

        // Nuns
        if (nunsRemaining > 0) {
            nunTimer += delta;
            if (nunTimer >= nunInterval) {
                nunTimer -= nunInterval;
                spawnEnemy(GameWorld.EnemyType.NUN);
                nunsRemaining--;
            }
        }

        // Priests
        if (priestsRemaining > 0) {
            priestTimer += delta;
            if (priestTimer >= priestInterval) {
                priestTimer -= priestInterval;
                spawnEnemy(GameWorld.EnemyType.PRIEST);
                priestsRemaining--;
            }
        }
    }

    public boolean isSpawningFinished() {
        return templarsRemaining <= 0 && nunsRemaining <= 0 && priestsRemaining <= 0;
    }

    private void spawnEnemy(GameWorld.EnemyType type) {
        float enemyWidth = 32f;
        float enemyHeight = 32f;
        float spawnX = 0;
        float spawnY = 0;
        boolean valid = false;

        // Try up to 10 times to find a valid spot
        for (int i = 0; i < 10; i++) {
            int side = MathUtils.random(3); // 0=top, 1=bottom, 2=left, 3=right

            switch (side) {
                case 0 -> { spawnX = gameWorld.mapWidth / 2f; spawnY = gameWorld.mapHeight - enemyHeight; }
                case 1 -> { spawnX = gameWorld.mapWidth / 2f; spawnY = enemyHeight; }
                case 2 -> { spawnX = enemyWidth; spawnY = gameWorld.mapHeight / 2f; }
                case 3 -> { spawnX = gameWorld.mapWidth - enemyWidth; spawnY = gameWorld.mapHeight / 2f; }
            }

            // Check collision with gameWorld objects
            boolean blocked = false;
            for (CollisionObject obj : gameWorld.getObjects()) {
                if (spawnX + enemyWidth > obj.posX && spawnX - enemyWidth / 2f < obj.posX + obj.width &&
                    spawnY + enemyHeight > obj.posY && spawnY - enemyHeight / 2f < obj.posY + obj.height) {
                    blocked = true;
                    break;
                }
            }

            if (!blocked) {
                valid = true;
                break;
            }
        }

        if (!valid) System.out.println("Warning: Forced spawn for " + type);

        Enemy newEnemy;
        switch (type) {
            case TEMPLAR -> newEnemy = new Templar(spawnX, spawnY, gameWorld);
            case NUN -> newEnemy = new Nun(spawnX, spawnY, gameWorld);
            case PRIEST -> newEnemy = new ExplodingPriest(spawnX, spawnY, gameWorld);
            default -> newEnemy = new Templar(spawnX, spawnY, gameWorld);
        }

        gameWorld.enemies.add(newEnemy);
    }
}
