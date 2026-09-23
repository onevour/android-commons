# MILESTONE — Android REST Repository Library

## Goal

Refactor the existing Android HTTP implementation into a lightweight REST Repository library where consumers only declare repository interfaces and annotations.

Target API:

```java
@RestRepository
public interface UserRepository {

    @GET("/users/{id}")
    void findById(@Path("id") long id, Callback<User> callback);
}
```

Usage:

```java
UserRepository repository =RestClient.create(UserRepository.class);
```

---

# M1 — Stabilize ApiQueue

- [ ] Fix response parsing to use the actual response body
- [ ] Review `multipart.finish()` response handling
- [ ] Handle empty response correctly
- [ ] Handle invalid JSON correctly
- [ ] Review fixed thread pool size
- [ ] Avoid hardcoded `16` threads
- [ ] Define executor configuration
- [ ] Define executor lifecycle
- [ ] Define behavior after executor shutdown
- [ ] Make singleton initialization thread-safe
- [ ] Review `newInstance()` naming
- [ ] Remove default full response logging
- [ ] Avoid logging sensitive response data
- [ ] Keep network execution off the Main Thread
- [ ] Keep callback delivery on the Android Main Thread
- [ ] Review callback executor abstraction

---

# M2 — Separate HTTP Execution and Response Parsing

Current:

```text
ApiQueue
├── Execute HTTP
├── Manage threads
├── Parse JSON
└── Deliver callback
```

Target:

```text
ApiQueue
└── HTTP execution

ResponseParser
└── Response conversion

Callback
└── Result delivery
```

Tasks:

- [ ] Separate response parsing from `ApiQueue`
- [ ] Review Gson dependency
- [ ] Keep Gson as implementation detail
- [ ] Avoid exposing Gson to library consumers
- [ ] Define response parser abstraction

---

# M3 — Replace Fragile Response Type Detection

Review the existing `getResponseType(HttpListener<T> listener)` approach.

- [ ] Remove response type detection from `ApiQueue`
- [ ] Do not rely on listener implementation structure
- [ ] Support explicit `Type`
- [ ] Support simple response types
- [ ] Support generic response types
- [ ] Support `List<T>`
- [ ] Support nested generic types

Target:

```text
Repository Method
        ↓
Callback<T>
        ↓
java.lang.reflect.Type
        ↓
ResponseParser
```

---

# M4 — Create REST Annotations

### Repository

- [ ] `@RestRepository`

### HTTP Methods

- [ ] `@GET`
- [ ] `@POST`
- [ ] `@PUT`
- [ ] `@DELETE`

### Parameters

- [ ] `@Path`
- [ ] `@Query`
- [ ] `@Header`
- [ ] `@Body`

Do not add additional annotations until the core API is stable.

---

# M5 — Implement RestClient

- [ ] Create `RestClient`
- [ ] Validate repository class
- [ ] Ensure repository is an interface
- [ ] Implement `Proxy.newProxyInstance()`
- [ ] Implement `InvocationHandler`
- [ ] Validate REST annotations
- [ ] Define invalid repository httpErrorResponse handling

Target:

```text
RestClient
    ↓
Dynamic Proxy
    ↓
RestInvocationHandler
```

---

# M6 — Implement Annotation Parser

Example:

```java
@GET("/users/{id}")
void findById(
    @Path("id") long id,
    @Query("detail") boolean detail,
    Callback<User> callback
);
```

- [ ] Parse HTTP method
- [ ] Parse URL
- [ ] Parse `@Path`
- [ ] Parse `@Query`
- [ ] Parse `@Header`
- [ ] Parse `@Body`
- [ ] Detect callback parameter
- [ ] Validate invalid parameter combinations

Expected request:

```text
GET /users/10?detail=true
```

---

# M7 — Request Builder

Flow:

```text
Repository Method
        ↓
Annotation Metadata
        ↓
Request Builder
        ↓
HttpRequest
        ↓
ApiQueue
```

- [ ] Create request metadata model
- [ ] Build URL
- [ ] Replace `@Path`
- [ ] Build query parameters
- [ ] Build headers
- [ ] Serialize request body
- [ ] Create `HttpRequest`
- [ ] Execute through `ApiQueue`

---

# M8 — Callback & Response Handling

- [ ] Define `Callback<T>`
- [ ] Define `ApiError`
- [ ] Handle HTTP errors
- [ ] Handle network errors
- [ ] Handle parsing errors
- [ ] Convert response into `T`
- [ ] Deliver callback on Main Thread

Target:

```java
repository.findById(10, new Callback<User>() {

    @Override
    public void onSuccess(User user) {
    }

    @Override
    public void onError(ApiError httpErrorResponse) {
    }
});
```

---

# M9 — Android Compatibility

- [ ] Verify Dynamic Proxy on Android
- [ ] Test minimum supported Android API
- [ ] Test Main Thread callback
- [ ] Test background execution
- [ ] Test Activity recreation
- [ ] Review Context usage
- [ ] Review potential memory leaks

---

# M10 — R8 / ProGuard

Because the REST layer uses runtime annotations and reflection:

- [ ] Enable R8
- [ ] Test release build
- [ ] Test runtime annotations
- [ ] Test Dynamic Proxy
- [ ] Test repository interface
- [ ] Test method annotations
- [ ] Test parameter annotations
- [ ] Create minimal `consumer-rules.pro`
- [ ] Verify consumer does not need manual rules

Test:

```text
Debug
  ↓
Release
  ↓
R8 / Shrinking
  ↓
RestClient.create()
  ↓
Annotation parsing
  ↓
HTTP request
```

---

# M11 — Testing

## ApiQueue

- [ ] Successful request
- [ ] Concurrent requests
- [ ] Network httpErrorResponse
- [ ] HTTP httpErrorResponse
- [ ] JSON httpErrorResponse
- [ ] Empty response
- [ ] Executor shutdown
- [ ] Main Thread callback

## Repository

- [ ] `@GET`
- [ ] `@POST`
- [ ] `@PUT`
- [ ] `@DELETE`
- [ ] `@Path`
- [ ] `@Query`
- [ ] `@Header`
- [ ] `@Body`

## Generic Response

- [ ] `User`
- [ ] `List<User>`
- [ ] `Map<String, User>`
- [ ] Nested generic type

## R8

- [ ] Debug build
- [ ] Release build
- [ ] Minified release build

---

# M12 — Public API Review

### Public

```text
RestClient
Callback
ApiError

@RestRepository
@GET
@POST
@PUT
@DELETE
@Path
@Query
@Header
@Body
```

### Internal

```text
ApiQueue
HttpRequest
HttpMultipart
RestInvocationHandler
RequestBuilder
ResponseParser
GsonHelper
```

- [ ] Review public class visibility
- [ ] Review public method signatures
- [ ] Remove unnecessary public APIs
- [ ] Review naming
- [ ] Review package structure
- [ ] Stabilize API before version `1.0.0`

---

# M13 — Dependency Review

Goal: keep the library lightweight.

- [ ] Review all external dependencies
- [ ] Remove unnecessary dependencies
- [ ] Do not add Joda-Time unless required
- [ ] Keep JSON parser implementation internal
- [ ] Review transitive dependencies
- [ ] Verify final APK/AAB impact

---

# M14 — Documentation & Sample

- [ ] Create README
- [ ] Create API usage documentation
- [ ] Create sample Android project
- [ ] Document annotations
- [ ] Document threading behavior
- [ ] Document httpErrorResponse handling
- [ ] Document R8 behavior
- [ ] Document supported Android versions

---

# M15 — Publishing

- [ ] Configure Maven publishing
- [ ] Configure sources JAR
- [ ] Configure documentation
- [ ] Configure POM metadata
- [ ] Define semantic versioning
- [ ] Create release process
- [ ] Test published artifact
- [ ] Publish first stable version

---

# Final Architecture

```text
                    Public API
                       │
                       ▼
              Repository Interface
                       │
                 Annotations
                       │
                       ▼
                  RestClient
                       │
                Dynamic Proxy
                       │
                       ▼
             RestInvocationHandler
                       │
                 Request Builder
                       │
                       ▼
                    ApiQueue
                       │
                  HTTP Layer
                       │
                       ▼
                Response Parser
                       │
                       ▼
                   Callback<T>
                       │
                       ▼
                  Main Thread
```

---

# Priority

1. M1 — Stabilize ApiQueue
2. M2 — Separate HTTP & Response Parsing
3. M3 — Response Type
4. M4 — REST Annotations
5. M5 — RestClient
6. M6 — Annotation Parser
7. M7 — Request Builder
8. M8 — Callback & Response
9. M9 — Android Compatibility
10. M10 — R8 / ProGuard
11. M11 — Testing
12. M12 — Public API Review
13. M13 — Dependency Review
14. M14 — Documentation
15. M15 — Publishing

---

# Current Status

**Phase: Refactoring**

Current focus:

> Stabilize the existing `ApiQueue` before implementing the annotation-based REST Repository layer.
