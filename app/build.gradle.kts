plugins { id("com.android.application") }

android {
    namespace = "com.clawwake.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.clawwake.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0-gateway-alpha"
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
