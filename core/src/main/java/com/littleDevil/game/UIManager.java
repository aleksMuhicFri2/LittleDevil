package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class UIManager {

    private final GameWorld world;
    private final Player player;

    private final SpriteBatch batch;

    private final OrthographicCamera camera;
    private final ExtendViewport viewport;

    private final BitmapFont scoreFont, waveFont, comboFont, levelFont;
    // Keeping fields for Skill Points display
    private final BitmapFont skillPointsFont;

    private final GlyphLayout scoreLayout, waveLayout, comboLayout, levelLayout;
    // Keeping fields for Skill Points display
    private final GlyphLayout skillPointsLayout;

    private final Texture playerUI, wavePanel, barsBackground, healthBar, energyBar, xpBar;

    private float uiOffsetY = 0f;
    private float targetUiOffsetY = 0f;
    private final float UI_SLIDE_DISTANCE = 70f;
    private final float UI_SLIDE_SPEED = 8f;

    private boolean upgradePageOpen = false;

    private float upgradePanelWidth = 500f;
    private float upgradePanelHeight = 380f;

    private TextureRegion[][] levelSprites;

    private Texture plusButton;
    private Texture panelBackground;

    private final Vector3 tmp = new Vector3();


    public UIManager(
        GameWorld world,
        Player player,
        SpriteBatch batch,
        OrthographicCamera camera,
        ExtendViewport viewport,
        BitmapFont scoreFont,
        BitmapFont waveFont,
        BitmapFont comboFont,
        BitmapFont levelFont,
        BitmapFont skillPointsFont, // Parameter is now correctly handled
        Texture playerUI,
        Texture wavePanel,
        Texture barsBackground,
        Texture healthBar,
        Texture energyBar,
        Texture xpBar
    ) {
        this.world = world;
        this.player = player;
        this.batch = batch;

        this.camera = camera;
        this.viewport = viewport;

        this.scoreFont = scoreFont;
        this.waveFont = waveFont;
        this.comboFont = comboFont;
        this.levelFont = levelFont;
        // 2. Assign skillPointsFont
        this.skillPointsFont = skillPointsFont;

        scoreLayout = new GlyphLayout();
        waveLayout = new GlyphLayout();
        comboLayout = new GlyphLayout();
        levelLayout = new GlyphLayout();
        // 2. Initialize skillPointsLayout
        skillPointsLayout = new GlyphLayout();

        this.playerUI = playerUI;
        this.wavePanel = wavePanel;
        this.barsBackground = barsBackground;
        this.healthBar = healthBar;
        this.energyBar = energyBar;
        this.xpBar = xpBar;

        plusButton = new Texture("GameUI/upgradeButton.png");
        panelBackground = new Texture("GameUI/upgradesPage.png");

        Texture attackSheet = new Texture("GameUI/attackUpgradesSpritesheet.png");
        Texture agilitySheet = new Texture("GameUI/agilityUpgradesSpritesheet.png");
        Texture defenseSheet = new Texture("GameUI/defenseUpgradesSpritesheet.png");
        Texture luckSheet    = new Texture("GameUI/luckUpgradesSpritesheet.png");
        Texture superSheet   = new Texture("GameUI/superUpgradesSpritesheet.png");

        levelSprites = new TextureRegion[5][8];

        sliceUpgradeSheet(levelSprites[0], attackSheet);
        sliceUpgradeSheet(levelSprites[1], agilitySheet);
        sliceUpgradeSheet(levelSprites[2], defenseSheet);
        sliceUpgradeSheet(levelSprites[3], luckSheet);
        sliceUpgradeSheet(levelSprites[4], superSheet);
    }


    private void sliceUpgradeSheet(TextureRegion[] output, Texture sheet) {
        int fw = 64;
        int fh = 16;
        for (int i = 0; i < 8; i++) {
            output[i] = new TextureRegion(sheet, i * fw, 0, fw, fh);
        }
    }


    public void render() {
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        updatePlayerDisplayValues(Gdx.graphics.getDeltaTime());

        boolean tabHeld = Gdx.input.isKeyPressed(Input.Keys.TAB);
        targetUiOffsetY = tabHeld ? UI_SLIDE_DISTANCE : 0f;
        uiOffsetY = MathUtils.lerp(uiOffsetY, targetUiOffsetY, Gdx.graphics.getDeltaTime() * UI_SLIDE_SPEED);

        batch.begin();

        drawPlayerUI();
        drawWavePanel();
        drawWaveNumber();
        drawScore();
        drawCombo();

        if (upgradePageOpen)
            renderUpgradePage();  // overlay, but HUD stays visible

        batch.end();
    }


    public void openUpgradePage() { upgradePageOpen = true; }
    public void closeUpgradePage() { upgradePageOpen = false; }


    private void renderUpgradePage() {

        float panelX = camera.position.x - upgradePanelWidth / 2f;
        float panelY = camera.position.y - upgradePanelHeight / 2f;

        batch.draw(panelBackground, panelX, panelY, upgradePanelWidth, upgradePanelHeight);

        // 4. Call the new method to draw skill points at the top of the panel
        drawSkillPoints(panelX, panelY, upgradePanelWidth, upgradePanelHeight);

        float rowSpacing = 40f;

        // Adjusted to accommodate the new header and the original "UPGRADES" title
        float y1 = panelY + upgradePanelHeight - 130f;

        float y2 = y1 - rowSpacing;
        float y3 = y2 - rowSpacing;
        float y4 = y3 - rowSpacing;
        float y5 = y4 - rowSpacing;

        drawUpgradeRow("Attack", 0, y1, player.attackLevel, Player.StatType.ATTACK);
        drawUpgradeRow("Agility", 1, y2, player.agilityLevel, Player.StatType.AGILITY);
        drawUpgradeRow("Defense", 2, y3, player.defenseLevel, Player.StatType.DEFENSE);
        drawUpgradeRow("Luck",    3, y4, player.luckLevel,    Player.StatType.LUCK);
        drawUpgradeRow("Super",   4, y5, player.superLevel,   Player.StatType.SUPER);
    }

    private void drawSkillPoints(float panelX, float panelY, float panelW, float panelH) {
        String text = "Skill Points: " + player.skillPoints;

        // Use the skillPointsFont and layout
        skillPointsLayout.setText(skillPointsFont, text);

        // Center the text horizontally within the panel
        float textX = panelX + (panelW - skillPointsLayout.width) / 2f;

        // Position the text near the top edge of the panel (adjusted for the title/header space)
        float textY = panelY + panelH - 80f;

        skillPointsFont.draw(batch, text, textX, textY);
    }


    private void drawUpgradeRow(String label, int statIndex, float y, int level, Player.StatType statType) {

        float panelX = camera.position.x - upgradePanelWidth / 2f;

        float nameX = panelX + 80;
        float iconX = panelX + 200;
        float plusX = panelX + 400;

        scoreFont.getData().setScale(0.7f);
        scoreFont.draw(batch, label, nameX, y);
        scoreFont.getData().setScale(1f);

        TextureRegion icon = levelSprites[statIndex][level];

        float iconScale = 2.8f;
        float iw = icon.getRegionWidth() * iconScale;
        float ih = icon.getRegionHeight() * iconScale;

        batch.draw(icon, iconX, y - ih * 0.7f, iw, ih);

        float btnScale = 2.8f;
        float bw = plusButton.getWidth() * btnScale;
        float bh = plusButton.getHeight() * btnScale;
        float plusY = y - bh * 0.7f;

        // CORRECTED: Draw the button unconditionally
        batch.draw(plusButton, plusX, plusY, bw, bh);

        // CORRECTED: Check for touch input unconditionally
        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = Gdx.input.getY();
            viewport.unproject(tmp.set(mx, my, 0));

            if (tmp.x >= plusX && tmp.x <= plusX + bw &&
                tmp.y >= plusY && tmp.y <= plusY + bh) {

                // The player.upgradeStat(statType) method is responsible for checking if
                // player.skillPoints > 0 before actually spending the point/upgrading.
                player.upgradeStat(statType);
            }
        }
    }


    private void drawPlayerUI() {
        float baseW = 112f;
        float baseH = 48f;

        float scale = getScale(3.5f);

        float w = baseW * scale;
        float h = baseH * scale;

        float x = (viewport.getWorldWidth() - w) / 2f;
        float y = -h * 0.4f + uiOffsetY;

        float hp = Math.min(1f, player.displayHP / player.baseHP);
        float en = Math.min(1f, player.displayEnergy / player.baseEnergy);
        float xp = Math.min(1f, player.displayXp / player.neededXp);

        batch.draw(barsBackground, x, y, w, h);

        batch.draw(healthBar, x + 12f * scale, y + 23f * scale, 43f * scale * hp, 5f * scale);
        batch.draw(energyBar, x + 57f * scale, y + 23f * scale, 43f * scale * en, 5f * scale);
        batch.draw(xpBar,     x + 18f * scale, y + 32f * scale, 76f * scale * xp, 2f * scale);

        drawLevelText(y, h, scale);
        batch.draw(playerUI, x, y, w, h);
    }


    private void drawLevelText(float uiY, float uiHeight, float scale) {
        String txt = "Lvl " + (int)player.level;
        levelLayout.setText(levelFont, txt);
        float x = (viewport.getWorldWidth() - levelLayout.width) / 2f;
        float y = uiY + uiHeight - 5f * scale;
        levelFont.draw(batch, txt, x, y);
    }


    private void drawWavePanel() {
        float base = 64f;
        float scale = getScale(3f);
        float w = base * scale;
        float h = base * scale;
        float x = (viewport.getWorldWidth() - w) / 2f;
        float y = viewport.getWorldHeight() - (h / 1.5f);
        batch.draw(wavePanel, x, y, w, h);
    }


    private void drawWaveNumber() {
        String label = "Wave";
        String value = String.valueOf(world.getWave());

        waveLayout.setText(waveFont, label);
        float lw = waveLayout.width;
        waveLayout.setText(waveFont, value);
        float vw = waveLayout.width;

        float total = lw + 10f + vw;
        float x = (viewport.getWorldWidth() - total) / 2f;
        float y = viewport.getWorldHeight() - 43f;

        waveFont.draw(batch, label, x, y);
        waveFont.draw(batch, value, x + lw + 10f, y);
    }


    private void drawScore() {
        scoreLayout.setText(scoreFont, "Score:");
        float x = 20f;
        float y = viewport.getWorldHeight() - 20f;
        scoreFont.draw(batch, "Score:", x, y);
        scoreFont.draw(batch, String.valueOf(world.getScore()), x + scoreLayout.width + 20f, y);
    }


    private void drawCombo() {
        int combo = world.getCombo();
        if (combo <= 0) return;

        comboLayout.setText(comboFont, "Combo:");
        float x = 20f;
        float y = viewport.getWorldHeight() - 60f;

        comboFont.draw(batch, "Combo:", x, y);
        comboFont.draw(batch, combo + "x", x + comboLayout.width + 20f, y);
    }


    private void updatePlayerDisplayValues(float delta) {
        float speed = 8f * delta;

        player.displayHP = MathUtils.lerp(player.displayHP, player.currentHP, speed);
        player.displayEnergy = MathUtils.lerp(player.displayEnergy, player.currentEnergy, speed);

        if (player.xpOverflowAnimating) {
            player.displayXp = MathUtils.lerp(player.displayXp, player.previousNeededXp, speed);

            if (Math.abs(player.displayXp - player.previousNeededXp) < 1f) {
                player.displayXp = 0f;
                player.xpOverflowAnimating = false;
                player.levelUp = false;
                player.level += 1;
            }
        } else {
            player.displayXp = MathUtils.lerp(player.displayXp, player.currentXp, speed);
        }
    }


    private float getScale(float maxScale) {
        float refW = 1920f;
        float refH = 1080f;
        float s = Math.min(
            Gdx.graphics.getWidth() / refW,
            Gdx.graphics.getHeight() / refH
        );
        return Math.min(s * 8f, maxScale);
    }


    public void resize(int w, int h) {
        viewport.update(w, h, true);
    }
}
