plugins {
    // AGP 9 の Kotlin ビルトインサポートにより、これ 1 つで Kotlin もコンパイルされる。
    id("com.android.application")
}

android {
    namespace = "com.volnotch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.volnotch"
        minSdk = 28      // Fire OS 7 = Android 9 = API 28（Fire OS 7/8 の Fire タブレットをカバー。DynamicsProcessing も API28）
        targetSdk = 30   // Fire OS 8 = Android 11。Amazon 公式が API 30 target を推奨
        versionCode = 1
        versionName = "1.0"
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
}
