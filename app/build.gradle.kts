plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.roomlibrary"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.roomlibrary"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.5"

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // LiveData & ViewModel
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")

    // MPAndroidChart for visualizations
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Google Generative AI (Gemini)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.guava:guava:31.1-android")

    testImplementation("androidx.room:room-testing:2.6.1")
}
