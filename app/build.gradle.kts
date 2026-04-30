plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.videochat"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.videochat"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Включите true для продакшена (ProGuard/R8)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Важно: убедитесь, что debuggable выключен для release
            isDebuggable = false
        }

        debug {
            // Для debug версии флаг debuggable включен по умолчанию
            isDebuggable = true
            // Можно добавить_suffix, чтобы отличать пакеты
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    //retrofit - for http queries
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.logging.interceptor)

    //websocket (if using OkHttp straight)
    implementation(libs.okhttp)

    implementation(libs.stompprotocolandroid)
    implementation(libs.java.websocket)
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    implementation("io.livekit:livekit-android:2.24.1")
//    implementation("io.livekit:livekit-android:2.0.0")
//    implementation("io.livekit:livekit-android-webrtc:2.24.1")

//    implementation(libs.rxandroid)

    implementation(libs.circleimageview)

    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")

    implementation(libs.jjwt.api)
    implementation(libs.core.ktx)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    implementation("com.google.protobuf:protobuf-javalite:3.21.12")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}