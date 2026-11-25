package weatherstepprojectjava;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Verileri Excel'in doğrudan açabildiği CSV formatında kaydeder ve okur.
 * Ayraç olarak ';' kullanılır, böylece Türkçe Excel'de sütunlar doğru ayrılır.
 */
public class CsvDosya {

    static final DateTimeFormatter TARIH_FORMATI = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String BASLIK = "Tarih;Adım Sayısı;Sıcaklık (°C);Hava Durumu;Motivasyon (1-5)";
    private static final char BOM = '﻿'; // Excel'in Türkçe karakterleri tanıması için

    static void kaydet(Path dosya, List<GunlukKayit> kayitlar) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(dosya, StandardCharsets.UTF_8)) {
            w.write(BOM);
            w.write(BASLIK);
            w.newLine();
            for (GunlukKayit k : kayitlar) {
                w.write(String.join(";",
                        k.tarih.format(TARIH_FORMATI),
                        String.valueOf(k.adimSayisi),
                        sayiYaz(k.sicaklik),
                        k.havaDurumu,
                        String.valueOf(k.motivasyon)));
                w.newLine();
            }
        }
    }

    static List<GunlukKayit> oku(Path dosya) throws IOException {
        List<GunlukKayit> kayitlar = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(dosya, StandardCharsets.UTF_8)) {
            String satir;
            int satirNo = 0;
            while ((satir = r.readLine()) != null) {
                satirNo++;
                if (satirNo == 1) {
                    continue; // başlık satırı
                }
                if (satir.isBlank()) {
                    continue;
                }
                String[] p = satir.split(";");
                if (p.length < 5) {
                    throw new IOException(satirNo + ". satırda eksik sütun var.");
                }
                GunlukKayit kayit;
                try {
                    kayit = new GunlukKayit(
                            LocalDate.parse(p[0].trim(), TARIH_FORMATI),
                            Double.parseDouble(p[2].trim().replace(',', '.')),
                            Integer.parseInt(p[1].trim()),
                            p[3].trim(),
                            Integer.parseInt(p[4].trim()));
                } catch (RuntimeException e) {
                    throw new IOException(satirNo + ". satır okunamadı: " + satir);
                }
                if (kayit.adimSayisi <= 0 || kayit.motivasyon < 1 || kayit.motivasyon > 5) {
                    throw new IOException(satirNo + ". satırda geçersiz değer var: " + satir);
                }
                for (GunlukKayit k : kayitlar) {
                    if (k.tarih.equals(kayit.tarih)) {
                        throw new IOException(satirNo + ". satır: "
                                + kayit.tarih.format(TARIH_FORMATI) + " tarihi birden fazla kez var.");
                    }
                }
                kayitlar.add(kayit);
            }
        }
        return kayitlar;
    }

    /** 18.0 -> "18", 18.5 -> "18,5" (Türkçe Excel ondalık virgül kullanır). */
    static String sayiYaz(double d) {
        if (d == Math.rint(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d).replace('.', ',');
    }
}
