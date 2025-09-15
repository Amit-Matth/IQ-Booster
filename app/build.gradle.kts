plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.amitmatth.iqbooster"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.amitmatth.iqbooster"
        minSdk = 21
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // Glide dependencies
    implementation(libs.glide)
    implementation(libs.firebase.config)
    annotationProcessor(libs.compiler)

    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.auth)
    implementation(libs.google.firebase.database)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.google.firebase.analytics)

    // Google Gemini Api dependencies
    implementation(libs.generativeai)
    implementation(libs.guava)
    implementation(libs.reactive.streams)

    // Gson library dependency
    implementation(libs.gson)

    implementation(libs.firebase.appcheck.playintegrity)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
