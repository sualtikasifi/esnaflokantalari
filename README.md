# Esnaf Lokantaları (Android)

[esnaflokantalari.com](https://esnaflokantalari.com/) sitesinden ilham alan, Android için geliştirilen mobil uygulama.

## Bu uygulamanın siteden farkı ne olacak?

- **Sadece il başına 3 mekan değil**, gerçek zamanlı Google Haritalar verisiyle daha geniş bir liste
- **Yakınımdaki Lokantalar** — telefonun konumuna göre en yakın esnaf lokantalarını gösterir
- **Favorilere ekleme** — beğendiğin lokantalar telefonuna kalıcı olarak kaydedilir (uygulamayı kapatsan bile silinmez)
- **Zengin lokanta profili** — kategori, adres, puan, mesafe, yol tarifi butonu
- İleride: foto galerisi, bildirimler, daha gelişmiş filtreleme

## Şu anki durum

- Ana sayfa → şehir listesi, favoriler, "yakınımda" girişleri
- Şehir sayfası → Google Haritalar'dan çekilen lokantalar (API anahtarı yoksa örnek veriyle çalışır)
- Yakınımdaki Lokantalar → konum izni isteyip GPS'e göre en yakın lokantaları listeler
- Lokanta detay sayfası → puan, adres, mesafe, favori ekle/çıkar, "Yol Tarifi Al" butonu (Google Haritalar'ı açar)
- Favorilerim sayfası → kalıcı olarak saklanan favoriler

## Kurulum

### 1. Android Studio'yu indir

[developer.android.com/studio](https://developer.android.com/studio) adresinden indirip kur, sonra bu klasörü "Open" ile aç.

### 2. Google API anahtarı al (gerçek lokanta verisi için)

Bu adım olmadan da uygulama çalışır, ama örnek (sahte) verilerle gösterim yapar. Gerçek Google Haritalar verisini görmek için:

1. [Google Cloud Console](https://console.cloud.google.com/) üzerinde ücretsiz bir proje oluştur.
2. **Places API** servisini etkinleştir (Google küçük bir ücretsiz kullanım kotası sunar, kredi kartı istenir ama başlangıç kotasında ücret alınmaz).
3. **API anahtarı (API key)** oluştur — bu, uygulamanın Google'a "ben yetkiliyim" demesini sağlayan bir şifre gibidir.
4. Proje klasöründeki `local.properties.example` dosyasını `local.properties` olarak kopyala.
5. İçine oluşturduğun anahtarı şu şekilde yapıştır:
   ```
   MAPS_API_KEY=senin-anahtarin
   ```
6. `local.properties` dosyası `.gitignore` içinde olduğu için GitHub'a asla yüklenmez — anahtarın güvende kalır.

### 3. Çalıştır

Android Studio'nun üstündeki ▶️ (Run) butonuna basarak bir emülatörde veya telefonunda çalıştırabilirsin.

## Teknik detaylar

- Dil: Kotlin
- Arayüz: Jetpack Compose (Material 3)
- Ağ katmanı: Retrofit + Google Places API
- Kalıcı depolama: Jetpack DataStore (favoriler için)
- Konum: Google Play Services Location (Fused Location Provider)
- Minimum Android sürümü: Android 7.0 (API 24)

## Tasarım

Google Stitch veya benzeri bir araçla hazırlanan tasarımlar bu uygulamaya birebir uygulanabilir — ekran görüntülerini veya export edilen dosyaları paylaşman yeterli.
