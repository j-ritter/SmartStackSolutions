plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ritter.smartstackbills"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ritter.smartstackbills"
        minSdk = 24
        targetSdk = 35
        versionCode = 21
        versionName = "1.14"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    constraints {
        implementation("androidx.core:core:1.13.1") {
            version { strictly("1.13.1") }
        }
        implementation("androidx.core:core-ktx:1.13.1") {
            version { strictly("1.13.1") }
        }
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation ("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore")
    releaseImplementation("com.google.firebase:firebase-appcheck-playintegrity:19.2.0")
    debugImplementation("com.google.firebase:firebase-appcheck-debug:19.2.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.3.0-beta04")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation ("androidx.work:work-runtime-ktx:2.7.1")
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    val billing_version = "9.1.0"
    implementation("com.android.billingclient:billing:$billing_version")

}
