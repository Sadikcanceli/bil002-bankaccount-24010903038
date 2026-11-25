package tr.edu.sakarya.bil002.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
 * Entegrasyon testleri: HTTP katmanından veritabanına kadar tüm akış (MockMvc).
 * Beklenen kodlar: docs/API.md ve Gereksinimler §7. Test ID'leri (API-xx) rapor Bölüm 6 ile eşleşir.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    private static final String OLMAYAN = "1111111111";

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------------------------ yardımcılar

    private String hesapAc(String tur, double bakiye) throws Exception {
        String govde = mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"Entegrasyon\",\"accountType\":\"" + tur
                                + "\",\"initialBalance\":" + bakiye + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(govde, "$.accountNumber");
    }

    private ResultActions postJson(String yol, String json) throws Exception {
        return mockMvc.perform(post(yol).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions postBos(String yol) throws Exception {
        return mockMvc.perform(post(yol));
    }

    // ===================== POST /api/accounts =====================

    @Test
    @DisplayName("API-01 POST /api/accounts geçerli istek -> 201 + hesap JSON")
    void createAccount_gecerliIstek_201Donmeli() throws Exception {
        postJson("/api/accounts", "{\"ownerName\":\"Ayşe Yılmaz\",\"accountType\":\"STANDARD\","
                + "\"initialBalance\":1000.0}")
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").isString())
                .andExpect(jsonPath("$.ownerName").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.balance").value(1000.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accountType").value("STANDARD"));
    }

    @Test
    @DisplayName("API-02 POST /api/accounts boş ad -> 400 + hata JSON formatı")
    void createAccount_bosAd_400Donmeli() throws Exception {
        postJson("/api/accounts", "{\"ownerName\":\"  \",\"accountType\":\"STANDARD\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/accounts"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("API-03 POST /api/accounts geçersiz tür -> 400")
    void createAccount_gecersizTur_400Donmeli() throws Exception {
        postJson("/api/accounts", "{\"ownerName\":\"Ali\",\"accountType\":\"GOLD\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-04 POST /api/accounts bozuk JSON -> 400")
    void createAccount_bozukJson_400Donmeli() throws Exception {
        postJson("/api/accounts", "{\"ownerName\": \"Ali\", ").andExpect(status().isBadRequest());
    }

    @Tag("hata")
    @Test
    @DisplayName("API-05 POST /api/accounts negatif başlangıç bakiyesi -> 400 olmalı")
    void createAccount_negatifBakiye_400Donmeli() throws Exception {
        // R-1.4 + §7. Rapor: H-01
        postJson("/api/accounts", "{\"ownerName\":\"Ali\",\"accountType\":\"STANDARD\",\"initialBalance\":-50}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-06 POST /api/accounts initialBalance gönderilmezse bakiye 0.0")
    void createAccount_bakiyesiz_sifir() throws Exception {
        postJson("/api/accounts", "{\"ownerName\":\"Ali\",\"accountType\":\"PREMIUM\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    // ===================== GET /api/accounts, /api/accounts/{no} =====================

    @Test
    @DisplayName("API-07 GET /api/accounts -> 200 + dizi")
    void getAll_200Donmeli() throws Exception {
        hesapAc("STANDARD", 1.0);
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("API-08 GET /api/accounts/{no} mevcut hesap -> 200")
    void getAccount_mevcut_200Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 10.0);
        mockMvc.perform(get("/api/accounts/" + no))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(no));
    }

    @Tag("hata")
    @Test
    @DisplayName("API-09 GET /api/accounts/{no} olmayan hesap -> 404 olmalı")
    void getAccount_bulunamayan_404Donmeli() throws Exception {
        // R-10.2: hesap yoksa 404 Not Found. Uygulama 200 + boş gövde döndürüyor. Rapor: H-13
        mockMvc.perform(get("/api/accounts/" + OLMAYAN)).andExpect(status().isNotFound());
    }

    // ===================== POST /deposit =====================

    @Test
    @DisplayName("API-10 POST /deposit aktif hesap -> 200 + yeni bakiye")
    void deposit_aktifHesap_200Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 1000.0);
        postJson("/api/accounts/" + no + "/deposit", "{\"amount\":500.0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1500.0));
    }

    @Test
    @DisplayName("API-11 POST /deposit eksik amount -> 400")
    void deposit_eksikTutar_400Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 10.0);
        postJson("/api/accounts/" + no + "/deposit", "{}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-12 POST /deposit sayısal olmayan amount -> 400")
    void deposit_sayisalOlmayanTutar_400Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 10.0);
        postJson("/api/accounts/" + no + "/deposit", "{\"amount\":\"abc\"}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-13 POST /deposit olmayan hesap -> 404")
    void deposit_olmayanHesap_404Donmeli() throws Exception {
        postJson("/api/accounts/" + OLMAYAN + "/deposit", "{\"amount\":10}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("API-14 POST /deposit FROZEN hesap -> 409")
    void deposit_dondurulmus_409Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 10.0);
        postBos("/api/accounts/" + no + "/freeze").andExpect(status().isOk());
        postJson("/api/accounts/" + no + "/deposit", "{\"amount\":10}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("API-15 POST /deposit limit aşımı -> 422")
    void deposit_limitAsimi_422Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 0.0);
        postJson("/api/accounts/" + no + "/deposit", "{\"amount\":10000.01}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Tag("hata")
    @Test
    @DisplayName("API-16 POST /deposit amount=0 -> 400 olmalı")
    void deposit_sifirTutar_400Donmeli() throws Exception {
        // R-2.2 + §7: amount <= 0 -> 400. Rapor: H-02
        String no = hesapAc("STANDARD", 10.0);
        postJson("/api/accounts/" + no + "/deposit", "{\"amount\":0}").andExpect(status().isBadRequest());
    }

    // ===================== POST /withdraw =====================

    @Test
    @DisplayName("API-17 POST /withdraw geçerli -> 200")
    void withdraw_gecerli_200Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 1000.0);
        postJson("/api/accounts/" + no + "/withdraw", "{\"amount\":200}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(800.0));
    }

    @Test
    @DisplayName("API-18 POST /withdraw negatif tutar -> 400")
    void withdraw_negatif_400Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 1000.0);
        postJson("/api/accounts/" + no + "/withdraw", "{\"amount\":-1}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-19 POST /withdraw olmayan hesap -> 404")
    void withdraw_olmayanHesap_404Donmeli() throws Exception {
        postJson("/api/accounts/" + OLMAYAN + "/withdraw", "{\"amount\":1}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("API-20 POST /withdraw yetersiz bakiye -> 422")
    void withdraw_yetersizBakiye_422Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 100.0);
        postJson("/api/accounts/" + no + "/withdraw", "{\"amount\":150}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Tag("hata")
    @Test
    @DisplayName("API-21 POST /withdraw FROZEN hesap -> 409 olmalı")
    void withdraw_dondurulmus_409Donmeli() throws Exception {
        // R-3.3 + §7. Rapor: H-03
        String no = hesapAc("STANDARD", 500.0);
        postBos("/api/accounts/" + no + "/freeze");
        postJson("/api/accounts/" + no + "/withdraw", "{\"amount\":100}").andExpect(status().isConflict());
    }

    @Tag("hata")
    @Test
    @DisplayName("API-22 POST /withdraw tam bakiye -> 200 olmalı")
    void withdraw_tamBakiye_200Donmeli() throws Exception {
        // R-3.4. Rapor: H-04
        String no = hesapAc("STANDARD", 300.0);
        postJson("/api/accounts/" + no + "/withdraw", "{\"amount\":300}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0.0));
    }

    // ===================== freeze / unfreeze / close =====================

    @Test
    @DisplayName("API-23 POST /freeze ACTIVE -> 200 FROZEN")
    void freeze_200Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 1.0);
        postBos("/api/accounts/" + no + "/freeze")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    @DisplayName("API-24 POST /freeze olmayan hesap -> 404")
    void freeze_olmayan_404Donmeli() throws Exception {
        postBos("/api/accounts/" + OLMAYAN + "/freeze").andExpect(status().isNotFound());
    }

    @Tag("hata")
    @Test
    @DisplayName("API-25 POST /freeze zaten FROZEN -> 409 olmalı")
    void freeze_zatenDondurulmus_409Donmeli() throws Exception {
        // R-5.1. Rapor: H-09
        String no = hesapAc("STANDARD", 1.0);
        postBos("/api/accounts/" + no + "/freeze");
        postBos("/api/accounts/" + no + "/freeze").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("API-26 POST /unfreeze FROZEN -> 200 ACTIVE")
    void unfreeze_200Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 1.0);
        postBos("/api/accounts/" + no + "/freeze");
        postBos("/api/accounts/" + no + "/unfreeze")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("API-27 POST /unfreeze ACTIVE -> 409")
    void unfreeze_aktif_409Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 1.0);
        postBos("/api/accounts/" + no + "/unfreeze").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("API-28 POST /close bakiye 0 -> 200 CLOSED")
    void close_200Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 0.0);
        postBos("/api/accounts/" + no + "/close")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("API-29 POST /close zaten kapalı -> 409")
    void close_zatenKapali_409Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 0.0);
        postBos("/api/accounts/" + no + "/close");
        postBos("/api/accounts/" + no + "/close").andExpect(status().isConflict());
    }

    @Tag("hata")
    @Test
    @DisplayName("API-30 POST /close bakiyesi olan hesap -> 409 olmalı")
    void close_bakiyeli_409Donmeli() throws Exception {
        // R-7.2 + §7. Rapor: H-10
        String no = hesapAc("STANDARD", 100.0);
        postBos("/api/accounts/" + no + "/close").andExpect(status().isConflict());
    }

    // ===================== interest / transactions =====================

    @Test
    @DisplayName("API-31 POST /interest ACTIVE -> 200")
    void interest_200Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 12000.0);
        postBos("/api/accounts/" + no + "/interest")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(12030.0));
    }

    @Test
    @DisplayName("API-32 POST /interest FROZEN -> 409")
    void interest_dondurulmus_409Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 100.0);
        postBos("/api/accounts/" + no + "/freeze");
        postBos("/api/accounts/" + no + "/interest").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("API-33 GET /transactions -> 200, en yeni ilk sırada")
    void transactions_200Donmeli() throws Exception {
        String no = hesapAc("STANDARD", 1000.0);
        postJson("/api/accounts/" + no + "/deposit", "{\"amount\":500}");
        postJson("/api/accounts/" + no + "/withdraw", "{\"amount\":200}");
        mockMvc.perform(get("/api/accounts/" + no + "/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[0].balanceAfter").value(1300.0))
                .andExpect(jsonPath("$[1].type").value("DEPOSIT"));
    }

    @Test
    @DisplayName("API-34 GET /transactions olmayan hesap -> 404")
    void transactions_olmayan_404Donmeli() throws Exception {
        mockMvc.perform(get("/api/accounts/" + OLMAYAN + "/transactions")).andExpect(status().isNotFound());
    }
}
