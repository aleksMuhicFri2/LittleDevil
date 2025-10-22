package com.littleDevil.game;

import com.badlogic.gdx.graphics.Texture;

public class CollisionObjectsData {

    public static  CollisionObject[] collisionObjects = {
        // careful about this, they are sorted (check GameScreen init of small altars)
        new CollisionObject("smallAltarTopLeft", 64, 312, 16, 8, 8, 12, 0, 0, 4),
        new CollisionObject("smallAltarTopRight", 496, 312, 16, 8, 8, 12, 0, 0, 4),
        new CollisionObject("smallAltarBotRight", 496, 48, 16, 8, 8, 12, 0, 0, 4),
        new CollisionObject("smallAltarBotLeft", 64, 48, 16, 8, 8, 12, 0, 0, 4),

        // fences DONE
        new CollisionObject("FenceTopRight", 392, 272, 76, 52, -4, 0, 72, 72, 2, new Texture("MapAssets/fenceTopRight.png")),
        new CollisionObject("FenceBottomLeft", 132, 64, 76, 56, -4, 0, 72, 72, 2, new Texture("MapAssets/fenceBottomLeft.png")),
        new CollisionObject("FenceTopLeftHorizontal", 132, 320, 76, 8, -4, -4, 72, 24, 2, new Texture("MapAssets/fenceTopLeftHorizontal.png")),
        new CollisionObject("FenceTopLeftVertical", 132, 272, 12, 40, -4, 0, 8, 48, 2, new Texture("MapAssets/fenceTopLeftVertical.png")),
        new CollisionObject("FenceBottomRightHorizontal", 392, 65, 76, 4, -4, 0, 72, 24, 2, new Texture("MapAssets/fenceBottomRightHorizontal.png")),
        new CollisionObject("FenceBottomRightVertical", 456, 87, 12, 44, -4, -12, 8, 48, 2, new Texture("MapAssets/fenceBottomRightVertical.png")),

        // tree and monument done
        new CollisionObject("Tree", 112, 300, 48, 24, 16, 0, 72, 78, 2, new Texture("MapAssets/tree.png")),
        new CollisionObject("Monolith", 432, 77, 24, 24, -4, -8, 18, 48, 2, new Texture("MapAssets/monolith.png")),

        // lights middle done
        new CollisionObject("LightCenterLeft", 122, 204, 4, 4, 18, 0, 32, 48, 2, new Texture("MapAssets/lightNormalLeft.png")),
        new CollisionObject("LightCenterTop", 296, 316, 4, 4, 0, 0, 32, 48, 2, new Texture("MapAssets/lightNormalRight.png")),
        new CollisionObject("LightCenterRight", 456, 204, 4, 4, 0, 0, 32, 48, 2, new Texture("MapAssets/lightMossyRight.png")),
        new CollisionObject("LightCenterBot", 296, 80, 4, 4, 0, 0, 32, 48, 2, new Texture("MapAssets/lightMossyRight.png")),

        // lamps side done
        new CollisionObject("LampTopLeft", 20, 352, 4, 4, 4, 0, 16, 48, 2, new Texture("MapAssets/lampNormal.png")),
        new CollisionObject("LampTopRight", 560, 352, 4, 4, 4, 0, 16, 48, 2, new Texture("MapAssets/lampMossy.png")),
        new CollisionObject("LampBotRight", 560, 32, 4, 4, 4, 0, 16, 48, 2, new Texture("MapAssets/lampNormal.png")),
        new CollisionObject("LampBotLeft", 20, 32, 4, 4, 4, 0, 16, 48, 2, new Texture("MapAssets/lampMossy.png")),

        // big altar stairs
        new CollisionObject("BigAltarStairs", 280, 152, 36, 20, 0, 0, 0, 0, 5),

        // big altar walls
        new CollisionObject("BigAltarCollisionStairsLeft", 276, 152, 1, 28, 0, 0, 0, 0, 2),
        new CollisionObject("BigAltarCollisionStairsRight", 320, 152, 1, 28, 0, 0, 0, 0, 2),
        new CollisionObject("BigAltarCollisionBaseLeft", 228, 160, 12, 68, -4, 0, 0, 0, 2),
        new CollisionObject("BigAltarCollisionDownLeft", 228, 160, 54, 20, -4, 0, 0, 0, 2),
        new CollisionObject("BigAltarCollisionUpLeft", 236, 224, 8, 24, 0, 0, 0, 0, 2),
        new CollisionObject("BigAltarCollisionUp2Left", 240, 244, 24, 8, 0, -4, 0, 0, 2),
        new CollisionObject("BigAltarCollisionUpMiddle", 252, 256, 92, 8, 0, -4, 0, 0, 2),
        new CollisionObject("BigAltarCollisionBaseRight", 366, 160, 12, 68, -4, 0, 0, 0, 2),
        new CollisionObject("BigAltarCollisionDownRight", 324, 160, 54, 20, -4, 0, 0, 0, 2),
        new CollisionObject("BigAltarCollisionUpRight", 352, 224, 8, 24, 0, 0, 0, 0, 2),
        new CollisionObject("BigAltarCollisionUp2Right", 332, 244, 24, 8, 0, -4, 0, 0, 2),

        // Top border
        new CollisionObject("BorderTop",0,400 - 4,600 - 4,1,0,0,600,0,1),

        // Bottom border
        new CollisionObject("BorderBottom",0,0,600,1,0,0, 0, 0, 1),

        // Left border
        new CollisionObject("BorderLeft", 0,0,1,400,0,0,0,0,1),

        // Right border
        new CollisionObject("BorderRight",600 - 4,0,1,400,0,0,0,0,1)
    };
}
