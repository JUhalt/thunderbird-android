package net.thunderbird.wear

import android.content.Context
import kotlin.test.Test
import net.thunderbird.wear.di.thunderWrenModule
import net.thunderbird.wear.ui.reader.MessageViewModel
import net.thunderbird.wear.ui.reply.ReplyViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify

class DependencyInjectionTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun testDependencyTree() {
        thunderWrenModule.verify(
            extraTypes = listOf(Context::class),
            injections = injectedParameters(
                definition<MessageViewModel>(String::class, String::class),
                definition<ReplyViewModel>(String::class, String::class),
            ),
        )
    }
}
