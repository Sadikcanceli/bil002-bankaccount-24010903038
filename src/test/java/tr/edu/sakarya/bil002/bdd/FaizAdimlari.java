package tr.edu.sakarya.bil002.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import tr.edu.sakarya.bil002.model.Transaction;
import tr.edu.sakarya.bil002.model.TransactionStatus;
import tr.edu.sakarya.bil002.model.TransactionType;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Aylık faiz adımları (faiz.feature).
 */
public class FaizAdimlari {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SenaryoBaglami baglam;

    @When("aylık faiz uygulanır")
    public void aylikFaizUygulanir() {
        String no = baglam.getAktifHesapNo();
        baglam.setOncekiBakiye(accountService.getAccount(no).getBalance());
        baglam.setSonHata(null);
        try {
            accountService.calculateInterest(no);
        } catch (Exception e) {
            baglam.setSonHata(e);
        }
    }

    @Then("işlem geçmişinde {double} TL tutarında başarılı bir faiz kaydı bulunmalı")
    public void faizKaydiBulunmali(double tutar) {
        Transaction son = accountService.getTransactionHistory(baglam.getAktifHesapNo()).get(0);
        assertEquals(TransactionType.INTEREST, son.getType());
        assertEquals(TransactionStatus.SUCCESS, son.getStatus());
        assertEquals(tutar, son.getAmount(), 0.001);
    }
}
