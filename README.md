# Common SDK Documentation (REST Client & NumberInput)

Dokumentasi penggunaan modul library `commons-sdk` untuk proyek Android, mencakup **REST Client Library** dan **NumberInput Component**.

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
