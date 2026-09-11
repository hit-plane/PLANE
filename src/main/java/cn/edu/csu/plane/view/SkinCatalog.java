package cn.edu.csu.plane.view;

import cn.edu.csu.plane.util.AssetLoader;
import javafx.scene.image.Image;

/**
 * 皮肤目录：集中管理玩家飞机皮肤与子弹皮肤的清单、旋转角度与加载。
 * GameView（游戏内渲染）与 MainMenuView（主界面预览）共用，保证两处朝向一致。
 */
public final class SkinCatalog {

    public static final String[] PLANE_SKINS = {"plane1", "plane2", "plane3", "plane4"};
    public static final String[] BULLET_SKINS = {"bullet1", "bullet2", "bullet3"};

    private SkinCatalog() {
    }

    /** 玩家飞机旋转角度：plane1/2 转 180°，plane4 逆时针 90°，plane3 默认朝上。 */
    public static double planeRotation(String name) {
        return switch (name) {
            case "plane1", "plane2" -> 180;
            case "plane4" -> -90;
            default -> 0;
        };
    }

    /** 玩家子弹旋转角度：bullet1 大头向上，bullet3 箭头向上（逆时针 45°）。 */
    public static double bulletRotation(String name) {
        return switch (name) {
            case "bullet1" -> 90;
            case "bullet3" -> -45;
            default -> 0;
        };
    }

    /** 加载飞机皮肤，等比缩放到目标高度。 */
    public static Image loadPlane(String name, double targetH) {
        return AssetLoader.prepare("plane/" + name + ".png", targetH, planeRotation(name));
    }

    /** 加载子弹皮肤，等比缩放到目标高度。 */
    public static Image loadBullet(String name, double targetH) {
        return AssetLoader.prepare("BulletOfPlane/" + name + ".png", targetH, bulletRotation(name));
    }
}
