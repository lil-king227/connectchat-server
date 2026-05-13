plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.connectchat"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.connectchat"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)

    // Room Database
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // OkHttp (WebSocket)
    implementation(libs.okhttp)

    // Glide (image loading)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Gson (JSON parsing)
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}