package tr.edu.sakarya.bil002.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * TransactionController entegrasyon testleri: /api/transactions/transfer ve /api/transactions/{no}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String hesapAc(double bakiye) throws Exception {
        String govde = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"Transfer\",\"accountType\":\"STANDARD\",\"initialBalance\":"
                                + bakiye + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(govde, "$.accountNumber");
    }

    private ResultActions transfer(String from, String to, String amount) throws Exception {
        return mockMvc.perform(post("/api/transactions/transfer").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAccount\":\"" + from + "\",\"toAccount\":\"" + to + "\",\"amount\":" + amount + "}"));
    }

    @Test
    @DisplayName("API-35 POST /transfer geçerli -> 200 + başarı mesajı, bakiyeler güncel")
    void transfer_gecerli_200Donmeli() throws Exception {
        String a = hesapAc(1000.0);
        String b = hesapAc(0.0);
        transfer(a, b, "250.0")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transfer başarılı"))
                .andExpect(jsonPath("$.fromAccount").value(a))
                .andExpect(jsonPath("$.toAccount").value(b))
                .andExpect(jsonPath("$.amount").value(250.0));
        mockMvc.perform(get("/api/accounts/" + a)).andExpect(jsonPath("$.balance").value(750.0));
        mockMvc.perform(get("/api/accounts/" + b)).andExpect(jsonPath("$.balance").value(250.0));
    }

    @Test
    @DisplayName("API-36 POST /transfer eksik alan (toAccount) -> 400")
    void transfer_eksikAlan_400Donmeli() throws Exception {
        String a = hesapAc(100.0);
        mockMvc.perform(post("/api/transactions/transfer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccount\":\"" + a + "\",\"amount\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-37 POST /transfer tutar 0 -> 400")
    void transfer_sifirTutar_400Donmeli() throws Exception {
        transfer(hesapAc(100.0), hesapAc(0.0), "0").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-38 POST /transfer olmayan hedef -> 404")
    void transfer_olmayanHedef_404Donmeli() throws Exception {
        transfer(hesapAc(100.0), "1111111111", "10").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("API-39 POST /transfer kaynak FROZEN -> 409")
    void transfer_kaynakDondurulmus_409Donmeli() throws Exception {
        String a = hesapAc(100.0);
        mockMvc.perform(post("/api/accounts/" + a + "/freeze"));
        transfer(a, hesapAc(0.0), "10").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("API-40 POST /transfer yetersiz bakiye -> 422")
    void transfer_yetersizBakiye_422Donmeli() throws Exception {
        transfer(hesapAc(100.0), hesapAc(0.0), "100.01").andExpect(status().isUnprocessableEntity());
    }

    @Tag("hata")
    @Test
    @DisplayName("API-41 POST /transfer aynı hesap -> 400 olmalı")
    void transfer_ayniHesap_400Donmeli() throws Exception {
        // R-4.4 + §7: aynı hesaba transfer 400. Rapor: H-08
        String a = hesapAc(100.0);
        transfer(a, a, "10").andExpect(status().isBadRequest());
    }

    @Tag("hata")
    @Test
    @DisplayName("API-42 POST /transfer hedef FROZEN -> 409 olmalı")
    void transfer_hedefDondurulmus_409Donmeli() throws Exception {
        // R-4.3 + §7. Rapor: H-07
        String b = hesapAc(0.0);
        mockMvc.perform(post("/api/accounts/" + b + "/freeze"));
        transfer(hesapAc(100.0), b, "10").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("API-43 GET /api/transactions/{no} -> 200 (alternatif yol)")
    void gecmis_alternatifYol_200Donmeli() throws Exception {
        String a = hesapAc(100.0);
        mockMvc.perform(post("/api/accounts/" + a + "/deposit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":5}"));
        mockMvc.perform(get("/api/transactions/" + a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"));
    }

    @Test
    @DisplayName("API-44 GET /api/transactions/{no} olmayan hesap -> 404")
    void gecmis_alternatifYol_404Donmeli() throws Exception {
        mockMvc.perform(get("/api/transactions/1111111111")).andExpect(status().isNotFound());
    }
}
