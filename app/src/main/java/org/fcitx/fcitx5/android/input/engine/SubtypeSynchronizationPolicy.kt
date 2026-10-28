package org.fcitx.fcitx5.android.input.engine

/** Suppresses our own platform notification without suppressing a user-selected subtype. */
internal class SubtypeSynchronizationPolicy {
    private var firstBinding = true
    private var expectedEcho: String? = null

    fun consumeInitialBinding(): Boolean = firstBinding.also { firstBinding = false }

    fun expectingEcho(inputMethod: String) { expectedEcho = inputMethod }

    fun shouldActivate(inputMethod: String): Boolean {
        val echo = expectedEcho == inputMethod
        expectedEcho = null
        return !echo
    }
}
