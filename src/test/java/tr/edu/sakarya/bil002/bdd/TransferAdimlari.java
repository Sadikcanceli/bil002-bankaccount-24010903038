package tr.edu.sakarya.bil002.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Hesaplar arası transfer adımları (transfer.feature).
 */
public class TransferAdimlari {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SenaryoBaglami baglam;

    @When("{string} hesabından {string} hesabına {double} TL transfer edilir")
    public void transferEdilir(String kaynak, String hedef, double tutar) {
        baglam.setAktifHesapNo(baglam.hesapNo(kaynak));
        baglam.setOncekiBakiye(accountService.getAccount(baglam.hesapNo(kaynak)).getBalance());
        baglam.setSonHata(null);
        try {
            accountService.transfer(baglam.hesapNo(kaynak), baglam.hesapNo(hedef), tutar);
        } catch (Exception e) {
            baglam.setSonHata(e);
        }
    }

    @When("{string} kendi hesabına {double} TL transfer etmeye çalışır")
    public void kendiHesabinaTransfer(String ad, double tutar) {
        transferEdilir(ad, ad, tutar);
    }

    @Then("{string} ve {string} hesaplarının toplam bakiyesi {double} TL olmalı")
    public void toplamBakiyeOlmali(String a, String b, double toplam) {
        double gercek = accountService.getAccount(baglam.hesapNo(a)).getBalance()
                + accountService.getAccount(baglam.hesapNo(b)).getBalance();
        assertEquals(toplam, gercek, 0.001);
    }
}
