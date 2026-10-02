import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

apply(plugin = "com.huawei.agconnect")

// Release signing — keystore.properties is gitignored; not present in fresh clones.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Supabase project URL + anon key — supabase.properties is gitignored; see
// supabase.properties.example. Never the service-role key or DB password.
val supabaseProperties = Properties().apply {
    val file = rootProject.file("supabase.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.matchpoint.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.matchpoint.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        buildConfigField("String", "SUPABASE_URL", "\"${supabaseProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystoreProperties.containsKey("storeFile")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // iOS-style continuous/smoothed ("squircle") corners in place of Compose's default arc corners.
    implementation("io.github.iamcalledrob:smooth-rounded-corner-shape:1.0.4")

    // Splash screen brand animation — plays the same Lottie file as the iOS app.
    implementation("com.airbnb.android:lottie-compose:6.5.2")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Huawei Watch GT 4 remote-controller support — see wear/README.md for AppGallery
    // Connect setup this needs before it can reach a real device.
    implementation("com.huawei.hms:wearengine:5.0.1.302")

    // Supabase backend foundation (installation tracking / release checks / future user
    // registration — never match/session/player data, that stays local-first in Room).
    // Pinned to 3.0.0: the newest stable supabase-kt release still built against Kotlin
    // 2.0.20 — every release after 3.0.3 moved to Kotlin 2.1.0+, which this project isn't on.
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-cio:3.0.0")
    // For the users/installations DTOs postgrest-kt serializes — required wherever a module
    // declares its own @Serializable classes, not just inside the Supabase library itself.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
