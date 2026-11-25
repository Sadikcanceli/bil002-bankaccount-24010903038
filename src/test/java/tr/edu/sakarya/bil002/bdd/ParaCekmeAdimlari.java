package tr.edu.sakarya.bil002.bdd;

import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Para çekme adımları (para_cekme.feature).
 */
public class ParaCekmeAdimlari {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SenaryoBaglami baglam;

    @When("hesaptan {double} TL çekilir")
    public void hesaptanCekilir(double tutar) {
        String no = baglam.getAktifHesapNo();
        baglam.setOncekiBakiye(accountService.getAccount(no).getBalance());
        baglam.setSonHata(null);
        try {
            accountService.withdraw(no, tutar);
        } catch (Exception e) {
            baglam.setSonHata(e);
        }
    }

    @When("aynı gün içinde önce {double} TL, ardından {double} TL çekilir")
    public void ayniGunIkiCekim(double ilk, double ikinci) {
        accountService.withdraw(baglam.getAktifHesapNo(), ilk);
        hesaptanCekilir(ikinci);
    }
}
