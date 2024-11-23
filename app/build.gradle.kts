plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.newEcom"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.newEcom"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources {
            merges += setOf("META-INF/NOTICE.md", "META-INF/LICENSE.md")
        }
    }
}

dependencies {
    // AndroidX Core Dependencies
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // Firebase Dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-dynamic-links")
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Image Loading and Processing
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.makeramen:roundedimageview:2.3.0")

    // UI Components
    implementation("com.github.mancj:MaterialSearchBar:0.8.5")
    implementation("me.relex:circleindicator:2.1.6")
    implementation("org.imaginativeworld.whynotimagecarousel:whynotimagecarousel:2.1.0")
    implementation("com.github.f0ris.sweetalert:library:1.6.2")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.airbnb.android:lottie:6.3.0")

    // Networking
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("org.json:json:20230618")

    // Email
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("org.chromium.net:cronet-embedded:119.6045.31")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}