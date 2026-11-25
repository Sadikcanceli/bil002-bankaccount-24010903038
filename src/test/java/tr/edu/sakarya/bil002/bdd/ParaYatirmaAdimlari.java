package tr.edu.sakarya.bil002.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.model.TransactionType;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Para yatırma adımları (ornek_senaryo.feature ve para_yatirma.feature). Senaryo durumu
 * SenaryoBaglami üzerinden diğer adım sınıflarıyla paylaşılır; böylece "işlem reddedilmeli" gibi
 * ortak adımlar tüm feature dosyalarında kullanılabilir.
 */
public class ParaYatirmaAdimlari {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SenaryoBaglami baglam;

    @Given("aktif bir hesap mevcut ve bakiyesi {int} TL")
    public void aktifBirHesapMevcut(int bakiye) {
        Account hesap = accountService.createAccount("Örnek Müşteri", "STANDARD", (double) bakiye);
        baglam.hesapEkle("hesap", hesap.getAccountNumber());
    }

    @Given("dondurulmuş bir hesap mevcut")
    public void dondurulmusBirHesapMevcut() {
        Account hesap = accountService.createAccount("Örnek Müşteri", "STANDARD", 250.0);
        accountService.freezeAccount(hesap.getAccountNumber());
        baglam.hesapEkle("hesap", hesap.getAccountNumber());
    }

    @When("hesaba {int} TL yatırılır")
    public void hesabaParaYatirilir(int tutar) {
        accountService.deposit(baglam.getAktifHesapNo(), (double) tutar);
    }

    @When("hesaba {int} TL yatırılmaya çalışılır")
    public void hesabaParaYatirilmayaCalisilir(int tutar) {
        hesabaTutarYatirilmayaCalisilir((double) tutar);
    }

    @When("hesaba {double} TL yatırılmak istenir")
    public void hesabaTutarYatirilmayaCalisilir(double tutar) {
        baglam.setOncekiBakiye(accountService.getAccount(baglam.getAktifHesapNo()).getBalance());
        baglam.setSonHata(null);
        try {
            accountService.deposit(baglam.getAktifHesapNo(), tutar);
        } catch (Exception e) {
            baglam.setSonHata(e);
        }
    }

    @Then("bakiye {int} TL olmalı")
    public void bakiyeOlmali(int beklenen) {
        double gercek = accountService.getAccount(baglam.getAktifHesapNo()).getBalance();
        assertEquals(beklenen, gercek, 0.001);
    }

    @Then("işlem geçmişinde {word} kaydı bulunmalı")
    public void islemGecmisindeKayitBulunmali(String tip) {
        TransactionType beklenenTip = TransactionType.valueOf(tip);
        boolean varMi = accountService.getTransactionHistory(baglam.getAktifHesapNo()).stream()
                .anyMatch(t -> t.getType() == beklenenTip);
        assertTrue(varMi, "İşlem geçmişinde " + tip + " kaydı bulunamadı");
    }

    @Then("işlem reddedilmeli")
    public void islemReddedilmeli() {
        assertNotNull(baglam.getSonHata(), "İşlemin reddedilmesi bekleniyordu ama başarılı oldu");
    }

    @Then("bakiye değişmemeli")
    public void bakiyeDegismemeli() {
        double gercek = accountService.getAccount(baglam.getAktifHesapNo()).getBalance();
        assertEquals(baglam.getOncekiBakiye(), gercek, 0.001);
    }
}
