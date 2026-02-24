package com.littleDevil.game;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.audio.Sound;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameWorld {

    public GameScreen gameScreen;

    // World dimensions
    public final int mapWidth, mapHeight, tileSize;
    public final int widthInTiles, heightInTiles;

    // Entities
    public Player player;
    public List<Enemy> enemies;
    private List<GameCandle> candles;

    // Altars
    public BigAltar bigAltar;
    public SmallAltar smallAltarTopLeft, smallAltarTopRight, smallAltarBotRight, smallAltarBotLeft;

    // Map and collision
    public Texture mapTexture;
    private final int[][] grid;
    private final List<CollisionObject> objects = new ArrayList<>();
    public boolean[][] collisionGrid;

    // WAVE SYSTEM
    public boolean waveActive = false;
    public int enemiesAlive = 0;
    public float timeSinceLastHit = 0f;
    public final float comboTimeout = 3f;
    public boolean canStartNextWave = true;
    public Sound waveSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/deathBellSound.mp3"));

    public float nextWaveTimer = 0f;
    public float TIME_BETWEEN_WAVES = 5f;
    public boolean waitingForNextWave = false;

    //private float statPrintTimer = 0f; - for debug

    // enemy spawner
    public Spawner spawner;

    // cannot start before this is true
    public boolean basicTutorialDone = false;

    // augments
    public AugmentManager augmentManager;
    public int pendingAugments = 0;

    // Candle Decorations
    private Texture candleSheet;

    // Assets
    private Texture orbSheet;
    private Sound orbPickupSound;

    Texture pixel = new Texture("whitePixel.png");
    Texture potionSheet = new Texture("Spritesheets/nunPotionSpritesheet.png");
    public Texture explosionTexture = new Texture("Spritesheets/explosionAnimation.png");
    public Texture healingTexture = new Texture("Spritesheets/healingAnimation.png");

    // Path finding
    private final float PATH_UPDATE_INTERVAL = 1f;

    // Tile Types
    public enum TileType { BLOCK, STAIRS, ALTAR, BOOST, BLOCKENEMY }

    // Damage Texts
    public ArrayList<DamageText> damageTexts = new ArrayList<>();
    public BitmapFont damageFont;

    private final List<Enemy> enemiesToRemove = new ArrayList<>();

    public List<Orb> orbs = new ArrayList<>();
    public List<BottleProjectile> potions = new ArrayList<>();
    public List<Explosion> explosions = new ArrayList<>();
    public ArrayList<HealingAnimation> healAnimations = new ArrayList<>();

    public enum EnemyType { TEMPLAR, NUN, PRIEST }

    public float score = 0f;
    public int wave = 1;
    public int combo = 0;
    public boolean gameWon = false;

    // player prefs
    Preferences prefs = Gdx.app.getPreferences("MyGameInfo");
    public int difficulty = prefs.getInteger("difficulty", 0);

    // difficulty changes
    private float passiveScoreTimer = 0f;
    private final int[] PASSIVE_SCORE_RATES = { 1, 2, 3, 5 };

    private float victoryTimer = 0f;
    private final float VICTORY_DELAY = 4.0f;

    // wave variables
    public int totalEnemiesInWave = 0;
    public int enemiesKilledInWave = 0;

    public GameWorld(int mapWidth, int mapHeight, int tileSize, GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.tileSize = tileSize;
        this.widthInTiles = mapWidth / tileSize;
        this.heightInTiles = mapHeight / tileSize;
        this.grid = new int[heightInTiles][widthInTiles];
    }

    public void initialize() {

        basicTutorialDone = false;

        // Map
        mapTexture = new Texture("MapAssets/map.png");

        // Orb texture
        orbSheet = new Texture("Spritesheets/xpOrbs.png");
        orbPickupSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/xpPickupSound.mp3"));

        // Player
        player = new Player(302, 236, "Spritesheets/playerSpritesheet.png", this);

        // Enemies
        enemies = new ArrayList<>();

        // init augment
        augmentManager = new AugmentManager(player);

        // Wait for trigger from tutorial
        waitingForNextWave = false;
        canStartNextWave = false;

        // spawner
        spawner = new Spawner(this);

        // Altars
        bigAltar = new BigAltar(262, 200, "Spritesheets/bigAltarSpritesheet.png");
        smallAltarTopLeft = new SmallAltar(66, 314, "Spritesheets/littleAltarSpritesheet.png", 7f, CollisionObjectsData.collisionObjects[0]);
        smallAltarTopRight = new SmallAltar(498, 314, "Spritesheets/littleAltarSpritesheet.png", 7f, CollisionObjectsData.collisionObjects[1]);
        smallAltarBotRight = new SmallAltar(498, 50, "Spritesheets/littleAltarSpritesheet.png", 7f, CollisionObjectsData.collisionObjects[2]);
        smallAltarBotLeft = new SmallAltar(66, 50, "Spritesheets/littleAltarSpritesheet.png", 7f, CollisionObjectsData.collisionObjects[3]);

        // Candles
        candleSheet = new Texture("Spritesheets/candleSmallSpritesheet.png");
        candles = new ArrayList<>();
        candles.add(new GameCandle(candleSheet, 225, 180));
        candles.add(new GameCandle(candleSheet, 225, 224));
        candles.add(new GameCandle(candleSheet, 253, 252));
        candles.add(new GameCandle(candleSheet, 358, 180));
        candles.add(new GameCandle(candleSheet, 358, 224));
        candles.add(new GameCandle(candleSheet, 330, 252));

        // Collision objects
        for (CollisionObject obj : CollisionObjectsData.collisionObjects) addObject(obj);
        addObject(bigAltar.interactionBox);
        addObject(smallAltarTopLeft.interactionBox);
        addObject(smallAltarTopRight.interactionBox);
        addObject(smallAltarBotRight.interactionBox);
        addObject(smallAltarBotLeft.interactionBox);

        // Generates grid used in Pathfinder Class
        generateCollisionGrid();

        // Damage Font init
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("pixelon.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = (int)(mapHeight * 0.03f);
        param.color = Color.WHITE;
        damageFont = generator.generateFont(param);
        generator.dispose();
    }

    // update all the logic
    public void update(float delta, GameScreen gameScreen) {

        // 1. STAT PRINTER (Keep this outside the pause so it still prints in menus if needed)
        /*
        statPrintTimer += delta;
        if (statPrintTimer >= 1f) {
            statPrintTimer = 0f;
            System.out.println("--- SECONDS LOG: PLAYER STATS ---");
            System.out.println("Health: " + (int)player.currentHP + "/" + (int)player.baseHP);
            System.out.println("Armor: " + (int)player.armor);
            System.out.println("Damage: " + player.baseDamage);
            System.out.println("Lifesteal: " + (int)player.lifesteal);
            System.out.println("Luck: " + player.luck);
            System.out.println("Pending Augments: " + pendingAugments);
            System.out.println("---------------------------------");
        }
         */

        if (gameWon) {
            victoryTimer += delta;

            if (victoryTimer >= VICTORY_DELAY) {
                gameScreen.game.setScreen(new StartScreen(gameScreen.game));
                return;
            }
        }

        if (player.isOnAltar(this) && bigAltar.isFullyOpen()) {

            // Priority 1: Pending Augments
            if (pendingAugments > 0) {
                if (!gameScreen.uiManager.isAugmentPageOpen()) {
                    gameScreen.uiManager.openAugmentPage();
                }
            }
            // Priority 2: Standard Upgrades (Skill Points)
            else if (player.skillPoints > 0) {
                if (!gameScreen.uiManager.isUpgradePageOpen()) {
                    gameScreen.uiManager.openUpgradePage();
                }
            }
        }

        // --- EVERYTHING BELOW THIS LINE IS PAUSED DURING MENUS ---

        player.update(delta, this);

        // 4. Process Spawn Logic FROM SPAWNER
        if (waveActive && !player.isOnAltar(this) && !player.isUnreachable(this)) {

            passiveScoreTimer += delta;
            if (passiveScoreTimer >= 1f) {
                passiveScoreTimer = 0f;
                int diffIndex = MathUtils.clamp(difficulty, 0, PASSIVE_SCORE_RATES.length - 1);
                score += PASSIVE_SCORE_RATES[diffIndex];
            }

            spawner.update(delta);
        }

        // 5. Wave Cooldown Logic
        if (waitingForNextWave) {

            if (nextWaveTimer >= TIME_BETWEEN_WAVES) {
                waitingForNextWave = false;
                nextWaveTimer = 0f;
                canStartNextWave = true;
                startWave();
            }
        }

        // 6. Update Entities
        for(Enemy enemy : enemies) {
            enemy.updatePathsForEnemy(delta, player, this, PATH_UPDATE_INTERVAL);
            enemy.update(delta, player, this, gameScreen);
        }

        // 7. Update Mechanics
        if (combo > 0) {
            timeSinceLastHit += delta;
            if (timeSinceLastHit > comboTimeout) combo = 0;
        }

        // 8. Remove Dead Enemies
        if (!enemiesToRemove.isEmpty()) {
            enemies.removeAll(enemiesToRemove);
            enemiesToRemove.clear();
        }

        if (waveActive &&
            spawner.isSpawningFinished() &&
            enemies.isEmpty()) {

            endWave();
        }

        // 9. Update Objects & Decor
        for (GameCandle candle : candles) candle.update(delta);
        bigAltar.update(delta, player, this);
        smallAltarTopLeft.update(delta, player, this);
        smallAltarTopRight.update(delta, player, this);
        smallAltarBotRight.update(delta, player, this);
        smallAltarBotLeft.update(delta, player, this);

        // 10. Update Effects & Projectiles
        for (int i = damageTexts.size() - 1; i >= 0; i--) {
            DamageText dt = damageTexts.get(i);
            dt.update(delta);
            if (dt.finished) damageTexts.remove(i);
        }
        for (int i = orbs.size() - 1; i >= 0; i--) {
            Orb orb = orbs.get(i);
            orb.update(delta, player, this);
            if (!orb.isAlive()) orbs.remove(i);
        }
        for (int i = potions.size() - 1; i >= 0; i--) {
            BottleProjectile p = potions.get(i);
            p.update(delta);
            if (p.isDead()) potions.remove(i);
        }
        for (int i = explosions.size() - 1; i >= 0; i--) {
            Explosion e = explosions.get(i);
            e.update(delta);
            if (e.done) explosions.remove(i);
        }
        for (int i = healAnimations.size() - 1; i >= 0; i--) {
            HealingAnimation h = healAnimations.get(i);
            h.update(delta);
            if (h.done) healAnimations.remove(i);
        }
    }

    public void render(SpriteBatch batch) {
        batch.draw(mapTexture, 0, 0, mapWidth, mapHeight);

        // Add other objects underneath
        smallAltarTopLeft.render(batch);
        smallAltarTopRight.render(batch);
        smallAltarBotRight.render(batch);
        smallAltarBotLeft.render(batch);
        bigAltar.render(batch);
        for (GameCandle candle : candles) {
            candle.draw(batch);
        }

        for (BottleProjectile p : potions) {
            p.render(batch);
        }

        // Player sword
        player.renderSword(batch);
        for (HealingAnimation h : healAnimations) h.render(batch);

        List<RenderEntity> renderList = new ArrayList<>();

        // Collision objects with texture
        for (CollisionObject obj : objects) {
            if (obj.texture != null) {
                renderList.add(new RenderEntity(
                    new TextureRegion(obj.texture),
                    obj.posX, obj.posY,
                    obj.width, obj.height,
                    obj.posY,
                    1f
                ));
            }
        }

        // Enemy objects
        for (Enemy e : enemies) {
            renderList.add(createRenderEntity(e.getCurrentFrame(), e.x, e.y, e.alpha));
        }

        // Player object
        renderList.add(createRenderEntity(player.getCurrentFrame(), player.x, player.y, 1f));

        renderList.sort((a, b) -> Integer.compare(b.baseY, a.baseY));
        for (RenderEntity e : renderList){
            batch.setColor(1f, 1f, 1f, e.alpha);
            batch.draw(e.region, e.x, e.y, e.width, e.height);
        }
        batch.setColor(1f, 1f, 1f, 1f); // Back to white
        renderList.clear();

        // For debugging set this to true
        renderDebug(false, batch);

        // Renders shield over anything
        for (Enemy e : enemies) {
            if (e instanceof Templar templar) {
                templar.renderShield(batch, player);
            }
        }

        // render damage texts
        for (DamageText dt : damageTexts) dt.render(batch);
        for (Orb orb : orbs) {
            orb.render(batch);
        }
        for (Explosion e : explosions) e.render(batch);
    }

    public List<CollisionObject> getObjects() {
        return objects;
    }

    // Adds a CollisionObject to the objects array of the GameWorld
    public void addObject(CollisionObject obj) {
        objects.add(obj);
        obj.markOnGrid(grid, tileSize);
    }

    // Get tile type for player and enemies collision, altars, boosts...
    public boolean isTileType(int tileX, int tileY, TileType type) {
        if (tileX < 0 || tileX >= widthInTiles || tileY < 0 || tileY >= heightInTiles) {

            // BLOCK
            if (type == TileType.BLOCK || type == TileType.BLOCKENEMY) {
                return true;
            }
            return false;
        }
        // --------------------

        int tile = grid[tileY][tileX];

        if (tile < 0) return false;
        return switch (type) {
            case BLOCK -> tile == 1 || tile == 2;
            case ALTAR -> tile == 3;
            case BOOST -> tile == 4;
            case STAIRS -> tile == 5;
            case BLOCKENEMY -> tile == 6;
        };
    }

    // Generates a collision grid for enemies pathfinding based on grid
    public void generateCollisionGrid() {
        collisionGrid = new boolean[heightInTiles][widthInTiles];

        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                int tile = grid[y][x];
                if(tile == 1 || tile == 2 || tile == 6){
                    collisionGrid[y][x] = true;
                }
            }
        }
    }

    // Function for rendering the debug objects and hitboxes
    private void renderDebug(boolean draw, SpriteBatch batch) {
        if(!draw) return;
        for(Enemy e: enemies) {
            e.renderPath(batch, pixel, this);
            e.renderCollisionBox(batch, pixel);
            e.renderHitbox(batch, pixel);
            if(e instanceof Templar) {
                ((Templar) e).renderShieldHitbox(batch, pixel);
            }
        }

        // Render tile debug overlay
        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {

                int tile = grid[y][x];

                // pick color based on tile value
                switch (tile) {
                    case 1, 2 -> batch.setColor(1f, 0f, 0f, 0.35f);       // BLOCK → red
                    case 3     -> batch.setColor(0.6f, 0f, 1f, 0.35f);     // ALTAR → purple
                    case 4     -> batch.setColor(1f, 1f, 0f, 0.35f);       // BOOST → yellow
                    case 5     -> batch.setColor(0.2f, 0.4f, 1f, 0.35f);   // STAIRS → blue
                    case 6     -> batch.setColor(1f, 0.5f, 0f, 0.35f);     // BLOCKENEMY → orange
                    default    -> {
                        continue;                                                     // EMPTY → skip drawing
                    }
                }

                // draw the tile
                batch.draw(pixel, x * tileSize, y * tileSize, tileSize, tileSize);
            }
        }

        batch.setColor(1f, 1f, 1f, 1f);

    }

    // Functions to display UI in GameScreen Class
    public int getScore() {
        return (int)score;
    }

    public int getWave() {
        return wave;
    }

    public int getCombo() {
        return combo;
    }

    public void spawnDamage(float x, float y, int amount, Color color, float scale) {
        damageTexts.add(new DamageText(x, y, String.valueOf(amount), 0.7f, damageFont, color, scale));
    }

    public void spawnText(float x, float y, String message, Color color, float scale) {
        damageTexts.add(new DamageText(x, y, message, 1f, damageFont, color, scale));
    }

    // Class that implements sortable entities for drawing
    private static class RenderEntity {
        TextureRegion region;
        float x, y, width, height;
        int baseY;
        float alpha;

        RenderEntity(TextureRegion region, float x, float y, float width, float height, int baseY, float alpha) {
            this.region = region;
            this.x = x; this.y = y; this.width = width; this.height = height; this.baseY = baseY;
            this.alpha = alpha;
        }
    }

    // Clean entity creation for inserting into the list
    private RenderEntity createRenderEntity(TextureRegion region, float entityX, float entityY, float alpha) {
        float width = region.getRegionWidth();
        float height = region.getRegionHeight();

        float drawX = entityX - width / 2f;
        float drawY = entityY - height / 2f;

        int baseY = (int)(entityY - height / 2f);

        return new RenderEntity(region, drawX, drawY, width, height, baseY, alpha);
    }


    public void removeEnemy(Enemy enemy) {
        if (enemy == null) return;
        if (enemiesToRemove.contains(enemy)) return;

        enemiesToRemove.add(enemy);

        if (waveActive) {
            enemiesAlive--;
        }
    }

    public void spawnOrbs(Enemy enemy, Player player) {
        Random rand = new Random();
        float angleToPlayer = (float) Math.atan2(player.y - enemy.y, player.x - enemy.x);
        float backAngle = angleToPlayer + (float) Math.PI;

        // Luck factor for general drops
        float luckFactor = player.luck * 0.2f;

        // --- 1. SKILL POINT DROP ---
        // Boosted by Grave Looter, capped at 5%
        float skillPointChance = MathUtils.clamp(0.005f + (luckFactor * 0.1f) + (player.hasGraveLooter ? 0.02f : 0f), 0f, 0.05f);
        if (MathUtils.random() < skillPointChance) {
            player.skillPoints++;
            spawnText(enemy.x, enemy.y + 20, "SKILL POINT!", Color.GOLD, 1.2f);
        }

        // --- 2. GUARANTEED & BONUS XP ORBS ---
        for (int i = 0; i < enemy.guaranteedOrbsCounts.length; i++) {
            Orb.OrbType type = (i == 1) ? Orb.OrbType.RARE : (i == 2 ? Orb.OrbType.GOLD : Orb.OrbType.COMMON);
            int count = (int) enemy.guaranteedOrbsCounts[i];

            // Chance for extra XP orbs (i == 0 is XP type)
            float extraOrbChance = luckFactor * 2;
            if (player.hasGraveLooter) extraOrbChance += 0.2f; // Grave Looter gives +20% extra orb chance

            if (i == 0 && MathUtils.random() < MathUtils.clamp(extraOrbChance, 0f, 0.8f)) {
                count++;
                spawnText(enemy.x, enemy.y + 10, "BONUS XP", new Color(0.8f, 0.4f, 1f, 1f), 0.8f);
            }

            for (int j = 0; j < count; j++) {
                spawnSingleOrb(enemy, backAngle, type, 65f, 20f, rand, player);
            }
        }

        // --- 3. EXTRA RANDOM ORB CHANCES ---
        for (int i = 0; i < enemy.firstExtraChances.length; i++) {
            float baseChance = enemy.firstExtraChances[i];
            float luckyChance = baseChance + ((1f - baseChance) * luckFactor);

            // Grave Looter adds a flat 15% to these random drop chances
            if (player.hasGraveLooter) luckyChance += 0.15f;

            if (rand.nextFloat() < MathUtils.clamp(luckyChance, 0f, 0.95f)) {
                Orb.OrbType type = (i == 1) ? Orb.OrbType.RARE : (i == 2 ? Orb.OrbType.GOLD : Orb.OrbType.COMMON);
                spawnSingleOrb(enemy, backAngle, type, 100f, 30f, rand, player);
            }
        }

        // --- 4. GRAVE LOOTER: DIRECT STAT BOOSTS ---
        if (player.hasGraveLooter) {
            // Rare chance (capped at 5%) to get a temporary boost directly upon kill
            float boostDropChance = MathUtils.clamp(luckFactor * 0.5f, 0.01f, 0.05f);

            if (rand.nextFloat() < boostDropChance) {
                int choice = MathUtils.random(2); // 0 = Speed, 1 = Damage, 2 = HP

                switch (choice) {
                    case 0: // Speed Boost
                        player.boostSpeed(8f, 1.3f);
                        spawnText(enemy.x, enemy.y + 30, "+SPEED", Color.CYAN, 1.1f);
                        break;
                    case 1: // Damage Boost
                        player.boostDamage(8f, 1.3f);
                        spawnText(enemy.x, enemy.y + 30, "+DAMAGE", Color.RED, 1.1f);
                        break;
                    case 2: // HP Boost
                        player.boostHP(player.baseHP * 0.15f); // 15% HP heal/boost
                        spawnText(enemy.x, enemy.y + 30, "+HEALTH", Color.GREEN, 1.1f);
                        break;
                }
            }
        }
    }

    // Handles wall checking before spawn to prevent instant sticking
    private void spawnSingleOrb(Enemy enemy, float backAngle, Orb.OrbType type, float baseSpeed, float randSpeed, Random rand, Player player) {
        float offset = ((rand.nextFloat() - 0.5f) * (float) Math.PI);
        float angle = backAngle + offset;
        float speed = baseSpeed + rand.nextFloat() * randSpeed;

        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;

        // PREDICTIVE SPAWN LOGIC:

        float checkDist = 20f;
        float futureX = enemy.x + (vx > 0 ? checkDist : -checkDist);
        float futureY = enemy.y + (vy > 0 ? checkDist : -checkDist);

        boolean blockedX = isTileType((int)(futureX / tileSize), (int)(enemy.y / tileSize), TileType.BLOCK);
        boolean blockedY = isTileType((int)(enemy.x / tileSize), (int)(futureY / tileSize), TileType.BLOCK);

        if (blockedX || blockedY) {
            // Blocked! Shoot towards player.
            float angleToPlayer = (float) Math.atan2(player.y - enemy.y, player.x - enemy.x);
            angleToPlayer += (rand.nextFloat() - 0.5f) * 0.5f;

            vx = (float) Math.cos(angleToPlayer) * speed;
            vy = (float) Math.sin(angleToPlayer) * speed;
        }

        orbs.add(new Orb(enemy.x, enemy.y, type, orbSheet, new Vector2(vx, vy)));
    }

    public void spawnBottleToPoint(float startX, float startY, float targetX, float targetY, float damage) {
        BottleProjectile p = new BottleProjectile(potionSheet, startX, startY, targetX, targetY, this, damage);
        potions.add(p);
    }

    public void startWave() {
        if (!canStartNextWave) return;

        waveSound.play(0.2f);

        waveActive = true;
        canStartNextWave = false;
        combo = 0;
        timeSinceLastHit = 0f;

        // Clamp wave index
        int index = Math.min(wave - 1, WaveManager.waves.length - 1);
        Wave w = WaveManager.waves[index];

        totalEnemiesInWave = w.templars + w.nuns + w.priests;
        enemiesKilledInWave = 0;

        Spawner.Difficulty diffEnum;
        switch (difficulty) {
            case 0 -> diffEnum = Spawner.Difficulty.EASY;
            case 1 -> diffEnum = Spawner.Difficulty.HARD;
            case 2 -> diffEnum = Spawner.Difficulty.CRAZY;
            case 3 -> diffEnum = Spawner.Difficulty.HELL;
            default -> diffEnum = Spawner.Difficulty.HARD;
        }

        spawner.startWave(wave, w, diffEnum);
    }

    public void endWave() {
        if (wave == 10) {
            waveActive = false;
            gameWon = true;
            waitingForNextWave = false;

            String key = "difficulty_" + difficulty + "_completed";
            if (!prefs.getBoolean(key, false)) {
                prefs.putBoolean(key, true);
                prefs.flush();
            }

            spawnText(player.x, player.y + 20, "STAGE CLEAR!", Color.WHITE, 2f);

            Preferences prefs = Gdx.app.getPreferences("MyGameInfo");
            int currentHighscore = prefs.getInteger("highscore", 0);
            if ((int)score > currentHighscore) {
                prefs.putInteger("highscore", (int)score);
                prefs.flush();
            }

            //System.out.println(">>> VICTORY! <<<");
            return;
        }

        waveActive = false;
        waitingForNextWave = true;
        nextWaveTimer = 0f;
        canStartNextWave = false;
        wave++;
    }

    public void playOrbSound(){
        float randomVolume = 0.05f + (float)Math.random() * 0.02f;
        float randomPitch = 0.8f + (float)Math.random() * 0.5f;
        orbPickupSound.play(randomVolume, randomPitch, 0f);
    }

    public void dispose() {
        mapTexture.dispose();
        candleSheet.dispose();
        if (orbSheet != null) orbSheet.dispose();
        if (player != null) player.dispose();
        for (Enemy e : enemies) e.dispose();
        bigAltar.dispose();
        smallAltarTopLeft.dispose();
        smallAltarTopRight.dispose();
        smallAltarBotRight.dispose();
        smallAltarBotLeft.dispose();
        augmentManager.dispose();
        orbPickupSound.dispose();
    }
}
