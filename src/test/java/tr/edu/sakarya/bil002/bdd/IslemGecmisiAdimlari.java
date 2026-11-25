package tr.edu.sakarya.bil002.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import tr.edu.sakarya.bil002.model.Transaction;
import tr.edu.sakarya.bil002.model.TransactionStatus;
import tr.edu.sakarya.bil002.model.TransactionType;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * İşlem geçmişi adımları (islem_gecmisi.feature).
 */
public class IslemGecmisiAdimlari {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SenaryoBaglami baglam;

    @Then("işlem geçmişinde {word} tipinde {word} durumunda bir kayıt bulunmalı")
    public void kayitBulunmali(String tip, String durum) {
        List<Transaction> gecmis = accountService.getTransactionHistory(baglam.getAktifHesapNo());
        assertTrue(gecmis.stream().anyMatch(t -> t.getType() == TransactionType.valueOf(tip)
                        && t.getStatus() == TransactionStatus.valueOf(durum)),
                "Geçmişte " + tip + "/" + durum + " kaydı yok: " + gecmis.size() + " kayıt var");
    }

    @Then("işlem geçmişinde {int} kayıt olmalı ve en yeni kayıt {word} olmalı")
    public void kayitSayisiVeSira(int adet, String ilkTip) {
        List<Transaction> gecmis = accountService.getTransactionHistory(baglam.getAktifHesapNo());
        assertEquals(adet, gecmis.size());
        assertEquals(TransactionType.valueOf(ilkTip), gecmis.get(0).getType());
    }
}
