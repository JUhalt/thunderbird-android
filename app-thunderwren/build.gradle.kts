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

    testOptions {
        unitTests.isIncludeAndroidResources = true
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
    implementation(projects.legacy.logging)
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.featureflag)
    implementation(projects.feature.telemetry.noop)
    implementation(projects.feature.migration.launcher.noop)
    implementation(projects.feature.autodiscovery.api)
    implementation(projects.backend.api)
    implementation(projects.core.android.common)
    implementation(projects.feature.mail.message.reader.api)
    implementation(projects.feature.mail.message.list.internal)

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

    testImplementation(projects.feature.account.api)
    testImplementation(projects.feature.account.common)
    testImplementation(projects.feature.mail.message.list.api)
    testImplementation(projects.feature.widget.messageList)
    testImplementation(projects.feature.widget.unread)
    testImplementation(projects.feature.changelog.api)
    testImplementation(projects.feature.changelog.internal)
    testImplementation(projects.feature.thundermail.internal.common)
    testImplementation(projects.plugins.openpgpApiLib.openpgpApi)
    testImplementation(libs.appauth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit.ktx)
}
