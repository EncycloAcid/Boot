# Saves are kotlinx.serialization JSON. The library ships its own keep rules for generated
# serializers; keep the core model's companions so R8 doesn't strip serializer() lookups.
-keepclassmembers class com.boot.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.boot.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
