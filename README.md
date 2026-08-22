# Esnaf Lokantaları (Android)

[esnaflokantalari.com](https://esnaflokantalari.com/) sitesinden ilham alan, Android için geliştirilen mobil uygulama.

## Bu uygulama sitesinden farkı ne olacak?

- **Sadece il başına 3 mekan değil**, daha geniş ve filtrelenebilir bir liste
- **Favorilere ekleme** — beğendiğin lokantaları kaydet
- **Zengin lokanta profili** — kategori, adres, puan, günün önerisi gibi bilgiler
- İleride: gerçek konum bazlı arama, foto galerisi, bildirimler

## Şu anki durum

Bu ilk sürüm, **örnek (mock) verilerle** çalışan bir iskelet uygulamadır:

- Ana sayfa → şehir listesi
- Şehir sayfası → o şehirdeki lokantalar
- Lokanta detay sayfası → puan, adres, favori ekle/çıkar
- Favorilerim sayfası

Gerçek restoran verileri (Google Haritalar'dan) henüz bağlı değil — bu bir sonraki adım.

## Teknik detaylar

- Dil: Kotlin
- Arayüz: Jetpack Compose (Material 3)
- Minimum Android sürümü: Android 7.0 (API 24)

## Nasıl açılır

1. [Android Studio](https://developer.android.com/studio) indir ve kur.
2. Bu klasörü Android Studio ile aç ("Open" seçeneği).
3. Android Studio gerekli bileşenleri otomatik indirecektir.
4. Üstteki ▶️ (Run) butonuna basarak bir emülatörde veya telefonunda çalıştırabilirsin.
