plugins {
    id(ThunderbirdPlugins.App.androidCompose)
}

android {
    namespace = "net.thunderbird.wear"

    defaultConfig {
        applicationId = "net.thunderbird.wear"

        versionCode = 1
        versionName = "0.1.0"

        minSdk = 30
        targetSdk = 35

        buildConfigField("String", "CLIENT_INFO_APP_NAME", "\"ThunderWren for Wear OS\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-SNAPSHOT"
            isDebuggable = true
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "kotlin/**",
                "DebugProbesKt.bin",
            )
        }
    }
}

dependencies {
    implementation(projects.appCommon)
    implementation(projects.legacy.core)
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.featureflag)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.material)
    implementation(libs.androidx.wear.watchface.complications.data.source)
    implementation(libs.play.services.wearable)
    implementation("com.google.guava:guava:33.4.0-android")

    debugImplementation(libs.jetbrains.compose.ui.tooling)
}
