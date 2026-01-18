package com.littleDevil.game;

public class Augment {
    public final String id;
    public final String name;
    public final String description;
    public final int iconIndex; // Position in the spritesheet (0, 1, 2...)

    public Augment(String id, String name, String description, int iconIndex) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconIndex = iconIndex;
    }
}
