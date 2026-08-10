# Keep Kotlin serialization metadata and generated serializers used by DataStore snapshots.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature,EnclosingMethod
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Persisted models. These are encoded into local JSON snapshots and must remain stable
# across release builds even when R8 obfuscation is enabled.
-keep @kotlinx.serialization.Serializable class com.battleheim.quantum2048.engine.** { *; }
-keep @kotlinx.serialization.Serializable class com.battleheim.quantum2048.domain.** { *; }
-keep @kotlinx.serialization.Serializable class com.battleheim.quantum2048.data.** { *; }
-keepclassmembers enum com.battleheim.quantum2048.engine.** { *; }
-keepclassmembers enum com.battleheim.quantum2048.domain.** { *; }

# DataStore serializer envelopes are decoded reflectively by generated serializers.
-keep class com.battleheim.quantum2048.data.Snapshot { *; }
-keep class com.battleheim.quantum2048.data.CollectionSnapshot { *; }
-keep class com.battleheim.quantum2048.data.SettingsSnapshot { *; }
-keep class com.battleheim.quantum2048.data.ProfileSnapshot { *; }
