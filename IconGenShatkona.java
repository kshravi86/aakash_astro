import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class IconGenShatkona {
    static Shape equilateral(double cx, double cy, double radius, double angleDeg) {
        double a = Math.toRadians(angleDeg);
        double a1 = a;
        double a2 = a + 2*Math.PI/3;
        double a3 = a + 4*Math.PI/3;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(cx + Math.cos(a1)*radius, cy + Math.sin(a1)*radius);
        p.lineTo(cx + Math.cos(a2)*radius, cy + Math.sin(a2)*radius);
        p.lineTo(cx + Math.cos(a3)*radius, cy + Math.sin(a3)*radius);
        p.closePath();
        return p;
    }

    public static void main(String[] args) throws Exception {
        final int SIZE = 512;
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background: Blue (Material Blue 500)
        Color blue = new Color(0x21, 0x96, 0xF3);
        g.setColor(blue);
        g.fillRect(0, 0, SIZE, SIZE);

        // Shatkona (two interlocking equilateral triangles), white strokes
        double cx = SIZE/2.0, cy = SIZE/2.0;
        double r = 170; // outer radius
        Shape up = equilateral(cx, cy, r, -90);     // pointing up
        Shape down = equilateral(cx, cy, r, 90);    // pointing down

        BasicStroke stroke = new BasicStroke(22f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g.setStroke(stroke);
        g.setColor(Color.WHITE);
        g.draw(up);
        g.draw(down);

        // Optional inner circle to balance (thin)
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(cx-115, cy-115, 230, 230));

        g.dispose();
        File out = new File("app_icon_512_shatkona.png");
        ImageIO.write(img, "PNG", out);
        System.out.println("Wrote: " + out.getAbsolutePath() + " (" + out.length() + " bytes)");
    }
}
