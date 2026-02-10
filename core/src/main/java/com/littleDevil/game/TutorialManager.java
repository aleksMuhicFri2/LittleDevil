package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;

public class TutorialManager {

    private final GameWorld gameWorld;
    private final Player player;
    private final BitmapFont font;
    private final GlyphLayout layout;

    // Textures
    private final Texture popupBackground;
    private final Texture iconBackground;

    private float animationTimer = 0f;
    private float warningTimer = 0f;

    public enum TutorialStep {
        // --- PHASE 1: BASICS ---
        MOVEMENT("Use WASD to Move"),
        WALK_OUT("Step out of the Safe Zone\nto use your weapons"),

        ATTACK("Left Click to Attack"),
        WARN_ATTACK_ALTAR("Cannot attack inside\nthe Safe zone!"),

        DASH("Press SPACE while moving\nto use Dash"),
        WARN_DASH_STILL("You must be moving\nto Dash!"),

        // --- PHASE 2: GAMEPLAY ---
        FIRST_WAVE("Survive the first wave!\nGood luck!"),

        // --- PHASE 3: DYNAMIC ALERTS (Post-Tutorial) ---
        SKILL_POINT("Level Up!\nSkill Point earned.\nGo to the center Altar."),
        AUGMENT_READY("Augment Available!\nReturn to the Altar."),

        ALTAR_UPGRADE("Step into the altar to\nupgrade your character"),
        NO_UPGRADES("No points to spend.\nKill enemies to gain XP!"),

        COMPLETED("");

        final String text;
        TutorialStep(String text) { this.text = text; }
    }

    private TutorialStep currentStep = TutorialStep.MOVEMENT;
    private TutorialStep previousStep = null;

    // Tracking progress
    private float moveTimer = 0f;

    // UI Dimensions
    private final float PADDING = 10f;

    public TutorialManager(GameWorld gameWorld, BitmapFont font) {
        this.gameWorld = gameWorld;
        this.player = gameWorld.player;
        this.font = font;
        this.layout = new GlyphLayout();

        // 1. BACKGROUND
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0f, 0f, 0f, 0.5f);
        pix.fill();
        popupBackground = new Texture(pix);

        // 2. ICON
        iconBackground = createCircleTexture(32, new Color(0.8f, 0.1f, 0.1f, 1f));
        pix.dispose();
    }

    private Texture createCircleTexture(int diameter, Color color) {
        Pixmap pix = new Pixmap(diameter, diameter, Pixmap.Format.RGBA8888);
        pix.setColor(color);
        pix.fillCircle(diameter / 2, diameter / 2, diameter / 2);
        Texture tex = new Texture(pix);
        pix.dispose();
        return tex;
    }

    // --- CLEAN HELPER ---
    private boolean isInSafeZone() {
        return player.isOnAltar(gameWorld) || player.isUnreachable(gameWorld);
    }

    public void update(float delta) {
        animationTimer += delta;

        // 1. PERSISTENT INPUT CHECK
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (isInSafeZone()) {
                triggerWarning(TutorialStep.WARN_ATTACK_ALTAR);
            }
        }

        // 2. HANDLE WARNING TIMER
        if (warningTimer > 0) {
            warningTimer -= delta;

            if (currentStep == TutorialStep.WARN_ATTACK_ALTAR && !isInSafeZone()) {
                warningTimer = 0;
            }

            if (warningTimer <= 0) {
                if (previousStep != null) {
                    currentStep = previousStep;
                    previousStep = null;
                } else {
                    currentStep = TutorialStep.COMPLETED;
                }
            }
            return;
        }

        // 3. CHECK TUTORIAL COMPLETION LOCK
        // --- FIX IS HERE: Changed >= to > ---
        // We only want to mark the tutorial "Done" AFTER we pass the FIRST_WAVE step.
        if (currentStep.ordinal() > TutorialStep.FIRST_WAVE.ordinal()) {
            gameWorld.basicTutorialDone = true;
        }

        // 4. BRANCH LOGIC
        if (gameWorld.basicTutorialDone) {
            updateDynamicEvents();
        } else {
            updateLinearTutorial(delta);
        }
    }

    // --- LOGIC A: Post-Tutorial Smart Tips ---
    private void updateDynamicEvents() {
        boolean inSafeZone = isInSafeZone();
        boolean hasAugments = gameWorld.pendingAugments > 0;
        boolean hasSkillPoints = player.skillPoints > 0;

        if (hasAugments || hasSkillPoints) {
            if (inSafeZone) {
                currentStep = TutorialStep.ALTAR_UPGRADE;
            } else {
                currentStep = hasAugments ? TutorialStep.AUGMENT_READY : TutorialStep.SKILL_POINT;
            }
        }
        else if (player.isOnAltar(gameWorld)) {
            currentStep = TutorialStep.NO_UPGRADES;
        }
        else {
            currentStep = TutorialStep.COMPLETED;
        }
    }

    // --- LOGIC B: The First-Run Tutorial ---
    private void updateLinearTutorial(float delta) {
        switch (currentStep) {
            case MOVEMENT:
                if (isMovingInputPressed()) {
                    moveTimer += delta;
                    if (moveTimer > 2.0f) {
                        if (isInSafeZone()) advanceStep(TutorialStep.WALK_OUT);
                        else advanceStep(TutorialStep.ATTACK);
                    }
                }
                break;

            case WALK_OUT:
                if (!isInSafeZone()) {
                    advanceStep(TutorialStep.ATTACK);
                }
                break;

            case ATTACK:
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    if (!isInSafeZone()) {
                        advanceStep(TutorialStep.DASH);
                    }
                }
                break;

            case DASH:
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    if (!isMovingInputPressed()) {
                        triggerWarning(TutorialStep.WARN_DASH_STILL);
                    } else {
                        advanceStep(TutorialStep.FIRST_WAVE);
                        gameWorld.canStartNextWave = true;
                        gameWorld.startWave();
                    }
                }
                break;

            case FIRST_WAVE:
                // Only mark completed when wave 2 starts
                if (gameWorld.getWave() > 1) {
                    advanceStep(TutorialStep.COMPLETED);
                }
                break;

            default: break;
        }
    }

    private void triggerWarning(TutorialStep warningStep) {
        if (warningTimer <= 0) {
            previousStep = currentStep;
            currentStep = warningStep;
            warningTimer = 2.0f;
        }
    }

    private void advanceStep(TutorialStep next) {
        if (this.currentStep == next) return;
        this.currentStep = next;
    }

    private boolean isMovingInputPressed() {
        return Gdx.input.isKeyPressed(Input.Keys.W) ||
            Gdx.input.isKeyPressed(Input.Keys.A) ||
            Gdx.input.isKeyPressed(Input.Keys.S) ||
            Gdx.input.isKeyPressed(Input.Keys.D);
    }

    public void render(SpriteBatch batch, float screenWidth, float screenHeight) {
        // 1. Don't draw if Completed AND we aren't showing a warning/tip
        if (currentStep == TutorialStep.COMPLETED && warningTimer <= 0) return;

        String text = currentStep.text;
        if (text == null || text.isEmpty()) return;

        font.getData().setScale(0.75f);
        float maxTextWidth = 300f;
        layout.setText(font, text, Color.WHITE, maxTextWidth, Align.center, true);

        float dynamicPadding = 25f;
        float dynamicBoxW = layout.width + (dynamicPadding * 2);
        float dynamicBoxH = layout.height + (dynamicPadding * 2);

        float x = screenWidth - dynamicBoxW - 30f;
        float y = screenHeight * 0.5f;

        // Background
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(popupBackground, x, y, dynamicBoxW, dynamicBoxH);

        // PULSING ANIMATION
        float pulse = 1f + MathUtils.sin(animationTimer * 6f) * 0.1f;

        boolean isWarning = (warningTimer > 0 || currentStep == TutorialStep.NO_UPGRADES);
        Color circleColor = isWarning ? Color.RED : new Color(0.8f, 0.1f, 0.1f, 1f);

        float baseIconSize = 60f;
        float currentIconSize = baseIconSize * pulse;

        float centerX = x + dynamicBoxW;
        float centerY = y + dynamicBoxH;

        batch.setColor(circleColor);
        batch.draw(iconBackground,
            centerX - currentIconSize / 2f,
            centerY - currentIconSize / 2f,
            currentIconSize, currentIconSize
        );
        batch.setColor(Color.WHITE);

        // "!" DRAWING
        font.getData().setScale(1.2f * pulse);
        GlyphLayout bangLayout = new GlyphLayout(font, "!");
        float bangX = centerX - bangLayout.width / 2f;
        float bangY = centerY + bangLayout.height / 2f;
        font.draw(batch, "!", bangX, bangY);

        // MAIN TEXT
        font.getData().setScale(0.75f);
        float textY = y + (dynamicBoxH + layout.height) / 2f;
        font.draw(batch, text, x + dynamicPadding, textY, layout.width, Align.center, true);

        font.getData().setScale(1.0f);
    }

    public void dispose() {
        popupBackground.dispose();
        iconBackground.dispose();
    }
}
