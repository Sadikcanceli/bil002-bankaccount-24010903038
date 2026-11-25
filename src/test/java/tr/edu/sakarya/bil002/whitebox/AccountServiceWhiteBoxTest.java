package tr.edu.sakarya.bil002.whitebox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import tr.edu.sakarya.bil002.exception.AccountNotFoundException;
import tr.edu.sakarya.bil002.exception.InsufficientBalanceException;
import tr.edu.sakarya.bil002.exception.InvalidAccountStateException;
import tr.edu.sakarya.bil002.exception.LimitExceededException;
import tr.edu.sakarya.bil002.model.Account;
import tr.edu.sakarya.bil002.model.AccountStatus;
import tr.edu.sakarya.bil002.model.Transaction;
import tr.edu.sakarya.bil002.model.TransactionStatus;
import tr.edu.sakarya.bil002.model.TransactionType;
import tr.edu.sakarya.bil002.repository.TransactionRepository;
import tr.edu.sakarya.bil002.service.AccountService;
import tr.edu.sakarya.bil002.service.TransactionService;

/**
 * Beyaz kutu testleri: AccountService ve TransactionService kaynak kodu incelenerek her if/else dalı
 * ve her koşul için yazılmıştır. Test ID'leri (WB-xx) rapor Bölüm 3'teki dal tablosuyla eşleşir.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountServiceWhiteBoxTest {

    private static final double TOLERANS = 0.001;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    private Account hesapAc(String tur, double bakiye) {
        return accountService.createAccount("Beyaz Kutu Musteri", tur, bakiye);
    }

    private Account guncel(Account hesap) {
        return accountService.getAccount(hesap.getAccountNumber());
    }

    /** Geçmiş tarihli (ör. dün) bir işlem kaydını doğrudan veritabanına ekler: zamanı kontrol etmenin yolu. */
    private void gecmisIslemEkle(Account hesap, TransactionType tip, double tutar, TransactionStatus durum,
                                 LocalDateTime zaman) {
        Transaction t = new Transaction(hesap.getAccountNumber(), tip, tutar, hesap.getBalance(), "test verisi",
                durum);
        t.setTimestamp(zaman);
        transactionRepository.save(t);
    }

    // ===================== calculateInterest =====================

    @Tag("hata")
    @Test
    @DisplayName("WB-01 calculateInterest: STANDARD dalı, kuruşlu bakiye 1.000,99 -> faiz 2,50")
    void calculateInterest_standartHesap_dogruFaiz() {
        // Dal: PREMIUM.equals(type) == false -> oran 3.
        // R-8.3: 1000,99 x 0,03 / 12 = 2,5025 -> 2,50. Kod bakiyeyi longValue() ile kesiyor ve tam sayı
        // bölmesi yapıyor (1000 * 3 / 100 / 12 = 2). Rapor: H-11
        Account hesap = hesapAc("STANDARD", 1000.99);
        Account sonuc = accountService.calculateInterest(hesap.getAccountNumber());
        assertEquals(1003.49, sonuc.getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("WB-02 calculateInterest: PREMIUM dalı, 2.400 TL -> faiz 10,00 (tam bölünen değer)")
    void calculateInterest_premiumHesap_dogruFaiz() {
        // Dal: PREMIUM.equals(type) == true -> oran 5. 2400 x 0,05 / 12 = 10 (kesme etkisiz)
        Account hesap = hesapAc("PREMIUM", 2400.0);
        Account sonuc = accountService.calculateInterest(hesap.getAccountNumber());
        assertEquals(2410.0, sonuc.getBalance(), TOLERANS);
        Transaction kayit = transactionService.getHistory(hesap.getAccountNumber()).get(0);
        assertEquals(TransactionType.INTEREST, kayit.getType());
        assertEquals(TransactionStatus.SUCCESS, kayit.getStatus());
        assertEquals(2410.0, kayit.getBalanceAfter(), TOLERANS);
    }

    @Test
    @DisplayName("WB-03 calculateInterest: sıfır bakiye -> faiz 0")
    void calculateInterest_sifirBakiye() {
        Account hesap = hesapAc("STANDARD", 0.0);
        assertEquals(0.0, accountService.calculateInterest(hesap.getAccountNumber()).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("WB-04 calculateInterest: requireActive false dalı (CLOSED)")
    void calculateInterest_kapaliHesap_reddedilir() {
        Account hesap = hesapAc("STANDARD", 0.0);
        accountService.closeAccount(hesap.getAccountNumber());
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.calculateInterest(hesap.getAccountNumber()));
    }

    // ===================== withdraw / checkDailyWithdrawalLimit =====================

    @Test
    @DisplayName("WB-05 checkDailyWithdrawalLimit: STANDARD dalı, total > 5000 -> limit aşımı")
    void withdraw_gunlukLimitKontrol_standart() {
        Account hesap = hesapAc("STANDARD", 8000.0);
        accountService.withdraw(hesap.getAccountNumber(), 4000.0);
        assertThrows(LimitExceededException.class, () -> accountService.withdraw(hesap.getAccountNumber(), 1500.0));
        assertEquals(4000.0, guncel(hesap).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("WB-06 checkDailyWithdrawalLimit: PREMIUM dalı, limit altı kabul / üstü red")
    void withdraw_gunlukLimitKontrol_premium() {
        Account hesap = hesapAc("PREMIUM", 40_000.0);
        accountService.withdraw(hesap.getAccountNumber(), 15_000.0);
        assertThrows(LimitExceededException.class, () -> accountService.withdraw(hesap.getAccountNumber(), 6000.0));
        assertEquals(25_000.0, guncel(hesap).getBalance(), TOLERANS);
    }

    @Tag("hata")
    @Test
    @DisplayName("WB-07 günlük limit: önceki günün çekimleri bugünün limitine dahil edilmemeli")
    void withdraw_dunkuCekimler_bugununLimitineSayilmamali() {
        // Bölüm 3 / G-6: yalnızca o gün yapılmış SUCCESS WITHDRAWAL'lar toplanır, her gün limit sıfırlanır.
        // getDailyWithdrawalTotal tarih filtresi uygulamıyor. Rapor: H-06
        Account hesap = hesapAc("STANDARD", 10_000.0);
        gecmisIslemEkle(hesap, TransactionType.WITHDRAWAL, 4500.0, TransactionStatus.SUCCESS,
                LocalDateTime.now().minusDays(1));
        Account sonuc = accountService.withdraw(hesap.getAccountNumber(), 2000.0);
        assertEquals(8000.0, sonuc.getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("WB-08 günlük limit: FAILED çekim denemeleri limite dahil edilmez")
    void withdraw_failedDenemeler_limiteSayilmaz() {
        // Bölüm 6 dipnot: FAILED kayıtlar günlük limit hesabına dahil edilmez
        Account hesap = hesapAc("STANDARD", 10_000.0);
        gecmisIslemEkle(hesap, TransactionType.WITHDRAWAL, 5000.0, TransactionStatus.FAILED, LocalDateTime.now());
        assertEquals(5000.0, accountService.withdraw(hesap.getAccountNumber(), 5000.0).getBalance(), TOLERANS);
    }

    @Test
    @DisplayName("WB-09 withdraw: amount == null dalı")
    void withdraw_nullTutar_reddedilir() {
        Account hesap = hesapAc("STANDARD", 100.0);
        assertThrows(IllegalArgumentException.class, () -> accountService.withdraw(hesap.getAccountNumber(), null));
    }

    @Test
    @DisplayName("WB-10 ensureSufficientBalance: amount > bakiye dalı")
    void withdraw_yetersizBakiye() {
        Account hesap = hesapAc("STANDARD", 100.0);
        assertThrows(InsufficientBalanceException.class,
                () -> accountService.withdraw(hesap.getAccountNumber(), 150.0));
    }

    @Test
    @DisplayName("WB-11 withdraw: başarılı yol, WITHDRAWAL/SUCCESS kaydı ve balanceAfter")
    void withdraw_basariliYol_kayitOlusur() {
        Account hesap = hesapAc("STANDARD", 1000.0);
        accountService.withdraw(hesap.getAccountNumber(), 250.0);
        Transaction kayit = transactionService.getHistory(hesap.getAccountNumber()).get(0);
        assertEquals(TransactionType.WITHDRAWAL, kayit.getType());
        assertEquals(750.0, kayit.getBalanceAfter(), TOLERANS);
    }

    // ===================== deposit =====================

    @Test
    @DisplayName("WB-12 deposit: amount == null dalı")
    void deposit_nullTutar_reddedilir() {
        Account hesap = hesapAc("STANDARD", 100.0);
        assertThrows(IllegalArgumentException.class, () -> accountService.deposit(hesap.getAccountNumber(), null));
    }

    @Test
    @DisplayName("WB-13 checkDepositLimit: PREMIUM dalı, limit üstü")
    void deposit_premiumLimitDali() {
        Account hesap = hesapAc("PREMIUM", 0.0);
        assertThrows(LimitExceededException.class, () -> accountService.deposit(hesap.getAccountNumber(), 60_000.0));
    }

    @Test
    @DisplayName("WB-14 checkDepositLimit: STANDARD dalı, limit altı")
    void deposit_standartLimitAlti() {
        Account hesap = hesapAc("STANDARD", 0.0);
        assertEquals(9_999.99, accountService.deposit(hesap.getAccountNumber(), 9_999.99).getBalance(), TOLERANS);
    }

    // ===================== transfer =====================

    @Test
    @DisplayName("WB-15 transfer: amount == null ve amount <= 0 dalları")
    void transfer_gecersizTutarDallari() {
        Account a = hesapAc("STANDARD", 100.0);
        Account b = hesapAc("STANDARD", 100.0);
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transfer(a.getAccountNumber(), b.getAccountNumber(), null));
        assertThrows(IllegalArgumentException.class,
                () -> accountService.transfer(a.getAccountNumber(), b.getAccountNumber(), -1.0));
    }

    @Test
    @DisplayName("WB-16 transfer: kaynak hesap yok -> getAccount(from) dalı")
    void transfer_kaynakYok() {
        Account b = hesapAc("STANDARD", 100.0);
        assertThrows(AccountNotFoundException.class,
                () -> accountService.transfer("1111111111", b.getAccountNumber(), 10.0));
    }

    @Test
    @DisplayName("WB-17 validateTransferParties: kaynak CLOSED dalı")
    void transfer_kaynakKapali() {
        Account a = hesapAc("STANDARD", 0.0);
        accountService.closeAccount(a.getAccountNumber());
        Account b = hesapAc("STANDARD", 100.0);
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.transfer(a.getAccountNumber(), b.getAccountNumber(), 10.0));
    }

    @Tag("hata")
    @Test
    @DisplayName("WB-18 transfer: hedef FROZEN iken G-4 -> hedef bakiyesi değişmemeli")
    void transfer_hedefDondurulmus_bakiyeDegismemeli() {
        // validateTransferParties yalnızca kaynağın durumunu kontrol ediyor (target için dal yok).
        // R-4.3 + 4.3 tablosu: FROZEN hesap transfer alamaz. Rapor: H-07
        Account a = hesapAc("STANDARD", 500.0);
        Account b = hesapAc("STANDARD", 0.0);
        accountService.freezeAccount(b.getAccountNumber());
        try {
            accountService.transfer(a.getAccountNumber(), b.getAccountNumber(), 200.0);
        } catch (InvalidAccountStateException beklenen) {
            // beklenen davranış
        }
        assertEquals(0.0, guncel(b).getBalance(), TOLERANS);
        assertEquals(500.0, guncel(a).getBalance(), TOLERANS);
    }

    // ===================== freeze / unfreeze / close =====================

    @Test
    @DisplayName("WB-19 freezeAccount: status == CLOSED dalı")
    void freeze_kapaliDal() {
        Account hesap = hesapAc("STANDARD", 0.0);
        accountService.closeAccount(hesap.getAccountNumber());
        assertThrows(InvalidAccountStateException.class, () -> accountService.freezeAccount(hesap.getAccountNumber()));
    }

    @Test
    @DisplayName("WB-20 unfreezeAccount: status != FROZEN dalı (CLOSED)")
    void unfreeze_kapaliDal() {
        Account hesap = hesapAc("STANDARD", 0.0);
        accountService.closeAccount(hesap.getAccountNumber());
        assertThrows(InvalidAccountStateException.class,
                () -> accountService.unfreezeAccount(hesap.getAccountNumber()));
    }

    @Test
    @DisplayName("WB-21 closeAccount: status == CLOSED dalı")
    void close_zatenKapaliDal() {
        Account hesap = hesapAc("STANDARD", 0.0);
        accountService.closeAccount(hesap.getAccountNumber());
        assertThrows(InvalidAccountStateException.class, () -> accountService.closeAccount(hesap.getAccountNumber()));
        assertEquals(AccountStatus.CLOSED, guncel(hesap).getStatus());
    }

    @Test
    @DisplayName("WB-22 closeAccount: kapatma işlemi Transaction kaydı oluşturmaz")
    void close_islemKaydiOlusmaz() {
        // Bölüm 6: dondurma / çözme / kapatma için kayıt oluşmaz
        Account hesap = hesapAc("STANDARD", 0.0);
        accountService.freezeAccount(hesap.getAccountNumber());
        accountService.unfreezeAccount(hesap.getAccountNumber());
        accountService.closeAccount(hesap.getAccountNumber());
        assertTrue(transactionService.getHistory(hesap.getAccountNumber()).isEmpty());
    }

    // ===================== createAccount / sorgulama =====================

    @Test
    @DisplayName("WB-23 createAccount: ad başındaki/sonundaki boşluklar kırpılır, PREMIUM dalı")
    void createAccount_adKirpilir() {
        Account hesap = accountService.createAccount("  Ayşe  ", "PREMIUM", 10.0);
        assertEquals("Ayşe", hesap.getOwnerName());
    }

    @Test
    @DisplayName("WB-24 findAccount / getAccount: bulunan ve bulunmayan dallar")
    void sorgulama_dallari() {
        Account hesap = hesapAc("STANDARD", 10.0);
        assertTrue(accountService.findAccount(hesap.getAccountNumber()).isPresent());
        assertTrue(accountService.findAccount("1111111111").isEmpty());
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount("1111111111"));
        assertThrows(AccountNotFoundException.class, () -> accountService.getTransactionHistory("1111111111"));
        assertTrue(accountService.getAllAccounts().size() >= 1);
    }

    @Test
    @DisplayName("WB-25 getHistory: tarihe göre azalan sıralama (en yeni ilk)")
    void gecmis_azalanSiralama() {
        // R-9.3
        Account hesap = hesapAc("STANDARD", 0.0);
        gecmisIslemEkle(hesap, TransactionType.DEPOSIT, 1.0, TransactionStatus.SUCCESS,
                LocalDateTime.now().minusDays(2));
        accountService.deposit(hesap.getAccountNumber(), 5.0);
        var gecmis = accountService.getTransactionHistory(hesap.getAccountNumber());
        assertEquals(5.0, gecmis.get(0).getAmount(), TOLERANS);
        assertEquals(1.0, gecmis.get(1).getAmount(), TOLERANS);
    }

    @Tag("hata")
    @Test
    @DisplayName("WB-26 generateAccountNumber: aynı rastgele değer üretilirse numara yine benzersiz olmalı")
    void hesapNo_cakismadaBenzersizOlmali() throws Exception {
        // R-1.5: hiçbir koşulda iki hesap aynı numarayı almamalı. generateAccountNumber() çakışma kontrolü
        // yapmıyor ve accountNumber sütununda unique kısıtı yok. Rastgele üreteci sabit değer döndüren bir
        // test dublörüyle değiştirerek çakışma durumunu zorluyoruz. Rapor: H-14
        Field alan = AccountService.class.getDeclaredField("random");
        alan.setAccessible(true);
        AccountService hedef = AopTestUtils.getTargetObject(accountService); // Spring proxy'sinin arkasındaki nesne
        Object orijinal = alan.get(hedef);
        alan.set(hedef, new SabitRandom());
        try {
            Account a = hesapAc("STANDARD", 1.0);
            Account b;
            try {
                b = hesapAc("STANDARD", 1.0);
            } catch (RuntimeException ret) {
                return; // sistem çakışmayı fark edip reddettiyse kural ihlali yok
            }
            assertNotEquals(a.getAccountNumber(), b.getAccountNumber(), "İki hesap aynı numarayı aldı");
        } finally {
            alan.set(hedef, orijinal);
        }
    }

    /** Her çağrıda aynı sayıyı üreten Random: hesap numarası çakışmasını deterministik hale getirir. */
    private static final class SabitRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        public long nextLong(long bound) {
            return 123_456_789L;
        }
    }
}
