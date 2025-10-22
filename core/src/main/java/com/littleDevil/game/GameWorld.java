package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;

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

    // Decorations
    private Texture candleSheet;
    Texture pixel = new Texture("whitePixel.png");

    // Pathing
    private final float PATH_UPDATE_INTERVAL = 1f; // 5 updates per second

    public enum TileType { BLOCK, STAIRS, ALTAR, BOOST }

    public GameWorld(int mapWidth, int mapHeight, int tileSize) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.tileSize = tileSize;
        this.widthInTiles = mapWidth / tileSize;
        this.heightInTiles = mapHeight / tileSize;
        this.grid = new int[heightInTiles][widthInTiles];
    }

    public void initialize() {
        mapTexture = new Texture("MapAssets/map.png");

        // Player
        player = new Player(230, 100, "Spritesheets/playerSpriteSheet.png");

        // Enemies
        enemies = new ArrayList<>();
        enemies.add(new Enemy(250, 140, "Spritesheets/templarSpritesheet.png", this));
        enemies.add(new Enemy(270, 140, "Spritesheets/templarSpritesheet.png", this));
        enemies.add(new Enemy(290, 140, "Spritesheets/templarSpritesheet.png", this));
        enemies.add(new Enemy(310, 140, "Spritesheets/templarSpritesheet.png", this));
        enemies.add(new Enemy(330, 140, "Spritesheets/templarSpritesheet.png", this));
        enemies.add(new Enemy(350, 140, "Spritesheets/templarSpritesheet.png", this));


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

        generateCollisionGrid();
    }

    // update all the logic
    public void update(float delta) {
        player.update(delta, this);

        // update A* paths when needed = reduce timer
        updatePathsForEnemies();

        for (Enemy enemy : enemies) enemy.update(delta, player, this);
        for (GameCandle candle : candles) candle.update(delta);

        bigAltar.update(delta, player,this);
        smallAltarTopLeft.update(delta, player, this);
        smallAltarTopRight.update(delta, player, this);
        smallAltarBotRight.update(delta, player, this);
        smallAltarBotLeft.update(delta, player, this);
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
                    obj.posY
                ));
            }
        }

        // Enemy objects
        for (Enemy e : enemies) {
            renderList.add(createRenderEntity(e.getCurrentFrame(), e.x, e.y));
        }

        // Player object
        renderList.add(createRenderEntity(player.getCurrentFrame(), player.x, player.y));

        renderList.sort((a, b) -> Integer.compare(b.baseY, a.baseY));
        for (RenderEntity e : renderList)
            batch.draw(e.region, e.x, e.y, e.width, e.height);

        renderList.clear();

        renderDebug(false, batch);
    }

    // Adds a CollisionObject to the objects array of the GameWorld
    public void addObject(CollisionObject obj) {
        objects.add(obj);
        obj.markOnGrid(grid, tileSize);
    }

    // get tile type for player and enemies collision, altars, boosts...
    public boolean isTileType(int tileX, int tileY, TileType type) {
        int tile = grid[tileY][tileX];
        if (tile < 0) return false;
        return switch (type) {
            case BLOCK -> tile == 1 || tile == 2;
            case ALTAR -> tile == 3;
            case BOOST -> tile == 4;
            case STAIRS -> tile == 5;
        };
    }

    // generates a collision grid for enemies pathfinding based on grid
    public void generateCollisionGrid() {
        collisionGrid = new boolean[heightInTiles][widthInTiles];

        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                int tile = grid[y][x];
                collisionGrid[y][x] = (tile == 1); // world edge
                collisionGrid[y][x] = (tile == 2); // obstacle collision
            }
        }
    }

    // Updates the path for enemies or reduces time until its updated
    public void updatePathsForEnemies() {
        float delta = Gdx.graphics.getDeltaTime();

        float playerCenterX = player.x + player.collisionOffsetX + player.collisionWidth / 2f;
        float playerCenterY = player.y + player.collisionOffsetY + player.collisionHeight / 2f;

        for (Enemy enemy : enemies) {
            // Increase individual timer
            enemy.pathTimer += delta;

            if (enemy.pathTimer < PATH_UPDATE_INTERVAL + enemy.pathUpdateOffset)
                continue; // not time for update yet, skip

            enemy.pathTimer = 0f;

            float enemyCenterX = enemy.x + enemy.collisionOffsetX + enemy.collisionWidth / 2f;
            float enemyCenterY = enemy.y + enemy.collisionOffsetY + enemy.collisionHeight / 2f;

            float distanceToPlayerX = enemyCenterX - playerCenterX;
            float distanceToPlayerY = enemyCenterY - playerCenterY;
            float dist = (float)Math.sqrt(distanceToPlayerX * distanceToPlayerX + distanceToPlayerY * distanceToPlayerY);

            // Distance-based random offset
            float distanceFactor = Math.min(dist / 300f, 1f);
            enemy.pathUpdateOffset = 0.2f + distanceFactor * (float)Math.random() * 2f;

            int startX = (int)(enemyCenterX / tileSize);
            int startY = (int)(enemyCenterY / tileSize);
            int targetX = (int)(playerCenterX / tileSize);
            int targetY = (int)(playerCenterY / tileSize);

            // --- Smooth continuation logic --- DON'T TOUCH otherwise enemy stops on update
            Node oldNextNode = null;
            if (enemy.currentPath != null && !enemy.currentPath.isEmpty() && enemy.currentTargetIndex < enemy.currentPath.size()) {
                oldNextNode = enemy.currentPath.get(enemy.currentTargetIndex);
            }

            enemy.currentPath = enemy.pathfinder.findPath(startX, startY, targetX, targetY);

            if (enemy.currentPath != null && !enemy.currentPath.isEmpty()) {


                enemy.currentTargetIndex = getClosestIndex(enemy, oldNextNode);
            } else {
                enemy.currentTargetIndex = 0;
            }
        }
    }


    // gets the second-closest node so the player doesn't stop
    private int getClosestIndex(Enemy enemy, Node oldNextNode) {
        int closestIndex = 0;
        float minDist = Float.MAX_VALUE;

        for (int i = 0; i < enemy.currentPath.size(); i++) {
            Node n = enemy.currentPath.get(i);
            float dx, dy;
            if (oldNextNode != null) {
                dx = n.x - oldNextNode.x;
                dy = n.y - oldNextNode.y;
            } else {
                dx = n.x - enemy.x / tileSize;
                dy = n.y - enemy.y / tileSize;
            }
            float d = dx*dx + dy*dy;
            if (d < minDist) {
                minDist = d;
                closestIndex = i;
            }
        }
        return closestIndex;
    }

    // function for rendering the debug objects
    private void renderDebug(boolean draw, SpriteBatch batch) {
        if(!draw) return;
        for(Enemy e: enemies) {
            e.renderPath(batch, pixel, this);
            e.renderCollisionBox(batch, pixel);
            e.renderHitbox(batch, pixel);
        }
        // Render collision grid
        batch.setColor(1f, 0f, 0f, 0.3f); // red semi-transparent
        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                if (collisionGrid[y][x]) {
                    batch.draw(pixel, x * tileSize, y * tileSize, tileSize, tileSize);
                }
            }
        }
        batch.setColor(1f, 1f, 1f, 1f); // reset color
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
        RenderEntity(TextureRegion region, float x, float y, float width, float height, int baseY) {
            this.region = region;
            this.x = x; this.y = y; this.width = width; this.height = height; this.baseY = baseY;
        }
    }

    // clean entity creation for inserting into the list
    private RenderEntity createRenderEntity(TextureRegion region, float entityX, float entityY) {
        float width = region.getRegionWidth();
        float height = region.getRegionHeight();

        float drawX = entityX - width / 2f;
        float drawY = entityY - height / 2f;

        int baseY = (int)(entityY - height / 2f);

        return new RenderEntity(region, drawX, drawY, width, height, baseY);
    }
}
