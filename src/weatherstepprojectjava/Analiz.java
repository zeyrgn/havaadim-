package weatherstepprojectjava;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * İstatistiksel analizler: ortalamalar, hipotez testi ve korelasyon.
 */
public class Analiz {

    // Hipotez: 15-25°C arası güneşli günler, 10°C altı yağmurlu günlere göre
    // en az %30 daha fazla adım içerir.
    static final double HIPOTEZ_MIN_SICAKLIK = 15;
    static final double HIPOTEZ_MAX_SICAKLIK = 25;
    static final double SOGUK_SINIR = 10;
    static final double BEKLENEN_FARK_YUZDE = 30;

    /** Hava durumu türüne göre ortalama adım sayısı (veri yoksa o tür eklenmez). */
    static Map<String, Double> havaDurumunaGoreOrtalama(List<GunlukKayit> kayitlar) {
        Map<String, Double> sonuc = new LinkedHashMap<>();
        for (String tur : GunlukKayit.HAVA_TURLERI) {
            List<GunlukKayit> grup = new ArrayList<>();
            for (GunlukKayit k : kayitlar) {
                if (k.havaDurumu.equals(tur)) {
                    grup.add(k);
                }
            }
            if (!grup.isEmpty()) {
                sonuc.put(tur, ortalamaAdim(grup));
            }
        }
        return sonuc;
    }

    static double ortalamaAdim(List<GunlukKayit> kayitlar) {
        double toplam = 0;
        for (GunlukKayit k : kayitlar) {
            toplam += k.adimSayisi;
        }
        return toplam / kayitlar.size();
    }

    /** Pearson korelasyon katsayısı. Hesaplanamıyorsa NaN döner. */
    static double korelasyon(double[] x, double[] y) {
        int n = x.length;
        if (n < 2) {
            return Double.NaN;
        }
        double ortX = 0, ortY = 0;
        for (int i = 0; i < n; i++) {
            ortX += x[i];
            ortY += y[i];
        }
        ortX /= n;
        ortY /= n;

        double pay = 0, paydaX = 0, paydaY = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - ortX;
            double dy = y[i] - ortY;
            pay += dx * dy;
            paydaX += dx * dx;
            paydaY += dy * dy;
        }
        if (paydaX == 0 || paydaY == 0) {
            return Double.NaN;
        }
        return pay / Math.sqrt(paydaX * paydaY);
    }

    /** Tüm analizleri içeren metin raporu üretir. */
    static String raporOlustur(List<GunlukKayit> kayitlar) {
        if (kayitlar.isEmpty()) {
            return "Analiz için yeterli veri yok.";
        }

        StringBuilder r = new StringBuilder();
        r.append("--- ANALİZ SONUCU ---\n");
        r.append(String.format("Toplam gün: %d   |   Genel ortalama: %,.0f adım%n%n",
                kayitlar.size(), ortalamaAdim(kayitlar)));

        // 1) Hava durumuna göre ortalamalar
        r.append("Hava durumuna göre ortalama adım:\n");
        for (Map.Entry<String, Double> e : havaDurumunaGoreOrtalama(kayitlar).entrySet()) {
            r.append(String.format("  %-9s: %,8.0f adım%n", e.getKey(), e.getValue()));
        }

        // 2) Hipotez testi
        List<GunlukKayit> guzelGunler = new ArrayList<>();
        List<GunlukKayit> kotuGunler = new ArrayList<>();
        for (GunlukKayit k : kayitlar) {
            if (k.havaDurumu.equals("Güneşli")
                    && k.sicaklik >= HIPOTEZ_MIN_SICAKLIK && k.sicaklik <= HIPOTEZ_MAX_SICAKLIK) {
                guzelGunler.add(k);
            } else if (k.havaDurumu.equals("Yağmurlu") && k.sicaklik < SOGUK_SINIR) {
                kotuGunler.add(k);
            }
        }

        r.append("\nHipotez testi:\n");
        if (guzelGunler.isEmpty() || kotuGunler.isEmpty()) {
            r.append("  Yeterli veri yok (iki grupta da en az 1 gün gerekli).\n");
        } else {
            double ortGuzel = ortalamaAdim(guzelGunler);
            double ortKotu = ortalamaAdim(kotuGunler);
            double fark = (ortGuzel - ortKotu) / ortKotu * 100;
            r.append(String.format("  Güneşli & 15-25°C  (%d gün): %,.0f adım%n", guzelGunler.size(), ortGuzel));
            r.append(String.format("  Yağmurlu & <10°C   (%d gün): %,.0f adım%n", kotuGunler.size(), ortKotu));
            r.append(String.format("  Fark: %%%.1f  ->  ", fark));
            r.append(fark >= BEKLENEN_FARK_YUZDE
                    ? "HİPOTEZ DOĞRULANDI (≥ %30)\n"
                    : "HİPOTEZ DOĞRULANMADI (< %30)\n");
        }

        // 3) Korelasyonlar
        int n = kayitlar.size();
        double[] sicaklik = new double[n];
        double[] motivasyon = new double[n];
        double[] adim = new double[n];
        for (int i = 0; i < n; i++) {
            sicaklik[i] = kayitlar.get(i).sicaklik;
            motivasyon[i] = kayitlar.get(i).motivasyon;
            adim[i] = kayitlar.get(i).adimSayisi;
        }
        r.append("\nKorelasyon (Pearson r):\n");
        r.append("  Sıcaklık  - Adım: ").append(korelasyonMetni(korelasyon(sicaklik, adim))).append("\n");
        r.append("  Motivasyon- Adım: ").append(korelasyonMetni(korelasyon(motivasyon, adim))).append("\n");

        return r.toString();
    }

    private static String korelasyonMetni(double r) {
        if (Double.isNaN(r)) {
            return "hesaplanamadı";
        }
        double mutlak = Math.abs(r);
        String guc = mutlak >= 0.7 ? "güçlü" : mutlak >= 0.4 ? "orta" : "zayıf";
        String yon = r >= 0 ? "pozitif" : "negatif";
        return String.format("%.2f (%s %s)", r, guc, yon);
    }
}
