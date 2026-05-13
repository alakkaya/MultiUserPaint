# MultiUserPaint

MultiUserPaint, birden fazla kullanıcının aynı tuval üzerinde eş zamanlı çalışabildiği masaüstü bir resim düzenleme uygulamasıdır. Uygulama istemci-sunucu mimarisiyle çalışır ve iletişim TCP soketleri üzerinden kurulur.

## Genel Özellikler

- Kullanıcı adı ile sunucuya bağlanma
- Paylaşılan dosyaları listede görme ve birden fazla dosyayı ayrı sekmelerde açma
- Aynı tuval üzerinde çok kullanıcılı çizim desteği
- Fırça rengi ve fırça boyutu değiştirme
- Kes-Kopyala-Yapıştır desteği
- Tuvali temizleme
- Sunucu tarafında otomatik kayıt
- Yeni boş tuval açma veya dosyadan resim yükleyip paylaşma

## Proje Yapısı

- `src/client` : İstemci bağlantı sınıfı
- `src/server` : Sunucu, istemci işleyici ve paylaşılan dosya yönetimi
- `src/ui` : Swing tabanlı arayüz bileşenleri

## Eclipse ile Çalıştırma

1. Eclipse açın.
2. `File > Import > Existing Projects into Workspace` yolunu izleyin.
3. Proje klasörü olarak `MultiUserPaint` klasörünü seçin.
4. Proje içe aktarıldıktan sonra `src` klasörünün source folder olarak tanındığını kontrol edin.
5. Gerekirse `Project > Clean` ile projeyi temizleyin.

### Sunucuyu Başlatma

1. `src/server/Server.java` dosyasını açın.
2. `Run As > Java Application` ile çalıştırın.
3. Sunucu varsayılan olarak `5005` portunda dinlemeye başlar.

### İstemciyi Başlatma

1. `src/ui/MainFrame.java` dosyasını açın.
2. `Run As > Java Application` ile çalıştırın.
3. Uygulama açılınca kullanıcı adınızı girin.

## Kullanım

- Giriş yaptıktan sonra paylaşılmış dosyalar ana listede görünür.
- Listeden bir dosyayı çift tıklayarak ya da `Seçilene Gir` butonuyla açabilirsiniz.
- `+ Yeni Boş Tuval` ile yeni bir boş çalışma alanı oluşturabilirsiniz.
- `Dosyadan Aç` ile yerel bir resim seçip paylaşabilirsiniz.
- Tuval içinde normal çizim yapabilir, renk ve kalem boyutu değiştirebilirsiniz.
- Seçim modunu açarak bir alan seçebilir, sağ tık menüsünden `Kopyala`, `Kes` ve `Yapıştır` işlemlerini yapabilirsiniz.
- `Kaydet` butonu açık tuvali yerel bilgisayarınıza PNG olarak kaydeder.

## Protokol Özeti

Uygulama, istemci ve sunucu arasında metin tabanlı bir mesaj protokolü kullanır. Her mesaj bir komut adı ile başlar ve alanlar `|` karakteri ile ayrılır. Protokol, dosya listesi paylaşımı, tuval senkronizasyonu ve çizim işlemlerinin eş zamanlı yürütülmesi için tasarlanmıştır.

### Mesaj Formatı

Genel yapı şöyledir:

- `MESAJ_TIPI|alan1|alan2|...`

Mesajlar iki ana gruba ayrılır:

- İstemciden sunucuya giden mesajlar: bağlanma, dosya paylaşma, tuvale katılma ve çizim işlemleri.
- Sunucudan istemcilere giden mesajlar: liste güncelleme, tuval senkronizasyonu, işlem yayını ve hata bildirimleri.

### Temel Akış

1. İstemci sunucuya bağlanır.
2. Kullanıcı adı kayıt edilir ve bağlantı onayı verilir.
3. Sunucu mevcut paylaşılan dosyaları istemciye gönderir.
4. Kullanıcı bir dosya paylaşır veya mevcut bir dosyaya katılır.
5. Çizim, kesme, yapıştırma ve temizleme işlemleri aynı dosyaya bağlı diğer istemcilere anlık olarak yayınlanır.
6. Sunucu, ilgili tuvali düzenli olarak ve bağlantı kapanışında otomatik kaydeder.

### Mesajlar ve İşlemler

| Mesaj         | Yön                                   | Amaç                           | Alındığında Yapılan İşlem                                                     |
| ------------- | ------------------------------------- | ------------------------------ | ----------------------------------------------------------------------------- |
| `CONNECT`     | İstemci -> Sunucu                     | Kullanıcı adı ile bağlanma     | Kullanıcı adı kontrol edilir, oturum açılır, dosya listesi gönderilir         |
| `SHARE_FILE`  | İstemci -> Sunucu                     | Yeni dosya veya resim paylaşma | Dosya sunucuda oluşturulur, tuval kaydı tutulur, diğer istemcilere bildirilir |
| `JOIN_FILE`   | İstemci -> Sunucu                     | Var olan bir tuvale katılma    | Tuval bilgisi istemciye senkronize edilir                                     |
| `DRAW`        | İstemci -> Sunucu -> Diğer istemciler | Çizim çizgisi gönderme         | Çizim sunucudaki tuvale uygulanır ve aynı dosyaya bağlı istemcilere yayılır   |
| `CUT`         | İstemci -> Sunucu -> Diğer istemciler | Seçilen alanı silme            | Seçilen bölge temizlenir ve diğer istemcilere aynı işlem gönderilir           |
| `PASTE`       | İstemci -> Sunucu -> Diğer istemciler | Kopyalanan alanı yapıştırma    | Görsel veri tuvale eklenir ve diğer istemcilere iletilir                      |
| `CLEAR`       | İstemci -> Sunucu -> Diğer istemciler | Tuvali tamamen temizleme       | Tuval beyaza boyanır ve işlem paylaşılır                                      |
| `FILE_LIST`   | Sunucu -> İstemci                     | Paylaşılan dosyaları listeleme | Dosya listesi arayüze basılır                                                 |
| `FILE_ADDED`  | Sunucu -> İstemci                     | Yeni dosya eklendi bildirimi   | Liste güncellenir                                                             |
| `CANVAS_SYNC` | Sunucu -> İstemci                     | Tuvalin tam kopyasını gönderme | İstemci ilgili sekmeyi açar ve resmi yükler                                   |
| `OK`          | Sunucu -> İstemci                     | Başarılı bağlantı              | Durum etiketi güncellenir                                                     |
| `ERROR`       | Sunucu -> İstemci                     | Hata bildirimi                 | Hata türüne göre kullanıcı uyarılır                                           |

### Durum Mantığı

Protokol tarafında basit bir durum sırası kullanılır: `Bağlanma -> Dosya Listesi Alma -> Dosya Seçme / Dosya Oluşturma -> Tuval Senkronizasyonu -> Çizim İşlemleri`. Aynı dosyaya bağlanan istemciler arasında işlem yayını yapıldığı için her kullanıcı güncel tuval durumunu görür.

Sunucu, paylaşılan dosyaları bellekte tutar ve düzenli olarak otomatik kayıt yapar. Ayrıca istemci bağlantısı kapandığında açık dosyaların son hali diske yazılır.

## Not

Bu proje eğitim amaçlı hazırlanmıştır. Arayüz sade tutulmuş, asıl odak çok kullanıcılı veri iletimi ve senkronizasyona verilmiştir.
