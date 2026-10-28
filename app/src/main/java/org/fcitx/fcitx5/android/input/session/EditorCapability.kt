package org.fcitx.fcitx5.android.input.session

/** Editor requirements, independent of any engine's native bit layout. */
enum class EditorCapability {
    Composition, CommitOnBlur, CursorAwareCommit, Ascii, Private, NoSuggestions, Spelling, Uppercase, CapitalSentences, CapitalWords, Multiline, Email, Password, Url, Number, Phone, Date, Time
}
