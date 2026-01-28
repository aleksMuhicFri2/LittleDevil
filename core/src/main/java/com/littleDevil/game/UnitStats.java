package com.littleDevil.game;

public class UnitStats {

    // --- DIFFICULTY MULTIPLIERS (4 Levels) ---
    // Index 0 = Easy, 1 = Normal, 2 = Hard, 3 = Nightmare

    private static final float[] DIFF_HP_MULT    = { 0.8f,  1.0f,  1.5f,  2.2f };
    private static final float[] DIFF_DMG_MULT   = { 0.8f,  1.0f,  1.5f,  2f };
    private static final float[] DIFF_SPD_MULT   = { 0.9f, 1.0f,  1.1f, 1.25f };
    private static final float[] DIFF_SCORE_MULT = { 0.7f,  1.0f,  1.5f,  2.5f };

    // --- CONFIGURATION ---
    public enum UnitType {
        // Format:     BaseHP,      HpPerWave,        BaseDmg,      DmgPerWave,         Speed,     Int,   BASE_SCORE

        TEMPLAR       (150f,    15f,        20f,     2f,         30f,   0.3f,  100),
        NUN           (100f,    10f,        20f,     3f,         45f,   0.15f, 200),
        PRIEST        (120f,    14f,        40f,     3f,         70f,   0.2f,  500);

        final float baseHp;
        final float hpPerWave;
        final float baseDmg;
        final float dmgPerWave;
        final float baseSpeed;
        final float intPerWave;
        final int baseScore;

        UnitType(float hp, float hpInc, float dmg, float dmgInc, float spd, float intInc, int score) {
            this.baseHp = hp;
            this.hpPerWave = hpInc;
            this.baseDmg = dmg;
            this.dmgPerWave = dmgInc;
            this.baseSpeed = spd;
            this.intPerWave = intInc;
            this.baseScore = score;
        }
    }

    // --- DATA CONTAINER ---
    public static class StatsResult {
        public float maxHp;
        public float damage;
        public float speed;
        public float intelligence;
        public int score;
    }

    // --- CALCULATION ---
    public static StatsResult get(UnitType type, int wave, int difficultyIndex) {
        StatsResult result = new StatsResult();

        // Safety clamp for difficulty index (0 to 3)
        if (difficultyIndex < 0) difficultyIndex = 0;
        if (difficultyIndex >= DIFF_HP_MULT.length) difficultyIndex = DIFF_HP_MULT.length - 1;

        // Get Multipliers based on difficulty
        float hpMult = DIFF_HP_MULT[difficultyIndex];
        float dmgMult = DIFF_DMG_MULT[difficultyIndex];
        float spdMult = DIFF_SPD_MULT[difficultyIndex];
        float scoreMult = DIFF_SCORE_MULT[difficultyIndex];

        // 1. Calculate HP
        // Formula: (Base + (Wave-1 * Growth)) * DifficultyMultiplier
        result.maxHp = (type.baseHp + ((wave - 1) * type.hpPerWave)) * hpMult;

        // 2. Calculate Damage
        result.damage = (type.baseDmg + ((wave - 1) * type.dmgPerWave)) * dmgMult;

        // 3. Calculate Speed
        result.speed = type.baseSpeed * spdMult;

        // 4. Calculate Intelligence
        // Base 1.0 + (Wave * Growth). Capped at 5.0 to prevent bugs.
        result.intelligence = 1f + ((wave - 1) * type.intPerWave);
        if (result.intelligence > 5.0f) result.intelligence = 5.0f;

        // 5. Calculate Score Reward
        result.score = (int) (type.baseScore * scoreMult);

        return result;
    }
}
