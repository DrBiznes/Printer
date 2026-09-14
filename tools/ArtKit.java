import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Shared drawing primitives for the Printer's programmatic art (README banner and mod icon),
 * so both are built from the same checked-in PNG textures with identical geometry and palette.
 */
public final class ArtKit {
    private ArtKit() {}

    public static final int INK = 0xff283331, SHADOW = 0xff596052, SHELL = 0xffc6c3a0;
    public static final int LIGHT = 0xffe8dfba, TEAL = 0xff578f87, CYAN = 0xff6ebbb4;
    public static final int MAGENTA = 0xffc66e85, GOLD = 0xffdcb564, WOOD = 0xff776047;

    public static final File ROOT = new File(".").getAbsoluteFile();
    public static final File TEX = new File(ROOT, "src/main/resources/assets/printer/textures");

    public static BufferedImage tex(String path) throws Exception { return ImageIO.read(new File(TEX, path)); }

    /**
     * Pixel-art ink splats on a 20x20 design grid. Each variant is {body rects..., separator, spatter flecks...}
     * where body entries are {x0,y0,x1,y1} and spatter entries are {x,y,w,h}. Variants differ in silhouette
     * (round, oblong, streaked, burst, misted, ...) so repeated splats never read as copies.
     */
    public static final int[][][] SPLATS = {
            // 0: round blot, heavy centre, small drip tail
            {{5,3,14,6},{3,5,16,13},{5,12,15,16},{7,15,12,17},{9,17,11,19},
             {-1,-1,-1,-1},{1,7,2,2},{17,4,2,2},{18,11,2,3},{4,18,2,2},{13,19,2,1}},
            // 1: wide oblong smear, weighted left
            {{2,6,17,8},{1,7,19,12},{3,11,16,14},{5,13,12,15},
             {-1,-1,-1,-1},{0,4,3,2},{19,9,1,3},{8,15,3,2},{15,15,2,2},{2,16,2,1}},
            // 2: tall narrow drip with long runoff
            {{6,1,13,4},{4,3,15,10},{6,9,13,14},{8,13,12,18},{9,17,11,20},
             {-1,-1,-1,-1},{3,1,2,2},{16,5,2,3},{2,8,2,2},{14,15,2,2},{12,19,2,1}},
            // 3: burst / starburst with radiating arms
            {{7,6,13,13},{5,8,15,11},{8,3,12,16},{4,5,7,8},{13,11,17,15},
             {-1,-1,-1,-1},{1,2,3,3},{17,1,2,2},{18,8,2,2},{0,13,2,3},{15,18,3,2},{6,18,2,2},{10,19,2,1}},
            // 4: small compact dot, tight spatter halo
            {{6,5,14,7},{5,6,15,13},{7,12,13,15},
             {-1,-1,-1,-1},{3,3,2,2},{16,4,2,2},{2,10,2,2},{17,11,2,2},{9,16,2,2},{5,15,1,2}},
            // 5: diagonal streak, flung upper-left to lower-right
            {{2,2,7,6},{4,4,11,9},{8,7,15,13},{12,11,18,16},{15,14,19,18},
             {-1,-1,-1,-1},{0,0,2,2},{9,3,2,2},{6,11,2,2},{17,9,2,2},{13,18,2,2},{19,19,1,1}},
            // 6: lopsided kidney blot with a pinched waist
            {{4,4,10,8},{2,6,12,11},{6,10,17,14},{9,13,16,17},
             {-1,-1,-1,-1},{12,2,3,2},{0,9,2,2},{18,6,2,3},{4,14,2,2},{17,17,2,2},{7,18,2,1}},
            // 7: fine speckled mist, no solid core
            {{6,7,10,10},{11,9,14,12},{8,12,12,14},
             {-1,-1,-1,-1},{2,2,2,2},{13,3,2,2},{17,6,2,2},{3,7,2,2},{15,14,2,2},{5,15,2,2},
             {10,17,2,2},{18,16,2,2},{0,12,2,2},{8,1,2,2}}
    };

    /** Draw splat variant {@code id} with its top-left at (x,y), fitted to {@code size} px, optionally mirrored. */
    public static void splash(Graphics2D g, int x, int y, int size, int id, boolean flip) {
        splash(g, x, y, size, id, flip, 0xff171e1c);
    }

    /** As above, in an explicit colour. */
    public static void splash(Graphics2D g, int x, int y, int size, int id, boolean flip, int color) {
        g.setColor(new Color(color, true));
        int[][] parts = SPLATS[Math.floorMod(id, SPLATS.length)];
        boolean spatter = false;
        for (int[] b : parts) {
            if (b[0] == -1) { spatter = true; continue; }
            int bx = b[0], by = b[1], bw = spatter ? b[2] : b[2] - b[0], bh = spatter ? b[3] : b[3] - b[1];
            if (flip) bx = 20 - bx - bw;
            g.fillRect(x + bx * size / 20, y + by * size / 20,
                    Math.max(1, bw * size / 20), Math.max(1, bh * size / 20));
        }
    }

    /** Draw a texture as flat pixel art at (x,y), scaled up with nearest-neighbour and rotated by {@code angle}. */
    public static void sprite(BufferedImage out, BufferedImage src, int x, int y, int scale, double angle) {
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int w = src.getWidth() * scale, h = src.getHeight() * scale;
        AffineTransform at = new AffineTransform();
        at.translate(x + w / 2.0, y + h / 2.0);
        at.rotate(Math.toRadians(angle));
        at.translate(-w / 2.0, -h / 2.0);
        at.scale(scale, scale);
        g.drawImage(src, at, null);
        g.dispose();
    }

    /**
     * Rasterise a 16x16 texture onto a parallelogram anchored at (ox,oy), where {@code u} is the per-texel
     * step across the texture's X axis and {@code v} the step down its Y axis. Texels are drawn as filled
     * parallelograms, overdrawn slightly so the three faces of a box tile without seams.
     */
    public static void isoFace(Graphics2D g, BufferedImage src, double ox, double oy,
                               double ux, double uy, double vx, double vy) {
        for (int py = 0; py < 16; py++) for (int px = 0; px < 16; px++) {
            int c = src.getRGB(px, py); if ((c >>> 24) == 0) continue;
            double x = ox + px * ux + py * vx, y = oy + px * uy + py * vy;
            double eu = 1.02, ev = 1.02;
            Polygon p = new Polygon(
                    new int[]{(int) Math.round(x), (int) Math.round(x + ux * eu),
                              (int) Math.round(x + ux * eu + vx * ev), (int) Math.round(x + vx * ev)},
                    new int[]{(int) Math.round(y), (int) Math.round(y + uy * eu),
                              (int) Math.round(y + uy * eu + vy * ev), (int) Math.round(y + vy * ev)}, 4);
            g.setColor(new Color(c, true));
            g.fillPolygon(p);
        }
    }

    /** Multiply an ARGB colour's RGB channels, for face shading. */
    public static Color shade(int argb, double k) {
        return new Color(Math.min(255, (int) (((argb >> 16) & 0xff) * k)),
                Math.min(255, (int) (((argb >> 8) & 0xff) * k)),
                Math.min(255, (int) ((argb & 0xff) * k)));
    }

    /** Return a copy of {@code src} with every RGB channel multiplied by {@code k}. */
    public static BufferedImage shadeImage(BufferedImage src, double k) {
        BufferedImage im = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
            int c = src.getRGB(x, y);
            im.setRGB(x, y, (c & 0xff000000) | (shade(c, k).getRGB() & 0x00ffffff));
        }
        return im;
    }

    /**
     * Wall height that makes {@link #isoBox} draw a true cube rather than a squashed slab.
     *
     * <p>A top-face edge runs {@code s} across and {@code s/2} down the screen, so its on-screen length
     * is {@code s*sqrt(1.25)}. The vertical walls must match that for the block to read as a cube; any
     * smaller value squashes it (at {@code lift == s} the block sits at ~0.89 of its proper height).
     */
    public static double cubeLift(double s) { return s * Math.sqrt(1.25); }

    /**
     * Draw a cube in a true 2:1 dimetric projection, proportioned by {@link #cubeLift}.
     *
     * @see #isoBox(Graphics2D, BufferedImage, BufferedImage, BufferedImage, double, double, double, double)
     */
    public static void isoBox(Graphics2D g, BufferedImage top, BufferedImage left, BufferedImage right,
                              double cx, double cy, double s) {
        isoBox(g, top, left, right, cx, cy, s, cubeLift(s));
    }

    /**
     * Draw a box in a true 2:1 dimetric projection. (cx,cy) is the apex - the topmost corner of the top
     * face. {@code s} is the horizontal half-width of a face and {@code lift} the height of the vertical
     * walls; pass {@link #cubeLift}({@code s}) for a cube. The top rhombus runs apex -> right corner
     * (cx+s, cy+s/2) -> bottom corner (cx, cy+s) -> left corner (cx-s, cy+s/2); the two walls hang from
     * its lower edges, sharing those exact corners.
     */
    public static void isoBox(Graphics2D g, BufferedImage top, BufferedImage left, BufferedImage right,
                              double cx, double cy, double s, double lift) {
        double ux = s / 16.0, uy = s / 32.0;   // one texel step down-right
        double vx = -s / 16.0, vy = s / 32.0;  // one texel step down-left
        double dy = lift / 16.0;               // one texel step straight down a wall

        isoFace(g, top, cx, cy, ux, uy, vx, vy);
        // Left wall: hangs from the left corner, running down-right to the bottom corner.
        isoFace(g, left, cx - s, cy + s / 2.0, ux, uy, 0, dy);
        // Right wall: hangs from the bottom corner, running up-right to the right corner.
        isoFace(g, right, cx, cy + s, ux, -uy, 0, dy);
    }

    /** As {@link #isoBox}, but darkens each wall so the three planes separate under a single light. */
    public static void isoBoxShaded(Graphics2D g, BufferedImage top, BufferedImage left, BufferedImage right,
                                    double cx, double cy, double s, double lift) {
        isoBox(g, top, shadeImage(left, 0.82), shadeImage(right, 0.66), cx, cy, s, lift);
    }

    /** Cube-proportioned {@link #isoBoxShaded}. */
    public static void isoBoxShaded(Graphics2D g, BufferedImage top, BufferedImage left, BufferedImage right,
                                    double cx, double cy, double s) {
        isoBoxShaded(g, top, left, right, cx, cy, s, cubeLift(s));
    }

    /** Cube-proportioned {@link #isoShadow}. */
    public static void isoShadow(Graphics2D g, double cx, double cy, double s, int color) {
        isoShadow(g, cx, cy, s, cubeLift(s), color);
    }

    /** Flat floor shadow for a box drawn by {@link #isoBox} with the same parameters. */
    public static void isoShadow(Graphics2D g, double cx, double cy, double s, double lift, int color) {
        double sy = cy + lift, sp = s / 9.0;
        g.setColor(new Color(color, true));
        g.fillPolygon(new int[]{(int) (cx - s - sp), (int) cx, (int) (cx + s + sp), (int) cx},
                new int[]{(int) (sy + s / 2), (int) (sy - sp / 2), (int) (sy + s / 2), (int) (sy + s + sp / 2)}, 4);
    }
}
