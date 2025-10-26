package com.littleDevil.game;

public class LightData {

    public static Light[] lightObjects = {
        // streetlights done
        new Light(316, 105, 128, 96, "MapAssets/light1.png", 1f),
        new Light(128, 228, 128, 96, "MapAssets/light1.png", 1f),
        new Light(476, 228, 128, 96, "MapAssets/light1.png", 1f),
        new Light(316, 340, 128, 96, "MapAssets/light1.png", 1f),
        new Light(28, 36, 128, 96, "MapAssets/light2.png", 0.8f),
        new Light(28, 356, 128, 96, "MapAssets/light2.png", 0.8f),
        new Light(568, 356, 128, 96, "MapAssets/light2.png", 0.8f),
        new Light(568, 36, 128, 96, "MapAssets/light2.png", 0.8f),

        // candles without interaction done
        new Light(233, 192, 40, 40, "MapAssets/light3.png", 0.7f),
        new Light(233, 236, 40, 40, "MapAssets/light3.png", 0.7f),
        new Light(366, 192, 40, 40, "MapAssets/light3.png", 0.7f),
        new Light(366, 236, 40, 40, "MapAssets/light3.png", 0.7f),
        new Light(261, 264, 40, 40, "MapAssets/light3.png", 0.7f),
        new Light(338, 264, 40, 40, "MapAssets/light3.png", 0.7f),


        // candles with interaction indexes [14, 15, 16, 17, 18]
        new Light(283, 215, 32, 32, "MapAssets/light5.png", 0f),
        new Light(301, 251, 32, 32, "MapAssets/light5.png", 0f),
        new Light(320, 215, 32, 32, "MapAssets/light5.png", 0f),
        new Light(273, 239, 32, 32, "MapAssets/light5.png", 0f),
        new Light(330, 239, 32, 32, "MapAssets/light5.png", 0f),

        // altars done indexes [19, 20, 21, 22]
        new Light(514, 66, 48, 32, "MapAssets/light4.png", 0f),
        new Light(82, 66, 48, 32, "MapAssets/light4.png", 0f),
        new Light(514, 330, 48, 32, "MapAssets/light4.png", 0f),
        new Light(82, 330, 48, 32, "MapAssets/light4.png", 0f),
    };
}
