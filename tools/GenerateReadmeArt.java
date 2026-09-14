import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Deterministic README art built from the Printer's checked-in PNG textures. */
public final class GenerateReadmeArt {
    static final int W = 1600, H = 1000;
    static final int INK = ArtKit.INK, SHADOW = ArtKit.SHADOW, SHELL = ArtKit.SHELL;
    static final int LIGHT = ArtKit.LIGHT, TEAL = ArtKit.TEAL, CYAN = ArtKit.CYAN;
    static final int MAGENTA = ArtKit.MAGENTA, GOLD = ArtKit.GOLD, WOOD = ArtKit.WOOD;
    static final File ROOT = ArtKit.ROOT;
    static final Map<Character, String[]> FONT = new HashMap<>();

    static {
        String[] rows = {
                "A:01110,10001,10001,11111,10001,10001,10001", "B:11110,10001,10001,11110,10001,10001,11110",
                "C:01111,10000,10000,10000,10000,10000,01111", "D:11110,10001,10001,10001,10001,10001,11110",
                "E:11111,10000,10000,11110,10000,10000,11111", "F:11111,10000,10000,11110,10000,10000,10000",
                "G:01111,10000,10000,10111,10001,10001,01111", "H:10001,10001,10001,11111,10001,10001,10001",
                "I:11111,00100,00100,00100,00100,00100,11111", "J:00111,00010,00010,00010,10010,10010,01100",
                "K:10001,10010,10100,11000,10100,10010,10001", "L:10000,10000,10000,10000,10000,10000,11111",
                "M:10001,11011,10101,10101,10001,10001,10001", "N:10001,11001,10101,10011,10001,10001,10001",
                "O:01110,10001,10001,10001,10001,10001,01110", "P:11110,10001,10001,11110,10000,10000,10000",
                "Q:01110,10001,10001,10001,10101,10010,01101", "R:11110,10001,10001,11110,10100,10010,10001",
                "S:01111,10000,10000,01110,00001,00001,11110", "T:11111,00100,00100,00100,00100,00100,00100",
                "U:10001,10001,10001,10001,10001,10001,01110", "V:10001,10001,10001,10001,10001,01010,00100",
                "W:10001,10001,10001,10101,10101,11011,10001", "X:10001,10001,01010,00100,01010,10001,10001",
                "Y:10001,10001,01010,00100,00100,00100,00100", "Z:11111,00001,00010,00100,01000,10000,11111"
        };
        for (String row : rows) {
            String[] p = row.split(":");
            FONT.put(p[0].charAt(0), p[1].split(","));
        }
        FONT.put(' ', new String[]{"00000","00000","00000","00000","00000","00000","00000"});
        FONT.put('.', new String[]{"00000","00000","00000","00000","00000","00100","00100"});
        FONT.put('-', new String[]{"00000","00000","00000","11111","00000","00000","00000"});
        FONT.put('/', new String[]{"00001","00010","00100","01000","10000","00000","00000"});
        FONT.put('&', new String[]{"01100","10010","10100","01000","10101","10010","01101"});
        FONT.put('0', new String[]{"01110","10001","10011","10101","11001","10001","01110"});
        FONT.put('1', new String[]{"00100","01100","00100","00100","00100","00100","01110"});
        FONT.put('!', new String[]{"00100","00100","00100","00100","00100","00000","00100"});
    }

    static BufferedImage tex(String path) throws Exception { return ArtKit.tex(path); }

    static void text(BufferedImage im, String value, int x, int y, int color, int scale, boolean center) {
        int advance = 6 * scale, width = value.length() * advance;
        if (center) x -= width / 2;
        Graphics2D g = im.createGraphics();
        g.setColor(new Color(color, true));
        for (int i = 0; i < value.length(); i++) {
            String[] rows = FONT.getOrDefault(value.charAt(i), FONT.get(' '));
            for (int row = 0; row < 7; row++) for (int col = 0; col < 5; col++)
                if (rows[row].charAt(col) == '1') g.fillRect(x + i * advance + col * scale, y + row * scale, scale, scale);
        }
        g.dispose();
    }

    public static void main(String[] args) throws Exception {
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics(); g.setColor(new Color(LIGHT)); g.fillRect(0,0,W,H);
        // {x, y, size, variant, mirrored} - varied silhouettes and scales, none repeating a neighbour.
        for (int[] s : new int[][]{
                {52, 70, 190, 0, 0}, {1330, 40, 168, 3, 0}, {1436, 316, 126, 1, 1},
                {150, 372, 88, 4, 1}, {1180, 120, 92, 7, 0}, {372, 108, 64, 5, 0},
                {928, 396, 70, 4, 0}, {1046, 66, 58, 7, 1}, {258, 258, 62, 1, 0},
                {1364, 418, 54, 5, 1}, {486, 396, 48, 4, 0}, {1268, 372, 44, 7, 0}})
            ArtKit.splash(g, s[0], s[1], s[2], s[3], s[4] == 1);
        for (int[] p : new int[][]{{250,110},{1270,245},{118,485},{1510,540},{1230,920}}) { g.setColor(new Color(INK)); g.fillRect(p[0],p[1],20,20); }
        text(out,"PRINTER",W/2+6,76,INK,10,true); text(out,"PRINTER",W/2,66,TEAL,10,true);
        text(out,"MOD",W/2+4,180,INK,6,true); text(out,"MOD",W/2,170,GOLD,6,true);
        g.setColor(new Color(INK)); g.fillRect(W/2-205,262,410,13); g.setColor(new Color(GOLD)); g.fillRect(W/2-160,262,320,4);
        ArtKit.sprite(out,tex("item/color_cartridge.png"),210,185,8,-13); ArtKit.sprite(out,tex("item/black_cartridge.png"),1235,188,8,12);
        ArtKit.sprite(out,tex("item/image.png"),410,295,7,-9); ArtKit.sprite(out,tex("item/image.png"),1045,300,7,10); ArtKit.sprite(out,tex("item/color_cartridge.png"),720,295,5,5);
        g.setColor(new Color(WOOD)); g.fillRect(56,500,W-112,453); g.setColor(new Color(SHELL)); g.fillRect(68,512,W-136,429);
        g.setColor(new Color(TEAL)); g.fillRect(68,512,W-136,16); g.setColor(new Color(INK)); g.fillRect(68,528,W-136,7); g.setColor(new Color(SHADOW)); g.fillRect(68,931,W-136,10);
        // Printer block in 2:1 dimetric. Apex is the top corner; the shadow is cast from the base corners.
        double bx = 336, by = 556, bs = 156;
        ArtKit.isoShadow(g, bx, by, bs, 0x33000000);
        ArtKit.isoBoxShaded(g, tex("block/printer_top.png"), tex("block/printer_front.png"),
                tex("block/printer_output.png"), bx, by, bs);
        text(out,"PRINT OUT IMAGES!",720,605,TEAL,5,false); g.setColor(new Color(INK)); g.fillRect(720,661,350,8);
        text(out,"TURN WEB OR LOCAL IMAGES INTO PRINTS.",720,700,INK,3,false); text(out,"PRINT IN COLOR OR BLACK & WHITE.",720,753,INK,3,false); text(out,"DISPLAY IN FRAMES OR ON YOUR WALLS.",720,806,INK,3,false);
        for (int i=0;i<3;i++) { g.setColor(new Color(INK)); g.fillRect(720+i*42,871,28,24); g.setColor(new Color(new int[]{CYAN,MAGENTA,GOLD}[i])); g.fillRect(726+i*42,877,16,12); }
        text(out,"P-01 / IMAGE PRINTING",720,905,SHADOW,2,false); g.dispose();
        File target = new File(ROOT,"docs/readme-art.png"); target.getParentFile().mkdirs(); ImageIO.write(out,"PNG",target); System.out.println("Generated " + target.getAbsolutePath());
    }
}
