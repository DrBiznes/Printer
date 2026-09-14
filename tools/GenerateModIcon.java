import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Deterministic mod icon, built from the same checked-in PNG textures as the README banner.
 *
 * <p>Deliberately spare, so it stays legible where mod icons are shown small: a flat single-colour
 * field and the printer block in 2:1 dimetric, centred and filling the square.
 *
 * <p>Renders at 512px for the CurseForge/Modrinth listing and 128px for {@code logoFile} in the JAR.
 */
public final class GenerateModIcon {
    /** Master render size; smaller outputs are downsampled from this. */
    static final int S = 512;

    /** Sizes to emit, as {pixels, filename}. */
    static final Object[][] OUTPUTS = {{512, "printer_icon_512.png"}, {128, "printer.png"}};

    public static void main(String[] args) throws Exception {
        BufferedImage master = render();

        File iconDir = new File(ArtKit.ROOT, "docs");
        iconDir.mkdirs();
        // 512px marketing icon lives in docs/; the 128px one ships in the JAR root for logoFile.
        File jarRoot = new File(ArtKit.ROOT, "src/main/resources");

        for (Object[] o : OUTPUTS) {
            int size = (Integer) o[0];
            String name = (String) o[1];
            BufferedImage scaled = downsample(master, size);
            File target = new File(size == 128 ? jarRoot : iconDir, name);
            ImageIO.write(scaled, "PNG", target);
            System.out.println("Generated " + target.getAbsolutePath() + " (" + size + "px)");
        }
    }

    static BufferedImage render() throws Exception {
        BufferedImage out = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- Flat single-colour background, full bleed. ---
        g.setColor(new Color(ArtKit.LIGHT));
        g.fillRect(0, 0, S, S);

        // --- Printer block, centred and filling the square with a small even margin. ---
        // The block's bounding box is 2*bs wide and bs + cubeLift(bs) tall; solve for the margin so
        // it is centred on both axes rather than only horizontally.
        double margin = 40, bs = (S - 2 * margin) / 2.0;
        double boxH = bs + ArtKit.cubeLift(bs);
        double bx = S / 2.0, by = (S - boxH) / 2.0;
        ArtKit.isoBoxShaded(g, ArtKit.tex("block/printer_top.png"), ArtKit.tex("block/printer_front.png"),
                ArtKit.tex("block/printer_output.png"), bx, by, bs);

        g.dispose();
        return out;
    }

    /**
     * Box-filter downsample. Averaging whole source blocks keeps the pixel-art edges crisp and the
     * ink splats legible at 128px, where a bilinear scale would smear them into grey.
     */
    static BufferedImage downsample(BufferedImage src, int size) {
        if (size == src.getWidth()) return src;
        int factor = src.getWidth() / size;
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            long a = 0, rr = 0, gg = 0, bb = 0;
            for (int sy = 0; sy < factor; sy++) for (int sx = 0; sx < factor; sx++) {
                int c = src.getRGB(x * factor + sx, y * factor + sy);
                int ca = c >>> 24;
                a += ca; rr += ((c >> 16) & 0xff) * ca; gg += ((c >> 8) & 0xff) * ca; bb += (c & 0xff) * ca;
            }
            int n = factor * factor;
            int outA = (int) (a / n);
            // Un-premultiply so edge texels keep their colour instead of darkening toward black.
            int outR = a == 0 ? 0 : (int) (rr / a), outG = a == 0 ? 0 : (int) (gg / a), outB = a == 0 ? 0 : (int) (bb / a);
            out.setRGB(x, y, (outA << 24) | (outR << 16) | (outG << 8) | outB);
        }
        return out;
    }
}
