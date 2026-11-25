# Gereksinimler §5.9 (R-9.x) ve §6 işlem kaydı tablosu.
Feature: İşlem Geçmişi
  Müşteri hesabındaki başarılı ve reddedilen tüm işlemleri en yeniden eskiye görebilmeli

  Scenario: İşlemler en yeniden eskiye listelenir
    Given STANDARD türünde ve 1000 TL bakiyeli aktif bir hesap var
    When hesaba 500 TL yatırılır
    And hesaptan 200 TL çekilir
    Then işlem geçmişinde 2 kayıt olmalı ve en yeni kayıt WITHDRAWAL olmalı

  Scenario: Dondurulmuş hesabın geçmişi de sorgulanabilir
    Given STANDARD türünde ve 1000 TL bakiyeli aktif bir hesap var
    And hesaba 100 TL yatırılır
    And hesap dondurulmuş
    Then işlem geçmişinde 1 kayıt olmalı ve en yeni kayıt DEPOSIT olmalı

  @hata
  Scenario: Reddedilen para yatırma denemesi FAILED olarak kaydedilir
    # R-2.7, §6 — Rapor: H-12
    Given STANDARD türünde ve 1000 TL bakiyeli aktif bir hesap var
    When hesaba 10000.01 TL yatırılmak istenir
    Then işlem reddedilmeli
    And işlem geçmişinde DEPOSIT tipinde FAILED durumunda bir kayıt bulunmalı

  @hata
  Scenario: Reddedilen para çekme denemesi FAILED olarak kaydedilir
    # R-3.8, §6 — Rapor: H-12
    Given STANDARD türünde ve 1000 TL bakiyeli aktif bir hesap var
    When hesaptan 5000 TL çekilir
    Then işlem reddedilmeli
    And işlem geçmişinde WITHDRAWAL tipinde FAILED durumunda bir kayıt bulunmalı
