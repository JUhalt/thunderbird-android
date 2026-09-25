plugins {
    id(ThunderbirdPlugins.App.androidCompose)
}

android {
    namespace = "net.thunderbird.wear"

    defaultConfig {
        // The Wearable Data Layer only connects apps with the same application ID and signing key, so this must match
        // app-thunderbird (including the build type suffixes below). See RFC 0010.
        applicationId = "net.thunderbird.android"

        versionCode = 4
        versionName = "0.1.0-beta4"

        minSdk = 30
        targetSdk = 35
    }

    // The same keys as app-thunderbird, so each watch build connects to the matching Thunderbird build.
    signingConfigs {
        val useUploadKey = providers.gradleProperty("tb.useUploadKey")
            .map(String::toBoolean)
            .orElse(true)
            .get()

        createSigningConfig(project, SigningType.TB_RELEASE, isUpload = useUploadKey)
        createSigningConfig(project, SigningType.TB_BETA, isUpload = useUploadKey)
        createSigningConfig(project, SigningType.TB_DAILY, isUpload = useUploadKey)
    }

    // The same build types and application ID suffixes as app-thunderbird: Thunderbird Beta and Daily are separate
    // apps, and each needs its own watch app.
    buildTypes {
        val isCI = providers.gradleProperty("ci")
            .map(String::toBoolean)
            .orElse(false)

        release {
            signingConfig = signingConfigs.getByType(SigningType.TB_RELEASE)

            isMinifyEnabled = !isCI.get()
            isShrinkResources = !isCI.get()
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("beta") {
            initWith(getByName("release"))

            signingConfig = signingConfigs.getByType(SigningType.TB_BETA)

            applicationIdSuffix = ".beta"
            versionNameSuffix = "b0"

            isMinifyEnabled = isCI.get()
            isShrinkResources = isCI.get()
            isDebuggable = false

            matchingFallbacks += listOf("release")
        }
        create("daily") {
            initWith(getByName("release"))

            signingConfig = signingConfigs.getByType(SigningType.TB_DAILY)

            applicationIdSuffix = ".daily"
            versionNameSuffix = "a1"

            isMinifyEnabled = isCI.get()
            isShrinkResources = isCI.get()
            isDebuggable = false

            matchingFallbacks += listOf("release")
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
    implementation(projects.feature.wear.companion.api)
    implementation(projects.core.logging.api)
    implementation(projects.core.logging.implConsole)
    implementation(projects.core.ui.contract)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.jetbrains.compose.components.ui.preview)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.material)
    implementation(libs.androidx.wear.watchface.complications.data.source)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.androidx.wear.input)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation("com.google.guava:guava:33.4.0-android")

    debugImplementation(libs.jetbrains.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit.ktx)
}
