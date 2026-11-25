# Gereksinimler §3 ve §5.3 (R-3.x). @hata etiketli senaryolar kasıtlı bir hatayı yakalar.
Feature: Para Çekme
  Müşteri hesabındaki parayı, bakiye ve günlük limit kuralları çerçevesinde çekebilmeli

  Scenario: Bakiyenin bir kısmı çekilir
    Given STANDARD türünde ve 1000 TL bakiyeli aktif bir hesap var
    When hesaptan 250 TL çekilir
    Then işlem başarılı olmalı
    And hesap bakiyesi 750 TL olmalı
    And işlem geçmişinde WITHDRAWAL kaydı bulunmalı

  Scenario: Bakiyeden fazla para çekilemez
    Given STANDARD türünde ve 1000 TL bakiyeli aktif bir hesap var
    When hesaptan 1000.01 TL çekilir
    Then işlem reddedilmeli
    And red nedeni "yetersiz bakiye" olmalı
    And bakiye değişmemeli

  @hata
  Scenario: Bakiyenin tamamı çekilebilir
    # R-3.4 — Rapor: H-04
    Given STANDARD türünde ve 1000 TL bakiyeli aktif bir hesap var
    When hesaptan 1000 TL çekilir
    Then işlem başarılı olmalı
    And hesap bakiyesi 0 TL olmalı

  @hata
  Scenario: Dondurulmuş hesaptan para çekilemez
    # R-3.3 — Rapor: H-03
    Given STANDARD türünde ve 1000 TL bakiyeli aktif bir hesap var
    And hesap dondurulmuş
    When hesaptan 100 TL çekilir
    Then işlem reddedilmeli
    And red nedeni "hesap aktif değil" olmalı
    And bakiye değişmemeli

  Scenario Outline: Günlük çekim limiti hesap türüne göre uygulanır
    Given <tur> türünde ve 50000 TL bakiyeli aktif bir hesap var
    When aynı gün içinde önce <ilk> TL, ardından <ikinci> TL çekilir
    Then <sonuc>

    Examples: Limit içinde ve limit aşımı
      | tur      | ilk   | ikinci   | sonuc                 |
      | STANDARD | 3000  | 2000     | işlem başarılı olmalı |
      | STANDARD | 3000  | 2000.01  | işlem reddedilmeli    |
      | PREMIUM  | 15000 | 5000.01  | işlem reddedilmeli    |
      | PREMIUM  | 10000 | 9999.99  | işlem başarılı olmalı |

    @hata
    Examples: PREMIUM günlük limitin tam sınırı (Bölüm 3, Rapor: H-05)
      | tur     | ilk   | ikinci | sonuc                 |
      | PREMIUM | 15000 | 5000   | işlem başarılı olmalı |
