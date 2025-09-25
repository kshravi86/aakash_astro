import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class FeatureGen {
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
        final int W = 1024, H = 500;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background gradient (blue tones)
        Color c1 = new Color(0x21,0x96,0xF3); // blue 500
        Color c2 = new Color(0x0D,0x47,0xA1); // darker
        GradientPaint gp = new GradientPaint(0,0,c1,0,H,c2);
        g.setPaint(gp);
        g.fillRect(0,0,W,H);

        // Subtle star dots
        g.setColor(new Color(255,255,255,60));
        for (int i=0; i<120; i++) {
            int x = (int)(Math.random()*W);
            int y = (int)(Math.random()*H);
            int r = 1 + (int)(Math.random()*2);
            g.fill(new Ellipse2D.Double(x, y, r, r));
        }

        // Centered shatkona symbol on left-half
        double cx = W*0.32, cy = H*0.52;
        double r = H*0.30;
        Shape up = equilateral(cx, cy, r, -90);
        Shape down = equilateral(cx, cy, r, 90);
        g.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(up);
        g.draw(down);
        // Inner circle for balance
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(cx - r*0.68, cy - r*0.68, r*1.36, r*1.36));

        // App name text on right-half
        g.setColor(Color.WHITE);
        // Try preferred fonts, fallback to default
        Font base = new Font("Segoe UI", Font.BOLD, 76);
        if (base.getFamily()==null) base = g.getFont().deriveFont(Font.BOLD, 76f);
        g.setFont(base);
        String title = "Vedic Light";
        FontMetrics fm = g.getFontMetrics();
        int tx = (int)(W*0.54);
        int ty = (int)(H*0.45);
        // subtle shadow
        g.setColor(new Color(0,0,0,80));
        g.drawString(title, tx+3, ty+3);
        g.setColor(Color.WHITE);
        g.drawString(title, tx, ty);

        // Subtitle
        Font sub = base.deriveFont(Font.PLAIN, 28f);
        g.setFont(sub);
        g.setColor(new Color(255,255,255,220));
        String subline = "Sidereal charts, periods, lunar mansions";
        g.drawString(subline, tx, ty + 48);
        
        g.dispose();
        File out = new File("feature_graphic_1024x500.png");
        ImageIO.write(img, "PNG", out);
        System.out.println("Wrote: " + out.getAbsolutePath() + " (" + out.length() + " bytes)");
    }
}
