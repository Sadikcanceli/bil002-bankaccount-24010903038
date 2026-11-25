package tr.edu.sakarya.bil002.tdd;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * TDD özelliği: GET /api/accounts/{no}/summary
 * Yanıt: accountNumber, totalDeposited (başarılı DEPOSIT toplamı), totalWithdrawn (başarılı WITHDRAWAL
 * toplamı) ve transactionCount (hesabın işlem geçmişindeki kayıt sayısı). Hesap yoksa 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountSummaryTddTest {

    @Autowired
    private MockMvc mockMvc;

    private String hesapAc(double bakiye) throws Exception {
        String govde = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"TDD\",\"accountType\":\"STANDARD\",\"initialBalance\":" + bakiye
                                + "}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(govde, "$.accountNumber");
    }

    private void islem(String no, String tur, double tutar) throws Exception {
        mockMvc.perform(post("/api/accounts/" + no + "/" + tur).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":" + tutar + "}"));
    }

    @Test
    @DisplayName("TDD-01 Özet: yatırılan/çekilen toplamları ve işlem sayısı")
    void ozet_toplamlarVeIslemSayisi() throws Exception {
        String no = hesapAc(1000.0);
        islem(no, "deposit", 500.0);
        islem(no, "deposit", 250.0);
        islem(no, "withdraw", 200.0);

        mockMvc.perform(get("/api/accounts/" + no + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(no))
                .andExpect(jsonPath("$.totalDeposited").value(750.0))
                .andExpect(jsonPath("$.totalWithdrawn").value(200.0))
                .andExpect(jsonPath("$.transactionCount").value(3));
    }

    @Test
    @DisplayName("TDD-02 Özet: hiç işlemi olmayan hesap için sıfırlar")
    void ozet_islemsizHesap_sifirlar() throws Exception {
        String no = hesapAc(100.0);
        mockMvc.perform(get("/api/accounts/" + no + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeposited").value(0.0))
                .andExpect(jsonPath("$.totalWithdrawn").value(0.0))
                .andExpect(jsonPath("$.transactionCount").value(0));
    }

    @Test
    @DisplayName("TDD-03 Özet: transfer ve faiz kayıtları toplamlara girmez ama sayılır")
    void ozet_transferVeFaizToplamlaraGirmez() throws Exception {
        String a = hesapAc(12000.0);
        String b = hesapAc(0.0);
        mockMvc.perform(post("/api/transactions/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccount\":\"" + a + "\",\"toAccount\":\"" + b + "\",\"amount\":100}"));
        mockMvc.perform(post("/api/accounts/" + a + "/interest"));

        mockMvc.perform(get("/api/accounts/" + a + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDeposited").value(0.0))
                .andExpect(jsonPath("$.totalWithdrawn").value(0.0))
                .andExpect(jsonPath("$.transactionCount").value(2));
    }

    @Test
    @DisplayName("TDD-04 Özet: olmayan hesap -> 404")
    void ozet_olmayanHesap_404() throws Exception {
        mockMvc.perform(get("/api/accounts/1111111111/summary")).andExpect(status().isNotFound());
    }
}
