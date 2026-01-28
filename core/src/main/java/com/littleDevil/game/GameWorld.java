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
    public float TIME_BETWEEN_WAVES = 10f;
    public boolean waitingForNextWave = false;

    // --- NEW INDEPENDENT SPAWN SYSTEM ---
    // Spawn Intervals
    private final float TEMPLAR_SPAWN_INTERVAL = 5f;
    private final float NUN_SPAWN_INTERVAL = 12f;
    private final float PRIEST_SPAWN_INTERVAL = 20f;

    // Timers
    private float templarTimer = 0f;
    private float nunTimer = 0f;
    private float priestTimer = 0f;

    // Remaining count for current wave
    private int templarsToSpawn = 0;
    private int nunsToSpawn = 0;
    private int priestsToSpawn = 0;
    // ------------------------------------

    public boolean basicTutorialDone = false;

    public AugmentManager augmentManager;
    // Flag to track if the player has reached a milestone level and needs to pick an augment
    public boolean augmentSelectionPending = false;
    private int lastAugmentLevel = 0; // Track last level we got an augment

    // Candle Decorations
    private Texture candleSheet;

    // NEW: Loaded once to save performance
    private Texture orbSheet;
    private Sound orbPickupSound;

    Texture pixel = new Texture("whitePixel.png");
    Texture potionSheet = new Texture("Spritesheets/nunPotionSpritesheet.png");
    public Texture explosionTexture = new Texture("Spritesheets/explosionAnimation.png");
    public Texture healingTexture = new Texture("Spritesheets/healingAnimation.png");

    // Pathing
    private final float PATH_UPDATE_INTERVAL = 1f; // 1 update per second (BASE)

    // Tile Types
    public enum TileType { BLOCK, STAIRS, ALTAR, BOOST, BLOCKENEMY }

    // Damage Texts
    public ArrayList<DamageText> damageTexts = new ArrayList<>();
    public BitmapFont damageFont;

    // For easier removing from the scene
    private final List<Enemy> enemiesToRemove = new ArrayList<>();

    public List<Orb> orbs = new ArrayList<>();
    public List<BottleProjectile> potions = new ArrayList<>();
    public List<Explosion> explosions = new ArrayList<>();
    public ArrayList<HealingAnimation> healAnimations = new ArrayList<>();

    public enum EnemyType {
        TEMPLAR,
        NUN,
        PRIEST
    }

    // Public UI info to be displayed on screen
    public float score = 0f;
    public int wave = 1; // Default to 1
    public int combo = 0;

    public boolean gameWon = false;

    Preferences prefs = Gdx.app.getPreferences("MyGameInfo");
    public int difficulty = prefs.getInteger("difficulty", 0);

    private float passiveScoreTimer = 0f;
    private final int[] PASSIVE_SCORE_RATES = { 1, 2, 3, 5 };

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

        // NEW: Load orb texture once
        orbSheet = new Texture("Spritesheets/xpOrbs.png");
        orbPickupSound = Gdx.audio.newSound(Gdx.files.internal("Sounds/xpPickupSound.mp3"));

        // Player
        player = new Player(302, 236, "Spritesheets/playerSpriteSheet.png", this);

        // Enemies
        enemies = new ArrayList<>();

        // Wait for trigger from tutorial
        waitingForNextWave = false;
        canStartNextWave = false;

        // Altars
        bigAltar = new BigAltar(262, 200, "Spritesheets/bigAltarSpritesheet.png");
        smallAltarTopLeft = new SmallAltar(66, 314, "Spritesheets/littleAltarSpritesheet.png", 5f, CollisionObjectsData.collisionObjects[0]);
        smallAltarTopRight = new SmallAltar(498, 314, "Spritesheets/littleAltarSpritesheet.png", 5f, CollisionObjectsData.collisionObjects[1]);
        smallAltarBotRight = new SmallAltar(498, 50, "Spritesheets/littleAltarSpritesheet.png", 5f, CollisionObjectsData.collisionObjects[2]);
        smallAltarBotLeft = new SmallAltar(66, 50, "Spritesheets/littleAltarSpritesheet.png", 5f, CollisionObjectsData.collisionObjects[3]);

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
        param.size = (int)(Gdx.graphics.getHeight() * 0.022f);
        param.color = Color.WHITE;
        damageFont = generator.generateFont(param);
        generator.dispose();

        augmentManager = new AugmentManager();
    }

    // update all the logic
    public void update(float delta, GameScreen gameScreen) {
        player.update(delta, this);

        // 1. Process Spawn Logic (Independent Timers)
        if (waveActive && !player.isOnAltar(this)) {

            passiveScoreTimer += delta;
            if (passiveScoreTimer >= 1f) {
                passiveScoreTimer = 0f;
                int diffIndex = MathUtils.clamp(difficulty, 0, PASSIVE_SCORE_RATES.length - 1);
                score += PASSIVE_SCORE_RATES[diffIndex];
            }

            // Templar Spawning (5s)
            if (templarsToSpawn > 0) {
                templarTimer += delta;
                if (templarTimer >= TEMPLAR_SPAWN_INTERVAL) {
                    templarTimer = 0f;
                    spawnEnemy(EnemyType.TEMPLAR);
                    templarsToSpawn--;
                }
            }

            // Nun Spawning (12s)
            if (nunsToSpawn > 0) {
                nunTimer += delta;
                if (nunTimer >= NUN_SPAWN_INTERVAL) {
                    nunTimer = 0f;
                    spawnEnemy(EnemyType.NUN);
                    nunsToSpawn--;
                }
            }

            // Priest Spawning (20s)
            if (priestsToSpawn > 0) {
                priestTimer += delta;
                if (priestTimer >= PRIEST_SPAWN_INTERVAL) {
                    priestTimer = 0f;
                    spawnEnemy(EnemyType.PRIEST);
                    priestsToSpawn--;
                }
            }
        }

        // 2. Wave Cooldown Logic
        if (waitingForNextWave) {
            int previousSeconds = (int) Math.ceil(TIME_BETWEEN_WAVES - nextWaveTimer);
            nextWaveTimer += delta;
            int currentSeconds = (int) Math.ceil(TIME_BETWEEN_WAVES - nextWaveTimer);

            if (currentSeconds != previousSeconds && currentSeconds >= 0) {
                System.out.println("Wave " + wave + " starting in: " + currentSeconds + "s");
            }

            if (nextWaveTimer >= TIME_BETWEEN_WAVES) {
                waitingForNextWave = false;
                nextWaveTimer = 0f;
                canStartNextWave = true;
                startWave();
            }
        }

        // 3. Update Entities
        for(Enemy enemy :  enemies) {
            enemy.updatePathsForEnemy(delta, player, this, PATH_UPDATE_INTERVAL);
            enemy.update(delta, player, this, gameScreen);
        }

        // 4. Update Mechanics
        if (combo > 0) {
            timeSinceLastHit += delta;
            if (timeSinceLastHit > comboTimeout) combo = 0;
        }

        // 5. Remove Dead Enemies
        if (!enemiesToRemove.isEmpty()) {
            enemies.removeAll(enemiesToRemove);
            enemiesToRemove.clear();
        }

        if (waveActive &&
            templarsToSpawn == 0 &&
            nunsToSpawn == 0 &&
            priestsToSpawn == 0 &&
            enemies.isEmpty()) {

            endWave();
        }

        // 7. Update objects
        for (GameCandle candle : candles) candle.update(delta);
        bigAltar.update(delta, player,this);
        smallAltarTopLeft.update(delta, player, this);
        smallAltarTopRight.update(delta, player, this);
        smallAltarBotRight.update(delta, player, this);
        smallAltarBotLeft.update(delta, player, this);

        // Update lists
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

        if (player.level % 5 == 0 && player.level > lastAugmentLevel && augmentManager.hasSpace()) {
            augmentSelectionPending = true;
            lastAugmentLevel = (int)player.level;
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

    // Adds a CollisionObject to the objects array of the GameWorld
    public void addObject(CollisionObject obj) {
        objects.add(obj);
        obj.markOnGrid(grid, tileSize);
    }

    // Get tile type for player and enemies collision, altars, boosts...
    public boolean isTileType(int tileX, int tileY, TileType type) {
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

    // Function for rendering the debug objects
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
                        continue;                                // EMPTY → skip drawing
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
        damageTexts.add(new DamageText(x, y, message, 0.7f, damageFont, color, scale));
    }

    public void dispose() {
        mapTexture.dispose();
        candleSheet.dispose();

        // NEW: Dispose the orb sheet
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

    public void spawnEnemy(EnemyType type) {
        float enemyWidth = 32f;
        float enemyHeight = 32f;

        float spawnX = 0;
        float spawnY = 0;
        boolean valid = false;

        // Try up to 10 times to find a valid one of the 4 spots
        for (int i = 0; i < 10; i++) {

            // Pick a side: 0=top, 1=bottom, 2=left, 3=right
            int side = MathUtils.random(3);

            switch (side) {
                case 0 -> { // Top center
                    spawnX = mapWidth / 2f;
                    spawnY = mapHeight - enemyHeight;
                }
                case 1 -> { // Bottom center
                    spawnX = mapWidth / 2f;
                    spawnY = enemyHeight;
                }
                case 2 -> { // Left center
                    spawnX = enemyWidth;
                    spawnY = mapHeight / 2f;
                }
                case 3 -> { // Right center
                    spawnX = mapWidth - enemyWidth;
                    spawnY = mapHeight / 2f;
                }
            }

            // Check collision
            boolean blocked = false;
            for (CollisionObject obj : objects) {
                if (spawnX + enemyWidth > obj.posX && spawnX - enemyWidth / 2f < obj.posX + obj.width &&
                    spawnY + enemyHeight > obj.posY && spawnY - enemyHeight / 2f < obj.posY + obj.height) {
                    blocked = true;
                    break;
                }
            }

            if (!blocked) {
                valid = true;
                break; // Found a spot, exit loop
            }
        }

        // If we failed 10 times, force spawn at the last calculated position
        if (!valid) {
            System.out.println("Warning: Forced spawn for " + type);
        }

        Enemy newEnemy;
        switch (type) {
            case TEMPLAR -> newEnemy = new Templar(spawnX, spawnY, this);
            case NUN -> newEnemy = new Nun(spawnX, spawnY, this);
            case PRIEST -> newEnemy = new ExplodingPriest(spawnX, spawnY, this);
            default -> newEnemy = new Templar(spawnX, spawnY, this);
        }

        enemies.add(newEnemy);
    }


    public void removeEnemy(Enemy enemy) {
        if (enemy == null) return;
        if (enemiesToRemove.contains(enemy)) return;

        enemiesToRemove.add(enemy);

        if (waveActive) {
            enemiesAlive--; // Keeps UI accurate
        }
    }

    public void spawnOrbs(Enemy enemy, Player player) {
        Random rand = new Random();
        float angleToPlayer = (float) Math.atan2(player.y - enemy.y, player.x - enemy.x);
        float backAngle = angleToPlayer + (float) Math.PI;

        float luckFactor = player.luck * 0.2f;

        if (MathUtils.random() < 0.005f + (luckFactor * 0.1f)) {
            player.skillPoints++;
            spawnText(enemy.x, enemy.y + 20, "SKILL POINT!", Color.GOLD, 1.2f);
        }

        // 3. Process Guaranteed Orbs
        for (int i = 0; i < enemy.guaranteedOrbsCounts.length; i++) {
            Orb.OrbType type = (i == 1) ? Orb.OrbType.RARE : (i == 2 ? Orb.OrbType.GOLD : Orb.OrbType.COMMON);
            int count = (int) enemy.guaranteedOrbsCounts[i];

            // Luck Bonus: Chance for an extra XP orb if it's the XP type (index 0)
            if (i == 0 && MathUtils.random() < luckFactor * 2) {
                count++;
                spawnText(enemy.x, enemy.y + 10, "BONUS XP", new Color(0.8f, 0.4f, 1f, 1f), 0.8f);
            }

            for (int j = 0; j < count; j++) {
                spawnSingleOrb(enemy, backAngle, type, 65f, 20f, rand, player);
            }
        }

        for (int i = 0; i < enemy.firstExtraChances.length; i++) {
            float baseChance = enemy.firstExtraChances[i];
            float luckyChance = baseChance + ((1f - baseChance) * luckFactor);

            if (rand.nextFloat() < luckyChance) {
                Orb.OrbType type = (i == 1) ? Orb.OrbType.RARE : (i == 2 ? Orb.OrbType.GOLD : Orb.OrbType.COMMON);
                spawnSingleOrb(enemy, backAngle, type, 100f, 30f, rand, player);
            }
        }
    }

    // NEW HELPER: Handles wall checking before spawn to prevent instant sticking
    private void spawnSingleOrb(Enemy enemy, float backAngle, Orb.OrbType type, float baseSpeed, float randSpeed, Random rand, Player player) {
        float offset = ((rand.nextFloat() - 0.5f) * (float) Math.PI);
        float angle = backAngle + offset;
        float speed = baseSpeed + rand.nextFloat() * randSpeed;

        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;

        // PREDICTIVE SPAWN LOGIC:
        // If the intended "scatter" direction hits a wall immediately (within 20px),
        // FORCE the orb to fly towards the player instead.
        float checkDist = 20f;
        float futureX = enemy.x + (vx > 0 ? checkDist : -checkDist);
        float futureY = enemy.y + (vy > 0 ? checkDist : -checkDist);

        boolean blockedX = isTileType((int)(futureX / tileSize), (int)(enemy.y / tileSize), TileType.BLOCK);
        boolean blockedY = isTileType((int)(enemy.x / tileSize), (int)(futureY / tileSize), TileType.BLOCK);

        if (blockedX || blockedY) {
            // Blocked! Shoot towards player.
            float angleToPlayer = (float) Math.atan2(player.y - enemy.y, player.x - enemy.x);
            // Add slight randomness so they don't stack perfectly line
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

        System.out.println(">>> WAVE " + wave + " HAS STARTED! <<<");
        waveSound.play(0.1f);

        waveActive = true;
        canStartNextWave = false;
        combo = 0;
        timeSinceLastHit = 0f;

        // Clamp wave index (safety)
        int index = Math.min(wave - 1, WaveManager.waves.length - 1);
        Wave w = WaveManager.waves[index];

        // --- NEW SPAWN SETUP ---
        // Load the counts for this specific wave
        templarsToSpawn = w.templars;
        nunsToSpawn = w.nuns;
        priestsToSpawn = w.priests;

        // Reset timers.
        // We set them to the INTERVAL so the first batch spawns immediately upon wave start,
        // rather than making the player wait 5-20 seconds for the first enemy.
        templarTimer = TEMPLAR_SPAWN_INTERVAL;
        nunTimer = NUN_SPAWN_INTERVAL;
        priestTimer = PRIEST_SPAWN_INTERVAL;
    }

    public void endWave() {
        if (wave == 10) {
            waveActive = false;
            gameWon = true;
            waitingForNextWave = false; // Stop the timer for the next wave
            System.out.println(">>> VICTORY! <<<");
            return;
        }

        // Normal wave transition
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
}
