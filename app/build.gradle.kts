plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

android {

    namespace = "com.victordev.flashcardbrasil"
    compileSdk {
        version = release(36)
    }
    flavorDimensions += "env"
    defaultConfig {
        applicationId = "com.victordev.flashcardbrasil"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("Boolean", "REVIEW_REMINDERS_TEST_MODE", "false")
        buildConfigField("Long", "REVIEW_REMINDER_REPEAT_MINUTES", "300L")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    productFlavors {
        create("dev") {
            dimension = "env"

            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            buildConfigField(
                "String",
                "BASE_URL",
                "\"http://192.168.1.102:4200\""
            )

            resValue("string", "app_name", "FlashCards Brasil - Debug")

            // Para testar notificacoes em desenvolvimento:
            // 1. troque REVIEW_REMINDERS_TEST_MODE para true
            // 2. troque REVIEW_REMINDER_REPEAT_MINUTES para 1L
            buildConfigField("Boolean", "REVIEW_REMINDERS_TEST_MODE", "true")
            buildConfigField("Long", "REVIEW_REMINDER_REPEAT_MINUTES", "1L")
        }

        create("prod") {
            dimension = "env"

            buildConfigField(
                "String",
                "BASE_URL",
                "\"d2hnmre6u9pung.cloudfront.net\""
            )

            resValue(
                "string",
                "app_name",
                "FlashCards Brasil"
            )

            buildConfigField("Boolean", "REVIEW_REMINDERS_TEST_MODE", "false")
            buildConfigField("Long", "REVIEW_REMINDER_REPEAT_MINUTES", "300L")
        }
    }
}

dependencies {
    implementation("androidx.room:room-runtime:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.webkit:webkit:1.15.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
