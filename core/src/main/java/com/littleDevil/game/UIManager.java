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
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.util.List;

public class UIManager {

    private final GameWorld world;
    private final Player player;

    private final SpriteBatch batch;

    private final OrthographicCamera camera;
    private final ExtendViewport viewport;

    private final BitmapFont scoreFont, waveFont, comboFont, levelFont;
    private final BitmapFont skillPointsFont;

    // Removed descFont, we will use tutorialFont instead for better quality
    private final BitmapFont tooltipFont;
    // Added reference to tutorialFont here for easy access
    private final BitmapFont tutorialFont;


    private final GlyphLayout scoreLayout, waveLayout, comboLayout, levelLayout;
    private final GlyphLayout skillPointsLayout;
    private final GlyphLayout layout;

    private final Texture playerUI, wavePanel, barsBackground, healthBar, energyBar, xpBar;

    private final Texture darkOverlay;
    private final Texture slotBorder;

    private float uiOffsetY = 0f;
    private float targetUiOffsetY = 0f;
    private final float UI_SLIDE_DISTANCE = 70f;
    private final float UI_SLIDE_SPEED = 8f;

    private boolean upgradePageOpen = false;
    private boolean augmentPageOpen = false;

    private float upgradePanelWidth = 500f;
    private float upgradePanelHeight = 380f;

    private TextureRegion[][] levelSprites;
    private Texture plusButton;
    private Texture panelBackground;

    private final Vector3 tmp = new Vector3();

    private TutorialManager tutorialManager;

    private final float CARD_SCALE = 5.5f;
    private final float CARD_W = 48f * CARD_SCALE;
    private final float CARD_H = 64f * CARD_SCALE;
    private final float CARD_SPACING = 300f;

    private final float SLOT_SIZE = 40f;
    private final float SLOT_VISIBLE_Y = 15f;
    private final float SLOT_SPACING = 29f;

    private float[] augmentCardScales = {1f, 1f, 1f};
    private final float HOVER_SCALE = 1.2f; // How big it grows (1.2 = 20% bigger)
    private final float SCALE_SPEED = 15f;


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
        BitmapFont skillPointsFont,
        BitmapFont tutorialFont, // Passed in constructor
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
        this.tutorialFont = tutorialFont; // Assign it

        // Removed descFont initialization

        this.tooltipFont = new BitmapFont();
        this.tooltipFont.getData().setScale(0.8f);

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
        viewport.apply();
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

        tutorialManager.render(batch, viewport.getWorldWidth(), viewport.getWorldHeight());

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

    public void openAugmentPage() {
        if (!augmentPageOpen) {
            augmentPageOpen = true;
            world.augmentManager.rollOptions();
        }
    }
    public void closeAugmentPage() { augmentPageOpen = false; }


    private void renderAugmentPage() {
        // 1. Draw Background Overlay
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(darkOverlay, camera.position.x - viewport.getWorldWidth()/2, camera.position.y - viewport.getWorldHeight()/2, viewport.getWorldWidth(), viewport.getWorldHeight());

        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;

        // --- NEW: DRAW HEADER TEXT ---
        tutorialFont.setColor(Color.WHITE);
        tutorialFont.getData().setScale(1.5f); // Big scale
        layout.setText(tutorialFont, "CHOOSE YOUR AUGMENT");

        float textX = centerX - layout.width / 2f;
        // Position it well above the cards (Card Height is roughly 350px)
        float textY = centerY + (CARD_H * 0.65f);

        tutorialFont.draw(batch, layout, textX, textY);

        // Reset font for next draws
        tutorialFont.getData().setScale(1f);
        // -----------------------------

        // 2. Get mouse position for hover checks
        Vector3 mouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

        float[] positionsX = { centerX - CARD_SPACING, centerX, centerX + CARD_SPACING };
        List<Augment> options = world.augmentManager.currentOptions;

        // 3. Draw Cards
        // Added index arguments (0, 1, 2)
        if (options.size() >= 1) {
            drawAugmentCardWithHover(options.get(0), positionsX[0], centerY, 0, mouse);
        }
        if (options.size() >= 2) {
            drawAugmentCardWithHover(options.get(1), positionsX[1], centerY, 1, mouse);
        }
        if (options.size() >= 3) {
            drawAugmentCardWithHover(options.get(2), positionsX[2], centerY, 2, mouse);
        }
    }

    // Helper to keep the main render method clean
    private void drawAugmentCardWithHover(Augment aug, float x, float y, int index, Vector3 mouse) {
        boolean isHovered = mouse.x >= x - CARD_W/2f && mouse.x <= x + CARD_W/2f &&
            mouse.y >= y - CARD_H/2f && mouse.y <= y + CARD_H/2f;

        // Smoothly animate scale
        float targetScale = isHovered ? HOVER_SCALE : 1.0f;
        augmentCardScales[index] = MathUtils.lerp(augmentCardScales[index], targetScale, Gdx.graphics.getDeltaTime() * SCALE_SPEED);

        drawAugmentCard(aug, x, y, index);
    }

    private void drawAugmentCard(Augment aug, float centerX, float centerY, int cardIndex) {

        // 1. Calculate Mouse State & Animation
        float dt = Gdx.graphics.getDeltaTime();

        // Unproject mouse coordinates
        float mx = Gdx.input.getX();
        float my = Gdx.input.getY();
        viewport.unproject(tmp.set(mx, my, 0));

        // Get current animated scale for this slot
        float currentScale = augmentCardScales[cardIndex];

        // Calculate dimensions based on CURRENT scale
        float drawW = CARD_W * currentScale;
        float drawH = CARD_H * currentScale;

        // Center the card based on its grown size
        float x = centerX - drawW / 2f;
        float y = centerY - drawH / 2f;

        // Check Hover (Collision)
        boolean isHovered = (tmp.x >= x && tmp.x <= x + drawW && tmp.y >= y && tmp.y <= y + drawH);

        // Smoothly interpolate scale based on hover state
        float targetScale = isHovered ? HOVER_SCALE : 1.0f;
        augmentCardScales[cardIndex] = MathUtils.lerp(augmentCardScales[cardIndex], targetScale, dt * SCALE_SPEED);

        // Re-assign scale for drawing after the update
        currentScale = augmentCardScales[cardIndex];

        // --- DRAWING ---

        // 2. Draw Card Background
        TextureRegion cardRegion = world.augmentManager.getAugmentCard(aug.iconIndex);
        if (cardRegion != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(cardRegion, x, y, drawW, drawH);
        }

        // 3. Draw Title
        // Scale font size by currentScale so text grows with the card
        float baseTitleScale = 0.7f;
        tutorialFont.getData().setScale(baseTitleScale * currentScale);
        tutorialFont.setColor(Color.GOLD);

        layout.setText(tutorialFont, aug.name);

        float titleX = centerX - layout.width / 2f; // Easiest to center using centerX directly
        // Adjust offset relative to card height
        float titleY = y + (drawH * 0.37f);

        tutorialFont.draw(batch, layout, titleX, titleY);

        // 4. Draw Description
        float baseDescScale = 0.55f;
        tutorialFont.getData().setScale(baseDescScale * currentScale);
        tutorialFont.setColor(Color.WHITE);

        float leftSidePadding = 65f * currentScale;
        float rightSidePadding = 65f * currentScale;
        float maxTextWidth = drawW - (leftSidePadding + rightSidePadding);

        layout.setText(tutorialFont, aug.description, Color.WHITE, maxTextWidth, Align.left, true);

        float descX = x + leftSidePadding;
        float descY = y + (drawH * 0.30f);

        tutorialFont.draw(batch, layout, descX, descY);

        // Reset Font State
        tutorialFont.getData().setScale(1f);
        tutorialFont.setColor(Color.WHITE);

        // 5. Input Logic
        // We already checked hover above, so we just check for click
        if (isHovered && Gdx.input.justTouched()) {
            world.augmentManager.selectAugment(aug);
            world.augmentSelectionPending = false;

            // Reset scales for next time we open the menu
            augmentCardScales[0] = 1f;
            augmentCardScales[1] = 1f;
            augmentCardScales[2] = 1f;

            closeAugmentPage();
            openUpgradePage();
        }
    }


    // --- AUGMENT SLOTS (BOTTOM BAR) ---
    private void drawAugmentSlots() {
        float totalWidth = (5 * SLOT_SIZE) + (4 * SLOT_SPACING);
        float startX = (viewport.getWorldWidth() - totalWidth) / 2f;

        float currentY = (SLOT_VISIBLE_Y - UI_SLIDE_DISTANCE) + uiOffsetY;

        for (int i = 0; i < 5; i++) {
            // This is the logical top-left corner of the "slot"
            float slotX = startX + i * (SLOT_SIZE + SLOT_SPACING);

            Augment aug = world.augmentManager.playerSlots[i];

            if (aug != null) {
                TextureRegion icon = world.augmentManager.getIcon(aug.iconIndex);

                if (icon != null) {
                    // 1. MAKE IT LARGER
                    // Multiply SLOT_SIZE by 1.5 (or whatever scale you prefer)
                    float iconScale = 1.4f;
                    float drawSize = SLOT_SIZE * iconScale;

                    // 2. CENTER IT
                    // Formula: SlotPosition + (SlotSize - ImageSize) / 2
                    float drawX = slotX + (SLOT_SIZE - drawSize) / 2f;
                    float drawY = currentY + (SLOT_SIZE - drawSize) / 2f;

                    batch.setColor(1f, 1f, 1f, 1f);
                    batch.draw(icon, drawX, drawY, drawSize, drawSize);

                    // 3. TOOLTIP DETECTION
                    // We check if the mouse is inside the drawn image area
                    if (currentY > 0) {
                        Vector3 mouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

                        if (mouse.x >= drawX && mouse.x <= drawX + drawSize &&
                            mouse.y >= drawY && mouse.y <= drawY + drawSize) {

                            // Draw tooltip centered above the icon
                            drawTooltip(aug, slotX + SLOT_SIZE / 2f, drawY + drawSize + 10f);
                        }
                    }
                }
            }
        }
    }

    private void drawTooltip(Augment aug, float x, float y) {
        String text = aug.name + "\n" + aug.description;
        tooltipFont.getData().setScale(0.8f);
        layout.setText(tooltipFont, text);

        float w = layout.width + 20f;
        float h = layout.height + 20f;

        batch.setColor(0f, 0f, 0f, 0.95f);
        batch.draw(slotBorder, x - w/2f, y, w, h);
        batch.setColor(1f, 1f, 1f, 1f);

        tooltipFont.setColor(Color.YELLOW);
        tooltipFont.draw(batch, text, x - layout.width/2f, y + h - 10f);
        tooltipFont.setColor(Color.WHITE);
    }


    private void renderUpgradePage() {
        float panelX = camera.position.x - upgradePanelWidth / 2f;
        float panelY = camera.position.y - upgradePanelHeight / 2f;

        batch.draw(panelBackground, panelX, panelY, upgradePanelWidth, upgradePanelHeight);
        drawSkillPoints(panelX, panelY, upgradePanelWidth, upgradePanelHeight);

        float rowSpacing = 40f;
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
        skillPointsLayout.setText(skillPointsFont, text);
        float textX = panelX + (panelW - skillPointsLayout.width) / 2f;
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

        // Only draw button if we have points
        if (player.skillPoints > 0) {
            batch.draw(plusButton, plusX, plusY, bw, bh);
        }

        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX();
            float my = Gdx.input.getY();
            viewport.unproject(tmp.set(mx, my, 0));

            // Check click
            if (tmp.x >= plusX && tmp.x <= plusX + bw &&
                tmp.y >= plusY && tmp.y <= plusY + bh) {

                // 1. Spend the point
                player.upgradeStat(statType);

                // 2. If that was the last point, close the page immediately
                if (player.skillPoints <= 0) {
                    closeUpgradePage();
                }
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

    public void dispose() {
        if(tutorialManager != null) tutorialManager.dispose();
        if(darkOverlay != null) darkOverlay.dispose();
        if(slotBorder != null) slotBorder.dispose();
        if(tooltipFont != null) tooltipFont.dispose();
    }
}
