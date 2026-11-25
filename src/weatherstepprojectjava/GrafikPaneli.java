package weatherstepprojectjava;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Harici kütüphane kullanmadan iki grafik çizer:
 *  1) Hava durumuna göre ortalama adım (bar grafik)
 *  2) Günlük adım sayısı ve sıcaklık (çizgi grafik)
 */
public class GrafikPaneli extends JPanel {

    private static final Color GUNESLI = new Color(0xF2A541);
    private static final Color BULUTLU = new Color(0x8A9BA8);
    private static final Color YAGMURLU = new Color(0x3C78D8);
    private static final Color SICAKLIK = new Color(0xD9534F);
    private static final Color IZGARA = new Color(0xE6E6E6);

    private final List<GunlukKayit> kayitlar;

    public GrafikPaneli(List<GunlukKayit> kayitlar) {
        this.kayitlar = kayitlar;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1000, 440));
    }

    static Color renk(String hava) {
        switch (hava) {
            case "Güneşli":  return GUNESLI;
            case "Yağmurlu": return YAGMURLU;
            default:         return BULUTLU;
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (kayitlar.isEmpty()) {
            g.drawString("Grafik için veri yok.", 20, 30);
            return;
        }
        int w = getWidth();
        int h = getHeight();
        int sol = (int) (w * 0.36);
        barGrafik(g, new Rectangle(10, 10, sol - 20, h - 20));
        cizgiGrafik(g, new Rectangle(sol + 10, 10, w - sol - 20, h - 20));
    }

    private void baslik(Graphics2D g, Rectangle a, String metin) {
        g.setColor(Color.DARK_GRAY);
        g.setFont(getFont().deriveFont(Font.BOLD, 14f));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(metin, a.x + (a.width - fm.stringWidth(metin)) / 2, a.y + 18);
        g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
    }

    private void barGrafik(Graphics2D g, Rectangle a) {
        baslik(g, a, "Hava Durumuna Göre Ortalama Adım");
        Map<String, Double> ort = Analiz.havaDurumunaGoreOrtalama(kayitlar);

        int ust = a.y + 40, alt = a.y + a.height - 30, solX = a.x + 55, sagX = a.x + a.width - 10;
        double max = yuvarlaYukari(ort.values().stream().mapToDouble(Double::doubleValue).max().orElse(1));

        izgara(g, solX, sagX, ust, alt, max, "");

        int n = ort.size();
        int bosluk = (sagX - solX) / n;
        int barG = (int) (bosluk * 0.55);
        int i = 0;
        FontMetrics fm = g.getFontMetrics();
        for (Map.Entry<String, Double> e : ort.entrySet()) {
            int x = solX + i * bosluk + (bosluk - barG) / 2;
            int y = (int) (alt - (alt - ust) * e.getValue() / max);
            g.setColor(renk(e.getKey()));
            g.fillRoundRect(x, y, barG, alt - y, 6, 6);
            g.fillRect(x, alt - 4, barG, 4);

            g.setColor(Color.DARK_GRAY);
            String deger = String.format("%,.0f", e.getValue());
            g.drawString(deger, x + (barG - fm.stringWidth(deger)) / 2, y - 5);
            g.drawString(e.getKey(), x + (barG - fm.stringWidth(e.getKey())) / 2, alt + 16);
            i++;
        }
    }

    private void cizgiGrafik(Graphics2D g, Rectangle a) {
        baslik(g, a, "Günlük Adım Sayısı ve Sıcaklık");

        int ust = a.y + 40, alt = a.y + a.height - 50, solX = a.x + 55, sagX = a.x + a.width - 45;
        int n = kayitlar.size();
        double maxAdim = yuvarlaYukari(kayitlar.stream().mapToInt(k -> k.adimSayisi).max().orElse(1));
        // Sıcaklık ekseni: 4 eşit aralık, tam sayı değerler (örn. 0, 6, 12, 18, 24)
        double minSic = Math.min(0, Math.floor(kayitlar.stream().mapToDouble(k -> k.sicaklik).min().orElse(0)));
        double enYuksek = Math.ceil(kayitlar.stream().mapToDouble(k -> k.sicaklik).max().orElse(1));
        double sicAdim = Math.max(1, Math.ceil((enYuksek - minSic) / 4));
        double maxSic = minSic + 4 * sicAdim;

        izgara(g, solX, sagX, ust, alt, maxAdim, "");

        // Sağ eksen: sıcaklık
        g.setColor(SICAKLIK);
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i <= 4; i++) {
            double v = minSic + (maxSic - minSic) * i / 4;
            int y = (int) (alt - (alt - ust) * i / 4.0);
            g.drawString(String.format("%.0f°C", v), sagX + 6, y + 4);
        }

        int[] xs = new int[n], ysAdim = new int[n], ysSic = new int[n];
        for (int i = 0; i < n; i++) {
            GunlukKayit k = kayitlar.get(i);
            xs[i] = n == 1 ? (solX + sagX) / 2 : solX + 15 + (sagX - solX - 30) * i / (n - 1);
            ysAdim[i] = (int) (alt - (alt - ust) * k.adimSayisi / maxAdim);
            ysSic[i] = (int) (alt - (alt - ust) * (k.sicaklik - minSic) / (maxSic - minSic));
        }

        // Adım çizgisi + hava durumuna göre renkli noktalar
        g.setStroke(new BasicStroke(2f));
        g.setColor(Color.GRAY);
        g.drawPolyline(xs, ysAdim, n);
        for (int i = 0; i < n; i++) {
            g.setColor(renk(kayitlar.get(i).havaDurumu));
            g.fillOval(xs[i] - 6, ysAdim[i] - 6, 12, 12);
        }

        // Sıcaklık çizgisi (kesikli)
        g.setColor(SICAKLIK);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 5f}, 0f));
        g.drawPolyline(xs, ysSic, n);
        g.setStroke(new BasicStroke(1f));

        // X ekseni: tarihler
        g.setColor(Color.DARK_GRAY);
        int adim = Math.max(1, n / 7);
        for (int i = 0; i < n; i += adim) {
            String t = kayitlar.get(i).tarih.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"));
            g.drawString(t, xs[i] - fm.stringWidth(t) / 2, alt + 16);
        }

        // Açıklama
        int ly = a.y + a.height - 12, lx = solX;
        for (String tur : GunlukKayit.HAVA_TURLERI) {
            g.setColor(renk(tur));
            g.fillOval(lx, ly - 9, 10, 10);
            g.setColor(Color.DARK_GRAY);
            g.drawString(tur, lx + 14, ly);
            lx += fm.stringWidth(tur) + 34;
        }
        g.setColor(SICAKLIK);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 5f}, 0f));
        g.drawLine(lx, ly - 4, lx + 22, ly - 4);
        g.setStroke(new BasicStroke(1f));
        g.drawString("Sıcaklık", lx + 28, ly);
    }

    private void izgara(Graphics2D g, int solX, int sagX, int ust, int alt, double max, String birim) {
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i <= 4; i++) {
            int y = (int) (alt - (alt - ust) * i / 4.0);
            g.setColor(IZGARA);
            g.drawLine(solX, y, sagX, y);
            g.setColor(Color.GRAY);
            String etiket = String.format("%,.0f%s", max * i / 4, birim);
            g.drawString(etiket, solX - 6 - fm.stringWidth(etiket), y + 4);
        }
    }

    /** Eksen üst sınırını okunaklı bir sayıya yuvarlar (örn. 13.500 -> 15.000). */
    private static double yuvarlaYukari(double v) {
        if (v <= 0) {
            return 1;
        }
        double us = Math.pow(10, Math.floor(Math.log10(v)));
        double[] adimlar = {1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10};
        for (double a : adimlar) {
            if (a * us >= v) {
                return a * us;
            }
        }
        return 10 * us;
    }
}
