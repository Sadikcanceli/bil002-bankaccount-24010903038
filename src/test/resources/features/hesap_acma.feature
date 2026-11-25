# Gereksinimler §5.1 (R-1.x). "(yok)" alanın hiç gönderilmediği anlamına gelir.
Feature: Hesap Açma
  Yeni bir müşteri STANDARD veya PREMIUM türünde banka hesabı açabilmeli

  Scenario: Geçerli bilgilerle hesap açılır
    When "Ayşe Yılmaz" adına "PREMIUM" türünde ve "2500" TL başlangıç bakiyesiyle hesap açılmak istenir
    Then işlem başarılı olmalı
    And hesabın durumu ACTIVE olmalı
    And hesap bakiyesi 2500 TL olmalı
    And hesap numarası 10 haneli olmalı ve 0 ile başlamamalı
    And hesabın işlem geçmişi boş olmalı

  Scenario: Başlangıç bakiyesi verilmezse hesap 0 TL ile açılır
    When "Ali Veli" adına "STANDARD" türünde ve "(yok)" TL başlangıç bakiyesiyle hesap açılmak istenir
    Then işlem başarılı olmalı
    And hesap bakiyesi 0 TL olmalı

  Scenario Outline: Geçersiz bilgilerle hesap açılamaz
    When "<ad>" adına "<tur>" türünde ve "<bakiye>" TL başlangıç bakiyesiyle hesap açılmak istenir
    Then işlem reddedilmeli
    And red nedeni "geçersiz girdi" olmalı

    Examples: Geçersiz ad ve tür
      | ad    | tur      | bakiye |
      |       | STANDARD | 100    |
      | (yok) | STANDARD | 100    |
      | Ali   | standard | 100    |
      | Ali   | GOLD     | 100    |
      | Ali   | (yok)    | 100    |

    @hata
    Examples: Negatif başlangıç bakiyesi (R-1.4 — Rapor: H-01)
      | ad  | tur      | bakiye |
      | Ali | STANDARD | -0.01  |
      | Ali | PREMIUM  | -500   |
