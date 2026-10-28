package org.fcitx.fcitx5.android.input.editing

/** Public entry point for editing intents. Does not expose mutable editor state. */
fun interface EditingCommandPort {
    fun execute(command: EditingCommand)
}
