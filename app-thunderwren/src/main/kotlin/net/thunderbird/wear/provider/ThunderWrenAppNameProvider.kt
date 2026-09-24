package net.thunderbird.wear.provider

import android.content.Context
import com.fsck.k9.preferences.FilePrefixProvider
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.wear.R

internal class ThunderWrenAppNameProvider(
    context: Context,
) : AppNameProvider, BrandNameProvider, FilePrefixProvider {
    override val appName: String by lazy {
        context.getString(R.string.app_name)
    }

    override val brandName: String by lazy {
        context.getString(R.string.brand_name)
    }

    override val filePrefix: String = "thunderwren"
}
