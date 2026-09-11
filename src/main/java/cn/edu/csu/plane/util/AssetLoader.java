package cn.edu.csu.plane.util;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 图片资源加载器：从 resource/pictures 下按相对路径加载贴图并缓存。
 * 支持"旋转 + 按目标高度等比缩放"的预处理，供视图直接绘制。
 *
 * <p>图片在项目根目录的 resource/pictures（不在 src/main/resources），
 * 因此用文件路径加载，保证 IntelliJ 与 mvn javafx:run（工作目录均为项目根）都能找到。</p>
 */
public final class AssetLoader {

    private static final String BASE = "resource" + File.separator + "pictures" + File.separator;

    private static final Map<String, Image> RAW_CACHE = new HashMap<>();
    private static final Map<String, Image> PREPARED_CACHE = new HashMap<>();

    private AssetLoader() {
    }

    /** 加载并缓存原始贴图；文件不存在时返回 null。 */
    public static Image load(String relativePath) {
        return RAW_CACHE.computeIfAbsent(relativePath, path -> {
            File file = new File(BASE + path);
            if (!file.exists()) {
                System.err.println("找不到贴图：" + file.getAbsolutePath());
                return null;
            }
            return new Image(file.toURI().toString());
        });
    }

    /**
     * 预处理贴图：旋转 {@code rotation} 度后，等比缩放到高度 {@code targetH}。
     * 宽度按旋转后的宽高比自动确定。结果缓存，避免每帧重复处理。
     */
    public static Image prepare(String relativePath, double targetH, double rotation) {
        String key = relativePath + "@" + targetH + "@" + rotation;
        return PREPARED_CACHE.computeIfAbsent(key, k -> doPrepare(relativePath, targetH, rotation));
    }

    private static Image doPrepare(String relativePath, double targetH, double rotation) {
        Image raw = load(relativePath);
        if (raw == null) {
            return null;
        }
        double rw = raw.getWidth();
        double rh = raw.getHeight();

        // 旋转 90/270 度后宽高互换
        boolean swap = Math.abs(rotation) % 180 == 90;
        double logicalW = swap ? rh : rw;
        double logicalH = swap ? rw : rh;

        double scale = targetH / logicalH;
        double w = logicalW * scale;
        double h = targetH;

        Canvas canvas = new Canvas(w, h);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.translate(w / 2, h / 2);
        gc.rotate(rotation);
        // 保持原始宽高比绘制，旋转后恰好填满 (w, h)
        gc.drawImage(raw, -rw * scale / 2, -rh * scale / 2, rw * scale, rh * scale);
        gc.restore();

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, null);
    }
}
