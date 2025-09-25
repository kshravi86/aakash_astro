import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class TabletShots {
    static String[][] PAGES = new String[][]{
        {"Accurate Sidereal Charts", "Swiss Ephemeris • Lahiri • On‑device"},
        {"South Indian Chart", "Fixed signs • Pisces top left • Lagna"},
        {"Planet Positions", "Degrees • Degrees in sign • Retrograde (R)"},
        {"Lunar Mansions", "Nakshatra and quarter for each body"},
        {"Vimshottari Dasha", "120‑year cycle • Tap to expand sub‑periods"},
        {"Antar & Pratyantar", "Expand on demand • Clear timelines"},
        {"Time & Place", "AM/PM time picker • Offline city list"},
        {"Private & Offline", "No accounts • No tracking • No internet"},
    };

    static Shape equilateral(double cx, double cy, double radius, double angleDeg){
        double a = Math.toRadians(angleDeg);
        Path2D.Double p = new Path2D.Double();
        for (int i=0;i<3;i++){
            double ang = a + i*2*Math.PI/3.0;
            double x = cx + Math.cos(ang)*radius;
            double y = cy + Math.sin(ang)*radius;
            if(i==0) p.moveTo(x,y); else p.lineTo(x,y);
        }
        p.closePath();
        return p;
    }

    static void drawShatkona(Graphics2D g, double cx, double cy, double r){
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.draw(equilateral(cx, cy, r, -90));
        g.draw(equilateral(cx, cy, r, 90));
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(cx - r*0.68, cy - r*0.68, r*1.36, r*1.36));
        g.setStroke(old);
    }

    static void render(int idx, String title, String subtitle) throws Exception {
        final int W=1920, H=1080; // 16:9
        BufferedImage img = new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Gradient background
        Color c1 = new Color(0x21,0x96,0xF3);
        Color c2 = new Color(0x0D,0x47,0xA1);
        GradientPaint gp = new GradientPaint(0,0,c1,0,H,c2);
        g.setPaint(gp);
        g.fillRect(0,0,W,H);

        // Decorative stars
        g.setColor(new Color(255,255,255,70));
        for(int i=0;i<180;i++){
            int x=(int)(Math.random()*W);
            int y=(int)(Math.random()*H);
            int r=1+(int)(Math.random()*2);
            g.fill(new Ellipse2D.Double(x,y,r,r));
        }

        // Left graphic: shatkona
        drawShatkona(g, W*0.25, H*0.55, H*0.26);

        // App name and page text
        g.setColor(Color.WHITE);
        Font titleFont = new Font("SansSerif", Font.BOLD, 88);
        Font h2 = new Font("SansSerif", Font.BOLD, 52);
        Font body = new Font("SansSerif", Font.PLAIN, 34);

        g.setFont(h2);
        // App name
        String app = "Vedic Light";
        // subtle shadow
        g.setColor(new Color(0,0,0,90)); g.drawString(app, (int)(W*0.45)+3, (int)(H*0.34)+3);
        g.setColor(Color.WHITE); g.drawString(app, (int)(W*0.45), (int)(H*0.34));

        // Page title
        g.setFont(titleFont);
        int ty = (int)(H*0.50);
        g.setColor(new Color(0,0,0,90)); g.drawString(title, (int)(W*0.45)+4, ty+4);
        g.setColor(Color.WHITE); g.drawString(title, (int)(W*0.45), ty);

        // Subtitle (wrap naive)
        g.setFont(body);
        g.setColor(new Color(255,255,255,230));
        int x = (int)(W*0.45), y = ty+60, max = (int)(W*0.48);
        for(String line : wrap(subtitle, g.getFontMetrics(), max)){
            g.drawString(line, x, y);
            y += 42;
        }

        g.dispose();
        File out = new File(String.format("tablet_ss_%02d.png", idx+1));
        ImageIO.write(img, "PNG", out);
        System.out.println("Wrote: "+out.getName()+" ("+out.length()+" bytes)");
    }

    static java.util.List<String> wrap(String text, FontMetrics fm, int maxWidth){
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] parts = text.split(" ");
        StringBuilder line = new StringBuilder();
        for(String w: parts){
            String test = line.length()==0 ? w : line+" "+w;
            if(fm.stringWidth(test) <= maxWidth){
                line = new StringBuilder(test);
            } else {
                if(line.length()>0) lines.add(line.toString());
                line = new StringBuilder(w);
            }
        }
        if(line.length()>0) lines.add(line.toString());
        return lines;
    }

    public static void main(String[] args) throws Exception {
        for (int i=0;i<PAGES.length;i++){
            render(i, PAGES[i][0], PAGES[i][1]);
        }
    }
}
