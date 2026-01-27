package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;

public class AugmentManager {

    private final List<Augment> masterAugmentList = new ArrayList<>();
    private final List<Augment> currentPool = new ArrayList<>();
    public final List<Augment> currentOptions = new ArrayList<>();
    public final Augment[] playerSlots = new Augment[5];
    private int slotsFilled = 0;

    // Textures
    public Texture augmentCardSheet;
    public Texture augmentIconSheet;

    private TextureRegion[] augmentIcons; // Small icons for the bar
    private TextureRegion[] augmentCards; // Full cards for the selection menu

    public AugmentManager() {
        loadAssets();
        initializeAugments();
        resetPool();
    }

    private void loadAssets() {
        // --- LOAD FULL CARDS (48x64) ---
        augmentCardSheet = new Texture("GameUI/augmentsSpritesheet.png");

        // Split the sheet into individual card regions
        TextureRegion[][] tmpCards = TextureRegion.split(augmentCardSheet, 48, 64);
        int numCards = tmpCards.length * tmpCards[0].length;
        augmentCards = new TextureRegion[numCards];
        int index = 0;
        for (TextureRegion[] row : tmpCards) {
            for (TextureRegion region : row) {
                augmentCards[index++] = region;
            }
        }

        // --- LOAD ICONS (32x32) ---
        augmentIconSheet = new Texture("GameUI/augmentIconsSpritesheet.png");
        TextureRegion[][] tmpIcons = TextureRegion.split(augmentIconSheet, 32, 32);

        int numIcons = tmpIcons.length * tmpIcons[0].length;
        augmentIcons = new TextureRegion[numIcons];
        index = 0;
        for (TextureRegion[] row : tmpIcons) {
            for (TextureRegion region : row) {
                augmentIcons[index++] = region;
            }
        }
    }

    private void initializeAugments() {
        // --- 1. VAMPIRIC ---
        masterAugmentList.add(new Augment("VAMPIRIC", "Vampiric", "Lose DMG near light, Gain Lifesteal", 0));
        // --- 2. BLOODTHIRSTY ---
        masterAugmentList.add(new Augment("BLOODTHIRSTY", "Bloodthirsty", "Attack DMG boost after a kill", 1));
// -    -- 3. POISON BLADE ---
        masterAugmentList.add(new Augment("POISON_BLADE", "Poison Blade", "Attacks apply Poison", 2));
        // --- 4. LUCKY THREES ---
        masterAugmentList.add(new Augment("LUCKY_THREES", "Lucky Threes", "Every 3rd Attack Heals & Deals Bonus Dmg", 3));
        // --- 5. AURA FARMING ---
        masterAugmentList.add(new Augment("AURA_FARMING", "Aura Farming", "If you stand Still: Forcefield + HP Regen", 4));
        // --- 6. VETERAN ---
        masterAugmentList.add(new Augment("VETERAN", "Veteran", "Taking Dmg increases Defense temporarily", 5));
        // --- 7. SLOW GROWTH ---
        masterAugmentList.add(new Augment("SLOW_GROWTH", "Slow Growth", "Gain Permanent HP on kill", 6));
        // --- 8. SUSPICIOUS DRUG ---
        masterAugmentList.add(new Augment("SUS_DRUG", "Sus Drug", "Trade Atk Stats for Vitality", 7));
        // --- 9. THE FLASH ---
        masterAugmentList.add(new Augment("THE_FLASH", "The Flash", "Gradually gain speed, Dmg scales with Speed", 8));
        // --- 10. PARKOUR ---
        masterAugmentList.add(new Augment("PARKOUR", "Parkour", "Dash to an object to Dash again", 9));
        // --- 11. SCAREDY CAT ---
        masterAugmentList.add(new Augment("SCAREDY_CAT", "Scaredy Cat", "Gain Speed near enemies", 10));
        // --- 12. SLOW BUT DANGEROUS ---
        masterAugmentList.add(new Augment("SLOW_DANGEROUS", "Slow & Dangerous", "-30% Speed, gain Dmg based on speed loss", 11));
        // --- 13. GRAVE LOOTER ---
        masterAugmentList.add(new Augment("GRAVE_LOOTER", "Grave Looter", "Enemies drop bonus loot", 13));
        // --- 14. MASOCHIST ---
        masterAugmentList.add(new Augment("MASOCHIST", "Masochist", "Taking damage can Heal you", 14));
        // --- 15. UNDERDOG ---
        masterAugmentList.add(new Augment("UNDERDOG", "Underdog", "Stats scale with nearby enemies", 16));
        // --- 16. LAST STAND ---
        masterAugmentList.add(new Augment("LAST_STAND", "Last Stand", "Gain Stats and lifesteal under 25% HP", 17));
        // --- 17. BOOSTED ANIMAL ---
        masterAugmentList.add(new Augment("BOOSTED_ANIMAL", "Boosted Animal", "Boosts are stronger, some Permanent", 18));
        // --- 18. COMBO GOD ---
        masterAugmentList.add(new Augment("COMBO_GOD", "Combo God", "Combo boosts all Stats", 19));
    }

    public void resetPool() {
        currentPool.clear();
        currentPool.addAll(masterAugmentList);
        slotsFilled = 0;
        for (int i = 0; i < 5; i++) playerSlots[i] = null;
    }

    public void rollOptions() {
        currentOptions.clear();
        if (currentPool.isEmpty()) return;

        List<Augment> tempPool = new ArrayList<>(currentPool);
        int tries = Math.min(3, tempPool.size());

        for (int i = 0; i < tries; i++) {
            int r = MathUtils.random(tempPool.size() - 1);
            currentOptions.add(tempPool.get(r));
            tempPool.remove(r);
        }
    }

    public void selectAugment(Augment aug) {
        if (slotsFilled < 5) {
            playerSlots[slotsFilled++] = aug;
            currentPool.remove(aug);
            System.out.println("Picked: " + aug.name);
        }
    }

    public boolean hasSpace() { return slotsFilled < 5; }

    // Get the small icon for the bottom bar
    public TextureRegion getIcon(int index) {
        if (index < 0 || index >= augmentIcons.length) return null;
        return augmentIcons[index];
    }

    // NEW: Get the full card graphic for the selection menu
    public TextureRegion getAugmentCard(int index) {
        if (index < 0 || index >= augmentCards.length) return null;
        return augmentCards[index];
    }

    public void dispose() {
        augmentCardSheet.dispose();
        augmentIconSheet.dispose();
    }
}
