package tr.edu.sakarya.bil002.bdd;

import java.util.HashMap;
import java.util.Map;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

/**
 * Bir senaryonun adım sınıfları arasında paylaşılan durum. @ScenarioScope sayesinde her senaryoda
 * yeni bir örnek oluşturulur; senaryolar birbirini etkilemez.
 */
@Component
@ScenarioScope
public class SenaryoBaglami {

    /** Senaryodaki takma ad -> hesap numarası (ör. "Ali" -> "4821093375"). */
    private final Map<String, String> hesaplar = new HashMap<>();

    /** Tek hesaplı senaryolarda işlemin uygulandığı hesap. */
    private String aktifHesapNo;

    /** Son işlemde yakalanan istisna (başarılıysa null). */
    private Exception sonHata;

    /** İşlemden hemen önceki bakiye (G-4 kontrolü için). */
    private double oncekiBakiye;

    public void hesapEkle(String takmaAd, String hesapNo) {
        hesaplar.put(takmaAd, hesapNo);
        aktifHesapNo = hesapNo;
    }

    public String hesapNo(String takmaAd) {
        return hesaplar.get(takmaAd);
    }

    public String getAktifHesapNo() {
        return aktifHesapNo;
    }

    public void setAktifHesapNo(String aktifHesapNo) {
        this.aktifHesapNo = aktifHesapNo;
    }

    public Exception getSonHata() {
        return sonHata;
    }

    public void setSonHata(Exception sonHata) {
        this.sonHata = sonHata;
    }

    public double getOncekiBakiye() {
        return oncekiBakiye;
    }

    public void setOncekiBakiye(double oncekiBakiye) {
        this.oncekiBakiye = oncekiBakiye;
    }
}
