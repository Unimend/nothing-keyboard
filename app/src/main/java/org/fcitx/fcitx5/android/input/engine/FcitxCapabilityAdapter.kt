package org.fcitx.fcitx5.android.input.engine

import org.fcitx.fcitx5.android.core.CapabilityFlag
import org.fcitx.fcitx5.android.core.CapabilityFlags
import org.fcitx.fcitx5.android.input.session.EditorCapability

internal fun Set<EditorCapability>.toFcitxCapabilities(): CapabilityFlags = CapabilityFlags(
    fold(0uL) { flags, capability -> flags or when (capability) {
        EditorCapability.Composition -> CapabilityFlag.Preedit.flag
        EditorCapability.CommitOnBlur -> CapabilityFlag.ClientUnfocusCommit.flag
        EditorCapability.CursorAwareCommit -> CapabilityFlag.CommitStringWithCursor.flag
        EditorCapability.Ascii -> CapabilityFlag.Alpha.flag
        EditorCapability.Private -> CapabilityFlag.Sensitive.flag
        EditorCapability.NoSuggestions -> CapabilityFlag.NoSpellCheck.flag
        EditorCapability.Spelling -> CapabilityFlag.SpellCheck.flag
        EditorCapability.Uppercase -> CapabilityFlag.Uppercase.flag
        EditorCapability.CapitalSentences -> CapabilityFlag.UppercaseSentences.flag
        EditorCapability.CapitalWords -> CapabilityFlag.UppercaseWords.flag
        EditorCapability.Multiline -> CapabilityFlag.Multiline.flag
        EditorCapability.Email -> CapabilityFlag.Email.flag
        EditorCapability.Password -> CapabilityFlag.Password.flag
        EditorCapability.Url -> CapabilityFlag.Url.flag
        EditorCapability.Number -> CapabilityFlag.Number.flag
        EditorCapability.Phone -> CapabilityFlag.Dialable.flag
        EditorCapability.Date -> CapabilityFlag.Date.flag
        EditorCapability.Time -> CapabilityFlag.Time.flag
    } }
)
