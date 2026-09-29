# Fikirhane — Android

> Herkesin yazabildiği, okuyabildiği ve tartışabildiği çok konulu bir blog ve fikir paylaşım platformu.

Bu depo Fikirhane'nin **Android uygulamasını** içerir. Web sitesiyle ([FikirhaneWeb](https://github.com/alpkcgl342/FikirhaneWeb)) aynı REST API'yi ([FikirhaneBackend](https://github.com/alpkcgl342/FikirhaneBackend)) kullanır; hesaplar ve içerikler ortaktır.

---

## Teknoloji

| Katman | Teknoloji |
|---|---|
| Dil | Kotlin 2.2 |
| Arayüz | Jetpack Compose, Material 3 |
| Navigasyon | Navigation Compose (tip güvenli rotalar) |
| Ağ | Ktor Client (OkHttp), kotlinx.serialization |
| Mimari | MVVM (ViewModel + StateFlow), manuel bağımlılık yönetimi (`AppContainer`) |
| Test | JUnit 4, kotlinx-coroutines-test, Ktor MockEngine |
| Derleme | Gradle 9.6, Android Gradle Plugin 9.4 |

- **minSdk** 27 (Android 8.1), **targetSdk / compileSdk** 37
- Paket adı: `com.alpkcgl.fikirhane`

---

## Proje Yapısı

```
app/src/main/java/com/alpkcgl/fikirhane/
├── FikirhaneApp.kt            # Application + AppContainer (bağımlılıklar)
├── MainActivity.kt
├── core/
│   └── AuthValidation.kt      # Form kuralları (web ve API ile aynı)
├── data/
│   ├── api/                   # Ktor istemcisi, FikirhaneApi, ApiException
│   ├── model/                 # API modelleri
│   ├── session/               # Oturumun cihazda saklanması
│   └── repository/            # AuthRepository: giriş, kayıt, token yenileme
└── ui/
    ├── theme/                 # Web sitesiyle aynı renkler, serif başlıklar
    ├── components/            # Ortak bileşenler (logo, form alanı, uyarı kutusu)
    ├── navigation/            # AppNavHost: oturum durumuna göre yönlendirme
    ├── auth/                  # Giriş ve kayıt ekranları
    └── home/                  # Ana ekran
```

---

## Kurulum

1. Android Studio ile bu klasörü açın (**File → Open**). Android Studio SDK yolunu `local.properties` dosyasına kendisi yazar.
2. Bir emülatör veya USB hata ayıklaması açık bir telefon seçip **Run ▶** ile çalıştırın.

Komut satırından:

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # Birim testleri
```

### API adresi

Uygulama varsayılan olarak canlı API'yi kullanır: `https://fikirhane-api.vercel.app/api/`.

Yerel backend ile denemek için `local.properties` dosyasına ekleyin (emülatör, bilgisayarın `localhost`'una `10.0.2.2` üzerinden ulaşır):

```properties
API_BASE_URL=http://10.0.2.2:3000/api/
```

Şifresiz (HTTP) bağlantıya yalnızca `10.0.2.2` için izin verilir (`res/xml/network_security_config.xml`). Release derlemesi her zaman canlı adresi kullanır.

---

## Oturum ve Güvenlik

- Giriş ve kayıt API'deki `/auth/login` ve `/auth/register` uç noktalarıyla yapılır (arka planda Supabase Auth).
- Access ve refresh token'ları uygulamaya özel `SharedPreferences`'ta tutulur. Başka uygulamalar erişemez; `allowBackup="false"` olduğu için cihaz yedeğine de girmez.
- `AuthRepository`, token'ın süresi dolmadan 60 saniye önce onu yeniler. Sunucu 401 dönerse bir kez yenileyip tekrar dener. Aynı anda birden fazla istek yenileme gerektirirse yalnızca bir yenileme isteği gider.
- Sunucu yenilemeyi reddederse oturum kapatılır ve uygulama giriş ekranına döner. Ağ hatasında oturum korunur.
- E-posta doğrulaması gerekiyorsa kayıttan sonra giriş ekranına bilgi mesajıyla dönülür. Doğrulanmamış hesapla giriş denenirse "Doğrulama e-postasını tekrar gönder" seçeneği çıkar.
- Hata mesajları sunucudan Türkçe gelir ve doğrudan gösterilir. Token'lar loglara yazılmaz.

---

## Yol Haritası

- [x] **Faz 1 — Temel:** Proje iskeleti, API katmanı, oturum saklama ve yenileme, giriş ve kayıt, ana ekran
- [ ] **Faz 2 — Yazılar:** Akış, yazı okuma (Markdown), yazı oluşturma ve düzenleme, kapak görseli, kategoriler, etiketler
- [ ] **Faz 3 — Etkileşim:** Yorumlar, beğeni, kaydetme, takip, profil
- [ ] **Faz 4 — Keşfet:** Popüler ve takip akışı, kategori ve etiket sayfaları, arama
- [ ] **Faz 5 — Topluluk:** Bildirimler, şikâyet, moderasyon

---

## Lisans

Bu proje [MIT Lisansı](LICENSE) ile lisanslanmıştır.
