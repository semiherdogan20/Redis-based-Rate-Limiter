# Redis-based-Rate-Limiter
Spring Boot ve Redis (Lua Scripting) kullanılarak Token Bucket algoritmasıyla geliştirilmiş, yüksek performanslı ve thread-safe bir API Rate Limiter projesi.

# Spring Boot & Redis Rate Limiter (Token Bucket)

Bu proje; yüksek trafikli API uç noktalarını korumak, kaynak sömürüsünü (DDoS) ve brute-force saldırılarını engellemek amacıyla geliştirilmiş **Token Bucket (Jeton Kovası)** tabanlı bir Rate Limiter (Oran Sınırlayıcı) sistemidir. 

Projenin temel amacı; ağ filtresi (HTTP), bellek içi veritabanı (Redis) ve eşzamanlılık (Concurrency) güvenliğini optimize ederek kurumsal standartlarda bir koruma kalkanı inşa etmektir.

## ✨ Öne Çıkan Özellikler

* **Token Bucket Algoritması:** Anlık trafik patlamalarına (burst traffic) belirli bir ölçüde tolerans gösteren esnek kısıtlama yapısı.
* **Lazy Refill (Tembel Yenileme):** Arka planda sürekli çalışan maliyetli thread'ler (cron job) yerine, kovaya her istek geldiğinde geçen süreyi milisaniye hassasiyetinde hesaplayan dinamik dolum mantığı.
* **%100 Atomik Mimari:** Verinin okunması, matematiksel hesaplama ve Redis'e geri yazılması adımlarının arasına başka isteklerin sızmasını (Race Condition) engellemek için tasarlanmış **Redis Lua Scripting** altyapısı.
* **Fail-Open (Açık Bırakma) Politikası:** Redis sunucusunun çökmesi veya yanıt vermemesi durumunda asıl uygulamanın kesintiye uğramaması için geliştirilmiş hata yakalama ve esneklik (resilience) mekanizması.
* **Tip Güvenli (Type-Safe) Konfigürasyon:** Dışarıdan YAML/Properties bağımlılığı olmadan, tamamen saf Java sınıfları üzerinden compile-time güvenliği sağlayan endpoint bazlı limit yönetimi.

## 📐 Projenin Çekirdek Mantığı ve Veri Stratejisi

### 1. Redis Veri Yapısı Seçimi
Sistemde her kullanıcı veya IP için veriler Redis **Hash** yapısı (`HSET`) altında izole olarak saklanır. JSON parse maliyetlerinden kaçınmak ve performansı en üst düzeye çıkarmak için kova durumları şu iki alanda float/long hassasiyetinde tutulur:
* `tokens`: Kovada anlık kalan jeton sayısı (küsuratlı jetonların kaybolmaması için float değer destekler).
* `last_updated`: En son başarılı isteğin işlendiği sunucu zaman damgası (Unix Epoch Milliseconds).

### 2. Dinamik Hafıza Yönetimi (TTL)
Aktif olmayan kullanıcıların Redis belleğini şişirmesini önlemek amacıyla, Lua script'i her çalıştırdığında kovanın maksimum dolum süresi hesaplanır ve anahtara dinamik olarak `PEXPIRE` (milisaniye cinsinden TTL) atanır. Kullanıcı istek atmayı bıraktıktan kısa süre sonra bellek otomatik olarak temizlenir.

### 3. HTTP Katmanı ve İstemci Tanımlama
Gelen istekler henüz Controller katmanına ulaşmadan `OncePerRequestFilter` tarafından yakalanır. Güvenli IP tespiti için `X-Forwarded-For` ve proxy zincirleri analiz edilerek benzersiz istemci anahtarı (`Key`) üretilir. Limit durumları istemciye standart HTTP başlıkları üzerinden şeffaf bir şekilde bildirilir:
* `X-RateLimit-Remaining`: Kullanıcının mevcut pencerede kalan istek hakkı.
* `Retry-After`: Limit aşıldığında (`HTTP 429`) istemcinin tekrar istek atabilmesi için beklemesi gereken net saniye süresi.

## 🚀 API Uç Noktaları (Endpoints)

| Endpoint | HTTP Metodu | Sorumluluk | Limit Kuralı |
| :--- | :--- | :--- | :--- |
| `/login` | `POST` | Giriş denemelerini brute-force'a karşı korur. | 5 saniyede maksimum 1 istek |
| `/hello` | `GET` | Genel API uç noktası simülasyonu. | Saniyede maksimum 5 istek |
| `/admin/rate-limit/status` | `GET` | Admin paneli için bir IP'nin kova durumunu izler. | Sorumlu Kişi (Limit Yok) |
| `/admin/rate-limit/reset` | `DELETE` | Engellenen bir kullanıcının limitini anında sıfırlar. | Sorumlu Kişi (Limit Yok) |

## 🛠️ Teknolojik Altyapı

* **Java 17**
* **Spring Boot 3.x**
* **Spring Data Redis** (StringRedisTemplate ve RedisScript altyapısı)
* **Lombok** (Temiz kod ve boilerplate azaltımı için)
