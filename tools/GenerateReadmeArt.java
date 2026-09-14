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
    static final int INK = 0xff283331, SHADOW = 0xff596052, SHELL = 0xffc6c3a0;
    static final int LIGHT = 0xffe8dfba, TEAL = 0xff578f87, CYAN = 0xff6ebbb4;
    static final int MAGENTA = 0xffc66e85, GOLD = 0xffdcb564, WOOD = 0xff776047;
    static final File ROOT = new File(".").getAbsoluteFile();
    static final File TEX = new File(ROOT, "src/main/resources/assets/printer/textures");
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
    }

    static BufferedImage tex(String path) throws Exception { return ImageIO.read(new File(TEX, path)); }

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

    static void splash(Graphics2D g, int x, int y, int size) {
        g.setColor(new Color(0xff171e1c, true));
        int[][] blocks = {{2,3,7,8},{5,1,12,10},{9,3,16,13},{3,8,14,16},{7,10,12,18},{0,8,4,12}};
        for (int[] b : blocks) g.fillRect(x + b[0]*size/18, y + b[1]*size/18, (b[2]-b[0])*size/18, (b[3]-b[1])*size/18);
        for (int[] b : new int[][]{{-2,5,2,2},{16,2,3,3},{18,14,2,2},{4,18,2,2}})
            g.fillRect(x + b[0]*size/18, y + b[1]*size/18, b[2]*size/18, b[3]*size/18);
    }

    static void sprite(BufferedImage out, BufferedImage src, int x, int y, int scale, double angle) {
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int w = src.getWidth()*scale, h = src.getHeight()*scale;
        AffineTransform at = new AffineTransform(); at.translate(x+w/2.0, y+h/2.0); at.rotate(Math.toRadians(angle)); at.translate(-w/2.0, -h/2.0); at.scale(scale, scale);
        g.drawImage(src, at, null); g.dispose();
    }

    static void isoFace(Graphics2D g, BufferedImage src, int ox, int oy, int ux, int uy, int vx, int vy) {
        for (int py=0; py<16; py++) for (int px=0; px<16; px++) {
            int c = src.getRGB(px, py); if ((c >>> 24) == 0) continue;
            int x=ox+px*ux+py*vx, y=oy+px*uy+py*vy;
            Polygon p = new Polygon(new int[]{x,x+ux,x+ux+vx,x+vx}, new int[]{y,y+uy,y+uy+vy,y+vy}, 4);
            g.setColor(new Color(c, true)); g.fillPolygon(p);
        }
    }

    public static void main(String[] args) throws Exception {
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics(); g.setColor(new Color(LIGHT)); g.fillRect(0,0,W,H);
        for (int[] s : new int[][]{{70,95,170},{1340,75,150},{1425,365,120},{40,715,120},{1370,810,145}}) splash(g,s[0],s[1],s[2]);
        for (int[] p : new int[][]{{250,110},{1270,245},{118,485},{1510,540},{1230,920}}) { g.setColor(new Color(INK)); g.fillRect(p[0],p[1],20,20); }
        text(out,"PRINTER",W/2+6,76,INK,10,true); text(out,"PRINTER",W/2,66,TEAL,10,true);
        text(out,"MOD",W/2+4,180,INK,6,true); text(out,"MOD",W/2,170,GOLD,6,true);
        g.setColor(new Color(INK)); g.fillRect(W/2-205,262,410,13); g.setColor(new Color(GOLD)); g.fillRect(W/2-160,262,320,4);
        sprite(out,tex("item/color_cartridge.png"),210,185,8,-13); sprite(out,tex("item/black_cartridge.png"),1235,188,8,12);
        sprite(out,tex("item/image.png"),410,295,7,-9); sprite(out,tex("item/image.png"),1045,300,7,10); sprite(out,tex("item/color_cartridge.png"),720,295,5,5);
        g.setColor(new Color(WOOD)); g.fillRect(56,500,W-112,453); g.setColor(new Color(SHELL)); g.fillRect(68,512,W-136,429);
        g.setColor(new Color(TEAL)); g.fillRect(68,512,W-136,16); g.setColor(new Color(INK)); g.fillRect(68,528,W-136,7); g.setColor(new Color(SHADOW)); g.fillRect(68,931,W-136,10);
        isoFace(g,tex("block/printer_front.png"),155,650,14,0,0,14); isoFace(g,tex("block/printer_output.png"),379,650,10,-6,0,14); isoFace(g,tex("block/printer_top.png"),155,650,14,-7,10,7);
        g.setColor(new Color(SHADOW)); g.fillPolygon(new int[]{138,405,500,235},new int[]{881,881,923,923},4);
        text(out,"PRINT YOUR WORLD",720,605,TEAL,5,false); g.setColor(new Color(INK)); g.fillRect(720,661,350,8);
        text(out,"TURN WEB OR LOCAL IMAGES INTO PRINTS.",720,700,INK,3,false); text(out,"PRINT IN COLOR OR BLACK & WHITE.",720,753,INK,3,false); text(out,"DISPLAY IN FRAMES OR ON YOUR WALLS.",720,806,INK,3,false);
        for (int i=0;i<3;i++) { g.setColor(new Color(INK)); g.fillRect(720+i*42,871,28,24); g.setColor(new Color(new int[]{CYAN,MAGENTA,GOLD}[i])); g.fillRect(726+i*42,877,16,12); }
        text(out,"P-01 / IMAGE PRINTING",720,905,SHADOW,2,false); g.dispose();
        File target = new File(ROOT,"docs/readme-art.png"); target.getParentFile().mkdirs(); ImageIO.write(out,"PNG",target); System.out.println("Generated " + target.getAbsolutePath());
    }
}
