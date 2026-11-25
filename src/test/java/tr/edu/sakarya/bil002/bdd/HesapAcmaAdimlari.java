package tr.edu.sakarya.bil002.bdd;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Hesap açma adımları (hesap_acma.feature). "(yok)" değeri ilgili alanın gönderilmediği (null) anlamına gelir.
 */
public class HesapAcmaAdimlari {

    private static final String YOK = "(yok)";

    @Autowired
    private AccountService accountService;

    @Autowired
    private SenaryoBaglami baglam;

    @When("{string} adına {string} türünde ve {string} TL başlangıç bakiyesiyle hesap açılmak istenir")
    public void hesapAcilmakIstenir(String ad, String tur, String bakiye) {
        baglam.setSonHata(null);
        try {
            Account hesap = accountService.createAccount(YOK.equals(ad) ? null : ad, YOK.equals(tur) ? null : tur,
                    YOK.equals(bakiye) ? null : Double.valueOf(bakiye));
            baglam.hesapEkle(ad, hesap.getAccountNumber());
        } catch (Exception e) {
            baglam.setSonHata(e);
        }
    }

    @Then("hesap numarası 10 haneli olmalı ve 0 ile başlamamalı")
    public void hesapNumarasiGecerli() {
        assertTrue(baglam.getAktifHesapNo().matches("[1-9][0-9]{9}"), baglam.getAktifHesapNo());
    }

    @Then("hesabın işlem geçmişi boş olmalı")
    public void islemGecmisiBos() {
        assertTrue(accountService.getTransactionHistory(baglam.getAktifHesapNo()).isEmpty());
    }
}
