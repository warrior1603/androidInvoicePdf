plugins {
    id("com.android.application")
}

android {
    namespace = "com.chouchene.factures"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chouchene.factures"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.github.androidmads:QRGenerator:1.0.1")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.zxing:core:3.2.0")
    implementation("com.google.firebase:protolite-well-known-types:18.0.0")
    implementation("com.github.yesidlazaro:GmailBackground:1.2.0")
    implementation("androidx.core:core-splashscreen:1.2.0-alpha02")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("androidx.room:room-common:2.5.0")
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("androidx.preference:preference:1.2.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("commons-io:commons-io:2.5")
    implementation("com.github.ismaeldivita:chip-navigation-bar:1.4.0")
    implementation("np.com.susanthapa:curved_bottom_navigation:0.6.5")
    implementation("com.github.Mindinventory:LiquidNavBar:0.0.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.61")
    //implementation(files("/Users/warrior/StudioProjects/Invoice_app/lib/meow-bottom-navigation-java-1.2.0.aar"))
    implementation("androidx.navigation:navigation-fragment:2.8.3")
    implementation("androidx.navigation:navigation-ui:2.8.3")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.media3:media3-common:1.4.1")
    annotationProcessor("androidx.room:room-compiler:2.5.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}