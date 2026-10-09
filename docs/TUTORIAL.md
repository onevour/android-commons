# android-commons tutorial

How to use every library in this repository, from installation to the pitfalls worth avoiding. Code examples come from the sample app (`app`) and the tests in this repository; class and method names have been checked against the code.

| Module | Package | Contents |
|---|---|---|
| `commons-sdk` | `com.onevour.core` | REST client, RefSession, BeanCopy, Gson helper, RecyclerView adapters, BaseActivity / BaseFragment, NumPad, fragment navigation, EventBus, number & date formatting, JWT, camera, geo, loader widgets |
| `commons-sdk-location` | `com.onevour.core.location` | Location tracking (battery-saving foreground service) and one-shot location for transactions |

The two modules are independent: an app that does not track only needs `commons-sdk`.

---

## Contents

1. [Installation](#1-installation)
2. [Application setup (required)](#2-application-setup-required)
3. [REST client](#3-rest-client)
4. [RefSession: storing small data](#4-refsession-storing-small-data)
5. [Gson helper and `@Expose`](#5-gson-helper-and-expose)
6. [BeanCopy: copying between objects](#6-beancopy-copying-between-objects)
7. [RecyclerView adapters](#7-recyclerview-adapters)
8. [BaseActivity and BaseFragment](#8-baseactivity-and-basefragment)
9. [NumPad: numeric input](#9-numpad-numeric-input)
10. [Fragment navigation](#10-fragment-navigation)
11. [EventBus `MessageEvent`](#11-eventbus-messageevent)
12. [Number and date formatting](#12-number-and-date-formatting)
13. [Other utilities](#13-other-utilities)
14. [Location capture (`commons-sdk-location`)](#14-location-capture-commons-sdk-location)
15. [Pitfalls at a glance](#15-pitfalls-at-a-glance)

---

## 1. Installation

### Gradle

```gradle
// settings.gradle / root build.gradle
repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

```gradle
// app module
dependencies {
    implementation 'com.github.onevour.android-commons:commons-sdk:<tag>'
    implementation 'com.github.onevour.android-commons:commons-sdk-location:<tag>' // only if the app tracks location

    // commons-sdk uses these types in its public API but does not expose them: add them yourself
    implementation 'com.google.code.gson:gson:2.10'
    implementation 'androidx.recyclerview:recyclerview:1.4.0'
    implementation 'com.google.android.material:material:1.14.0'
    implementation 'org.greenrobot:eventbus:3.3.1'        // if you use MessageEvent
}

android {
    buildFeatures { viewBinding true }                     // the RecyclerView adapters use ViewBinding
}
```

Next, do the required setup in [section 2](#2-application-setup-required): `ContextHelper.init` in an `Application` class registered in the manifest.

`<tag>` is the repository's git tag. JitPack builds every module per tag, so both modules always share one version. Avoid the old coordinate `com.github.onevour:android-commons:<tag>`: it pulls in **all** modules, including the location permissions.

### Requirements

* minSdk 23, Java 11.
* An AppCompat / Material app theme (the NumPad BottomSheet needs Material).

### What gets merged into the app manifest

Manifests are merged automatically.

| Module | Permissions / components |
|---|---|
| `commons-sdk` | `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA`, `WAKE_LOCK`, `WRITE_EXTERNAL_STORAGE`, feature `android.hardware.location.gps` |
| `commons-sdk-location` | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, service `LocationService` |

> The `android.hardware.location.gps` feature from `commons-sdk` is not marked `required="false"`, so the Play Store will not offer the app to devices without GPS. If needed, override it in the app manifest:
> ```xml
> <uses-feature android:name="android.hardware.location.gps" android:required="false" tools:replace="android:required" />
> ```

---

## 2. Application setup (required)

> **Required before using anything from `commons-sdk`:** call `ContextHelper.init(this)` in your `Application` class, and register that class in the manifest. Skip it and the first use of RefSession, `BaseActivity` / `BaseFragment` or `DimensionValue.dpToPx` fails.

`ContextHelper` keeps the application context for the library. It also starts loading RefSession's values from its database in the background, so they are ready by the time the first screen reads them.

### 2.1 Create an `Application` class

```java
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ContextHelper.init(this);                                   // FIRST, before anything else of the library
        RestLog.setLevel(BuildConfig.DEBUG ? RestLog.Level.BASIC : RestLog.Level.NONE);

        // only when using commons-sdk-location (section 14)
        LocationCapture.init(this, MyTracking.config(this));
        LocationCapture.startWatchdog();
    }
}
```

### 2.2 Register it in the manifest

Without `android:name`, Android never creates `MyApplication`, so `onCreate` (and `ContextHelper.init`) never runs.

```xml
<application
    android:name=".MyApplication"
    android:theme="@style/AppTheme"
    ... >
```

### 2.3 What needs it

| Uses `ContextHelper` | What happens without `ContextHelper.init` |
|---|---|
| `RefSession` (every save / find, `BaseActivity.session`) | `IllegalStateException: call ContextHelper.init(application) first` |
| `DimensionValue.dpToPx`, `BaseActivity.dpToPx` | `NullPointerException` |
| REST client, NumPad, adapters, BeanCopy, Gson helper, formats, `PermissionUtils` | not needed |
| `commons-sdk-location` | not needed: it uses the `Application` passed to `LocationCapture.init` |

Notes:

* Call it once, in `Application.onCreate()`. Calling it again (some activities in the sample do) is harmless.
* It keeps the **application** context, never an activity, so nothing leaks.
* Android always runs `Application.onCreate()` before restoring any screen, also after it killed the process in the background, so the library is ready on every start.
* Unit tests on an emulator: call `ContextHelper.init(ApplicationProvider.getApplicationContext())` in `@Before`.

---

## 3. REST client

An annotation-based asynchronous HTTP client. You write an interface; the library generates the implementation. Callbacks always run on the **main thread**.

### 3.1 Define a repository

```java
@RestRepository
public interface UserRepository {

    @Get(url = "https://api.example.com/users")
    void search(@Query("name") String name, @Query("page") int page, HttpListener<UserResponse> callback);

    @Get(url = "https://api.example.com/users/{id}")
    void detail(@Path("id") String id, @Header("Authorization") String token, HttpListener<UserResponse> callback);

    @Post(url = "https://api.example.com/users")
    void create(@Body UserRequest request, HttpListener<UserResponse> callback);

    @Put(url = "https://api.example.com/users/{id}")
    void update(@Path("id") String id, @Body UserRequest request, HttpListener<UserResponse> callback);

    @Delete(url = "https://api.example.com/users/{id}")
    void delete(@Path("id") String id, HttpListener<String> callback);

    @Get(url = "https://api.example.com/users", connect = 5, read = 10)   // timeouts in SECONDS
    void slow(HttpListener<List<UserResponse>> callback);
}
```

* Method annotations: `@Get`, `@Post`, `@Put`, `@Patch`, `@Delete`. Their attributes are `url`, `key` (a base URL, joined as `key + url`), `connect` / `read` (seconds), and `contentType` (default `application/json`).
* Parameter annotations: `@Path`, `@Query`, `@Header`, `@Body`. A parameter of type `HttpHeaders` is merged into the headers.
* Methods must return `void`, and the last parameter is an `HttpListener<T>`.

### 3.2 Call it

```java
UserRepository repository = new RestClient().create(UserRepository.class);

repository.create(new UserRequest("Budi"), new HttpListener<UserResponse>() {
    @Override
    public void onSuccess(HttpResponse<UserResponse> response) {
        UserResponse body = response.getBody();      // also getCode(), getHeaders(), getMessage()
    }

    @Override
    public void onError(HttpErrorResponse error) {
        Log.e(TAG, error.getCode() + " " + error.getMessage() + " " + error.getError(), error.getException());
    }
});
```

### 3.3 Models

```java
public class UserRequest {
    @Expose private String name;        // without @Expose the field is neither sent nor read
    public UserRequest(String name) { this.name = name; }
}
```

For responses shaped as a `{code, message, result}` envelope, use the built-in `Response<T>` (`success()`, `error()`).

### 3.4 File upload (multipart)

Multipart is not supported through the interface. Use `RestRequest`:

```java
try {
    HttpMultipart multipart = new HttpMultipart(context, "https://api.example.com/upload");   // opens the connection: IOException
    multipart.setParam("description", "store photo");
    multipart.setParamFile("photo", file);
    RestRequest.post(multipart, new HttpListener<UploadResponse>() { ... });
} catch (IOException e) {
    showError("Upload could not start");
}
```

### 3.5 Logging

```java
RestLog.setLevel(RestLog.Level.BODY);          // NONE (default), BASIC, HEADERS, BODY
RestLog.addSensitiveHeader("X-Session");       // this header's value is masked in the log
```

`Authorization`, `Cookie`, `Set-Cookie`, `Proxy-Authorization` and `X-Api-Key` are always masked.

### 3.6 Pitfalls

* **The listener must be an anonymous class or a named class with a concrete type.** The response type is read from the listener's generic type; a lambda is not possible (two methods), and an erased generic type turns the body into a `String`.
* **JSON is only parsed when the response `Content-Type` is `application/json`.** Otherwise the body is the raw `String`, and a `ClassCastException` follows where it is used.
* Timeouts are in seconds, with an effective minimum of 1.5 s (connect) and 4.5 s (read).
* Non-2xx statuses go to `onError` with an empty `getMessage()`. Read `getCode()`.
* 204 goes to `onSuccess` with a `null` body.
* A `null` `@Header` / `@Query` value is sent as the text `"null"`.
* `http://` (non-https) URLs on Android 9+ need a network security config in the app.

---

## 4. RefSession: storing small data

A key-value store that can also store objects (through its own Gson). Good for tokens, settings, the logged-in user. **Not** suitable for large, growing data (history, transactions): use the app's database for those.

### 4.0 How it stores

* Values live in a SQLite table (`databases/ref_session.db`, one row per key) behind an in-memory copy.
* **Reads come from memory.** The table is loaded once in the background by `ContextHelper.init(this)`; a read in the first milliseconds waits for it.
* **Writes change memory right away** (the next read sees them) and reach the table on a background thread; many writes, or many writes of the same key, become one transaction. The main thread does not wait for the disk.
* Pending writes are flushed when the app goes to the background (no activity started), so Android killing the process later loses nothing. Call `session.flush()` to wait for them yourself, e.g. right after login.
* **Upgrading is automatic:** the first time, the values of the old SharedPreferences file `RefSession.xml` move into the table and the file is emptied, so users stay logged in.

### 4.1 Types

```java
RefSession session = new RefSession();          // needs ContextHelper.init in Application (section 2)

session.saveString("API_TOKEN", token);
session.saveInt("PIN_TRIES", 3);
session.saveLong("LAST_SYNC", System.currentTimeMillis());
session.saveFloat("RATIO", 0.25f);
session.saveDouble("LAT", -6.2005);             // kept as text: exact, NaN allowed
session.saveBoolean("ONBOARDED", true);
session.saveDecimal("BALANCE", new BigDecimal("1500000.75"));   // money: every digit kept
session.saveDate("SYNCED_AT", new Date());      // epoch millis: no text format, no locale
session.saveSet("READ_IDS", ids);               // Set<String>, order kept
session.saveEnum("STATUS", Status.OPEN);        // by name
session.saveBytes("HASH", hash);                // small binary values only; keep files on disk
session.saveUuid("DEVICE_ID", UUID.randomUUID());
session.save("USER", user);                     // an object, as JSON (@Expose fields)
session.save("STOCK", stockBySku);              // any generic value, e.g. Map<String, Integer>
session.saveCollection("MENU", menus);          // List<T>
```

```java
String token     = session.findString("API_TOKEN");             // null when missing
int tries        = session.findInt("PIN_TRIES", 3);             // fallback when missing
long lastSync    = session.findLong("LAST_SYNC");               // 0 when missing
BigDecimal saldo = session.findDecimal("BALANCE");
Date syncedAt    = session.findDate("SYNCED_AT");
Set<String> read = session.findSet("READ_IDS");
Status status    = session.findEnum("STATUS", Status.class, Status.OPEN);   // fallback for unknown names
User user        = session.find("USER", User.class);
Map<String, Integer> stock = session.find("STOCK", new TypeToken<Map<String, Integer>>() {}.getType());
List<Menu> menus = session.findCollection("MENU", Menu.class);

boolean known = session.contains("PIN_TRIES");  // tells "missing" from 0 / false

session.delete("API_TOKEN", "USER");
```

Reads never crash on a type mismatch. A value saved as another type is converted when that is exact (`saveLong(5L)` read with `findInt` gives 5); otherwise the fallback / `null` comes back and a warning is logged. Avoid `double` / `float` for money (`0.1 + 0.2 != 0.3`): use `saveDecimal`.

### 4.2 Secrets: the `Secure` methods

Tokens, PINs and personal data use the same methods with a **`Secure` suffix**, for every type. Each value is encrypted with an AES-GCM key kept in the **Android Keystore** (the key never leaves the phone's secure hardware) and stored in the same RefSession file under its key + `_SECURE`, so the clear text is never on disk.

```java
RefSession session = new RefSession();

session.saveStringSecure("API_TOKEN", login.getAccessToken());
session.saveStringSecure("REFRESH_TOKEN", login.getRefreshToken());
session.saveSecure("PROFILE", login.getProfile());
session.saveDecimalSecure("BALANCE", balance);
session.saveIntSecure("PIN_TRIES", 3);

String token    = session.findStringSecure("API_TOKEN");      // decrypted once per process, then from memory
Profile profile = session.findSecure("PROFILE", Profile.class);
int tries       = session.findIntSecure("PIN_TRIES", 3);

session.deleteSecure("REFRESH_TOKEN");
session.clearSecure();                                        // logout: every secret gone, other values stay
```

| Plain | Secure |
|---|---|
| `saveString` / `findString` | `saveStringSecure` / `findStringSecure` |
| `saveInt`, `saveLong`, `saveFloat`, `saveDouble`, `saveBoolean` | `saveIntSecure`, `saveLongSecure`, `saveFloatSecure`, `saveDoubleSecure`, `saveBooleanSecure` (finders with a fallback too) |
| `save(key, obj)` / `find(key, Class or Type)` | `saveSecure` / `findSecure` |
| `saveCollection` / `findCollection` | `saveCollectionSecure` / `findCollectionSecure` |
| `saveDecimal`, `saveDate`, `saveSet`, `saveEnum`, `saveBytes`, `saveUuid` | the same names + `Secure` |
| `contains`, `delete` | `containsSecure`, `deleteSecure`, `clearSecure` |

* A copied file (backup, rooted phone) cannot be decrypted elsewhere.
* A value that cannot be decrypted (key lost after a reset, backup restored on another phone) reads as missing and is removed: handle it as "not logged in".
* Secrets and plain values are separate: `findString("API_TOKEN")` does not see `saveStringSecure("API_TOKEN", ...)`.
* Encryption protects data at rest. It does not stop code running inside the app on a rooted phone; pair it with short-lived access tokens and server-side revocation on logout.

### 4.3 Pitfalls

* Keys are upper-cased, so `"token"` and `"TOKEN"` are the same key.
* A write is in memory at once and on disk within milliseconds. For a value that must survive an immediate crash (a token right after login), call `session.flush()`.
* Values are per process: a service in another process (`android:process`) sees its own copy.
* Stored objects must mark their fields with `@Expose` (see section 5).
* `save(user)` / `find(User.class)` use the class name as the key: two classes with the same simple name collide, and R8 renames classes in release builds. Prefer `save("USER", user)` / `find("USER", User.class)`.
* Saving a `null` value throws a `NullPointerException`; use `delete`.
* Reading a generic value without its type (`find(key, Map.class)`) turns every number into a `Double`; pass the full type with `TypeToken`.

---

## 5. Gson helper and `@Expose`

One shared Gson instance for REST, RefSession and the adapters.

```java
Gson gson = GsonHelper.newInstance().getGson();
DeeplinkResult result = gson.fromJson(json, DeeplinkResult.class);
```

* Only `@Expose` fields are serialized. This is the most important rule in the whole library.
* A `null` string is written as `""`, and leading / trailing spaces are trimmed.
* Dates are read from the formats `yyyy-MM-dd'T'HH:mm:ss.SSSZ`, `yyyy-MM-dd HH:mm:ss`, `yyyy-MM-dd`, `HH:mm:ss`.
* To change the configuration: `GsonHelper.newInstance().initialize(new GsonBuilder()...)` (`@Expose` is still enforced).
* Avoid the static field `GsonHelper.gson`: it is `null` until `newInstance()` has been called once.

---

## 6. BeanCopy: copying between objects

Copies fields with the same name (case-insensitive) and the **same type**, superclass fields included.

```java
Person person = BeanCopy.value(employee, Person.class);              // new object
List<Person> people = BeanCopy.values(employees, Person.class);
BeanCopy.copyValue(employee, existingPerson, "id");                  // fill an existing object, except "id"
Person deep = BeanCopy.gson(employee, Person.class);                 // deep copy through Gson
```

Which field goes where is worked out once per source class, target class and ignore list, then reused: repeated copies are fast and safe from several threads.

Pitfalls:

* Fields with a different type are silently skipped, `int` vs `Integer` included. In the sample, `id` String to `int` is not copied.
* `transient` and `static` fields are never copied.
* The target class needs a public no-argument constructor for `value` / `values`.
* The copy is shallow: lists and nested objects are shared. Use `gson` for a deep copy (it keeps `Date` milliseconds and `NaN`).

---

## 7. RecyclerView adapters

Generic ViewBinding-based adapters. You do not write `onCreateViewHolder`, `getItemViewType` or `inflate`: register a holder per type, and the library picks the holder for each row from its `type`. One adapter can hold many kinds of rows (**dynamic holders**), like a store home page: banner, titles, categories, products and a loader in a single RecyclerView.

### 7.1 Concepts

| Part | Role |
|---|---|
| `AdapterModel<T>` | One row: `type` (which holder) + `model` (its data). Default `type` = 1 |
| `AdapterGeneric<E extends AdapterModel>` | The adapter. In `registerHolder()` you map each `type` to a holder class |
| `HolderGeneric<Binding, E>` | The holder. The binding is inflated automatically from the first generic; `binding`, `value` and `context` are ready to use |
| `HolderGeneric.Listener` | Marker interface for events from a holder to the screen (click, quantity change, ...) |

Rules for a holder to be created automatically:

* A `public static` class (or a public class in its own file).
* A `public` constructor with one parameter of **exactly the binding type** used as the first generic.
* The first generic is a generated ViewBinding class (`viewBinding true`).

### 7.2 A single kind of row

```java
public class SampleMV extends AdapterModel<Sample> {
    public SampleMV(Sample sample) { super(sample); }
}

public class SampleAdapter extends AdapterGeneric<SampleMV> {

    @Override
    protected void registerHolder() {
        registerBindView(SampleHolder.class);          // = registerBindView(1, ...)
    }

    public static class SampleHolder extends HolderGeneric<AdapterSampleBinding, SampleMV> implements View.OnClickListener {

        public SampleHolder(AdapterSampleBinding binding) {
            super(binding);
            binding.getRoot().setOnClickListener(this);
        }

        @Override
        protected void onBindViewHolder(SampleMV model, int position) {
            super.onBindViewHolder(model, position);    // required: sets the `value` field
            binding.name.setText(model.getModel().getName());
        }

        @Override
        public void onClick(View view) {
            Listener listener = getListener(Listener.class);
            if (Objects.isNull(listener)) return;
            listener.onSelected(getCurrentPosition(), value.getModel());
        }

        public interface Listener extends HolderGeneric.Listener {
            void onSelected(int index, Sample sample);
        }
    }
}
```

```java
public class MainActivity extends BaseActivity implements SampleAdapter.SampleHolder.Listener {

    private final SampleAdapter adapter = new SampleAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        UIHelper.initRecyclerView(binding.rvSample, adapter);
        adapter.setHolderListener(this);
        adapter.setValue(items);
    }

    @Override
    public void onSelected(int index, Sample sample) { ... }
}
```

### 7.3 Dynamic holders: many kinds of rows in one adapter

This pattern is used in fuguh (`AdapterHome`, `AdapterPayment`, `AdapterTransactionDetail`, `AdapterKeyword`): one screen is built from different rows, each with its own holder.

**1. Define the types as constants.** Type numbers must be > 0, unique, and the same in the adapter and wherever the list is built.

```java
public final class HomeType {
    public static final int BANNER = 1;
    public static final int TITLE = 2;
    public static final int PRODUCT = 3;
    public static final int CATEGORY = 5;
    public static final int LOADER = 100;      // "loading..." row at the end of the list
    private HomeType() { }
}
```

**2. One model for every row.** The model carries the data every kind of holder needs; each holder reads only its part.

```java
public class HomeModel extends AdapterModel<Home> {

    public HomeModel(int type) { super(type); }                  // a row without data, e.g. the loader

    public HomeModel(int type, Home home) { super(type, home); }

    public HomeModel(int type, Product product) {                 // shortcut for product rows
        super(type);
        Home home = new Home();
        home.setProduct(product);
        setModel(home);
    }
}
```

**3. Register a holder per type.** Holders can be nested in the adapter or live in their own files (`HolderKeywordHeader.java`, ...) once the adapter grows. One holder class may serve more than one type (in fuguh, `AdapterPayment` uses `HolderPaymentProduct` for types 3 and 4).

```java
public class AdapterHome extends AdapterGeneric<HomeModel> {

    @Override
    protected void registerHolder() {
        registerBindView(HomeType.BANNER, HolderHomeBanner.class);
        registerBindView(HomeType.TITLE, HolderTitle.class);
        registerBindView(HomeType.PRODUCT, HolderHomeProduct.class);
        registerBindView(HomeType.CATEGORY, HolderHomeCategory.class);
        registerBindView(HomeType.LOADER, HolderHomeLoader.class);
    }

    public static class HolderTitle extends HolderGeneric<HolderTitleBinding, HomeModel> implements View.OnClickListener {

        public HolderTitle(HolderTitleBinding binding) {
            super(binding);
            binding.all.setOnClickListener(this);
        }

        @Override
        protected void onBindViewHolder(HomeModel o) {
            super.onBindViewHolder(o);
            binding.title.setText(o.getModel().getTitleSpace().getTitle());
        }

        @Override
        public void onClick(View view) {
            HolderTitleListener listener = getListener(HolderTitleListener.class);
            if (Objects.isNull(listener)) return;
            listener.onSeeAll(value.getModel().getTitleSpace());
        }

        public interface HolderTitleListener extends HolderGeneric.Listener {
            void onSeeAll(TitleSpace titleSpace);
        }
    }

    public static class HolderHomeProduct extends HolderGeneric<HolderProductBinding, HomeModel> {

        public HolderHomeProduct(HolderProductBinding binding) { super(binding); }

        @Override
        protected void onBindViewHolder(HomeModel o, int position) {
            super.onBindViewHolder(o, position);
            Product product = o.getModel().getProduct();
            binding.name.setText(product.getName());
            binding.price.setText(NFormat.currencyFormat(product.getPriceTotal()));
        }
    }

    public static class HolderHomeLoader extends HolderGeneric<HolderLoaderBinding, HomeModel> {

        public HolderHomeLoader(HolderLoaderBinding binding) { super(binding); }

        @Override
        protected void onBindViewHolder(HomeModel o) {
            super.onBindViewHolder(o);                  // a static progress view, no data
        }
    }

    // HolderHomeBanner, HolderHomeCategory: see 7.9 for a holder containing another RecyclerView
}
```

**4. Build the list in display order.** Usually in a ViewModel / presenter, from the API response.

```java
List<HomeModel> rows = new ArrayList<>();
if (!response.getBanner().isEmpty()) rows.add(new HomeModel(HomeType.BANNER, new Home(response.getBanner())));

Home brandTitle = new Home();
brandTitle.setTitleSpace(new TitleSpace("Brands", "All", 1));
brandTitle.setBrand(response.getProductBrand());
rows.add(new HomeModel(HomeType.TITLE, brandTitle));
rows.add(new HomeModel(HomeType.CATEGORY, brandTitle));

Home productTitle = new Home();
productTitle.setTitleSpace(new TitleSpace("Products", "All", 1));
rows.add(new HomeModel(HomeType.TITLE, productTitle));
for (Product product : response.getProduct()) rows.add(new HomeModel(HomeType.PRODUCT, product));
if (response.getProduct().size() == PAGE_SIZE) rows.add(new HomeModel(HomeType.LOADER));   // there is a next page

adapter.setValue(rows);
```

**5. The screen implements the holder listeners it needs.** A single `setHolderListener(this)` serves every holder; each holder looks up its own interface with `getListener(...)`.

```java
public class HomeFragment extends BaseFragment implements
        AdapterHome.HolderHomeBanner.HolderHomeBannerListener,
        AdapterHome.HolderHomeCategory.HolderHomeCategoryListener,
        AdapterHome.HolderTitle.HolderTitleListener {

    private final AdapterHome adapter = new AdapterHome();

    private void initList() {
        adapter.setHolderListener(this);
        ...
    }

    @Override public void onSelectedBanner(Banner banner) { ... }
    @Override public void onSelectedBrand(Brand brand) { ... }
    @Override public void onSeeAll(TitleSpace titleSpace) { ... }
}
```

An unregistered type crashes the app when that row is shown (`Null holder class, not define before`). Registering the same type twice fails right away in the adapter constructor (`holder already register!`).

### 7.4 Choosing an `onBindViewHolder`

Override **only one**; the library calls only the overloads that are actually overridden. Always call `super` so `value` is set.

| Overload | Use when |
|---|---|
| `onBindViewHolder(E o)` | the data is enough |
| `onBindViewHolder(E o, int position)` | you need the position (row number, alternating colours) |
| `onBindViewHolder(E o, int position, int size)` | you need the row count (e.g. "3 of 10") |
| `onBindViewHolder(E o, int position, boolean isFirst, boolean isLast)` | hide the divider on the last row, round the corners at the start / end |
| `onBindViewHolder(List<E> list, int position)` / `(List<E>, int, int)` | you need neighbouring rows (e.g. a date header when the date differs from the previous row) |

`getCurrentPosition()` returns the position of the last bind; use it when handling clicks.

### 7.5 Listeners from holder to screen

* Declare the interface inside the holder, extending `HolderGeneric.Listener`, with a name unique per holder.
* The screen (activity / fragment / parent holder) implements it, then calls `adapter.setHolderListener(this)`.
* `setHolderListener` may be called several times for several listener objects; all of them reach the holders.
* `getListener(X.class)` only recognises interfaces implemented **directly** by the listener's class. If `BaseHomeFragment implements X` and `HomeFragment extends BaseHomeFragment`, `getListener` returns `null`; implement it in the class you register.
* Always check for `null` before calling a listener.

### 7.6 Changing data

| Method | Effect |
|---|---|
| `setValue(list)` | replace everything |
| `setValue(list, () -> {...})` | replace everything, then run an action once the list is shown |
| `setValueToTop(list)` | replace, then scroll to the first row (e.g. after a new sort) |
| `addMore(item)` / `addMore(list)` | append at the end |
| `updateItem(index, item)` | replace one row (stored as a copy) |
| `clear()` | empty the list |
| `getItem(position)` | the row **currently shown** at that position |
| `getAdapterList()` | a copy of the last list set, including changes not shown yet |
| `setKeepAtTop(true)` | when the list is at the very top, keep it there as new rows arrive above |

Updating one row from a holder click (sample `AdapterSampleActivity`):

```java
@Override
public void updateAge(int index, SampleDataMV row) {
    row.getModel().setAge(row.getModel().getAge() + 1);
    adapter.updateItem(index, row);
}
```

To remove or insert in the middle: take `getAdapterList()`, change the copy, then `setValue(list)`. With a diff (7.7), only the rows that changed are redrawn.

### 7.7 Diff: redraw only the rows that changed

```java
@Override
protected void registerHolder() {
    registerBindView(HomeType.TITLE, HolderTitle.class);
    registerBindView(HomeType.PRODUCT, HolderHomeProduct.class);
    registerAsyncListDiffer(new DiffUtil.ItemCallback<HomeModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull HomeModel a, @NonNull HomeModel b) {
            if (a.getType() != b.getType()) return false;
            if (a.getType() == HomeType.PRODUCT) {
                return Objects.equals(a.getModel().getProduct().getProductId(), b.getModel().getProduct().getProductId());
            }
            return true;                                // single row per type: banner, loader
        }

        @Override
        public boolean areContentsTheSame(@NonNull HomeModel a, @NonNull HomeModel b) {
            if (a.getType() == HomeType.PRODUCT) {
                return Objects.equals(a.getModel().getProduct().getPriceTotal(), b.getModel().getProduct().getPriceTotal());
            }
            return false;                               // not sure: redraw
        }
    });
}
```

* `areItemsTheSame` = the same row (id); `areContentsTheSame` = its content did not change.
* **Compare ids, not just `type`.** If every product counts as "the same row" because they share a type, the diff cannot detect products that moved: animations go wrong and holders are bound with mismatched data.
* With a diff, `setValue` / `addMore` are applied **asynchronously**. Run follow-up actions (scrolling, totals) in the `Runnable` of `setValue(list, () -> ...)`, and remember that `getItem()` reflects what is shown while `getAdapterList()` already holds the latest data.

### 7.8 Mixed grid: full-width titles, two-column products

**GridLayoutManager**: set the width per type.

```java
GridLayoutManager grid = new GridLayoutManager(context, 2);
grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
    @Override
    public int getSpanSize(int position) {
        return adapter.getItem(position).getType() == HomeType.PRODUCT ? 1 : 2;   // everything but products is full width
    }
});
binding.rvHome.setLayoutManager(grid);
binding.rvHome.setAdapter(adapter);
adapter.setHolderListener(this);
```

**StaggeredGridLayoutManager** (cards of varying height, like the fuguh home): mark full-width holders in the holder itself.

```java
@Override
protected void onBindViewHolder(HomeModel o) {
    super.onBindViewHolder(o);
    ViewGroup.LayoutParams params = itemView.getLayoutParams();
    if (params instanceof StaggeredGridLayoutManager.LayoutParams) {
        ((StaggeredGridLayoutManager.LayoutParams) params).setFullSpan(true);
    }
    ...
}
```

After `setValue` on a staggered grid, call `layoutManager.invalidateSpanAssignments()` so the columns are laid out again (fuguh does this in its LiveData observer).

Ready-made helpers for simple cases: `UIHelper.initRecyclerViewGrid(rv, adapter, columnWidthPx)` (the column count follows the screen width) and `UIHelper.initRecyclerViewGridInLine(rv, adapter, columnCount)`.

### 7.9 A holder containing another RecyclerView (horizontal list inside a list)

The fuguh `HolderHomeCategory`: the "Brands" row holds a horizontal grid of categories. The inner adapter is created **once** in the holder constructor and refilled on every bind. Clicks from the inner adapter are forwarded to the screen's listener.

```java
public static class HolderHomeCategory extends HolderGeneric<HolderCategoryRowBinding, HomeModel>
        implements AdapterCategory.HolderCategoryItem.HolderCategoryListener {

    private final AdapterCategory adapter = new AdapterCategory();
    private List<Brand> brands = new ArrayList<>();

    public HolderHomeCategory(HolderCategoryRowBinding binding) {
        super(binding);
        binding.rvCategory.setLayoutManager(new GridLayoutManager(getContext(), 2, GridLayoutManager.HORIZONTAL, false));
        binding.rvCategory.setItemAnimator(null);
        binding.rvCategory.setAdapter(adapter);
        adapter.setHolderListener(this);                 // this holder listens to the inner adapter's clicks
    }

    @Override
    protected void onBindViewHolder(HomeModel o) {
        super.onBindViewHolder(o);
        brands = new ArrayList<>(o.getModel().getBrand());
        List<CategoryModel> rows = new ArrayList<>();
        for (Brand brand : brands) rows.add(new CategoryModel(2, new Category(brand)));
        adapter.setValue(rows);
    }

    @Override
    public void onSelectedCategory(Category category, int index) {
        HolderHomeCategoryListener listener = getListener(HolderHomeCategoryListener.class);   // forward to the screen
        if (Objects.isNull(listener)) return;
        listener.onSelectedBrand(brands.get(index));
    }

    public interface HolderHomeCategoryListener extends HolderGeneric.Listener {
        void onSelectedBrand(Brand brand);
    }
}
```

For many similar horizontal rows, share one `RecyclerView.RecycledViewPool` across all inner RecyclerViews so holders are reused between rows.

### 7.10 Pagination

**Option A, built in (`LinearLayoutManager` only):**

```java
UIHelper.initRecyclerView(binding.rv, adapter,
        (RecyclerViewScrollListener.PaginationListener<SampleDataMV>) last -> loadNextPage(last), true);
adapter.setHolderListener(this);

private void onPageLoaded(List<SampleDataMV> rows) {
    adapter.addMore(rows);         // also clears the "loading" state
}
```

* The last argument `true` (`isLoadFirst`) means the first page is loading: load the first page yourself, then `setValue(rows)`; that clears the loading state.
* `loadMoreItems(last)` is called when the last row becomes visible, with the last row as the anchor for the next page.
* While loading, the adapter is in the loader state (`isLoader()`), so it does not call again; the state is cleared by `addMore(list)` / `setValue(list)` / `removeLoader()`.
* Pagination only kicks in once the list has at least 15 rows (the minimum page size).
* **Track "no more pages" yourself.** If the last page is empty and you still call `addMore`, the next scroll asks for a page again. For example: `if (rows.isEmpty()) { endReached = true; adapter.removeLoader(); return; }`, and ignore `loadMoreItems` while `endReached`.
* `showLoader()` is only a state; no "loading..." row is drawn.

**Option B, a loader row as its own type (the fuguh way, works for grids / staggered grids):**

1. Add `new HomeModel(HomeType.LOADER)` at the end of the list when there is a next page (7.3 step 4).
2. In `addOnScrollListener`, when near the end and the last row is a `LOADER`, request the next page, anchored on the row before the loader:

```java
binding.rvHome.addOnScrollListener(new RecyclerView.OnScrollListener() {
    @Override
    public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
        if (dy <= 0 || loading) return;
        int total = layoutManager.getItemCount();
        if (total == 0 || lastVisible(layoutManager) + 5 < total) return;
        HomeModel last = adapter.getItem(total - 1);
        if (last.getType() != HomeType.LOADER) return;          // no more pages
        loading = true;
        viewModel.nextProduct(adapter.getItem(total - 2).getModel().getProduct());
    }
});
```

3. When the page arrives: drop the loader row, append the new products, add the loader again if there is another page, then `setValue`.

```java
List<HomeModel> rows = new ArrayList<>();
for (HomeModel row : adapter.getAdapterList()) if (row.getType() != HomeType.LOADER) rows.add(row);
for (Product product : page) rows.add(new HomeModel(HomeType.PRODUCT, product));
if (page.size() == PAGE_SIZE) rows.add(new HomeModel(HomeType.LOADER));
adapter.setValue(rows, () -> loading = false);
```

Advantages of option B: users see a "loading..." row, it works with every layout manager, and the end of the data is explicit (no loader row means no more pages).

### 7.11 Basic adapter: a simple list without `AdapterModel`

For a single kind of row holding plain objects (e.g. Bluetooth devices), use `AdapterGenericBasic<Holder, T>`.

```java
public class AdapterDevice extends AdapterGenericBasic<AdapterDevice.HolderDevice, BluetoothDevice> {

    public static class HolderDevice extends HolderGenericBasic<AdapterDeviceBinding, BluetoothDevice> {

        public HolderDevice(AdapterDeviceBinding binding) {
            super(binding);
            binding.getRoot().setOnClickListener(v -> {
                HolderDeviceListener listener = getListener(HolderDeviceListener.class);
                if (Objects.nonNull(listener)) listener.onSelectedDevice(value);
            });
        }

        @Override
        protected void onBindViewHolder(BluetoothDevice device) {    // abstract: required
            binding.name.setText(device.getName());
            binding.address.setText(device.getAddress());
        }
    }

    public interface HolderDeviceListener extends HolderGenericBasic.Listener {
        void onSelectedDevice(BluetoothDevice device);
    }
}
```

```java
UIHelper.initRecyclerView(binding.rvDevice, adapter, this);   // HolderGenericBasic.Listener variant: the listener is registered
adapter.setValues(devices);
adapter.addMore(device);
```

Inside `onBindViewHolder(device)` use the parameter, not `value`: `value` is only set **after** that method returns (in the click handler `value` is correct).

The basic adapter has no diff, multiple types or pagination; when you need them, use `AdapterGeneric`.

### 7.12 Adapter pitfalls

* `UIHelper.initRecyclerView(rv, adapter, HolderGeneric.Listener)` does **not** register the listener. Always call `adapter.setHolderListener(this)`. (The `HolderGenericBasic.Listener` variant works.)
* The adapter must directly `extend AdapterGeneric<ConcreteModel>`; an intermediate generic adapter makes `updateItem` fail to resolve the model type.
* `updateItem` stores a Gson copy: the model's fields need `@Expose`, and the object you hold is no longer the one in the adapter.
* Holders are reused across rows: always reset every view in bind (including `setVisibility` and colours), not only for some conditions.
* A holder that creates a `Handler`, timer or carousel (banner) must stop it itself; fuguh restarts the banner through `adapter.resumeBanner()` in `onResume`.
* Call every adapter method from the main thread.

---

## 8. BaseActivity and BaseFragment

Base classes with a `session` (RefSession) and UI shortcuts.

```java
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(binding.rvSample, adapter);                       // = UIHelper.initRecyclerView
        session.saveString("LAST_SCREEN", "main");

        binding.save.setOnClickListener(v -> {
            if (isEmpty(binding.name, binding.phone)) {
                error("Required", binding.name, binding.phone);
                return;
            }
            shortToast("Saved: " + string(binding.name));
        });
    }
}
```

Also available: `initHorizontal`, `initGrid`, `hideInput`, `clearError`, `dpToPx`, `shortSnack` / `longSnack`, and `api()` (the older REST builder). Form validation in the library is limited to `isEmpty` + `error`.

---

## 9. NumPad: numeric input

Integer / decimal input with its own keypad (dialog or BottomSheet), with min / max and a locale-aware decimal separator.

### 9.1 From XML

```xml
<com.onevour.core.components.numpad.NumPadField
    android:id="@+id/input_amount"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:isDecimal="true"
    app:isShowMax="true"
    app:minValue="0"
    app:maxValue="50000000"
    app:useBottomSheet="true"
    app:titleText="Payment amount" />
```

Style attributes: `dialogFontFamily`, `dialogBackground`, `dialogTitleColor`, `dialogTitleTextSize`, `dialogResultColor`, `dialogResultTextSize`, `dialogKeyTextColor`, `dialogKeyTextSize`, `dialogKeyBackground`.

### 9.2 From code

```java
NumberFormat indonesia = NumberFormat.getNumberInstance(new Locale("id", "ID"));
indonesia.setMinimumFractionDigits(2);
indonesia.setMaximumFractionDigits(2);

NumPad numPad = new NumPad();
numPad.setUseBottomSheet(true);                          // BEFORE setup
numPad.setup(binding.inputPrice, indonesia, 0, 10_000_000.0);
numPad.setListener(new NumPad.Listener() {
    @Override public void onSubmitValue() { }
    @Override public void onValue(int id, boolean isDecimal, int intValue, double doubleValue) {
        total = doubleValue;
    }
});

@Override
protected void onDestroy() {
    numPad.destroy();                                    // required: shuts down the internal thread
    super.onDestroy();
}
```

A `null` `NumberFormat` means integer mode. Styling from code: `new NumPadStyle.Builder().setDialogBackgroundColor(...).setKeyTextColor(...).build()`, then `setStyle(style)`; text sizes are in **pixels**.

### 9.3 Pitfalls

* `setUseBottomSheet(true)` must come before `setup()`. For `NumPadField`, use the XML attribute `app:useBottomSheet`.
* `NumPadField` inside a RecyclerView: the field calls `destroy()` when detached from the window, so it cannot be reused. Use a plain `NumPad` in the holder.
* A decimal `NumberFormat` must be a `DecimalFormat`.
* A max of `Double.MAX_VALUE` means no limit (the max label is hidden).

---

## 10. Fragment navigation

```java
// replace the fragment and keep it on the back stack
FragmentNavigation.addBackStack(this, R.id.container, PageOneFragment.newInstance(), "A");

@Override
public void onBackPressed() {
    if (FragmentNavigation.onBackPressed(this, "A")) super.onBackPressed(); else finish();
}
```

Bottom navigation that keeps each tab's state:

```java
manager.onCreateStateHandler(savedInstanceState);
manager.setup(this, R.id.container, getSupportFragmentManager(), binding.navigation, this);
manager.register(R.id.nav_home, HomeFragment.newInstance(), "HOME");
manager.register(R.id.nav_profile, ProfileFragment.newInstance(), "PROFILE");
manager.active(R.id.nav_home);

@Override public boolean onNavigationItemSelected(@NonNull MenuItem item) { return manager.onNavigationItemSelected(item); }
@Override protected void onSaveInstanceState(@NonNull Bundle out) { manager.onSaveInstanceState(out); super.onSaveInstanceState(out); }
```

Pitfalls: only for `AppCompatActivity` (not child fragments), it uses `commit()` (avoid after `onSaveInstanceState`), and the active tab is not restored correctly when the activity is recreated.

---

## 11. EventBus `MessageEvent`

A generic `(event, value)` payload for greenrobot EventBus. Add `org.greenrobot:eventbus` to the app.

```java
EventBus.getDefault().post(new MessageEvent("PRINT_DONE", json));

@Override protected void onStart() { super.onStart(); EventBus.getDefault().register(this); }
@Override protected void onStop() { EventBus.getDefault().unregister(this); super.onStop(); }

@Subscribe(threadMode = ThreadMode.MAIN)
public void onMessageEvent(MessageEvent event) {
    if ("PRINT_DONE".equalsIgnoreCase(event.getEvent())) handle(event.getValue());
}
```

---

## 12. Number and date formatting

```java
String amount  = NFormat.currencyFormat(5_603_169.26);   // device locale, 2 decimals
double value   = NFormat.currencyParse("5.603.169,26");   // 0 when it fails
String percent = NFormat.percentFormat(0.125);

String today   = DTFormat.now();                          // yyyy-MM-dd
String time    = DTFormat.formatTime(new Date());         // HH:mm
String stamp   = DTFormat.nowFull();                      // yyyyMMddHHmmss
```

`DTFormat` uses static `SimpleDateFormat` instances, which are not thread-safe: call it from a single thread (main).

---

## 13. Other utilities

| Class | Use for | Notes |
|---|---|---|
| `ValueOf` / `UIValue` | `isNull`, `isEmpty` (trims spaces), `nonEmpty`, `isZero`, `string(editText)`, `clearError` | static |
| `DimensionValue.dpToPx(int)` | dp to pixels | needs `ContextHelper.init` |
| `UIHelper` | `hideSoftInput`, `setMarginsInDP`, `initRecyclerViewGrid`, `snapScroll` | |
| `GeoHelper.distance(lat1, lon1, lat2, lon2, GeoHelper.unitKilometer)` | distance between two coordinates | |
| `JWTCommons.isExpired(token)` | check whether a JWT has expired | |
| `ImageHelper` | `compressImage`, `toBase64`, `rotateBitmapByDegree`, `loadImageRounded` | |
| `Loader`, `ButtonLoader` | loading / success / error state views | must be inflated from XML |
| `AutoFitGridLayoutManager`, `ViewPagerState` | fixed-column-width grid, ViewPager that keeps state | |
| `PermissionUtils` | request a fixed set of permissions | asks for location, phone and storage at once: better to request only what you need |

Legacy code to avoid in new work:

* `PhotoHandler` (camera): uses `Uri.fromFile`, which fails on Android 7+ and with scoped storage. Use the Activity Result API + `FileProvider`.
* `GPSHelper`: reads an old setting that can be empty on modern Android. Use `commons-sdk-location`.
* `RestRequestBuilder.validateToken()`: token refresh is not active; when the token has expired, the request is silently not executed.

---

## 14. Location capture (`commons-sdk-location`)

Captures the device position while the app asks for it. **When** to track (clock-in, schedule) and **what** to do with a location (store, send) stay in the app.

### 14.1 Two entry points

| | Tracking | Transaction |
|---|---|---|
| API | `init` + `sync` | `current(...)` |
| How it runs | foreground service, continuously async | one shot, awaited with a callback |
| Accuracy | battery saving (Wi-Fi / cell) | high-accuracy GPS |
| When | every interval, after moving the minimum distance | when the user taps a button |

### 14.2 Tracking

```java
// Application#onCreate
LocationCapture.init(this, LocationCapture.Config.builder(
                () -> session.isClockedIn(),                 // does the app still want the position? (clock-in, working hours)
                fix -> repository.storeThenSend(fix))         // a location that passed the filter: store first, then send
        .interval(TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(8), 300f)  // optional
        .notification("Location tracking active", "Getting the device location...", R.drawable.ic_location)
        .build());
LocationCapture.startWatchdog();
```

```java
// from a VISIBLE screen: home, after clock-in, after the permission is granted
LocationCapture.sync(this);    // start when wanted and permitted, stop otherwise

// logout
LocationCapture.stop(this);
```

* `LocationFix` holds `getLatitude()`, `getLongitude()`, `getAccuracy()` (metres, may be `null`), `getTime()`.
* `onFix(location -> ...)` is optional: every raw location, including those the filter drops.
* Changing the interval while running: `LocationCapture.reconfigure(this, newConfig)` (the service restarts).
* Only **"while using the app"** location permission is needed. The service is only started from a visible screen, never from the background or at boot.

Built-in filter (`FixFilter`): not a fake GPS, accuracy ≤ 100 m, not near 0,0, and at least 90% of the minimum interval after the previous delivered location.

### 14.3 Configuration reference

Points per hour while moving ≈ 60 ÷ interval to 60 ÷ minimum interval. Testing on a phone showed locations usually arrive at the minimum interval.

| Target | Interval | Minimum | Distance | Points/hour | Battery |
|---|---|---|---|---|---|
| Very sparse | 30 min | 20 min | 500 m | 2–3 | very low |
| **Library default** | 20 min | 10 min | 500 m | 3–6 | very low |
| Sparse | 15 min | 12 min | 300 m | 4–5 | low |
| Medium | 10 min | 8 min | 300 m | 6–7.5 | low |
| Dense | 6 min | 5 min | 200 m | 10–12 | low |
| Very dense | 5 min | 3 min | 200 m | 12–20 | medium |
| Emulator route testing | 1 min | 1 min | 50 m + `priority(Priority.PRIORITY_HIGH_ACCURACY)` | ±60 | high |

While the phone is stationary (moving less than the minimum distance), no points are sent.

### 14.4 Location for a transaction

```java
private LocationCapture.Pending pending;

private void checkIn() {
    showLoading(true);
    pending = LocationCapture.current(this, 0, 15_000, new LocationCapture.CurrentListener() {
        @Override
        public void onFix(@NonNull LocationFix fix) {
            showLoading(false);
            saveCheckIn(fix.getLatitude(), fix.getLongitude(), fix.getAccuracy());
        }

        @Override
        public void onNoFix(@NonNull LocationCapture.NoFix reason) {
            showLoading(false);
            switch (reason) {
                case NO_PERMISSION: showError("Precise location permission not granted"); break;
                case LOCATION_OFF:  showError("Location is off, turn it on first"); break;
                case MOCK:          showError("Fake GPS detected"); break;
                default:            showError("No GPS signal, try in an open area");
            }
        }
    });
}

@Override
protected void onDestroy() {
    if (pending != null) pending.cancel();      // GPS off, no callback into a screen that is gone
    super.onDestroy();
}
```

A `maxAgeMs` of 0 requires a fresh location. Registering a customer, for example, can allow `120_000` (a GPS location from the last 2 minutes). This works without the tracking service.

### 14.5 Testing on an emulator and a phone

* Emulator routes (Extended controls → Location → Routes) only feed the GPS: use the GPS priority and a short interval while testing, because battery-saving mode gets no locations on an emulator.
* On Xiaomi / Oppo / Vivo / Realme, enable Autostart and set the app's battery usage to "No restrictions" so the service is not killed when the app is swiped away.
* The full sample is the **Location Capture** menu in the `app` module: status, interval & distance settings (with the reference dialog), a simulated home / clock-in / check-in / customer registration with a map, and the tracking history (timeline, map, summary, addresses).

---

## 15. Pitfalls at a glance

1. Call `ContextHelper.init(this)` first in `Application.onCreate()`, and register the class with `android:name` in the manifest.
2. Every JSON model needs `@Expose` (REST, RefSession, adapter `updateItem`).
3. REST listeners: anonymous classes with a concrete type; the server must send `Content-Type: application/json`.
4. Add gson, recyclerview, material and eventbus to the app yourself.
5. Call `adapter.setHolderListener(this)` yourself; holders are `public` with a constructor taking exactly their binding; dynamic holder types are > 0, unique, and compared by id in the diff.
6. `NumPad.destroy()` in `onDestroy`, and `setUseBottomSheet` before `setup`.
7. RefSession is for small data only (it is all kept in memory); history and transactions go to the app's database.
8. Location: `sync` only from a visible screen, and `stop` on logout.
