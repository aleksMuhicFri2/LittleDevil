package com.littleDevil.game;

import com.badlogic.gdx.Game;

public class Main extends Game {

    @Override
    public void create() {

        this.setScreen(new StartScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
