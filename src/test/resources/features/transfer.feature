# Gereksinimler §5.4 (R-4.x) ve G-5 atomiklik kuralı.
Feature: Hesaplar Arası Transfer
  Müşteri kendi hesabından başka bir aktif hesaba para gönderebilmeli

  Background:
    Given "Ayşe" adına 1000 TL bakiyeli aktif bir hesap var
    And "Mehmet" adına 200 TL bakiyeli aktif bir hesap var

  Scenario: İki aktif hesap arasında transfer
    When "Ayşe" hesabından "Mehmet" hesabına 300 TL transfer edilir
    Then işlem başarılı olmalı
    And "Ayşe" adlı müşterinin bakiyesi 700 TL olmalı
    And "Mehmet" adlı müşterinin bakiyesi 500 TL olmalı

  Scenario: Yetersiz bakiyede transfer yapılmaz ve hiçbir hesap değişmez
    When "Ayşe" hesabından "Mehmet" hesabına 1000.01 TL transfer edilir
    Then işlem reddedilmeli
    And red nedeni "yetersiz bakiye" olmalı
    And "Ayşe" ve "Mehmet" hesaplarının toplam bakiyesi 1200 TL olmalı
    And "Mehmet" adlı müşterinin bakiyesi 200 TL olmalı

  Scenario Outline: Geçersiz tutarla transfer reddedilir
    When "Ayşe" hesabından "Mehmet" hesabına <tutar> TL transfer edilir
    Then işlem reddedilmeli
    And red nedeni "geçersiz girdi" olmalı

    Examples:
      | tutar |
      | 0     |
      | -50   |

  Scenario: Gönderen hesap dondurulmuşsa transfer reddedilir
    Given "Ayşe" adlı müşterinin hesabı dondurulmuş
    When "Ayşe" hesabından "Mehmet" hesabına 100 TL transfer edilir
    Then işlem reddedilmeli
    And red nedeni "hesap aktif değil" olmalı

  @hata
  Scenario: Alıcı hesap dondurulmuşsa transfer reddedilir
    # R-4.3 — Rapor: H-07
    Given "Mehmet" adlı müşterinin hesabı dondurulmuş
    When "Ayşe" hesabından "Mehmet" hesabına 100 TL transfer edilir
    Then işlem reddedilmeli
    And "Mehmet" adlı müşterinin bakiyesi 200 TL olmalı

  @hata
  Scenario: Müşteri kendi hesabına transfer yapamaz
    # R-4.4 — Rapor: H-08
    When "Ayşe" kendi hesabına 100 TL transfer etmeye çalışır
    Then işlem reddedilmeli
    And red nedeni "geçersiz girdi" olmalı
