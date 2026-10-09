# Consumer ProGuard / R8 rules of commons-sdk.

# RestClient finds the code generated for a @RestRepository by the interface's name:
# keep the names of the repositories, and the generated classes with their constructor.
-keepnames @com.onevour.core.rest.repository.RestRepository interface *
-keep class * implements com.onevour.core.rest.builder.GeneratedRepository { public <init>(); }

# The Proxy fallback (an app without commons-sdk-processor) reads the annotations at run time.
-keepattributes *Annotation*,Signature
-keep @com.onevour.core.rest.repository.RestRepository interface * { *; }
