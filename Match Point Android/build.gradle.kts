buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://developer.huawei.com/repo/") // AGConnect plugin
    }
    dependencies {
        // agcp's own plugin-application check looks for AGP on the buildscript classpath
        // (legacy style) even though it's actually applied below via the plugins{} DSL —
        // this line only satisfies that check, AGP itself still comes from plugins{}.
        classpath("com.android.tools.build:gradle:8.6.0")
        classpath("com.huawei.agconnect:agcp:1.9.1.301")
    }
}

plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}
