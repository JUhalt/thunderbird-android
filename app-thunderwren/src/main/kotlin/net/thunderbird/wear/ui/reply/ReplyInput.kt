package net.thunderbird.wear.ui.reply

import android.app.RemoteInput
import android.content.Intent
import androidx.wear.input.RemoteInputIntentHelper

/** The system's reply input on Wear OS: voice, keyboard, and emoji, in one screen. */
internal object ReplyInput {
    private const val RESULT_KEY = "reply"

    fun createIntent(label: String): Intent {
        val remoteInput = RemoteInput.Builder(RESULT_KEY)
            .setLabel(label)
            .build()

        return RemoteInputIntentHelper.createActionRemoteInputIntent().also { intent ->
            RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
        }
    }

    /** The entered text, or `null` if the input was cancelled. */
    fun getText(data: Intent?): String? {
        return data?.let(RemoteInput::getResultsFromIntent)?.getCharSequence(RESULT_KEY)?.toString()
    }
}
