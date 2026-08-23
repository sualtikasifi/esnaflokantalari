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

## Lokanta fotoğrafları

Fotoğrafların iki kaynağı var:

| Kaynak | Nerede saklanır | Kim görür |
|---|---|---|
| Uygulamadan eklenen | Sadece o telefonda | Yalnızca o kullanıcı |
| `assets/photos/` içine gömülen | Uygulamanın içinde | **Herkes** |

### Kendi çektiğin fotoğrafları herkese göstermek

1. **Telefonda:** Lokanta detayına gir, görselin sağ alt köşesindeki kamera
   ikonuyla galeriden fotoğraf seç. (İstediğin kadar lokantaya ekle.)
2. **Dışa aktar:** Ana sayfada sağ üstteki ⋮ menüsünden
   **"Fotoğrafları dışa aktar"** → paylaşım menüsünden kendine gönder
   (e-posta, Drive, WhatsApp fark etmez). Tek bir `.zip` dosyası gelir.
3. **Bilgisayarda:** Zip'i aç, içindeki `.jpg` dosyalarını
   `app/src/main/assets/photos/` klasörüne kopyala.
   Dosya adları lokanta kimliğidir (`adana-kaya-kebap.jpg` gibi) — değiştirme.
4. Uygulamayı yeniden derle. Artık o fotoğraflar herkeste görünür.

Öncelik sırası: kullanıcının kendi eklediği fotoğraf → gömülü fotoğraf →
üretilen renkli kapak.

⚠️ Fotoğraflar APK boyutunu büyütür. Her fotoğraf ~200-400 KB; 50 fotoğraf
uygulamayı ~15-20 MB büyütür. En çok gidilen mekanlarla başlamak mantıklı.

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

## Google Play'e yayınlama

### 1. İmza anahtarını oluştur (bir kez)

⚠️ **Bu anahtarı kaybedersen uygulamayı bir daha güncelleyemezsin.** Yedeğini
güvenli bir yerde (parola yöneticisi, bulut yedeği) sakla.

```bash
keytool -genkeypair -v \
  -keystore esnaflokantalari-release.keystore \
  -alias esnaflokantalari \
  -keyalg RSA -keysize 2048 -validity 10000
```

Komut sana bir parola soracak ve birkaç bilgi isteyecek. Ardından
`keystore.properties.example` dosyasını `keystore.properties` olarak kopyalayıp
parolaları yaz. Bu dosya `.gitignore`'da, GitHub'a gitmez.

### 2. Yükleme paketini üret

```bash
./gradlew bundleRelease
```

Çıktı: `app/build/outputs/bundle/release/app-release.aab` — Play Console'a
yüklenecek dosya budur (APK değil).

### 3. Play Console'da yapılacaklar

`store/` klasöründe hazır olanlar:

| Dosya | Ne için |
|---|---|
| `icon-512.png` | Uygulama simgesi (zorunlu) |
| `feature-graphic.png` | Öne çıkan görsel (zorunlu) |
| `magaza-metinleri.md` | Başlık, açıklamalar, form cevapları |
| `gizlilik-politikasi.md` | Gizlilik politikası metni |

Senin yapman gerekenler:

1. [Play Console](https://play.google.com/console) hesabı aç (tek seferlik 25 USD)
2. Gizlilik politikasını bir web adresinde yayımla (yöntem `magaza-metinleri.md` içinde)
3. Telefonundan en az 2 ekran görüntüsü al
4. `magaza-metinleri.md` içindeki metinleri ve form cevaplarını kopyala
5. `.aab` dosyasını yükle

### Sürüm numarası

Her yeni yüklemede `app/build.gradle.kts` içindeki `versionCode` bir artmalı
(1, 2, 3...). `versionName` kullanıcıya görünen sürümdür ("1.0.0", "1.1.0").
