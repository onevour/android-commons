REST TEST CASES

Included:
1. GetQueryTest.java
2. PutPatchDeleteTest.java
3. HttpHeadersTest.java
4. ResponseTypesTest.java
5. ConcurrencyTest.java
6. TimeoutTest.java
7. HttpsTest.java

IMPORTANT:
- These tests assume UserRepository already exists in com.onevour.core.rest.
- These tests assume the repository has the methods referenced by the tests:
  getUser(...)
  searchUser(...)
  update(...)
  patch(...)
  delete(...)
  getWithHeaders(...)
  getObject(...)
  getString(...)
  getList(...)
  getWithTimeout(...)

- The repository URL is assumed to use localhost:3000.
- MockWebServer therefore starts on port 3000.
- HTTPS test is intentionally @Ignore because it requires the project's SSL/certificate setup.
- SslTestSupport.sslSocketFactory() is a placeholder for the project's existing SSL test helper and should be wired to the actual helper before enabling the test.

The RestClient is instantiated as:
    new RestClient().create(UserRepository.class)
