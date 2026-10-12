# Common SDK Documentation (REST Client, NumberInput, Location, Form Draft & EvoForm)

Dokumentasi penggunaan modul library `commons-sdk` untuk proyek Android, mencakup **REST Client Library**, **NumberInput Component**, **Location capture**, **Form Draft** dan **EvoForm**.

---

## 1. REST Client Library (`com.onevour.core.rest`)

### Overview
Client HTTP lightweight berbasis Annotation & Dynamic Proxy di Android untuk pemanggilan REST API secara asinkron tanpa boilerplate.

### Fitur Utama
- **Annotation-driven API**: `@RestRepository`, `@Get`, `@Post`, `@Put`, `@Patch`, `@Delete`, `@Header`, `@Query`, `@Path`, `@Body`.
- **Custom Timeout**: Pengaturan timeout khusus per method menggunakan `@HttpTimeout`.
- **Multipart Support**: Mengunggah file / data multipart dengan `@Post` + `HttpMultipart`.
- **Callback Listener**: Interface `HttpListener<T>` dengan method `onSuccess(HttpResponse<T>)` dan `onError(HttpErrorResponse)`.

---

### Cara Penggunaan REST Client

#### A. Definisi API Repository Interface
```java
package com.your.app;

import com.onevour.core.rest.repository.RestRepository;
import com.onevour.core.rest.annotations.Get;
import com.onevour.core.rest.annotations.Post;
import com.onevour.core.rest.annotations.Header;
import com.onevour.core.rest.annotations.Path;
import com.onevour.core.rest.annotations.Query;
import com.onevour.core.rest.annotations.Body;
import com.onevour.core.rest.configurations.HttpTimeout;
import com.onevour.core.rest.listener.HttpListener;
import com.onevour.core.rest.models.HttpResponse;
import com.onevour.core.rest.models.HttpErrorResponse;

import java.util.List;

@RestRepository
public interface UserRepository {

    @Get("/users/{id}")
    @HttpTimeout(connectTimeout = 5000, readTimeout = 5000)
    void getUserById(
        @Header("Authorization") String token,
        @Path("id") int userId,
        HttpListener<UserResponse> listener
    );

    @Post("/users")
    void createUser(
        @Body UserRequest userRequest,
        HttpListener<UserResponse> listener
    );

    @Get("/users")
    void searchUsers(
        @Query("query") String keyword,
        @Query("page") int page,
        HttpListener<List<UserResponse>> listener
    );
}
```

#### B. Inisialisasi & Pemanggilan API
```java
// 1. Buat instance RestClient
RestClient client = new RestClient();

// 2. Buat objek Repository dari Interface
UserRepository userRepository = client.create(UserRepository.class);

// 3. Panggil method API secara async
userRepository.getUserById("Bearer token_abc123", 1, new HttpListener<UserResponse>() {
    @Override
    public void onSuccess(HttpResponse<UserResponse> response) {
        UserResponse user = response.getBody();
        System.out.println("User Name: " + user.getName());
    }

    @Override
    public void onError(HttpErrorResponse error) {
        System.err.println("Error Code: " + error.getStatusCode());
        System.err.println("Error Message: " + error.getErrorMessage());
    }
});
```

---

## 2. NumberInput Library (`com.onevour.core.utilities.input`)

### Overview
Komponen input angka/uang serbaguna di Android dengan Numpad khusus, mendukung mode **AlertDialog** maupun **BottomSheetDialog**, kustomisasi tema (Light & Dark Mode), lokalisasi (Indonesia & US), serta aksesibilitas (TalkBack) dan efek touch ripple.

---

### Fitur Utama NumberInput
- **Format Numeric & Decimal**: Mendukung angka bulat (`InputNumeric`) dan pecahan desimal (`InputDecimal`).
- **Tampilan Flexible**: Pilihan antara **AlertDialog** dan **BottomSheetDialog** (`app:useBottomSheet="true"` / `.setUseBottomSheet(true)`).
- **Custom Light & Dark Styling**: Kustomisasi warna background, font/typeface, warna teks hasil, dan warna tombol numpad via XML Attr atau Java `NumberInputStyle`.
- **Localization Support**: Otomatis menyesuaikan pemisah desimal berdasarkan `Locale` (misal Indonesia `,` dan US `.`).
- **Aksesibilitas & Touch Ripple**: Memiliki efek sentuhan `RippleDrawable` yang halus dan label TalkBack yang ramah disabilitas.

---

### Cara Penggunaan NumberInput

#### A. Penggunaan di Layout XML (`activity_main.xml`)
```xml
<!-- Mode Default (AlertDialog & Theme Otomatis) -->
<com.onevour.core.utilities.input.NumberInputTextField
    android:id="@+id/input_currency"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:isDecimal="true"
    app:minValue="0"
    app:maxValue="10000000"
    app:titleText="Masukkan Jumlah Transfer" />

<!-- Mode BottomSheet & Custom Dark Theme -->
<com.onevour.core.utilities.input.NumberInputTextField
    android:id="@+id/input_bottom_sheet"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:useBottomSheet="true"
    app:isDecimal="true"
    app:dialogBackground="#121212"
    app:dialogTitleColor="#BB86FC"
    app:dialogResultColor="#03DAC6"
    app:dialogKeyTextColor="#FFFFFF" />
```

#### B. Atribut Styling di XML (`attrs.xml`)
| Atribut XML | Tipe Data | Deskripsi |
| :--- | :--- | :--- |
| `app:minValue` | String | Nilai minimum input |
| `app:maxValue` | String | Nilai maksimum input |
| `app:isDecimal` | Boolean | `true` untuk mode desimal, `false` untuk angka bulat |
| `app:useBottomSheet` | Boolean | `true` untuk memakai BottomSheetDialog, `false` untuk AlertDialog |
| `app:dialogFontFamily` | Reference | Resource font/typeface (misal `@font/poppins`) |
| `app:dialogBackground` | Color / Ref | Warna/Drawable background dialog Numpad |
| `app:dialogTitleColor` | Color | Warna teks judul Numpad |
| `app:dialogResultColor` | Color | Warna teks angka layar hasil |
| `app:dialogKeyTextColor` | Color | Warna teks tombol-tombol Numpad |
| `app:dialogKeyBackground` | Color / Ref | Warna/Drawable background tombol Numpad |

---

#### C. Kustomisasi Style di Kode Java/Kotlin (`NumberInputStyle`)
```java
NumberInputTextField inputTextField = findViewById(R.id.input_currency);

// Buat konfigurasi style kustom
NumberInputStyle customStyle = new NumberInputStyle.Builder()
    .setTypeface(ResourcesCompat.getFont(context, R.font.poppins_bold))
    .setDialogBackgroundColor(Color.parseColor("#121212"))
    .setTitleTextColor(Color.WHITE)
    .setResultTextColor(Color.parseColor("#00E676"))
    .setKeyTextColor(Color.parseColor("#E0E0E0"))
    .setKeyBackgroundColor(Color.parseColor("#2C2C2C"))
    .build();

// Terapkan style
inputTextField.setStyle(customStyle);
```

---

#### D. Penggunaan Lokalisasi (Indonesia vs US)
```java
// 1. Format Indonesia (Pemisah desimal koma ',')
NumberFormat formatIndo = NumberFormat.getNumberInstance(new Locale("id", "ID"));
formatIndo.setMinimumFractionDigits(2);
formatIndo.setMaximumFractionDigits(2);

NumberInput numPadIndo = new NumberInput();
numPadIndo.setup(editTextIndo, formatIndo, 0, 10000000.0);
editTextIndo.setText(formatIndo.format(1250000.50)); // Hasil: 1.250.000,50

// 2. Format United States (Pemisah desimal titik '.')
NumberFormat formatUS = NumberFormat.getNumberInstance(Locale.US);
formatUS.setMinimumFractionDigits(2);
formatUS.setMaximumFractionDigits(2);

NumberInput numPadUS = new NumberInput();
numPadUS.setup(editTextUS, formatUS, 0, 10000000.0);
editTextUS.setText(formatUS.format(1250000.50)); // Hasil: 1,250,000.50
```

---

#### E. Penggunaan Mode BottomSheet Programmatis
```java
NumberInput numPad = new NumberInput();

// Aktifkan mode BottomSheet
numPad.setUseBottomSheet(true);

numPad.setup(this, NFormat.currency(), 0, 100000.0);
numPad.setTitle("Nominal Pembayaran");
numPad.setListener(new NumberInput.Listener() {
    @Override
    public void onSubmitValue() {
        // Callback saat tombol Selesai ditekan
    }

    @Override
    public void onValue(int id, boolean isDecimal, int intValue, double doubleValue) {
        System.out.println("Selected Double: " + doubleValue);
    }
});

numPad.show();
```

---

## 3. Location capture (module `commons-sdk-location`, package `com.onevour.core.location`)

### Overview
Module terpisah dari `commons-sdk` dan tidak bergantung padanya: aplikasi yang tidak tracking tidak ikut membawa izin lokasi maupun `play-services-location`. Contoh pemakaiannya ada di module `app` (menu **Location Capture**).

Menangkap posisi perangkat saat aplikasi memintanya — hanya itu. **Kapan** perlu tracking (absen, check-in, jam kerja) dan **apa** yang dilakukan dengan lokasinya (simpan, kirim ke server) tetap di aplikasi masing-masing.

### Perilaku
* **Service:** foreground service lokasi (`LocationService`), hemat baterai: 1 fix per 20 menit (min. 10 menit), hanya setelah pindah 500 m. Bisa diganti lewat `Config.builder(...).interval(intervalMs, minIntervalMs, minDistanceMetres)`, mis. interval pendek untuk uji rute, dan `.priority(Priority.PRIORITY_HIGH_ACCURACY)` (GPS) karena route emulator hanya mengisi GPS.
* **Ganti saat jalan:** `LocationCapture.reconfigure(context, config)` dari layar yang tampil memakai config baru dan me-restart service.
* **Lokasi untuk transaksi:** `LocationCapture.current(context, maxAgeMs, timeoutMs, listener)` — sekali ambil dengan GPS, fake GPS ditolak, alasan gagal berupa `NoFix` (pesan ditulis aplikasi). Tidak butuh service tracking; batalkan dengan `Pending.cancel()` saat layar ditutup.
* **Filter (`FixFilter`):** hanya fix asli (bukan Fake GPS), akurasi ≤ 100 m, bukan sekitar 0,0, dan paling cepat 9 menit (90% interval minimum) dari fix sebelumnya yang dikirim ke aplikasi.
* **Izin:** lokasi "saat aplikasi digunakan" saja. Service hanya dijalankan dari layar yang terlihat (`LocationCapture.sync`), tidak pernah dari background / boot.
* **Watchdog:** `LocationWatchdogWorker` tiap 15 menit menghentikan service bila aplikasi tidak lagi memintanya. Tidak pernah menyalakan.

### Cara Penggunaan
#### A. Dependency
```gradle
implementation 'com.github.onevour.android-commons:commons-sdk-location:<tag>'
```

Module ini membawa `play-services-location` dan WorkManager, dan manifest-nya sudah berisi izin lokasi (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`) serta `LocationService` — aplikasi tidak perlu menulisnya lagi. Aplikasi tinggal meminta izin lokasi tepat (dan `POST_NOTIFICATIONS` di Android 13+ agar notifikasi tracking tampil) saat runtime. Waktu fix terakhir disimpan di SharedPreferences milik module (`onevour_location`).

#### B. Inisialisasi (`Application#onCreate`) & pemanggilan
```java
LocationCapture.init(this, LocationCapture.Config.builder(
                () -> myApp.wantsPositionNow(),          // keputusan aplikasi: login, jadwal, dsb.
                fix -> myApp.keep(fix))                  // lokasi yang lolos filter: simpan / kirim oleh aplikasi
        .onFix(location -> keepLastKnown(location))      // opsional; setiap fix
        .notification("Tracking Lokasi Aktif", "Sedang mengambil lokasi perangkat...", R.drawable.ic_location)
        .build());
LocationCapture.startWatchdog();

LocationCapture.sync(activity);    // dari layar yang terlihat: jalan bila aplikasi minta, berhenti bila tidak
LocationCapture.stop(context);     // logout
```

---

## 4. Form Draft (`com.onevour.core.draft`, bagian dari `commons-sdk`)

> Untuk form baru, pakai **EvoForm** (bagian 5): draft cukup `.draft(form, key, owner)`. `EvoFormDraft` (dulu `FormDraft`) di bawah ini untuk layar lama yang belum memakai EvoForm.

### Overview
Isian form disimpan otomatis sebagai **draft** selama user mengisi, jadi baterai habis, aplikasi di-kill atau HP restart tidak membuat user mengetik ulang. Saat form dibuka lagi, draft ditawarkan kembali. Seperti `RefSession`, library punya **database sendiri** (`commons_drafts.db`), jadi aplikasi **tidak perlu migrasi** dan **tidak perlu init** selain `ContextHelper.init(application)` yang sudah ada.

> Cara termudah memakai draft: lewat **EvoForm** (bagian 5), yang membungkus form, validasi dan draft sekaligus. Bagian ini menjelaskan mesin draft-nya, untuk form yang tidak memakai EvoForm.

Library menyediakan mekanismenya. Aplikasi hanya **mengimplementasikan** sesuai kebutuhan:
- `EvoFormDraft.Host<T>`: cara form mengambil dan mengisi kembali isiannya (atau pakai `forModel` / `forLayout`).
- `AttachmentHandler` (opsional): kalau form punya lampiran (foto) yang ikut berstatus draft.

### Perilaku
- **Aturan form:** isian adalah draft sampai form **disimpan** (`saved()` → draft dihapus).
- **Simpan otomatis:** setiap perubahan ditulis ±0,7 detik setelahnya (ketikan beruntun = 1x tulis), dan langsung saat aplikasi ke background (`onPause`).
- **Saat form dibuka** (`offerRestore()`): kalau ada draft, muncul dialog *"Lanjutkan draft?"*. Pilih **Lanjutkan** untuk mengisi kembali, atau **Buang** untuk menghapus draft dan lampirannya.
- **Back tanpa simpan** (`leave()`): dialog *Lanjutkan nanti* (draft disimpan), *Buang* (draft dan lampiran dihapus), atau *Tetap di sini*. Kalau tidak ada perubahan, langsung keluar.
- **"Ada perubahan"** = state form berbeda dari saat selesai dimuat (dibandingkan sebagai JSON). Nilai yang diisi form sendiri (spinner awal, hasil hitungan) **bukan** perubahan.
- **Teks dialog** tersedia EN (default) dan ID (`values-in`), dengan nama `commons_draft_*`. Aplikasi boleh menimpanya.
- **Thread:** semua akses DB berjalan di thread latar milik library. Dialog dan callback berjalan di main thread.

### Cara Penggunaan

#### A. Syarat
Cukup `ContextHelper.init(this)` di `Application#onCreate` (sudah dipakai oleh `RefSession`). Database draft dibuka otomatis saat pertama dibutuhkan.

#### B. Konfigurasi opsional (`Application#onCreate`)
Hanya kalau form punya lampiran draft (foto) atau ingin mencatat siapa yang menulis draft:
```java
Drafts.configure(new MyPhotoAttachments(), () -> session.currentUserId());
```

#### C. Form rumit: `forModel` (disarankan)
Isian dipegang ViewModel (stepper, list, foto, pilihan id/tanggal). Yang disimpan adalah model, bukan view, jadi tahan terhadap perubahan layout.
```java
public class SosActivity extends AppCompatActivity {

    private EvoFormDraft<SosState> draft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        draft = EvoFormDraft.forModel(this, "SOS", visitId + ":" + period, customerId, SosState.class,
                        viewModel::formState,          // ambil isian (Gson → JSON)
                        viewModel::restore)            // isi kembali draft; ViewModel menggambar ulang layar
                .photos(SosState::photoIds);           // id lampiran draft, ikut dibuang saat "Buang"
        viewModel.getLoaded().observe(this, loaded -> draft.offerRestore());   // setelah data tampil
        viewModel.onChange(draft::changed);            // setiap model berubah
        binding.btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        if (!viewModel.validate()) return;
        viewModel.save(() -> {
            Drafts.run(() -> Drafts.commitAttachments(viewModel.photoIds()));   // lampiran draft → final
            draft.saved();                                                      // draft selesai, dihapus
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        draft.leave(this::finish);
    }
}

// state yang disimpan: field sederhana saja (primitive, String, Long id, List), tanggal sebagai millis
public class SosState {
    public List<Entry> entries = new ArrayList<>();
    public static Collection<Long> photoIds(SosState s) { ... }
}
```

#### D. Form satu layar sederhana: `forLayout`
Semua input yang punya `android:id` di bawah root (EditText, CheckBox/Switch, RadioGroup, Spinner) otomatis jadi isian, dengan nama id sebagai kunci.
```java
draft = EvoFormDraft.forLayout(this, "REASON", visitId, customerId, binding.getRoot());
draft.offerRestore();                       // setelah data tampil
...
draft.saved();                              // setelah simpan
@Override public void onBackPressed() { draft.leave(this::finish); }
```
- Hanya TextWatcher yang dipasang otomatis (bisa ditumpuk). Listener checkbox, radio dan spinner milik form **tidak ditimpa**. Perubahannya ikut tertulis pada penulisan berikutnya (ketik teks, onPause, back), atau panggil `draft.changed()` di listener form untuk menulis langsung.
- Untuk mengecualikan view (misalnya total hasil hitungan), beri `android:tag="draft:ignore"`.
- Kelemahannya: bergantung pada `android:id`, dan tidak cocok untuk form multi-langkah. Gunakan `forModel` untuk form seperti itu.

#### E. Implementasi `EvoFormDraft.Host` langsung
Kalau Activity sendiri yang mengambil dan mengisi isiannya:
```java
public class ReasonActivity extends AppCompatActivity implements EvoFormDraft.Host<ReasonState> {

    draft = new EvoFormDraft<>(this, "REASON", key, customerId, ReasonState.class, this);

    @Override public ReasonState capture() { return new ReasonState(selectedReasonId(), noteText()); }
    @Override public void restore(ReasonState state) { checkReason(state.reasonId); setNote(state.description); }
    @Override public Collection<Long> photoIds(ReasonState state) { return Collections.emptyList(); }   // opsional
    @Override public void onDiscarded() { reloadSavedData(); }                                         // opsional
}
```

#### F. Lampiran draft (foto): implementasi `AttachmentHandler`
Library tidak tahu tabel foto aplikasi. Aplikasi menandai foto yang diambil di form sebagai draft (misalnya kolom status), lalu library memanggil handler saat simpan atau buang:
```java
public class MyPhotoAttachments implements AttachmentHandler {
    @Override public void commit(Collection<Long> ids) { photoDao.markFinal(ids); }       // simpan → final, ikut upload
    @Override public void discard(Collection<Long> ids) { photoDao.deleteDrafts(ids); }   // buang → hapus baris + file
}
// saat simpan:  Drafts.commitAttachments(ids);   saat buang otomatis lewat EvoFormDraft (ids dari Host.photoIds / photos())
```

#### G. API `Drafts` (dipanggil di thread latar, misalnya `Drafts.run(...)`)
| Method | Kegunaan |
|---|---|
| `formsOf(ownerId)` | form mana yang punya draft untuk owner itu (misalnya chip "Draft" di menu kunjungan) |
| `count()` / `count(forms)` | jumlah draft, misalnya untuk menahan logout selama masih ada draft |
| `deleteByOwner(ownerId)` | buang semua draft milik owner (misalnya check-out tanpa menyimpan) |
| `retainForms(forms)` | hapus draft dari form yang sudah tidak memakai draft (panggil saat start) |
| `deleteOlderThan(date)` | bersihkan draft lama (misalnya periode sebelumnya) |
| `deleteAll()` | hapus semua draft (misalnya saat logout) |
| `commitAttachments(ids)` / `discardAttachments(ids)` | teruskan ke `AttachmentHandler` aplikasi |
| `save` / `load` / `delete` | akses langsung, jarang perlu karena `EvoFormDraft` sudah memakainya |

#### H. Tips
- **Key** harus unik per instance form, misalnya `visitId + ":" + itemId` (item baru: `"new"` atau id sementara). **ownerId** = pemilik draft untuk pengelompokan, misalnya customer/kunjungan.
- **Nama form** sebaiknya konstanta atau enum di aplikasi (misalnya `enum AppForms { SOS, PROMO, ... }` → `AppForms.SOS.name()`). Panggil `Drafts.retainForms(...)` saat start supaya draft form yang dihentikan ikut dibersihkan.
- **State class** harus bisa di-Gson: tanpa referensi View/Context/entity Room, tanggal sebagai `long`.
- **Field hitungan** jangan disimpan. Hitung ulang dari model setelah `restore`.
- **Panggil `offerRestore()` setelah data form tampil**, karena di situ "keadaan awal" diambil.

#### I. Unit test (JVM)
```java
Drafts.useStore(new MemoryDraftStore());          // ganti DB dengan store di memori (implementasi DraftStore)
Drafts.configure(fakeAttachments, () -> "U01");
Drafts.save("REASON", "k1", "C1", state);
Drafts.Draft<ReasonState> back = Drafts.load("REASON", "k1", ReasonState.class);
```

---

## 5. EvoForm (`com.onevour.core.form`, bagian dari `commons-sdk`)

### Overview
Form dengan konsep seperti `useForm` dari react-hook-form, dipisah menjadi dua bagian:
- **`EvoForm<T>`**: state form, disimpan di **ViewModel** (tanpa View dan Activity). Isinya nilai field (LiveData per field), aturan (anotasi pada `T`), error, status dirty, validasi, dan **draft bawaan** (memakai Form Draft di bagian 4). Tetap utuh saat layar diputar dan bisa di-unit test tanpa view.
- **`EvoFormView<T>`**: pengikat UI, dibuat dengan `form.bind(activity, root)`. View diikat ke field EvoForm dua arah berdasarkan nama id. Error tampil di field (field pertama yang error mendapat fokus), dialog "Lanjutkan draft?" muncul, dan ada konfirmasi saat keluar dengan input yang belum disimpan.

| react-hook-form | EvoForm |
|---|---|
| `useForm({ defaultValues })` | `EvoForm.of(Values.class)` di ViewModel + `reset(defaults)` |
| `register("name")` | otomatis: view dengan id = nama field (atau snake_case, atau `@Bind`); manual: `form.register(view, "name")` |
| aturan `register("x", { required })` | anotasi `@Required`, `@Min`, ... atau `model.rule("x", Rules...)` |
| `handleSubmit(onValid)` | `model.validate()` di ViewModel, atau `form.handleSubmit(values -> ...)` |
| `watch("name")` | `model.value("name").observe(...)` (LiveData) |
| `setValue` / `getValues` | `model.setValue("name", v)` / `model.values()` (bertipe `T`) |
| `formState.errors` / `isDirty` | `model.errors()` (LiveData) / `model.isDirty()` |
| `setError` / `clearErrors` | `model.setError("name", FormError.of(...))` / `clearErrors()` |
| `reset(values)` | `model.reset(values)`: nilai awal, lalu menawarkan draft |
| *(tidak ada)* | **draft otomatis**: `.draft(form, key, owner)` |

### Cara Penggunaan

#### A. Syarat
Dependency lifecycle (`lifecycle-viewmodel`, `lifecycle-livedata`) sudah dibawa oleh `commons-sdk`. Draft memakai `Drafts` (bagian 4); attachment (foto) memakai `AttachmentHandler` yang didaftarkan aplikasi.

#### B. Class nilai + anotasi
Field class nilai otomatis terikat ke view dengan id yang sama dengan nama field. Misalnya `normalPrice` mencari `@+id/normalPrice` atau `@+id/normal_price`, atau gunakan `@Bind("cio_normal_price")`. Field tanpa view (id produk, daftar foto) disimpan oleh model.
```java
import com.onevour.core.form.annotation.*;

public class PromoValues {
    @Required @Numeric @Min(1)   public Double normalPrice;    // @+id/normal_price
    @Required @Numeric @Min(1)   public Double promoPrice;     // @+id/promo_price
    @Required                    public String display;        // RadioGroup @+id/display, nilai = android:tag tombol
    @MaxLength(255)              public String description;    // @+id/description
    @Required                    public Long productId;        // tanpa view: dipegang model
    @Attachment                  public List<Long> photoIds;   // lampiran draft (foto)
    @Ignore                      public Double savings;        // bukan field form
}
```
Anotasi ada di paket `com.onevour.core.form.annotation`: `@Required`, `@Numeric`, `@Min`, `@Max`, `@MinLength`, `@MaxLength`, `@Email`, `@Regex(value, message)`, `@Attachment`, `@Bind`, `@Ignore`. Semuanya punya `message` opsional (`@StringRes`). Kalau tidak diisi (0), dipakai pesan bawaan EN/ID.

#### C. ViewModel: state form (disarankan)
```java
public class PromoViewModel extends ViewModel {

    public final EvoForm<PromoValues> form = EvoForm.of(PromoValues.class)
            .draft("PROMO", visitId + ":" + promoId, customerId)   // draft otomatis (opsional)
            .in(this);                                             // ditutup bersama ViewModel (draft di-flush)

    private final MutableLiveData<Boolean> done = new MutableLiveData<>();

    public void load() {
        repository.findPromo(promoId, promo -> form.reset(promo));  // nilai awal; draft yang ada ditawarkan ke UI
    }

    public void pickProduct(Product product) {
        form.setValue("productId", product.getId());              // field tanpa view
    }

    public void save() {
        if (!form.validate()) return;                              // error tampil di field, yang pertama difokus
        repository.save(form.values(), () -> {
            form.saved();                                          // attachment final, draft dihapus, tidak dirty
            done.postValue(true);
        });
    }
}
```

#### D. Activity / Fragment: pengikat UI
```java
EvoFormView<PromoValues> form = viewModel.form.bind(this, binding.getRoot());
binding.btnSave.setOnClickListener(v -> viewModel.save());
viewModel.form.value("normalPrice").observe(this, v -> updateSavings());   // seperti watch()
@Override public void onBackPressed() { form.leave(this::finish); }
```
- **Fragment:** `model.bind(fragment, view)` memakai `getViewLifecycleOwner()`.
- **Form bertahap (stepper):** satu `EvoForm` di ViewModel Activity (`activityViewModels`/`ViewModelProvider(requireActivity())`). Setiap step memanggil `model.bind(this, view)` ke model yang sama. Validasi per step: `model.validate("name", "phone")` hanya memeriksa field step itu; `model.validate()` di langkah terakhir memeriksa semuanya.
- **Layar sederhana tanpa ViewModel:** `EvoForm.of(Values.class).bind(this, root)`, lalu `form.model().reset(...)`. Model tidak bertahan saat layar diputar, tetapi draft tetap menyimpan input.

#### E. Aturan antar-field dan register manual
```java
EvoForm.of(PromoValues.class)
        .rule("promoPrice", Rules.required().number().min(1)
                .custom((v, all) -> Rules.toNumber(v) <= Rules.toNumber(all.get("normalPrice")), R.string.err_promo_above_normal));
form.register(binding.customPicker, "display");      // view yang id-nya tidak cocok dengan nama field
viewModel.form.setError("promoPrice", FormError.of(serverMessage));   // error dari server
form.errorView("reasonId", binding.reasonError);   // error tampil di TextView sendiri (mis. di bawah RadioGroup)
form.refresh("reasonId");                          // setelah pilihan RadioGroup/Spinner dibuat ulang: nilai dipilih lagi
```
Pesan `@StringRes` dari **modul library aplikasi** (R yang tidak final) tidak bisa dipakai di anotasi. Pakai `rule(name, Rules.required(R.string.x))` di ViewModel.

#### F. Perilaku
- **Jenis view:** EditText (teks; error tampil di TextInputLayout kalau ada), CheckBox/Switch (boolean), RadioGroup (nilai = `android:tag` tombol atau nama id-nya), Spinner (posisi). Nilai lain dipegang model lewat `setValue`.
- **Perbandingan nilai:** teks "12" dan angka 12 dianggap sama, dan teks kosong dianggap null. Jadi form tidak menjadi dirty hanya karena view menampilkan angka sebagai teks.
- **Draft:** setiap edit ditulis sekitar 0,7 detik kemudian di thread latar, dan langsung ditulis saat aplikasi masuk background atau ViewModel ditutup. `reset()` menawarkan draft yang tersimpan lewat `draftOffer()` (EvoFormView menampilkan dialognya). `leave()` memberi pilihan Lanjutkan nanti / Buang / Tetap di sini. `saved()` menghapus draft. Tanpa `.draft(...)`, `leave()` hanya bertanya "keluar tanpa menyimpan?" kalau ada perubahan.
- **Pesan bawaan Rules** tersedia EN/ID (`commons_form_*`). Error disimpan sebagai `FormError` (resource + argumen) dan diterjemahkan oleh UI, sehingga ViewModel tidak butuh Context.
- **Thread:** panggil EvoForm di main thread (nilainya LiveData).

#### G. Rules
| Rule | Arti |
|---|---|
| `Rules.required()` / `required(@StringRes)` / `required(String)` | wajib: tidak null, tidak kosong, bukan false, list tidak kosong |
| `.number()` | angka (koma atau titik) |
| `.min(n)` / `.max(n)` | batas nilai angka |
| `.minLength(n)` / `.maxLength(n)` | panjang teks |
| `.pattern(regex, @StringRes)` / `.email()` | pola teks |
| `.custom(value -> ..., msg)` / `.custom((value, all) -> ..., msg)` | aturan sendiri, bisa membandingkan field lain |
| `Rules.of()` | tanpa aturan awal, lalu disambung rule lain |

Nilai kosong lolos dari semua rule kecuali `required`, sama seperti react-hook-form.

#### H. Unit test (JVM)
```java
@Rule public InstantTaskExecutorRule liveData = new InstantTaskExecutorRule();   // androidx.arch.core:core-testing

@Before public void setUp() {
    EvoForm.testing();            // model berikutnya: tanpa Handler/thread latar, draft di memori (MemoryDraftStore)
    viewModel = new ReasonViewModel(...);
}

viewModel.initialize();
viewModel.form.setValue("reasonId", "2");
viewModel.save();
assertEquals(FormError.of(R.string.reason_fill_note), viewModel.form.errors().getValue().get("description"));
```
Untuk satu model saja: `EvoForm.of(...).withScheduler(scheduler, executor)`.
Lihat `EvoFormTest` di library.
