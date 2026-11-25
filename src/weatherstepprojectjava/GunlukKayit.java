package weatherstepprojectjava;

import java.time.LocalDate;

/**
 * Veri modeli: bir günün kaydı.
 */
public class GunlukKayit {

    public static final String[] HAVA_TURLERI = {"Güneşli", "Bulutlu", "Yağmurlu"};

    final LocalDate tarih;
    final double sicaklik;
    final int adimSayisi;
    final String havaDurumu;
    final int motivasyon;

    public GunlukKayit(LocalDate tarih, double sicaklik, int adimSayisi,
                       String havaDurumu, int motivasyon) {
        this.tarih = tarih;
        this.sicaklik = sicaklik;
        this.adimSayisi = adimSayisi;
        this.havaDurumu = havaDurumu;
        this.motivasyon = motivasyon;
    }
}
