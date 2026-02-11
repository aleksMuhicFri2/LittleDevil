package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport; // CHANGED: Generic Viewport import

import java.util.List;

public class UIManager {

    private final GameWorld world;
    private final Player player;

    private final SpriteBatch batch;

    private final OrthographicCamera camera;
    private final Viewport viewport; // CHANGED: From ExtendViewport to Viewport

    private final BitmapFont scoreFont, waveFont, comboFont, levelFont;
    private final BitmapFont skillPointsFont;
    private final BitmapFont tooltipFont;
    private final BitmapFont tutorialFont;


    private final GlyphLayout scoreLayout, waveLayout, comboLayout, levelLayout;
    private final GlyphLayout skillPointsLayout;
    private final GlyphLayout layout;

    private final Texture playerUI, wavePanel, barsBackground, healthBar, energyBar, xpBar;

    private final Texture darkOverlay;
    private final Texture slotBorder;

    private float uiOffsetY = 0f;
    private float targetUiOffsetY = 0f;
    private final float UI_SLIDE_DISTANCE = 120f;
    private final float UI_SLIDE_SPEED = 8f;

    private boolean upgradePageOpen = false;
    private boolean augmentPageOpen = false;

    private float upgradePanelWidth = 900f;  // Was 500f
    private float upgradePanelHeight = 700f; // Was 380f

    private TextureRegion[][] levelSprites;
    private Texture plusButton;
    private Texture panelBackground;

    private final Vector3 tmp = new Vector3();

    private TutorialManager tutorialManager;

    private final float CARD_SCALE = 10.0f;
    private final float CARD_W = 48f * CARD_SCALE;
    private final float CARD_H = 64f * CARD_SCALE;
    private final float CARD_SPACING = 500f;

    private final float SLOT_SIZE = 80f;
    private final float SLOT_VISIBLE_Y = 18f;
    private final float SLOT_SPACING = 49f;

    private float[] augmentCardScales = {1f, 1f, 1f};
    private final float HOVER_SCALE = 1.2f;
    private final float SCALE_SPEED = 15f;

    private boolean augmentClicked = false; // The safety lock


    public UIManager(
        GameWorld world,
        Player player,
        SpriteBatch batch,
        OrthographicCamera camera,
        Viewport viewport, // CHANGED: From ExtendViewport to Viewport
        BitmapFont scoreFont,
        BitmapFont waveFont,
        BitmapFont comboFont,
        BitmapFont levelFont,
        BitmapFont skillPointsFont,
        BitmapFont tutorialFont,
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
        this.skillPointsFont = skillPointsFont;
        this.tutorialFont = tutorialFont;

        this.tooltipFont = new BitmapFont();
        this.tooltipFont.getData().setScale(2f);

        this.tutorialManager = new TutorialManager(world, tutorialFont);

        scoreLayout = new GlyphLayout();
        waveLayout = new GlyphLayout();
        comboLayout = new GlyphLayout();
        levelLayout = new GlyphLayout();
        skillPointsLayout = new GlyphLayout();
        layout = new GlyphLayout();

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

        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0f, 0f, 0f, 0.85f);
        pix.fill();
        darkOverlay = new Texture(pix);

        pix.setColor(1f, 1f, 1f, 1f);
        pix.fill();
        slotBorder = new Texture(pix);
        pix.dispose();
    }

    private void sliceUpgradeSheet(TextureRegion[] output, Texture sheet) {
        int fw = 64;
        int fh = 16;
        for (int i = 0; i < 8; i++) {
            output[i] = new TextureRegion(sheet, i * fw, 0, fw, fh);
        }
    }

    public void render() {
        viewport.apply(); // Ensures the batch respects the FitViewport (black bars)
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        updatePlayerDisplayValues(Gdx.graphics.getDeltaTime());
        tutorialManager.update(Gdx.graphics.getDeltaTime());

        boolean tabHeld = Gdx.input.isKeyPressed(Input.Keys.TAB);
        targetUiOffsetY = tabHeld ? UI_SLIDE_DISTANCE : 0f;
        uiOffsetY = MathUtils.lerp(uiOffsetY, targetUiOffsetY, Gdx.graphics.getDeltaTime() * UI_SLIDE_SPEED);

        batch.begin();

        drawPlayerUI();
        drawWavePanel();
        drawWaveNumber();
        drawScore();
        drawCombo();

        drawAugmentSlots();

        tutorialFont.getData().setScale(2f);
        tutorialManager.render(batch, viewport.getWorldWidth(), viewport.getWorldHeight());
        tutorialFont.getData().setScale(1.0f);

        if (augmentPageOpen) {
            renderAugmentPage();
        } else if (upgradePageOpen) {
            renderUpgradePage();
        }

        batch.end();
    }

    public void openUpgradePage() { if (player.skillPoints > 0) {
        upgradePageOpen = true;
    } }
    public void closeUpgradePage() { upgradePageOpen = false; }
    public boolean isUpgradePageOpen() {
        return upgradePageOpen;
    }

    public void openAugmentPage() {
        if (!augmentPageOpen) {
            augmentPageOpen = true;
            augmentClicked = false; // <--- RESET HERE
            world.augmentManager.rollOptions();
        }
    }

    public void closeAugmentPage() { augmentPageOpen = false; }

    private void renderAugmentPage() {
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(darkOverlay, 0, 0, 1920, 1080);

        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;

        // --- FIX: Header Text Smaller and Higher ---
        tutorialFont.setColor(Color.WHITE);
        tutorialFont.getData().setScale(1.4f); // Smaller (was 1.8f)
        layout.setText(tutorialFont, "CHOOSE YOUR AUGMENT");
        float textX = centerX - layout.width / 2f;
        // Higher offset (using 0.85f instead of 0.65f)
        float textY = centerY + (CARD_H * 0.72f);

        tutorialFont.draw(batch, layout, textX, textY);
        tutorialFont.getData().setScale(1f);

        viewport.unproject(tmp.set(Gdx.input.getX(), Gdx.input.getY(), 0));
        Vector3 mouse = tmp;

        float[] positionsX = { centerX - CARD_SPACING, centerX, centerX + CARD_SPACING };
        List<Augment> options = world.augmentManager.currentOptions;

        if (options.size() >= 1) drawAugmentCardWithHover(options.get(0), positionsX[0], centerY, 0, mouse);
        if (options.size() >= 2) drawAugmentCardWithHover(options.get(1), positionsX[1], centerY, 1, mouse);
        if (options.size() >= 3) drawAugmentCardWithHover(options.get(2), positionsX[2], centerY, 2, mouse);
    }

    private void drawAugmentCardWithHover(Augment aug, float x, float y, int index, Vector3 mouse) {
        boolean isHovered = mouse.x >= x - CARD_W/2f && mouse.x <= x + CARD_W/2f &&
            mouse.y >= y - CARD_H/2f && mouse.y <= y + CARD_H/2f;

        float targetScale = isHovered ? HOVER_SCALE : 1.0f;
        augmentCardScales[index] = MathUtils.lerp(augmentCardScales[index], targetScale, Gdx.graphics.getDeltaTime() * SCALE_SPEED);

        drawAugmentCard(aug, x, y, index);
    }

    private void drawAugmentCard(Augment aug, float centerX, float centerY, int cardIndex) {
        // --- SAFETY LOCK 1 ---
        // If a card was already clicked this frame, stop processing immediately.
        // This prevents double-selection or ghost clicks on overlapping UI.
        if (augmentClicked) return;

        float dt = Gdx.graphics.getDeltaTime();
        viewport.unproject(tmp.set(Gdx.input.getX(), Gdx.input.getY(), 0));

        float currentScale = augmentCardScales[cardIndex];
        float drawW = CARD_W * currentScale;
        float drawH = CARD_H * currentScale;

        float x = centerX - drawW / 2f;
        float y = centerY - drawH / 2f;

        boolean isHovered = (tmp.x >= x && tmp.x <= x + drawW && tmp.y >= y && tmp.y <= y + drawH);
        float targetScale = isHovered ? HOVER_SCALE : 1.0f;
        augmentCardScales[cardIndex] = MathUtils.lerp(augmentCardScales[cardIndex], targetScale, dt * SCALE_SPEED);
        currentScale = augmentCardScales[cardIndex];

        // 1. Card Texture
        TextureRegion cardRegion = world.augmentManager.getAugmentCard(aug.iconIndex);
        if (cardRegion != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(cardRegion, x, y, drawW, drawH);
        }

        float baseTitleScale = 0.6f;
        tutorialFont.getData().setScale(baseTitleScale * currentScale);
        tutorialFont.setColor(Color.GOLD);
        layout.setText(tutorialFont, aug.name);

        float titleX = centerX - layout.width / 2f;
        float titleY = y + (drawH * 0.36f);
        tutorialFont.draw(batch, layout, titleX, titleY);

        // Description
        float baseDescScale = 0.45f;
        tutorialFont.getData().setScale(baseDescScale * currentScale);
        tutorialFont.setColor(Color.WHITE);

        float sidePadding = 120f * currentScale;
        float maxTextWidth = drawW - (sidePadding * 2);

        layout.setText(tutorialFont, aug.description, Color.WHITE, maxTextWidth, Align.center, true);

        float descX = x + sidePadding;
        float descY = titleY - 50f * currentScale;

        tutorialFont.draw(batch, aug.description, descX, descY, maxTextWidth, Align.center, true);

        tutorialFont.getData().setScale(1f);
        tutorialFont.setColor(Color.WHITE);

        if (isHovered && Gdx.input.justTouched()) {
            // --- SAFETY LOCK 2 ---
            // Lock input immediately so the loop doesn't check the next card
            augmentClicked = true;

            world.augmentManager.selectAugment(aug);
            world.pendingAugments--;

            augmentCardScales[0] = 1f; augmentCardScales[1] = 1f; augmentCardScales[2] = 1f;
            closeAugmentPage();

            if (world.pendingAugments <= 0) {
                openUpgradePage();
            }
        }
    }

    public boolean isAugmentPageOpen() {
        return augmentPageOpen;
    }

    public boolean isAnyMenuOpen() {
        return augmentPageOpen || upgradePageOpen;
    }

    private void drawAugmentSlots() {
        float totalWidth = (5 * SLOT_SIZE) + (4 * SLOT_SPACING);
        float startX = (viewport.getWorldWidth() - totalWidth) / 2f;
        float currentY = (SLOT_VISIBLE_Y - UI_SLIDE_DISTANCE) + uiOffsetY;

        for (int i = 0; i < 5; i++) {
            float slotX = startX + i * (SLOT_SIZE + SLOT_SPACING);
            Augment aug = world.augmentManager.playerSlots[i];
            if (aug != null) {
                TextureRegion icon = world.augmentManager.getIcon(aug.iconIndex);
                if (icon != null) {
                    float iconScale = 1.4f;
                    float drawSize = SLOT_SIZE * iconScale;
                    float drawX = slotX + (SLOT_SIZE - drawSize) / 2f;
                    float drawY = currentY + (SLOT_SIZE - drawSize) / 2f;
                    batch.setColor(1f, 1f, 1f, 1f);
                    batch.draw(icon, drawX, drawY, drawSize, drawSize);

                    if (currentY > 0) {
                        Vector3 mouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                        if (mouse.x >= drawX && mouse.x <= drawX + drawSize &&
                            mouse.y >= drawY && mouse.y <= drawY + drawSize) {
                            drawTooltip(aug, slotX + SLOT_SIZE / 2f, drawY + drawSize + 10f);
                        }
                    }
                }
            }
        }
    }

    private void drawTooltip(Augment aug, float x, float y) {
        String text = aug.name + "\n" + aug.description;

        // Ensure scale is set before measuring layout
        tooltipFont.getData().setScale(2f);
        layout.setText(tooltipFont, text);

        // Increase padding for the background box
        float padding = 30f;
        float w = layout.width + padding;
        float h = layout.height + padding;

        // Draw dark background box (slotBorder)
        batch.setColor(0f, 0f, 0f, 0.95f);
        // Draw centered above the icon
        batch.draw(slotBorder, x - w/2f, y, w, h);

        // Draw Text
        batch.setColor(1f, 1f, 1f, 1f);
        tooltipFont.setColor(Color.YELLOW);

        // Position text precisely within the box
        float textX = x - layout.width / 2f;
        float textY = y + h - (padding / 2f);

        tooltipFont.draw(batch, text, textX, textY);

        tooltipFont.setColor(Color.WHITE);
    }

    private void renderUpgradePage() {
        float panelX = (viewport.getWorldWidth() - upgradePanelWidth) / 2f;
        float panelY = (viewport.getWorldHeight() - upgradePanelHeight) / 2f;

        batch.draw(panelBackground, panelX, panelY, upgradePanelWidth, upgradePanelHeight);
        drawSkillPoints(panelX, panelY, upgradePanelWidth, upgradePanelHeight);

        // INCREASED: More space between rows
        float rowSpacing = 80f;
        // MOVED DOWN: Lower startY to clear the "Skill Points" area
        float startY = panelY + upgradePanelHeight - 240f;

        drawUpgradeRow("Attack",  0, startY, player.attackLevel, Player.StatType.ATTACK);
        drawUpgradeRow("Agility", 1, startY - rowSpacing, player.agilityLevel, Player.StatType.AGILITY);
        drawUpgradeRow("Defense", 2, startY - rowSpacing * 2, player.defenseLevel, Player.StatType.DEFENSE);
        drawUpgradeRow("Luck",    3, startY - rowSpacing * 3, player.luckLevel, Player.StatType.LUCK);
        drawUpgradeRow("Super",   4, startY - rowSpacing * 4, player.superLevel, Player.StatType.SUPER);
    }

    private void drawSkillPoints(float panelX, float panelY, float panelW, float panelH) {
        String text = "Skill Points: " + player.skillPoints;

        skillPointsFont.getData().setScale(1f);
        skillPointsLayout.setText(skillPointsFont, text);

        float textX = panelX + (panelW - skillPointsLayout.width) / 2f;
        float textY = panelY + panelH - 135f;

        skillPointsFont.draw(batch, text, textX, textY);
        skillPointsFont.getData().setScale(1.0f);
    }

    private void drawUpgradeRow(String label, int statIndex, float y, int level, Player.StatType statType) {
        float panelX = (viewport.getWorldWidth() - upgradePanelWidth) / 2f;

        float nameX = panelX + 110f;
        float iconX = panelX + 360f;
        float plusX = panelX + 710f;

        // 1. SMALLER FONT: 0.9f is better for the 1080p layout
        scoreFont.getData().setScale(0.9f);
        scoreLayout.setText(scoreFont, label);

        // CENTER TEXT: We draw at y + half-height to align the middle of the text with y
        scoreFont.draw(batch, label, nameX, y + scoreLayout.height / 2f);
        scoreFont.getData().setScale(1.0f);

        // 2. SCALE ICONS
        TextureRegion icon = levelSprites[statIndex][level];
        float iconScale = 4.2f;
        float iw = icon.getRegionWidth() * iconScale;
        float ih = icon.getRegionHeight() * iconScale;
        // Align middle of icon with y
        batch.draw(icon, iconX, y - ih / 2f, iw, ih);

        // 3. SCALE PLUS BUTTON
        float btnScale = 4.2f;
        float bw = plusButton.getWidth() * btnScale;
        float bh = plusButton.getHeight() * btnScale;
        float plusY = y - bh / 2f; // Align middle of button with y

        if (player.skillPoints > 0) {
            batch.draw(plusButton, plusX, plusY, bw, bh);
        }

        // Input Detection
        if (Gdx.input.justTouched()) {
            viewport.unproject(tmp.set(Gdx.input.getX(), Gdx.input.getY(), 0));
            if (tmp.x >= plusX && tmp.x <= plusX + bw &&
                tmp.y >= plusY && tmp.y <= plusY + bh) {
                player.upgradeStat(statType);
                if (player.skillPoints <= 0) closeUpgradePage();
            }
        }
    }

    private void drawPlayerUI() {
        float baseW = 112f;
        float baseH = 48f;
        float scale = getScale(6.5f);
        float w = baseW * scale;
        float h = baseH * scale;

        float x = (viewport.getWorldWidth() - w) / 2f;
        float y = -120f + uiOffsetY;

        float hpPercent = Math.min(1f, player.displayHP / player.baseHP);
        float enPercent = Math.min(1f, player.displayEnergy / player.baseEnergy);
        float xpPercent = Math.min(1f, player.displayXp / player.neededXp);

        // 1. Draw Background
        batch.draw(barsBackground, x, y, w, h);

        // 2. Draw Health Bar
        batch.draw(healthBar, x + 12f * scale, y + 23f * scale, 43f * scale * hpPercent, 5f * scale);

        // 3. ENERGY BAR PULSE LOGIC
        Color originalColor = batch.getColor();
        if (player.isSuperActive) {
            float pulse = MathUtils.sin(Gdx.graphics.getFrameId() * 1f) * 0.3f + 0.6f;
            batch.setColor(0.2f * pulse, 1f, 1f, 1f);
        } else if (player.currentEnergy >= player.baseEnergy) {
            float pulse = MathUtils.sin(Gdx.graphics.getFrameId() * 0.15f) * 0.25f + 0.75f;
            batch.setColor(0f, 1f * pulse, 1f * pulse, 1f);
        } else {
            batch.setColor(Color.WHITE);
        }

        batch.draw(energyBar, x + 57f * scale, y + 23f * scale, 43f * scale * enPercent, 5f * scale);
        batch.setColor(Color.WHITE); // Reset immediately

        // 4. Draw XP Bar
        batch.draw(xpBar, x + 18f * scale, y + 32f * scale, 76f * scale * xpPercent, 2f * scale);

        // 5. Draw UI Frame
        batch.draw(playerUI, x, y, w, h);

        // 6. Draw Text (HP & Energy)
        float offset = 0.3f;

        // --- HP TEXT ---
        tutorialFont.getData().setScale(0.5f);
        tutorialFont.setColor(Color.BLACK);
        String hpText = (int)player.currentHP + "/" + (int)player.baseHP;
        layout.setText(tutorialFont, hpText);
        float hpX = x + (12f * scale) + (43f * scale)/2f - layout.width/2f;
        float hpY = y + (23f * scale) + (5f * scale)/2f + layout.height/2f + 1f;
        drawBoldText(hpText, hpX, hpY, offset);

        // --- ENERGY TEXT (WITH PRESS R & PULSE) ---
        String enText;
        float energyTextScale = 0.5f;

        if (player.isSuperActive) {
            enText = String.format("SUPER: %.1fs", player.superDurationTimer);
            tutorialFont.setColor(new Color(0f, 0.8f, 1f, 1f));
        } else if (player.currentEnergy >= player.baseEnergy) {
            // MODE: Super Ready
            enText = "PRESS R";
            tutorialFont.setColor(Color.WHITE);

            // PULSE SCALE: Bounces scale between 0.45 and 0.65
            float textPulse = MathUtils.sin(Gdx.graphics.getFrameId() * 0.04f) * 0.07f;
            energyTextScale = 0.55f + textPulse;
        } else {
            // MODE: Charging
            enText = (int)player.currentEnergy + "/" + (int)player.baseEnergy;
            tutorialFont.setColor(Color.BLACK);
        }

        tutorialFont.getData().setScale(energyTextScale);
        layout.setText(tutorialFont, enText);

        // Recalculate alignment because pulsing scale shifts the center
        float enX = x + (57f * scale) + (43f * scale)/2f - layout.width/2f;
        float enY = y + (23f * scale) + (5f * scale)/2f + layout.height/2f + 1f;

        drawBoldText(enText, enX, enY, offset);

        // Reset Font for other UI elements
        tutorialFont.getData().setScale(1.0f);
        tutorialFont.setColor(Color.WHITE);

        // 7. Draw Level
        drawLevelText(y, h, scale);
    }

    // Helper for the fake bolding
    private void drawBoldText(String text, float x, float y, float offset) {
        tutorialFont.draw(batch, text, x - offset, y);
        tutorialFont.draw(batch, text, x + offset, y);
        tutorialFont.draw(batch, text, x, y - offset);
        tutorialFont.draw(batch, text, x, y + offset);
        tutorialFont.draw(batch, text, x, y);
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
        // INCREASED: from 3f to 5f
        float scale = getScale(6f);
        float w = base * scale;
        float h = base * scale;

        float x = (viewport.getWorldWidth() - w) / 2f;
        // Move it down slightly from the very top
        float y = viewport.getWorldHeight() - h / 1.4f;
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
        float y = viewport.getWorldHeight() - 105f;
        waveFont.draw(batch, label, x, y);
        waveFont.draw(batch, value, x + lw + 10f, y);
    }

    private void drawScore() {
        scoreFont.getData().setScale(1.2f); // Make the font a bit beefier
        scoreLayout.setText(scoreFont, "Score: ");
        float x = 40f; // More padding from left
        float y = viewport.getWorldHeight() - 40f; // More padding from top
        scoreFont.draw(batch, "Score: ", x, y);
        scoreFont.draw(batch, String.valueOf(world.getScore()), x + scoreLayout.width, y);
    }

    private void drawCombo() {
        int combo = world.getCombo();
        if (combo <= 0) return;

        comboFont.getData().setScale(1.2f);
        comboLayout.setText(comboFont, "Combo: ");
        float x = 40f;
        float y = viewport.getWorldHeight() - 120f;

        comboFont.draw(batch, "Combo: ", x, y);
        comboFont.draw(batch, combo + "x", x + comboLayout.width, y);
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
        return maxScale;
    }

    public void resize(int w, int h) {
        viewport.update(w, h, true);
    }

    public void dispose() {
        if(tutorialManager != null) tutorialManager.dispose();
        if(darkOverlay != null) darkOverlay.dispose();
        if(slotBorder != null) slotBorder.dispose();
        if(tooltipFont != null) tooltipFont.dispose();
    }
}
