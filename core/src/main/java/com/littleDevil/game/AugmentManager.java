package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;

public class AugmentManager {

    private final GameWorld world;
    private final Player player;

    private final List<Augment> masterAugmentList = new ArrayList<>();
    private final List<Augment> currentPool = new ArrayList<>();
    public final List<Augment> currentOptions = new ArrayList<>();
    public final Augment[] playerSlots = new Augment[5];
    private int slotsFilled = 0;

    // Textures
    public Texture augmentCardSheet;
    public Texture augmentIconSheet;

    private TextureRegion[] augmentIcons;
    private TextureRegion[] augmentCards;

    public AugmentManager(GameWorld world, Player player) {
        this.world = world;
        this.player = player;

        loadAssets();
        initializeAugments();
        resetPool();
    }

    private void loadAssets() {
        augmentCardSheet = new Texture("GameUI/augmentsSpritesheet.png");
        TextureRegion[][] tmpCards = TextureRegion.split(augmentCardSheet, 48, 64);
        int numCards = tmpCards.length * tmpCards[0].length;
        augmentCards = new TextureRegion[numCards];
        int index = 0;
        for (TextureRegion[] row : tmpCards) {
            for (TextureRegion region : row) {
                augmentCards[index++] = region;
            }
        }

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
        masterAugmentList.add(new Augment("VAMPIRIC", "Vampiric", "Lose DMG near light, Gain Lifesteal", 0));
        masterAugmentList.add(new Augment("BLOODTHIRSTY", "Bloodthirsty", "Attack DMG boost after a kill", 1));
        masterAugmentList.add(new Augment("LUCKY_THREES", "Lucky Threes", "Every 3rd Attack Heals & Deals Bonus Dmg", 3));
        masterAugmentList.add(new Augment("VETERAN", "Veteran", "Taking Dmg increases Defense temporarily", 5));
        masterAugmentList.add(new Augment("SLOW_GROWTH", "Slow Growth", "Gain Permanent HP on kill", 6));
        masterAugmentList.add(new Augment("SUS_DRUG", "Sus Drug", "Trade Atk Stats for Vitality", 7));
        masterAugmentList.add(new Augment("THE_FLASH", "The Flash", "Gradually gain speed, Dmg scales with Speed", 8));
        masterAugmentList.add(new Augment("PARKOUR", "Parkour", "Dash to an object to Dash again", 9));
        masterAugmentList.add(new Augment("SCAREDY_CAT", "Scaredy Cat", "Gain Speed near enemies", 10));
        masterAugmentList.add(new Augment("SLOW_DANGEROUS", "Slow & Dangerous", "-30% Speed, gain Dmg based on speed loss", 11));
        masterAugmentList.add(new Augment("GRAVE_LOOTER", "Grave Looter", "Enemies drop bonus loot", 13));
        masterAugmentList.add(new Augment("MASOCHIST", "Masochist", "Taking damage can Heal you", 14));
        masterAugmentList.add(new Augment("LAST_STAND", "Last Stand", "Gain damage and lifesteal at low HP", 17));
        masterAugmentList.add(new Augment("BOOSTED_ANIMAL", "Boosted Animal", "Boosts are stronger, some Permanent", 18));
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

            // Apply the logic / stats immediately
            activateAugmentOnPlayer(aug);
        }
    }

    // =================================================================
    //                STAT APPLICATION & FLAG SETTING
    // =================================================================

    private void activateAugmentOnPlayer(Augment aug) {
        switch (aug.id) {
            // ATTACK
            case "VAMPIRIC":
                player.hasVampiric = true; // Set flag
                // Apply immediate stats
                player.baseLifesteal += 0.15f;
                player.lifesteal = player.baseLifesteal;
                break;

            case "BLOODTHIRSTY":
                player.hasBloodthirsty = true;
                break;

            case "LUCKY_THREES":
                player.hasLuckyThrees = true;
                break;

            // DEFENSE

            case "VETERAN":
                player.hasVeteran = true;
                break;

            case "SLOW_GROWTH":
                player.hasSlowGrowth = true;
                break;

            case "SUS_DRUG":
                // Pure stat change, no boolean flag needed unless you want visuals
                float difference = player.baseDamage * 0.3f;
                player.baseDamage *= 0.7f;
                player.damage = player.baseDamage;

                player.baseHP *= 1.2f + (int)difference;
                player.currentHP *= 1.2f + (int)difference;
                break;

            // SPEED

            case "THE_FLASH":
                player.hasTheFlash = true;
                break;

            case "PARKOUR":
                player.hasParkour = true;
                break;

            case "SCAREDY_CAT":
                player.hasScaredyCat = true;
                break;

            case "SLOW_DANGEROUS":
                player.hasSlowDangerous = true;
                // Immediate stat change
                int diff = (int)(player.baseSpeed * 0.5f);
                player.baseSpeed *= 0.7f;
                player.speed = player.baseSpeed;

                player.baseDamage += 20f + diff;
                player.damage = player.baseDamage;
                break;


            // LUCK

            case "GRAVE_LOOTER":
                player.hasGraveLooter = true;
                break;

            case "MASOCHIST":
                player.hasMasochist = true;
                break;

            // ALL

            case "LAST_STAND":
                player.hasLastStand = true;
                break;

            case "BOOSTED_ANIMAL":
                player.hasBoostedAnimal = true;
                break;

            case "COMBO_GOD":
                player.hasComboGod = true;
                break;
        }
    }

    public boolean hasSpace() { return slotsFilled < 5; }

    public TextureRegion getIcon(int index) {
        if (index < 0 || index >= augmentIcons.length) return null;
        return augmentIcons[index];
    }

    public TextureRegion getAugmentCard(int index) {
        if (index < 0 || index >= augmentCards.length) return null;
        return augmentCards[index];
    }

    public void dispose() {
        augmentCardSheet.dispose();
        augmentIconSheet.dispose();
    }
}
