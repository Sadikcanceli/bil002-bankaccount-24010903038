# Gereksinimler §4.2 durum geçiş tablosu ve §5.5–5.7 (R-5.x, R-6.x, R-7.x).
Feature: Hesap Durumu Yönetimi
  Banka, hesapları dondurabilmeli, dondurmayı kaldırabilmeli ve bakiyesi sıfır olan hesapları kapatabilmeli

  Scenario Outline: İzin verilen durum geçişleri
    Given durumu <baslangic> olan ve bakiyesi <bakiye> TL olan bir hesap var
    When hesap için "<islem>" işlemi istenir
    Then işlem başarılı olmalı
    And hesabın durumu <yeni> olmalı
    And bakiye değişmemeli

    Examples:
      | baslangic | bakiye | islem              | yeni   |
      | ACTIVE    | 300    | dondurma           | FROZEN |
      | FROZEN    | 300    | dondurmayı kaldırma | ACTIVE |
      | ACTIVE    | 0      | kapatma            | CLOSED |
      | FROZEN    | 0      | kapatma            | CLOSED |

  Scenario Outline: İzin verilmeyen durum geçişleri reddedilir
    Given durumu <baslangic> olan ve bakiyesi <bakiye> TL olan bir hesap var
    When hesap için "<islem>" işlemi istenir
    Then işlem reddedilmeli
    And red nedeni "hesap aktif değil" olmalı
    And hesabın durumu <baslangic> olmalı

    Examples: Doğru şekilde reddedilen geçişler
      | baslangic | bakiye | islem              |
      | ACTIVE    | 300    | dondurmayı kaldırma |
      | CLOSED    | 0      | dondurma           |
      | CLOSED    | 0      | dondurmayı kaldırma |
      | CLOSED    | 0      | kapatma            |

    @hata
    Examples: Uygulamanın kabul ettiği yasak geçişler (R-5.1, R-7.2 — Rapor: H-09, H-10)
      | baslangic | bakiye | islem    |
      | FROZEN    | 300    | dondurma |
      | ACTIVE    | 300    | kapatma  |
      | FROZEN    | 300    | kapatma  |
