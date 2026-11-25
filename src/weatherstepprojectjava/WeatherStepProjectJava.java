/*
 * Proje: HavaAdım - Hava Durumu ve Adım Sayısı Analizi
 * Metodoloji: Scrum (2 Sprint)
 * IDE: NetBeans
 */
package weatherstepprojectjava;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ana uygulama penceresi: veri girişi, tablo, analiz ve grafik.
 */
public class WeatherStepProjectJava extends JFrame {

    private final JTextField txtTarih = new JTextField();
    private final JTextField txtSicaklik = new JTextField();
    private final JTextField txtAdim = new JTextField();
    private final JComboBox<String> cmbHava = new JComboBox<>(GunlukKayit.HAVA_TURLERI);
    private final JSpinner spnMotivasyon = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));

    private final DefaultTableModel tableModel;
    private final JTable tablo;
    private final JTextArea txtSonuc = new JTextArea(18, 20);
    private final List<GunlukKayit> kayitListesi = new ArrayList<>();

    public WeatherStepProjectJava() {
        setTitle("HavaAdım: Hava Durumu ve Adım Analizi");
        setSize(880, 940);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));
        setLocationRelativeTo(null);

        // 1. Üst Panel: Veri Girişi
        JPanel panelGiris = new JPanel(new GridLayout(5, 2, 10, 6));
        panelGiris.setBorder(BorderFactory.createTitledBorder("Veri Girişi"));

        txtTarih.setText(LocalDate.now().format(CsvDosya.TARIH_FORMATI));
        panelGiris.add(new JLabel("Tarih (gg.aa.yyyy):"));
        panelGiris.add(txtTarih);
        panelGiris.add(new JLabel("Hava Sıcaklığı (°C):"));
        panelGiris.add(txtSicaklik);
        panelGiris.add(new JLabel("Günlük Adım Sayısı:"));
        panelGiris.add(txtAdim);
        panelGiris.add(new JLabel("Hava Durumu:"));
        panelGiris.add(cmbHava);
        panelGiris.add(new JLabel("Motivasyon (1-5):"));
        panelGiris.add(spnMotivasyon);

        JPanel panelButonlar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton btnEkle = new JButton("Kaydı Ekle");
        JButton btnSil = new JButton("Seçili Kaydı Sil");
        JButton btnAnaliz = new JButton("Analiz Et");
        JButton btnGrafik = new JButton("Grafik");
        JButton btnKaydet = new JButton("Kaydet (CSV)");
        JButton btnYukle = new JButton("Yükle (CSV)");
        btnAnaliz.setBackground(Color.ORANGE);
        for (JButton b : new JButton[]{btnEkle, btnSil, btnAnaliz, btnGrafik, btnKaydet, btnYukle}) {
            panelButonlar.add(b);
        }

        JPanel panelUst = new JPanel(new BorderLayout());
        panelUst.add(panelGiris, BorderLayout.CENTER);
        panelUst.add(panelButonlar, BorderLayout.SOUTH);
        add(panelUst, BorderLayout.NORTH);

        // 2. Orta Panel: Tablo
        String[] kolonlar = {"Tarih", "Sıcaklık (°C)", "Adım Sayısı", "Hava Durumu", "Motivasyon"};
        tableModel = new DefaultTableModel(kolonlar, 0) {
            @Override
            public boolean isCellEditable(int satir, int sutun) {
                return false; // tablo ile liste her zaman aynı kalsın
            }
        };
        tablo = new JTable(tableModel);
        tablo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(tablo);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Girilen Veriler"));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Alt Panel: Analiz Sonuçları
        txtSonuc.setEditable(false);
        txtSonuc.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        JScrollPane scrollSonuc = new JScrollPane(txtSonuc);
        scrollSonuc.setBorder(BorderFactory.createTitledBorder("Analiz Raporu"));
        add(scrollSonuc, BorderLayout.SOUTH);

        // Buton aksiyonları
        btnEkle.addActionListener(e -> veriEkle());
        btnSil.addActionListener(e -> seciliyiSil());
        btnAnaliz.addActionListener(e -> analizYap());
        btnGrafik.addActionListener(e -> grafikGoster());
        btnKaydet.addActionListener(e -> dosyayaKaydet());
        btnYukle.addActionListener(e -> dosyadanYukle());
    }

    private void veriEkle() {
        LocalDate tarih;
        double sicaklik;
        int adim;
        try {
            tarih = LocalDate.parse(txtTarih.getText().trim(), CsvDosya.TARIH_FORMATI);
        } catch (DateTimeParseException ex) {
            hata("Tarih gg.aa.yyyy biçiminde olmalı (örn. 24.12.2025).");
            return;
        }
        try {
            // Türkçe klavyede ondalık için virgül de kabul edilir (18,5)
            sicaklik = Double.parseDouble(txtSicaklik.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            hata("Sıcaklık sayı olmalı (örn. 18 veya 18,5).");
            return;
        }
        if (sicaklik < -50 || sicaklik > 60) {
            hata("Sıcaklık -50 ile 60 °C arasında olmalı.");
            return;
        }
        try {
            adim = Integer.parseInt(txtAdim.getText().trim());
        } catch (NumberFormatException ex) {
            hata("Adım sayısı tam sayı olmalı.");
            return;
        }
        if (adim <= 0) {
            hata("Adım sayısı pozitif olmalı.");
            return;
        }
        for (GunlukKayit k : kayitListesi) {
            if (k.tarih.equals(tarih)) {
                hata(tarih.format(CsvDosya.TARIH_FORMATI) + " için zaten bir kayıt var.");
                return;
            }
        }

        String hava = (String) cmbHava.getSelectedItem();
        int motivasyon = (Integer) spnMotivasyon.getValue();
        kayitListesi.add(new GunlukKayit(tarih, sicaklik, adim, hava, motivasyon));
        tabloyuYenile();

        // Sonraki gün için hazırla
        txtTarih.setText(tarih.plusDays(1).format(CsvDosya.TARIH_FORMATI));
        txtSicaklik.setText("");
        txtAdim.setText("");
        txtSicaklik.requestFocus();
    }

    private void seciliyiSil() {
        int satir = tablo.getSelectedRow();
        if (satir < 0) {
            hata("Silmek için tablodan bir satır seçin.");
            return;
        }
        kayitListesi.remove(satir);
        tabloyuYenile();
    }

    /** Listeyi tarihe göre sıralar ve tabloyu baştan doldurur. */
    private void tabloyuYenile() {
        kayitListesi.sort(Comparator.comparing(k -> k.tarih));
        tableModel.setRowCount(0);
        for (GunlukKayit k : kayitListesi) {
            tableModel.addRow(new Object[]{
                k.tarih.format(CsvDosya.TARIH_FORMATI),
                CsvDosya.sayiYaz(k.sicaklik),
                String.format("%,d", k.adimSayisi),
                k.havaDurumu,
                k.motivasyon});
        }
    }

    void analizYap() {
        txtSonuc.setText(Analiz.raporOlustur(kayitListesi));
        txtSonuc.setCaretPosition(0);
    }

    void grafikGoster() {
        if (kayitListesi.isEmpty()) {
            hata("Grafik için önce veri girin veya CSV yükleyin.");
            return;
        }
        JDialog pencere = new JDialog(this, "Grafikler", false);
        pencere.add(new GrafikPaneli(new ArrayList<>(kayitListesi)));
        pencere.pack();
        pencere.setLocationRelativeTo(this);
        pencere.setVisible(true);
    }

    private void dosyayaKaydet() {
        if (kayitListesi.isEmpty()) {
            hata("Kaydedilecek veri yok.");
            return;
        }
        JFileChooser secici = csvSecici();
        secici.setSelectedFile(new File("veriler.csv"));
        if (secici.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File dosya = secici.getSelectedFile();
        if (!dosya.getName().toLowerCase().endsWith(".csv")) {
            dosya = new File(dosya.getParentFile(), dosya.getName() + ".csv");
        }
        try {
            CsvDosya.kaydet(dosya.toPath(), kayitListesi);
            JOptionPane.showMessageDialog(this, kayitListesi.size() + " kayıt kaydedildi:\n" + dosya);
        } catch (IOException ex) {
            hata("Dosya kaydedilemedi: " + ex.getMessage());
        }
    }

    private void dosyadanYukle() {
        JFileChooser secici = csvSecici();
        if (secici.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            yukle(secici.getSelectedFile().toPath());
        }
    }

    void yukle(Path dosya) {
        try {
            List<GunlukKayit> okunan = CsvDosya.oku(dosya);
            kayitListesi.clear();
            kayitListesi.addAll(okunan);
            tabloyuYenile();
            txtSonuc.setText(okunan.size() + " kayıt yüklendi. Sonuçlar için \"Analiz Et\"e basın.");
        } catch (IOException ex) {
            hata("Dosya okunamadı: " + ex.getMessage());
        }
    }

    private JFileChooser csvSecici() {
        JFileChooser secici = new JFileChooser(new File("data").exists() ? new File("data") : new File("."));
        secici.setFileFilter(new FileNameExtensionFilter("CSV dosyası (*.csv)", "csv"));
        return secici;
    }

    private void hata(String mesaj) {
        JOptionPane.showMessageDialog(this, mesaj, "Hata", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WeatherStepProjectJava().setVisible(true));
    }
}
