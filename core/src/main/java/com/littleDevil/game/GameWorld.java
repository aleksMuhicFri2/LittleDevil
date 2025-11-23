package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameWorld {

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

    // Candle Decorations
    private Texture candleSheet;
    Texture pixel = new Texture("whitePixel.png");
    Texture potionSheet = new Texture("Spritesheets/nunPotionSpritesheet.png");

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

    public enum EnemyType {
        TEMPLAR,
        NUN
    }

    // Public UI info to be displayed on screen
    public float score = 0f;
    public int wave = 1;
    public int combo = 0;

    public GameWorld(int mapWidth, int mapHeight, int tileSize) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.tileSize = tileSize;
        this.widthInTiles = mapWidth / tileSize;
        this.heightInTiles = mapHeight / tileSize;
        this.grid = new int[heightInTiles][widthInTiles];
    }

    public void initialize() {
        // Map
        mapTexture = new Texture("MapAssets/map.png");

        // Player
        player = new Player(230, 100, "Spritesheets/playerSpriteSheet.png");

        // Enemies
        enemies = new ArrayList<>();
        spawnEnemy(EnemyType.TEMPLAR);
        spawnEnemy(EnemyType.NUN);
        //enemies.add(new Templar(290, 140, this));
        //enemies.add(new Templar(310, 140, this));
        //enemies.add(new Templar(330, 140, this));
        //enemies.add(new Templar(350, 140, this));

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

        // Damage Font init - white so we can customize it later (Tint)
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("pixelon.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = (int)(Gdx.graphics.getHeight() * 0.022f); // scale relative to screen
        param.color = Color.WHITE;
        damageFont = generator.generateFont(param);
        generator.dispose();
    }

    // update all the logic
    public void update(float delta, GameScreen gameScreen) {
        player.update(delta, this);

        // update A* paths when needed = reduce timer
        for(Enemy enemy :  enemies) {
            enemy.updatePathsForEnemy(delta, player, this, PATH_UPDATE_INTERVAL);
            enemy.update(delta, player, this, gameScreen);
        }

        // remove all enemies marked for removal
        if (!enemiesToRemove.isEmpty()) {
            enemies.removeAll(enemiesToRemove);
            enemiesToRemove.clear();
        }
        for (GameCandle candle : candles) candle.update(delta);

        bigAltar.update(delta, player,this);
        smallAltarTopLeft.update(delta, player, this);
        smallAltarTopRight.update(delta, player, this);
        smallAltarBotRight.update(delta, player, this);
        smallAltarBotLeft.update(delta, player, this);

        // Update damage texts
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
        renderDebug(true, batch);

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

    public void dispose() {
        mapTexture.dispose();
        candleSheet.dispose();
        if (player != null) player.dispose();
        for (Enemy e : enemies) e.dispose();
        bigAltar.dispose();
        smallAltarTopLeft.dispose();
        smallAltarTopRight.dispose();
        smallAltarBotRight.dispose();
        smallAltarBotLeft.dispose();
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

        // Pick a side: 0=top, 1=bottom, 2=left, 3=right
        int side = (int)(Math.random() * 4);

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

        // Safety clamp
        spawnX = MathUtils.clamp(spawnX, enemyWidth / 2f, mapWidth - enemyWidth / 2f);
        spawnY = MathUtils.clamp(spawnY, enemyHeight / 2f, mapHeight - enemyHeight / 2f);

        // Collision check (optional but kept from your old logic)
        boolean valid = true;
        for (CollisionObject obj : objects) {
            if (spawnX + enemyWidth > obj.posX && spawnX - enemyWidth/2f < obj.posX + obj.width &&
                spawnY + enemyHeight > obj.posY && spawnY - enemyHeight/2f < obj.posY + obj.height) {

                valid = false;
                break;
            }
        }

        if (!valid) {
            Gdx.app.log("SpawnEnemy", "Edge spawn blocked by collision object.");
            return;
        }

        Enemy newEnemy;
        switch (type) {
            case TEMPLAR -> newEnemy = new Templar(spawnX, spawnY, this);
            case NUN -> newEnemy = new Nun(spawnX, spawnY, this);
            default -> newEnemy = new Templar(spawnX, spawnY, this);
        }

        enemies.add(newEnemy);
    }


    public void removeEnemy(Enemy enemy) {
        if (enemy != null) enemiesToRemove.add(enemy);
    }

    public void spawnOrbs(Enemy enemy, Player player) {
        Texture orbSheet = new Texture("Spritesheets/xpOrbs.png");
        Random rand = new Random();

        // Angle from enemy to player
        float angleToPlayer = (float) Math.atan2(player.y - enemy.y, player.x - enemy.x);

        // Back direction (opposite the player)
        float backAngle = angleToPlayer + (float) Math.PI;

        // Spawn all guaranteed orbs for each type
        for (int i = 0; i < enemy.guaranteedOrbsCounts.length; i++) {
            Orb.OrbType type;
            switch (i) {
                case 0 -> type = Orb.OrbType.COMMON;
                case 1 -> type = Orb.OrbType.RARE;
                case 2 -> type = Orb.OrbType.GOLD;
                default -> type = Orb.OrbType.COMMON;
            }

            int guaranteed = (int) enemy.guaranteedOrbsCounts[i];
            for (int j = 0; j < guaranteed; j++) {
                // Random angle within 90° behind enemy
                float offset = ((rand.nextFloat() - 0.5f) * (float) Math.PI); // -45° to +45°
                float angle = backAngle + offset;

                float speed = 65f + rand.nextFloat() * 20f;
                Vector2 initialVelocity = new Vector2(
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed
                );

                orbs.add(new Orb(enemy.x, enemy.y, type, orbSheet, initialVelocity));
            }
        }

        // Handle extra chance orbs
        for (int i = 0; i < enemy.firstExtraChances.length; i++) {
            if (rand.nextFloat() < enemy.firstExtraChances[i]) {
                Orb.OrbType type;
                switch (i) {
                    case 0 -> type = Orb.OrbType.COMMON;
                    case 1 -> type = Orb.OrbType.RARE;
                    case 2 -> type = Orb.OrbType.GOLD;
                    default -> type = Orb.OrbType.COMMON;
                }

                float offset = ((rand.nextFloat() - 0.5f) * (float) Math.PI); // -45° to +45°
                float angle = backAngle + offset;
                float speed = 100f + rand.nextFloat() * 30f;
                Vector2 initialVelocity = new Vector2(
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed
                );

                orbs.add(new Orb(enemy.x, enemy.y, type, orbSheet, initialVelocity));
            }
        }
    }

    public void spawnBottleToPoint(float startX, float startY, float targetX, float targetY) {
        BottleProjectile p = new BottleProjectile(potionSheet, startX, startY, targetX, targetY);
        potions.add(p);
    }
}
