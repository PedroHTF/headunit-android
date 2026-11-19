plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.headunit.core.hal"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }

    // API do Android Automotive (CarPropertyManager etc.). Em aparelhos que não
    // são AAOS a biblioteca não existe em tempo de execução, por isso só tocamos
    // nessas classes depois de checar FEATURE_AUTOMOTIVE.
    useLibrary("android.car")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
