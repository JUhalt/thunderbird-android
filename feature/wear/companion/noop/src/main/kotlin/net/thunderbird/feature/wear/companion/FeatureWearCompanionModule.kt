package net.thunderbird.feature.wear.companion

import org.koin.dsl.module

/** The Wear OS companion needs Google Play Services, so builds without it include this empty module instead. */
val featureWearCompanionModule = module { }
