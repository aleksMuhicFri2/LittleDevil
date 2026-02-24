package com.littleDevil.game;

import com.badlogic.gdx.math.MathUtils;

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

    public enum Difficulty {
        EASY(1.2f),    // 50% slower spawns (Enemies trickle in)
        HARD(1.1f),  // Standard speed
        CRAZY(0.9f),    // 30% faster spawns (Chaos)
        HELL(0.8f);

        final float multiplier;
        Difficulty(float multiplier) {
            this.multiplier = multiplier;
        }
    }

    public Spawner(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    // enemies finish spawning at the same time
    public void startWave(int waveNumber, Wave waveData, Difficulty difficulty) {
        this.templarsRemaining = waveData.templars;
        this.nunsRemaining = waveData.nuns;
        this.priestsRemaining = waveData.priests;

        float baseDuration = 15f + (waveNumber * 4f);
        float targetWaveDuration = baseDuration * difficulty.multiplier;

        // Calculate intervals
        if (templarsRemaining > 0) {
            templarInterval = targetWaveDuration / templarsRemaining;
            templarTimer = templarInterval;
        }

        if (nunsRemaining > 0) {
            nunInterval = targetWaveDuration / nunsRemaining;
            nunTimer = nunInterval;
        }

        if (priestsRemaining > 0) {
            priestInterval = targetWaveDuration / priestsRemaining;
            priestTimer = priestInterval;
        }

        //System.out.println("Wave " + waveNumber + " Started (" + difficulty + ")");
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
