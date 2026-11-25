package tr.edu.sakarya.bil002.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Hesap durum geçişleri adımları (hesap_durumu.feature).
 */
public class HesapDurumuAdimlari {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SenaryoBaglami baglam;

    @Given("durumu {word} olan ve bakiyesi {double} TL olan bir hesap var")
    public void durumuOlanHesap(String durum, double bakiye) {
        Account hesap = accountService.createAccount("Durum Müşterisi", "STANDARD", bakiye);
        String no = hesap.getAccountNumber();
        if ("FROZEN".equals(durum)) {
            accountService.freezeAccount(no);
        } else if ("CLOSED".equals(durum)) {
            accountService.closeAccount(no);
        }
        baglam.hesapEkle("hesap", no);
    }

    @When("hesap için {string} işlemi istenir")
    public void islemIstenir(String islem) {
        String no = baglam.getAktifHesapNo();
        baglam.setOncekiBakiye(accountService.getAccount(no).getBalance());
        baglam.setSonHata(null);
        try {
            switch (islem) {
                case "dondurma" -> accountService.freezeAccount(no);
                case "dondurmayı kaldırma" -> accountService.unfreezeAccount(no);
                case "kapatma" -> accountService.closeAccount(no);
                default -> throw new IllegalStateException("Bilinmeyen işlem: " + islem);
            }
        } catch (Exception e) {
            baglam.setSonHata(e);
        }
    }
}
