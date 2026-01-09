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
        DASH("Press SPACE to Dash"),
        ATTACK("Left Click to Attack"),
        FIRST_WAVE("Survive the first wave!"),
        SKILL_POINT("Level Up! Skill Point earned.\nGo to the center Altar."),
        ALTAR_EXPLAIN("Safe Zone Active.\nStay here to open Upgrades."),
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
        // 1. UPDATE GLOBAL LOCK (Basic Tutorial Done?)
        // If we have passed the DASH step, the basic mechanics are learned.
        if (currentStep.ordinal() > TutorialStep.DASH.ordinal()) {
            gameWorld.basicTutorialDone = true;
        }

        // 2. HIGH PRIORITY: DYNAMIC EVENTS (Skill Points)
        // If basic tutorial is done, Skill Points ALWAYS take priority over everything else.
        if (gameWorld.basicTutorialDone) {
            if (player.skillPoints > 0) {
                // We have points, so we MUST show one of these two messages
                if (player.isOnAltar(gameWorld)) {
                    currentStep = TutorialStep.ALTAR_EXPLAIN;
                } else {
                    currentStep = TutorialStep.SKILL_POINT;
                }
                // Stop here. Do not process linear tutorial logic.
                return;
            } else {
                // We have 0 points.
                // If we were showing a skill message, we are now done with it.
                if (currentStep == TutorialStep.SKILL_POINT || currentStep == TutorialStep.ALTAR_EXPLAIN) {
                    currentStep = TutorialStep.COMPLETED;
                }
            }
        }

        // 3. LOW PRIORITY: LINEAR TUTORIAL
        // If we are completed, stop.
        if (currentStep == TutorialStep.COMPLETED) return;

        switch (currentStep) {
            case MOVEMENT:
                if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.A) ||
                    Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.D)) {
                    moveTimer += delta;
                    if (moveTimer > 1.5f) advanceStep(TutorialStep.ATTACK);
                }
                break;
            case ATTACK:
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    hasAttacked = true;
                    advanceStep(TutorialStep.DASH);
                }
                break;
            case DASH:
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                    hasDashed = true;
                    advanceStep(TutorialStep.FIRST_WAVE);
                }
                break;
            case FIRST_WAVE:
                // If wave 2 starts, we are done with the linear part
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

    public void render(SpriteBatch batch, float screenWidth, float screenHeight) {
        // Don't render if completed
        if (currentStep == TutorialStep.COMPLETED) return;

        // Specific hiding rule: Hide "First Wave" text while fighting to avoid clutter.
        // BUT: Do NOT hide Skill Point text even if fighting.
        if (currentStep == TutorialStep.FIRST_WAVE && gameWorld.enemiesAlive > 0) return;

        String text = currentStep.text;
        if (text.isEmpty()) return;

        float x = screenWidth - BOX_WIDTH - 20f;
        float y = screenHeight * 0.66f;

        // Draw Background
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(popupBackground, x, y, BOX_WIDTH, BOX_HEIGHT);

        // Draw Circle Icon
        float iconSize = 32f;
        float iconX = x + BOX_WIDTH - iconSize / 2f;
        float iconY = y + BOX_HEIGHT - iconSize / 2f;
        batch.draw(iconBackground, iconX, iconY, iconSize, iconSize);

        // Draw "!"
        font.getData().setScale(1.0f);
        layout.setText(font, "!");
        font.setColor(Color.WHITE);
        font.draw(batch, "!", iconX + (iconSize - layout.width)/2f, iconY + (iconSize + layout.height)/2f);

        // Draw Text
        font.getData().setScale(0.7f);
        layout.setText(font, text, Color.WHITE, BOX_WIDTH - PADDING * 2, Align.center, true);
        float textY = y + (BOX_HEIGHT + layout.height) / 2f;

        font.draw(batch, text, x + PADDING, textY, BOX_WIDTH - PADDING*2, Align.center, true);
    }

    public void dispose() {
        popupBackground.dispose();
        iconBackground.dispose();
    }
}
