import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class IconGen {
    static Shape createStar(double cx, double cy, double rOuter, double rInner, int points) {
        Path2D.Double p = new Path2D.Double();
        double angleStep = Math.PI / points; // half-step for inner points
        double angle = -Math.PI/2; // start at top
        for (int i = 0; i < points*2; i++) {
            double r = (i % 2 == 0) ? rOuter : rInner;
            double x = cx + Math.cos(angle) * r;
            double y = cy + Math.sin(angle) * r;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            angle += angleStep;
        }
        p.closePath();
        return p;
    }

    public static void main(String[] args) throws Exception {
        int size = 512;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Blue background
        Color blue = new Color(0x21, 0x96, 0xF3);
        g.setColor(blue);
        g.fillRect(0, 0, size, size);

        // White crescent (two circles: white big, blue small offset to the right)
        int cx = size/2;
        int cy = size/2;
        int rOuter = 180;
        int rInner = 150;
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - rOuter, cy - rOuter, rOuter*2, rOuter*2));
        // carve with background-colored circle offset to create crescent
        g.setColor(blue);
        g.fill(new Ellipse2D.Double(cx - rInner + 40, cy - rInner, rInner*2, rInner*2));

        // Centered star (white) on top
        g.setColor(Color.WHITE);
        Shape star = createStar(cx, cy, 85, 38, 5);
        g.fill(star);

        g.dispose();
        File out = new File("app_icon_512.png");
        ImageIO.write(img, "PNG", out);
        System.out.println("Wrote: " + out.getAbsolutePath() + " (" + out.length() + " bytes)");
    }
}
