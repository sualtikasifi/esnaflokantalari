# Esnaf Lokantaları (Android)

Türkiye'nin 81 ilindeki esnaf lokantalarını gösteren, **tamamen ücretsiz ve
internetsiz çalışan** Android uygulaması.

## Nasıl çalışıyor?

Uygulama hiçbir API'ye bağlanmaz, hiçbir sunucu kullanmaz, hiçbir ücret
doğurmaz. Tüm lokanta bilgileri uygulamanın içine gömülüdür
(`app/src/main/assets/restaurants.json`).

Sonuçları:

- **Sıfır maliyet** — API anahtarı, kota, fatura yok
- **İnternetsiz çalışır** — uçakta, yolda, çekmeyen yerde tam çalışır
- **Anında açılır** — bekleme, dönen çark yok
- **Gizlilik** — konum cihazdan çıkmaz, hiçbir veri toplanmaz

## Özellikler

- **Ana sayfa** — saate göre selamlama, öne çıkan lokantalar galerisi,
  "Bugün ne yesem?" rastgele öneri, 81 il kartları
- **Şehir sayfası** — o ildeki lokantalar, etiket filtreleri
- **Yakınımda** — konuma göre en yakın lokantalar (izin isteğe bağlı)
- **Favoriler** — cihazda kalıcı saklanır, uygulama silinmedikçe kaybolmaz
- **Lokanta öner** — her şehirde kullanıcı öneri sayfası
- **Detay** — puan, adres, etiketler, yol tarifi, arama, paylaşma
- Karanlık tema desteği

## Kullanıcı önerileri

Her şehir sayfasının sağ üstündeki **+** ikonuyla kullanıcı lokanta önerebilir.
Öneriler:

1. Cihazda saklanır (sunucu maliyeti olmasın diye)
2. Kullanıcı **Gönder** ikonuna basınca WhatsApp / e-posta / mesaj ile sana ulaşır
3. Sen ayda bir gelen önerileri CSV'ye ekleyip yeni sürüm yayınlarsın

## Veriyi güncelleme (ayda bir yapacağın iş)

Tek yapman gereken bir tabloyu düzenlemek:

**1. `tools/data/restaurants.csv` dosyasını aç** (Excel, Numbers veya Google
E-Tablolar ile açılır) ve satır ekle:

| il | ad | kategori | etiketler | puan | yorum_sayisi | adres | telefon | fiyat | enlem | boylam |
|---|---|---|---|---|---|---|---|---|---|---|
| Ankara | Kebapçı Ali | Kebap Restoranı | Kebap\|Yöresel | 4.7 | 2400 | Ulus, Ankara | 03121234567 | 2 | 39.94 | 32.85 |

- `etiketler`: `|` işaretiyle ayrılır (`Kebap\|Çorba`)
- `fiyat`: 1–4 arası (₺ ile ₺₺₺₺)
- `enlem`/`boylam`: boş bırakılabilir, ama girilirse "Yakınımda" özelliğinde çıkar
- **4.3 puanın altındaki mekanlar otomatik elenir** (sitedeki kalite kuralı)

**2. Betiği çalıştır:**

```bash
python3 tools/build_dataset.py
```

**3. Uygulamayı yeniden derleyip yayınla.**

Şehir listesini değiştirmek istersen `tools/data/cities.csv` dosyasını düzenle.

### Koordinat ve puanı nereden bulurum?

Google Haritalar'da mekanı aç:

- **Puan ve yorum sayısı** sayfada zaten yazıyor
- **Koordinat**: mekana sağ tıkla → çıkan sayı çiftini kopyala (`39.9334, 32.8597`)
- **Adres ve telefon** yine aynı sayfada

Tek seferlik toplu veri çekmek istersen Google Places API'yi bir kez kullanıp
sonucu CSV'ye dökebilirsin — ama uygulama bunu asla çalışma anında yapmaz.

## Geliştirme

```bash
# Veriyi derle
python3 tools/build_dataset.py

# Uygulama simgelerini yeniden üret (gerekirse)
python3 tools/make_icons.py

# Testleri çalıştır
./gradlew testDebugUnitTest

# Kurulabilir dosya üret
./gradlew assembleDebug      # test sürümü
./gradlew assembleRelease    # yayın sürümü (~1,5 MB)
```

Android Studio ile açmak için: klasörü "Open" ile aç, ▶️ butonuna bas.
`local.properties` içinde sadece `sdk.dir` satırı yeterlidir.

## Teknik

- Kotlin + Jetpack Compose (Material 3)
- Veri: gömülü JSON (`org.json` ile okunur, ek kütüphane yok)
- Kalıcı depolama: Jetpack DataStore (favoriler + öneriler)
- Konum: Google Play Services Location
- Min Android 7.0 (API 24) · Hedef Android 15 (API 35)
- Yayın sürümü R8 ile küçültülür (~1,5 MB)

## Yayınlamadan önce

`app/build.gradle.kts` içine imzalama ayarı eklenmeli — Google Play'e
yüklemek için imzalı bir `.aab` gerekiyor. Android Studio'da
**Build → Generate Signed Bundle** ile yapılabilir.
