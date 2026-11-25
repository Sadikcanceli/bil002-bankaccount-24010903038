package tr.edu.sakarya.bil002.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import tr.edu.sakarya.bil002.exception.AccountNotFoundException;
import tr.edu.sakarya.bil002.exception.InsufficientBalanceException;
import tr.edu.sakarya.bil002.exception.InvalidAccountStateException;
import tr.edu.sakarya.bil002.exception.LimitExceededException;
import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.model.AccountStatus;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Birden fazla feature dosyasında kullanılan hesap hazırlama ve doğrulama adımları.
 */
public class OrtakHesapAdimlari {

    /** İş dilindeki red nedeni -> servis katmanının fırlattığı istisna (Gereksinimler §7). */
    private static final Map<String, Class<? extends Exception>> RED_NEDENLERI = Map.of(
            "geçersiz girdi", IllegalArgumentException.class,
            "hesap bulunamadı", AccountNotFoundException.class,
            "hesap aktif değil", InvalidAccountStateException.class,
            "yetersiz bakiye", InsufficientBalanceException.class,
            "limit aşımı", LimitExceededException.class);

    @Autowired
    private AccountService accountService;

    @Autowired
    private SenaryoBaglami baglam;

    @Given("{word} türünde ve {double} TL bakiyeli aktif bir hesap var")
    public void hesapVar(String tur, double bakiye) {
        Account hesap = accountService.createAccount("BDD Müşterisi", tur, bakiye);
        baglam.hesapEkle("hesap", hesap.getAccountNumber());
    }

    @Given("{string} adına {double} TL bakiyeli aktif bir hesap var")
    public void adinaHesapVar(String ad, double bakiye) {
        Account hesap = accountService.createAccount(ad, "STANDARD", bakiye);
        baglam.hesapEkle(ad, hesap.getAccountNumber());
    }

    @Given("hesap dondurulmuş")
    public void hesapDondurulmus() {
        accountService.freezeAccount(baglam.getAktifHesapNo());
    }

    @Given("{string} adlı müşterinin hesabı dondurulmuş")
    public void musterininHesabiDondurulmus(String ad) {
        accountService.freezeAccount(baglam.hesapNo(ad));
    }

    @Given("{string} adlı müşterinin hesabı kapatılmış")
    public void musterininHesabiKapatilmis(String ad) {
        accountService.closeAccount(baglam.hesapNo(ad));
    }

    @Then("işlem başarılı olmalı")
    public void islemBasariliOlmali() {
        assertNull(baglam.getSonHata(), () -> "İşlem reddedildi: " + baglam.getSonHata());
    }

    @Then("red nedeni {string} olmalı")
    public void redNedeniOlmali(String neden) {
        Class<? extends Exception> beklenen = RED_NEDENLERI.get(neden);
        assertInstanceOf(beklenen, baglam.getSonHata(), "Beklenen red nedeni: " + neden);
    }

    @Then("hesap bakiyesi {double} TL olmalı")
    public void hesapBakiyesiOlmali(double beklenen) {
        assertEquals(beklenen, accountService.getAccount(baglam.getAktifHesapNo()).getBalance(), 0.001);
    }

    @Then("{string} adlı müşterinin bakiyesi {double} TL olmalı")
    public void musterininBakiyesiOlmali(String ad, double beklenen) {
        assertEquals(beklenen, accountService.getAccount(baglam.hesapNo(ad)).getBalance(), 0.001);
    }

    @Then("hesabın durumu {word} olmalı")
    public void hesabinDurumuOlmali(String durum) {
        assertEquals(AccountStatus.valueOf(durum), accountService.getAccount(baglam.getAktifHesapNo()).getStatus());
    }
}
