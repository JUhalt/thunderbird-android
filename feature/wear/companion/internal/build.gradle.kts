plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.feature.wear.companion.internal"
}

dependencies {
    api(projects.feature.wear.companion.api)

    implementation(projects.core.android.account)
    implementation(projects.core.common)
    implementation(projects.core.featureflag)
    implementation(projects.core.logging.api)
    implementation(projects.core.preference.api)
    implementation(projects.feature.account.avatar.api)
    implementation(projects.feature.account.storage.api)
    implementation(projects.feature.search.implLegacy)
    implementation(projects.legacy.core)
    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)
    implementation(projects.legacy.ui.legacy)
    implementation(projects.mail.common)

    implementation(libs.play.services.wearable)

    testImplementation(projects.core.logging.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.kotlin)
}
