# Gereksinimler §5.8 (R-8.x): aylık faiz = bakiye x yıllık oran / 12, kuruşa yuvarlanır (yarım yukarı).
Feature: Aylık Faiz Hesaplama
  Banka aktif hesaplara her ay, hesap türüne göre faiz ekleyebilmeli

  Scenario Outline: Gereksinim belgesindeki örnek faiz hesapları
    Given <tur> türünde ve <bakiye> TL bakiyeli aktif bir hesap var
    When aylık faiz uygulanır
    Then işlem başarılı olmalı
    And hesap bakiyesi <yeni> TL olmalı
    And işlem geçmişinde <faiz> TL tutarında başarılı bir faiz kaydı bulunmalı

    Examples: Tam sayı sonuç veren örnekler
      | tur      | bakiye | faiz | yeni  |
      | STANDARD | 12000  | 30   | 12030 |
      | STANDARD | 0      | 0    | 0     |

    @hata
    Examples: Kuruşlu sonuç veren örnekler (R-8.3 — Rapor: H-11)
      | tur      | bakiye  | faiz  | yeni    |
      | STANDARD | 1000    | 2.50  | 1002.50 |
      | PREMIUM  | 1000    | 4.17  | 1004.17 |
      | PREMIUM  | 2500.50 | 10.42 | 2510.92 |

  Scenario: Dondurulmuş hesaba faiz uygulanmaz
    Given PREMIUM türünde ve 1000 TL bakiyeli aktif bir hesap var
    And hesap dondurulmuş
    When aylık faiz uygulanır
    Then işlem reddedilmeli
    And red nedeni "hesap aktif değil" olmalı
    And bakiye değişmemeli
