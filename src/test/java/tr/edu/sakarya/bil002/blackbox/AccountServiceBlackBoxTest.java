package tr.edu.sakarya.bil002.blackbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import tr.edu.sakarya.bil002.exception.AccountNotFoundException;
import tr.edu.sakarya.bil002.exception.InsufficientBalanceException;
import tr.edu.sakarya.bil002.exception.InvalidAccountStateException;
import tr.edu.sakarya.bil002.exception.LimitExceededException;
import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.model.AccountStatus;
import tr.edu.sakarya.bil002.model.Transaction;
import tr.edu.sakarya.bil002.model.TransactionStatus;
import tr.edu.sakarya.bil002.model.TransactionType;
import tr.edu.sakarya.bil002.service.AccountService;

/**
 * Kara kutu testleri: yalnızca docs/GEREKSINIMLER.md belgesine bakarak yazılmıştır.
 * Her testin @DisplayName değerindeki kimlik (EP-xx, BVA-xx, DT-xx, ST-xx) rapordaki
 * tablolarla eşleşir. Kasıtlı bir hatayı yakalayan testler @Tag("hata") ile işaretlidir.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountServiceBlackBoxTest {

    private static final double TOLERANS = 0.001;

    @Autowired
    private AccountService accountService;

    // ------------------------------------------------------------------ yardımcılar

    private Account hesapAc(String tur, double bakiye) {
        return accountService.createAccount("Kara Kutu Musteri", tur, bakiye);
    }

    private Account guncel(Account hesap) {
        return accountService.getAccount(hesap.getAccountNumber());
    }

    private Account dondurulmusHesap(double bakiye) {
        Account hesap = hesapAc("STANDARD", bakiye);
        accountService.freezeAccount(hesap.getAccountNumber());
        return hesap;
    }

    private Account kapaliHesap() {
        Account hesap = hesapAc("STANDARD", 0.0);
        accountService.closeAccount(hesap.getAccountNumber());
        return hesap;
    }

    // ===================== EP TESTLERİ — Denklik Sınıfı Bölümleme =====================

    @Test
    @DisplayName("EP-01 deposit: geçerli tutar (500 TL) kabul edilir")
    void deposit_gecerliMiktar_basarili() {
        // R-2.5, R-2.6
        Account hesap = hesapAc("STANDARD", 1000.0);
        Account sonuc = accountService.deposit(hesap.getAccountNumber(), 500.0);
        assertEquals(1500.0, sonuc.getBalance(), TOLERANS);
        List<Transaction> gecmis = accountService.getTransactionHistory(hesap.getAccountNumber());
        assertEquals(TransactionType.DEPOSIT, gecmis.get(0).getType());
        assertEquals(TransactionStatus.SUCCESS, gecmis.get(0).getStatus());
    }

    @Tag("hata")
    @Test
    @DisplayName("EP-02 deposit: sıfır tutar geçersiz sınıftadır, reddedilmeli")
    void deposit_sifirMiktar_reddedilmeli() {
        // R-2.2: amount <= 0 (0 dahil) reddedilir. Rapor: H-02
        Account hesap = hesapAc("STANDARD", 1000.0);
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(hesap.getAccountNumber(), 0.0));
    }

    @Test
    @DisplayName("EP-03 deposit: negatif tutar reddedilir")
    void deposit_negatifMiktar_reddedilmeli() {
        // R-2.2, G-4
        Account hesap = hesapAc("STANDARD", 1000.0);
        assertThrows(IllegalArgumentException.class,
                () -> accountService.deposit(hesap.getAccountNumber(), -100.0));
        assertEquals(1000.0, guncel(hesap).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("EP-04 deposit: FROZEN hesaba yatırma reddedilir")
    void deposit_dondurulmusHesap_reddedilmeli() {
        // R-2.3, G-4
        Account hesap = dondurulmusHesap(250.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.deposit(hesap.getAccountNumber(), 100.0));
        assertEquals(250.0, guncel(hesap).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("EP-05 deposit: CLOSED hesaba yatırma reddedilir")
    void deposit_kapaliHesap_reddedilmeli() {
        // R-2.3
        Account hesap = kapaliHesap();
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.deposit(hesap.getAccountNumber(), 100.0));
        assertEquals(0.0, guncel(hesap).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("EP-06 deposit: mevcut olmayan hesap -> hesap bulunamadı")
    void deposit_olmayanHesap_bulunamadi() {
        // R-2.1
        assertThrows(AccountNotFoundException.class, () -> accountService.deposit("1111111111", 100.0));
    }

    @Test
    @DisplayName("EP-07 createAccount: boş / boşluklu / null ad reddedilir")
    void createAccount_gecersizAd_reddedilmeli() {
        // R-1.1
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount(null, "STANDARD", 10.0));
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount("", "STANDARD", 10.0));
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount("   ", "STANDARD", 10.0));
    }

    @Test
    @DisplayName("EP-08 createAccount: geçersiz hesap türü (küçük harf, bilinmeyen, null) reddedilir")
    void createAccount_gecersizTur_reddedilmeli() {
        // R-1.2
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount("Ali", "standard", 10.0));
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount("Ali", "GOLD", 10.0));
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount("Ali", null, 10.0));
    }

    @Test
    @DisplayName("EP-09 createAccount: geçerli PREMIUM hesap ACTIVE açılır, işlem kaydı oluşmaz")
    void createAccount_gecerliPremium_basarili() {
        // R-1.5, R-1.6, R-1.7
        Account hesap = hesapAc("PREMIUM", 2000.0);
        assertEquals(AccountStatus.ACTIVE, hesap.getStatus());
        assertEquals("PREMIUM", hesap.getAccountType());
        assertTrue(hesap.getAccountNumber().matches("[1-9][0-9]{9}"), "Hesap no 10 hane, ilk hane 0 değil");
        assertTrue(accountService.getTransactionHistory(hesap.getAccountNumber()).isEmpty());
    }

    @Test
    @DisplayName("EP-10 createAccount: initialBalance null ise bakiye 0.0")
    void createAccount_nullBakiye_sifirKabulEdilir() {
        // R-1.3
        Account hesap = accountService.createAccount("Ali", "STANDARD", null);
        assertEquals(0.0, hesap.getBalance(), TOLERANS);
    }

    @Tag("hata")
    @Test
    @DisplayName("EP-11 createAccount: negatif başlangıç bakiyesi reddedilmeli")
    void createAccount_negatifBakiye_reddedilmeli() {
        // R-1.4: initialBalance negatif ise reddedilir. Rapor: H-01
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount("Ali", "STANDARD", -500.0));
    }

    @Tag("hata")
    @Test
    @DisplayName("EP-12 withdraw: FROZEN hesaptan çekim reddedilmeli")
    void withdraw_dondurulmusHesap_reddedilmeli() {
        // R-3.3: FROZEN hesaptan çekim reddedilir. Rapor: H-03
        Account hesap = dondurulmusHesap(500.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.withdraw(hesap.getAccountNumber(), 100.0));
    }

    @Tag("hata")
    @Test
    @DisplayName("EP-13 withdraw: CLOSED hesaptan çekim 'hesap aktif değil' (409) ile reddedilmeli")
    void withdraw_kapaliHesap_durumHatasiDonmeli() {
        // R-3.3 + Bölüm 7: durum hatası 409 olmalı, uygulama yetersiz bakiye (422) veriyor. Rapor: H-03
        Account hesap = kapaliHesap();
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.withdraw(hesap.getAccountNumber(), 10.0));
    }

    @Test
    @DisplayName("EP-14 withdraw: negatif ve sıfır tutar reddedilir")
    void withdraw_gecersizTutar_reddedilmeli() {
        // R-3.2
        Account hesap = hesapAc("STANDARD", 500.0);
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(hesap.getAccountNumber(), 0.0));
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(hesap.getAccountNumber(), -5.0));
        assertEquals(500.0, guncel(hesap).getBalance(), TOLERANS);
    }

    @Tag("hata")
    @Test
    @DisplayName("EP-15 deposit: reddedilen deneme için FAILED kaydı oluşmalı")
    void deposit_reddedilenDeneme_failedKaydiOlusmali() {
        // R-2.7, Bölüm 6: reddedilen yatırma denemesinde DEPOSIT/FAILED kaydı oluşur. Rapor: H-12
        Account hesap = hesapAc("STANDARD", 100.0);
        assertThrows(Exception.class, () -> accountService.deposit(hesap.getAccountNumber(), 20_000.0));
        List<Transaction> gecmis = accountService.getTransactionHistory(hesap.getAccountNumber());
        assertTrue(gecmis.stream().anyMatch(t -> t.getType() == TransactionType.DEPOSIT
                && t.getStatus() == TransactionStatus.FAILED), "DEPOSIT/FAILED kaydı bulunamadı");
    }

    @Tag("hata")
    @Test
    @DisplayName("EP-16 withdraw: reddedilen deneme için FAILED kaydı oluşmalı")
    void withdraw_reddedilenDeneme_failedKaydiOlusmali() {
        // R-3.8, Bölüm 6: reddedilen çekim denemesinde WITHDRAWAL/FAILED kaydı oluşur. Rapor: H-12
        Account hesap = hesapAc("STANDARD", 100.0);
        assertThrows(Exception.class, () -> accountService.withdraw(hesap.getAccountNumber(), 500.0));
        List<Transaction> gecmis = accountService.getTransactionHistory(hesap.getAccountNumber());
        assertTrue(gecmis.stream().anyMatch(t -> t.getType() == TransactionType.WITHDRAWAL
                && t.getStatus() == TransactionStatus.FAILED), "WITHDRAWAL/FAILED kaydı bulunamadı");
    }

    @Tag("hata")
    @Test
    @DisplayName("EP-17 interest: STANDARD 1.000 TL -> aylık faiz 2,50, yeni bakiye 1.002,50")
    void interest_standart1000_dogruFaiz() {
        // R-8.2, R-8.3 (örnek tablo satır 1). Rapor: H-11
        Account hesap = hesapAc("STANDARD", 1000.0);
        Account sonuc = accountService.calculateInterest(hesap.getAccountNumber());
        assertEquals(1002.50, sonuc.getBalance(), TOLERANS);
    }

    @Tag("hata")
    @Test
    @DisplayName("EP-18 interest: PREMIUM 2.500,50 TL -> aylık faiz 10,42, yeni bakiye 2.510,92")
    void interest_premiumKurusluBakiye_dogruFaiz() {
        // R-8.2, R-8.3 (örnek tablo satır 4). Rapor: H-11
        Account hesap = hesapAc("PREMIUM", 2500.50);
        Account sonuc = accountService.calculateInterest(hesap.getAccountNumber());
        assertEquals(2510.92, sonuc.getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("EP-19 interest: STANDARD 12.000 TL -> 30,00 faiz, INTEREST/SUCCESS kaydı")
    void interest_standart12000_dogruFaiz() {
        // R-8.4, R-8.5 (örnek tablo satır 3)
        Account hesap = hesapAc("STANDARD", 12_000.0);
        Account sonuc = accountService.calculateInterest(hesap.getAccountNumber());
        assertEquals(12_030.0, sonuc.getBalance(), TOLERANS);
        Transaction kayit = accountService.getTransactionHistory(hesap.getAccountNumber()).get(0);
        assertEquals(TransactionType.INTEREST, kayit.getType());
        assertEquals(30.0, kayit.getAmount(), TOLERANS);
    }

    @Test
    @DisplayName("EP-20 interest: FROZEN hesaba faiz uygulanmaz")
    void interest_dondurulmusHesap_reddedilmeli() {
        // R-8.1
        Account hesap = dondurulmusHesap(1000.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.calculateInterest(hesap.getAccountNumber()));
        assertEquals(1000.0, guncel(hesap).getBalance(), TOLERANS);
    }

    // ===================== BVA TESTLERİ — Sınır Değer Analizi =====================

    @Tag("hata")
    @Test
    @DisplayName("BVA-01 deposit: alt sınır 0,00 TL reddedilmeli")
    void deposit_sifirTL_sinirDegeri() {
        // R-2.2: 0 dahil reddedilir; bakiye değişmemeli (G-4). Rapor: H-02
        Account hesap = hesapAc("STANDARD", 100.0);
        try {
            accountService.deposit(hesap.getAccountNumber(), 0.0);
        } catch (IllegalArgumentException beklenen) {
            // beklenen davranış
        }
        List<Transaction> gecmis = accountService.getTransactionHistory(hesap.getAccountNumber());
        assertTrue(gecmis.stream().noneMatch(t -> t.getStatus() == TransactionStatus.SUCCESS),
                "0 TL yatırma başarılı işlem olarak kaydedildi");
    }

    @Test
    @DisplayName("BVA-02 deposit: minimum geçerli 0,01 TL kabul edilir")
    void deposit_birKurus_minimumGecerli() {
        // G-1, R-2.2
        Account hesap = hesapAc("STANDARD", 100.0);
        assertEquals(100.01, accountService.deposit(hesap.getAccountNumber(), 0.01).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("BVA-03 deposit: STANDARD üst sınır tam 10.000,00 TL kabul edilir")
    void deposit_standartMaksimum_10000TL() {
        // Bölüm 3: limitler kapsayıcıdır
        Account hesap = hesapAc("STANDARD", 0.0);
        assertEquals(10_000.0, accountService.deposit(hesap.getAccountNumber(), 10_000.0).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("BVA-04 deposit: STANDARD üst sınırın üstü 10.000,01 TL reddedilir")
    void deposit_standartMaksimumuAsan_10001TL() {
        // R-2.4, Bölüm 3
        Account hesap = hesapAc("STANDARD", 0.0);
        assertThrows(LimitExceededException.class, () -> accountService.deposit(hesap.getAccountNumber(), 10_000.01));
        assertEquals(0.0, guncel(hesap).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("BVA-05 deposit: PREMIUM üst sınır tam 50.000,00 TL kabul edilir")
    void deposit_premiumMaksimum_50000TL() {
        Account hesap = hesapAc("PREMIUM", 0.0);
        assertEquals(50_000.0, accountService.deposit(hesap.getAccountNumber(), 50_000.0).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("BVA-06 deposit: PREMIUM 50.000,01 TL reddedilir")
    void deposit_premiumMaksimumuAsan_reddedilmeli() {
        Account hesap = hesapAc("PREMIUM", 0.0);
        assertThrows(LimitExceededException.class, () -> accountService.deposit(hesap.getAccountNumber(), 50_000.01));
    }

    @Tag("hata")
    @Test
    @DisplayName("BVA-07 withdraw: tam bakiye (sınır) çekilebilmeli")
    void withdraw_tamBakiye_cekilebilmeli() {
        // R-3.4: bakiyenin tamamı çekilebilir. Rapor: H-04
        Account hesap = hesapAc("STANDARD", 500.0);
        Account sonuc = accountService.withdraw(hesap.getAccountNumber(), 500.0);
        assertEquals(0.0, sonuc.getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("BVA-08 withdraw: bakiyenin 0,01 TL üstü reddedilir")
    void withdraw_bakiyedenFazla_reddedilmeli() {
        // R-3.4
        Account hesap = hesapAc("STANDARD", 500.0);
        assertThrows(InsufficientBalanceException.class,
                () -> accountService.withdraw(hesap.getAccountNumber(), 500.01));
        assertEquals(500.0, guncel(hesap).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("BVA-09 withdraw: bakiyenin 0,01 TL altı kabul edilir")
    void withdraw_bakiyeninBirKurusAlti_basarili() {
        Account hesap = hesapAc("STANDARD", 500.0);
        assertEquals(0.01, accountService.withdraw(hesap.getAccountNumber(), 499.99).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("BVA-10 withdraw: STANDARD günlük toplam tam 5.000,00 TL kabul edilir")
    void withdraw_standartGunlukLimitTam_basarili() {
        // Bölüm 3: (bugünkü toplam + yeni) <= limit
        Account hesap = hesapAc("STANDARD", 9_000.0);
        accountService.withdraw(hesap.getAccountNumber(), 3_000.0);
        assertEquals(4_000.0, accountService.withdraw(hesap.getAccountNumber(), 2_000.0).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("BVA-11 withdraw: STANDARD günlük toplam 5.000,01 TL reddedilir")
    void withdraw_standartGunlukLimitAsimi_reddedilmeli() {
        Account hesap = hesapAc("STANDARD", 9_000.0);
        accountService.withdraw(hesap.getAccountNumber(), 3_000.0);
        assertThrows(LimitExceededException.class,
                () -> accountService.withdraw(hesap.getAccountNumber(), 2_000.01));
    }

    @Tag("hata")
    @Test
    @DisplayName("BVA-12 withdraw: PREMIUM günlük toplam tam 20.000,00 TL kabul edilmeli")
    void withdraw_premiumGunlukLimitTam_basarili() {
        // Bölüm 3: PREMIUM hesaptan bir gün içinde toplam tam 20.000,00 TL çekilebilir. Rapor: H-05
        Account hesap = hesapAc("PREMIUM", 30_000.0);
        Account sonuc = accountService.withdraw(hesap.getAccountNumber(), 20_000.0);
        assertEquals(10_000.0, sonuc.getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("BVA-13 withdraw: PREMIUM günlük toplam 20.000,01 TL reddedilir")
    void withdraw_premiumGunlukLimitAsimi_reddedilmeli() {
        Account hesap = hesapAc("PREMIUM", 30_000.0);
        assertThrows(LimitExceededException.class,
                () -> accountService.withdraw(hesap.getAccountNumber(), 20_000.01));
    }

    @Test
    @DisplayName("BVA-14 createAccount: başlangıç bakiyesi sınırı 0 kabul edilir")
    void createAccount_sifirBakiye_kabulEdilir() {
        // R-1.4: 0 ve pozitif değerler kabul edilir
        assertEquals(0.0, hesapAc("STANDARD", 0.0).getBalance(), TOLERANS);
    }

    @Tag("hata")
    @Test
    @DisplayName("BVA-15 createAccount: başlangıç bakiyesi -0,01 reddedilmeli")
    void createAccount_eksiBirKurus_reddedilmeli() {
        // R-1.4. Rapor: H-01
        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount("Ali", "STANDARD", -0.01));
    }

    // ===================== DURUM GEÇİŞ TESTLERİ (Gereksinimler §4.2 — 11 satır) =====================

    @Test
    @DisplayName("ST-01 ACTIVE --freeze--> FROZEN (bakiye korunur)")
    void hesap_aktiftenDonduruldu() {
        // R-5.2
        Account hesap = hesapAc("STANDARD", 300.0);
        Account sonuc = accountService.freezeAccount(hesap.getAccountNumber());
        assertEquals(AccountStatus.FROZEN, sonuc.getStatus());
        assertEquals(300.0, sonuc.getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("ST-02 ACTIVE --unfreeze--> reddedilir")
    void hesap_aktifHesapCozulemez() {
        // R-6.1
        Account hesap = hesapAc("STANDARD", 300.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.unfreezeAccount(hesap.getAccountNumber()));
        assertEquals(AccountStatus.ACTIVE, guncel(hesap).getStatus());
    }

    @Test
    @DisplayName("ST-03 ACTIVE --close [bakiye=0]--> CLOSED")
    void hesap_aktiftenKapali() {
        // R-7.3
        Account hesap = hesapAc("STANDARD", 0.0);
        assertEquals(AccountStatus.CLOSED, accountService.closeAccount(hesap.getAccountNumber()).getStatus());
    }

    @Tag("hata")
    @Test
    @DisplayName("ST-04 ACTIVE --close [bakiye≠0]--> reddedilmeli")
    void hesap_aktifBakiyeliKapatma_reddedilmeli() {
        // R-7.2: bakiyesi olan hesap kapatılamaz. Rapor: H-10
        Account hesap = hesapAc("STANDARD", 750.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.closeAccount(hesap.getAccountNumber()));
        assertEquals(AccountStatus.ACTIVE, guncel(hesap).getStatus());
    }

    @Tag("hata")
    @Test
    @DisplayName("ST-05 FROZEN --freeze--> reddedilmeli (zaten dondurulmuş)")
    void hesap_dondurulmusTekrarDondurma_reddedilmeli() {
        // R-5.1. Rapor: H-09
        Account hesap = dondurulmusHesap(100.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.freezeAccount(hesap.getAccountNumber()));
    }

    @Test
    @DisplayName("ST-06 FROZEN --unfreeze--> ACTIVE")
    void hesap_donduruldutanAktife() {
        // R-6.2
        Account hesap = dondurulmusHesap(100.0);
        Account sonuc = accountService.unfreezeAccount(hesap.getAccountNumber());
        assertEquals(AccountStatus.ACTIVE, sonuc.getStatus());
        assertEquals(100.0, sonuc.getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("ST-07 FROZEN --close [bakiye=0]--> CLOSED")
    void hesap_dondurulmusHesapKapatma_bakiyeSifirsa() {
        Account hesap = dondurulmusHesap(0.0);
        assertEquals(AccountStatus.CLOSED, accountService.closeAccount(hesap.getAccountNumber()).getStatus());
    }

    @Tag("hata")
    @Test
    @DisplayName("ST-08 FROZEN --close [bakiye≠0]--> reddedilmeli")
    void hesap_dondurulmusBakiyeliKapatma_reddedilmeli() {
        // R-7.2. Rapor: H-10
        Account hesap = dondurulmusHesap(400.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.closeAccount(hesap.getAccountNumber()));
        assertEquals(AccountStatus.FROZEN, guncel(hesap).getStatus());
    }

    @Test
    @DisplayName("ST-09 CLOSED --freeze--> reddedilir")
    void hesap_kapaliHesapDondurulamaz() {
        Account hesap = kapaliHesap();
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.freezeAccount(hesap.getAccountNumber()));
    }

    @Test
    @DisplayName("ST-10 CLOSED --unfreeze--> reddedilir (kapalı hesap tekrar açılamaz)")
    void hesap_kapaliHesapYenidenAcilamaz() {
        // R-7.4
        Account hesap = kapaliHesap();
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.unfreezeAccount(hesap.getAccountNumber()));
        assertEquals(AccountStatus.CLOSED, guncel(hesap).getStatus());
    }

    @Test
    @DisplayName("ST-11 CLOSED --close--> reddedilir (zaten kapalı)")
    void hesap_kapaliHesapTekrarKapatilamaz() {
        Account hesap = kapaliHesap();
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.closeAccount(hesap.getAccountNumber()));
    }

    // ===================== KARAR TABLOSU — Transfer kuralları =====================
    // Koşullar: kaynak ACTIVE? / hedef ACTIVE? / aynı hesap? / bakiye yeterli?

    @Test
    @DisplayName("DT-01 transfer: K1 (A, A, farklı, yeterli) -> başarılı, 2 TRANSFER kaydı")
    void transfer_ikisiDeAktif_basarili() {
        // R-4.6, R-4.7
        Account kaynak = hesapAc("STANDARD", 1000.0);
        Account hedef = hesapAc("STANDARD", 200.0);
        accountService.transfer(kaynak.getAccountNumber(), hedef.getAccountNumber(), 300.0);
        assertEquals(700.0, guncel(kaynak).getBalance(), TOLERANS);
        assertEquals(500.0, guncel(hedef).getBalance(), TOLERANS);
        assertEquals(TransactionType.TRANSFER,
                accountService.getTransactionHistory(kaynak.getAccountNumber()).get(0).getType());
        assertEquals(TransactionType.TRANSFER,
                accountService.getTransactionHistory(hedef.getAccountNumber()).get(0).getType());
    }

    @Test
    @DisplayName("DT-02 transfer: K2 (kaynak FROZEN) -> reddedilir")
    void transfer_kaynakDondurulmus_reddedilmeli() {
        Account kaynak = dondurulmusHesap(1000.0);
        Account hedef = hesapAc("STANDARD", 0.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.transfer(kaynak.getAccountNumber(), hedef.getAccountNumber(), 100.0));
        assertEquals(0.0, guncel(hedef).getBalance(), TOLERANS);
    }

    @Tag("hata")
    @Test
    @DisplayName("DT-03 transfer: K3 (hedef FROZEN) -> reddedilmeli")
    void transfer_hedefDondurulmus_reddedilmeli() {
        // R-4.3: her iki hesap da ACTIVE olmalıdır. Rapor: H-07
        Account kaynak = hesapAc("STANDARD", 1000.0);
        Account hedef = dondurulmusHesap(0.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.transfer(kaynak.getAccountNumber(), hedef.getAccountNumber(), 100.0));
    }

    @Tag("hata")
    @Test
    @DisplayName("DT-04 transfer: K4 (hedef CLOSED) -> reddedilmeli")
    void transfer_hedefKapali_reddedilmeli() {
        // R-4.3. Rapor: H-07
        Account kaynak = hesapAc("STANDARD", 1000.0);
        Account hedef = kapaliHesap();
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.transfer(kaynak.getAccountNumber(), hedef.getAccountNumber(), 100.0));
    }

    @Tag("hata")
    @Test
    @DisplayName("DT-05 transfer: K5 (aynı hesap) -> reddedilmeli")
    void transfer_ayniHesap_reddedilmeli() {
        // R-4.4: fromAccount == toAccount reddedilir (400). Rapor: H-08
        Account hesap = hesapAc("STANDARD", 1000.0);
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transfer(hesap.getAccountNumber(), hesap.getAccountNumber(), 100.0));
    }

    @Test
    @DisplayName("DT-06 transfer: K6 (bakiye yetersiz) -> reddedilir, atomiklik korunur")
    void transfer_yetersizBakiye_reddedilmeli() {
        // R-4.5, G-5
        Account kaynak = hesapAc("STANDARD", 100.0);
        Account hedef = hesapAc("STANDARD", 0.0);
        assertThrows(InsufficientBalanceException.class,
                () -> accountService.transfer(kaynak.getAccountNumber(), hedef.getAccountNumber(), 100.01));
        assertEquals(100.0, guncel(kaynak).getBalance(), TOLERANS);
        assertEquals(0.0, guncel(hedef).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("DT-07 transfer: K7 (tam bakiye) -> başarılı")
    void transfer_tamBakiye_basarili() {
        // R-4.5: bakiyenin tamamı transfer edilebilir
        Account kaynak = hesapAc("STANDARD", 250.0);
        Account hedef = hesapAc("STANDARD", 0.0);
        accountService.transfer(kaynak.getAccountNumber(), hedef.getAccountNumber(), 250.0);
        assertEquals(0.0, guncel(kaynak).getBalance(), TOLERANS);
        assertEquals(250.0, guncel(hedef).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("DT-08 transfer: K8 (hesaplardan biri yok) -> hesap bulunamadı")
    void transfer_hedefYok_bulunamadi() {
        // R-4.2
        Account kaynak = hesapAc("STANDARD", 250.0);
        assertThrows(AccountNotFoundException.class,
                () -> accountService.transfer(kaynak.getAccountNumber(), "1111111111", 10.0));
        assertEquals(250.0, guncel(kaynak).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("DT-09 transfer: tutar 0 -> reddedilir")
    void transfer_sifirTutar_reddedilmeli() {
        // R-4.1
        Account kaynak = hesapAc("STANDARD", 250.0);
        Account hedef = hesapAc("STANDARD", 0.0);
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transfer(kaynak.getAccountNumber(), hedef.getAccountNumber(), 0.0));
    }

    @Test
    @DisplayName("DT-10 transfer günlük çekim limitine dahil edilmez")
    void transfer_gunlukLimiteDahilDegil() {
        // R-4.8: 4.000 TL transferden sonra aynı gün limitin tamamı (5.000 TL) çekilebilmeli
        Account kaynak = hesapAc("STANDARD", 10_000.0);
        Account hedef = hesapAc("STANDARD", 0.0);
        accountService.transfer(kaynak.getAccountNumber(), hedef.getAccountNumber(), 4_000.0);
        Account sonuc = accountService.withdraw(kaynak.getAccountNumber(), 5_000.0);
        assertNotNull(sonuc);
        assertEquals(1_000.0, sonuc.getBalance(), TOLERANS);
    }
}
