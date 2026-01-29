package com.littleDevil.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;

public class TutorialManager {

    private final GameWorld gameWorld;
    private final Player player;
    private final BitmapFont font;
    private final GlyphLayout layout;

    // Textures
    private final Texture popupBackground;
    private final Texture iconBackground;

    public enum TutorialStep {
        MOVEMENT("Use WASD to Move"),
        DASH("Press SPACE while moving\nto use Dash"),
        ATTACK("Left Click to Attack"),
        FIRST_WAVE("Survive the first wave!\nGood luck!"),
        SKILL_POINT("Level Up! \nSkill Point earned.\nGo to the center Altar."),
        ALTAR_EXPLAIN("Here you can upgrade \nyour character"),
        COMPLETED("");

        final String text;
        TutorialStep(String text) { this.text = text; }
    }

    private TutorialStep currentStep = TutorialStep.MOVEMENT;

    // Tracking progress
    private float moveTimer = 0f;
    private boolean hasDashed = false;
    private boolean hasAttacked = false;

    // UI Dimensions
    private final float BOX_WIDTH = 260f;
    private final float BOX_HEIGHT = 70f;
    private final float PADDING = 10f;

    public TutorialManager(GameWorld gameWorld, BitmapFont font) {
        this.gameWorld = gameWorld;
        this.player = gameWorld.player;
        this.font = font;
        this.layout = new GlyphLayout();

        // 1. RECTANGLE BACKGROUND
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0f, 0f, 0f, 0.5f); // Translucent Black
        pix.fill();
        popupBackground = new Texture(pix);

        // 2. CIRCLE ICON
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

    public void update(float delta) {
        // 1. UPDATE GLOBAL LOCK
        if (currentStep.ordinal() > TutorialStep.DASH.ordinal()) {
            gameWorld.basicTutorialDone = true;
        }

        // 2. HIGH PRIORITY: DYNAMIC EVENTS (Skill Points)
        if (gameWorld.basicTutorialDone) {
            if (player.skillPoints > 0) {
                if (player.isOnAltar(gameWorld)) {
                    currentStep = TutorialStep.ALTAR_EXPLAIN;
                } else {
                    currentStep = TutorialStep.SKILL_POINT;
                }
                return;
            } else {
                if (currentStep == TutorialStep.SKILL_POINT || currentStep == TutorialStep.ALTAR_EXPLAIN) {
                    currentStep = TutorialStep.COMPLETED;
                }
            }
        }

        // 3. LOW PRIORITY: LINEAR TUTORIAL
        if (currentStep == TutorialStep.COMPLETED) return;

        switch (currentStep) {
            case MOVEMENT:
                // Check if any movement key is held
                if (isMovingInputPressed()) {
                    moveTimer += delta;
                    if (moveTimer > 1.5f) advanceStep(TutorialStep.ATTACK);
                }
                break;

            case ATTACK:
                // Only count the attack if they are NOT standing on the Altar
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    if (!player.isOnAltar(gameWorld) && !player.isUnreachable(gameWorld)) {
                        hasAttacked = true;
                        advanceStep(TutorialStep.DASH);
                    }
                }
                break;

            case DASH:
                // Only count the dash if SPACE is pressed WHILE a movement key is held
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    if (isMovingInputPressed()) {
                        hasDashed = true;
                        advanceStep(TutorialStep.FIRST_WAVE);
                        gameWorld.canStartNextWave = true;
                        gameWorld.startWave();
                    }
                }
                break;

            case FIRST_WAVE:
                if (gameWorld.getWave() > 1) {
                    advanceStep(TutorialStep.COMPLETED);
                }
                break;
            default:
                break;
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
        if (currentStep == TutorialStep.COMPLETED) return;
        if (currentStep == TutorialStep.FIRST_WAVE && gameWorld.enemiesAlive > 0) return;

        String text = currentStep.text;
        if (text.isEmpty()) return;

        // 1. SMALLER FONT SCALE
        // Reducing from 1.0f to 0.7f or 0.8f for a more compact look
        font.getData().setScale(0.75f);

        // 2. SMALLER DYNAMIC BOX
        // We tighten the max width so the box is more "vertical" and compact
        float maxTextWidth = 300f; // Reduced from 400f
        layout.setText(font, text, Color.WHITE, maxTextWidth, Align.center, true);

        // Reduced padding for a smaller footprint
        float dynamicPadding = 25f; // Reduced from 40f
        float dynamicBoxW = layout.width + (dynamicPadding * 2);
        float dynamicBoxH = layout.height + (dynamicPadding * 2);

        // 3. MOVE DOWN AND RIGHT
        // Move Right: Margin reduced from 60f to 30f
        float x = screenWidth - dynamicBoxW - 30f;

        // Move Down: Changed from 0.7f (70%) to 0.5f (50%) to place it near the vertical center
        float y = screenHeight * 0.5f;

        // 4. DRAW BACKGROUND
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(popupBackground, x, y, dynamicBoxW, dynamicBoxH);

        // 5. DRAW CIRCLE ICON (Scaled down slightly)
        float iconSize = 38f; // Reduced from 48f
        float iconX = x + dynamicBoxW - iconSize / 2f;
        float iconY = y + dynamicBoxH - iconSize / 2f;
        batch.draw(iconBackground, iconX, iconY, iconSize, iconSize);

        // 6. DRAW "!"
        // Calculate "!" position specifically
        GlyphLayout bangLayout = new GlyphLayout(font, "!");
        font.draw(batch, "!", iconX + (iconSize - bangLayout.width)/2f, iconY + (iconSize + bangLayout.height)/2f);

        // 7. DRAW MAIN TEXT
        float textY = y + (dynamicBoxH + layout.height) / 2f;
        font.draw(batch, text, x + dynamicPadding, textY, layout.width, Align.center, true);

        // RESET scale so it doesn't break other UI fonts
        font.getData().setScale(1.0f);
    }

    public void dispose() {
        popupBackground.dispose();
        iconBackground.dispose();
    }
}
