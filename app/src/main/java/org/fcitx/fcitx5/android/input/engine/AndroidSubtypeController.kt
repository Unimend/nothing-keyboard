package org.fcitx.fcitx5.android.input.engine

import android.os.Build
import android.view.inputmethod.InputMethodSubtype
import org.fcitx.fcitx5.android.core.SubtypeManager

interface PlatformInputMethodPort {
    suspend fun synchronize()
}

/** Owns system subtype feedback and initial binding, isolated from editor/session state. */
class AndroidSubtypeController(
    private val queries: InputEngineQueries,
    private val commands: EngineCommandPort,
    private val currentSubtype: () -> InputMethodSubtype?,
    private val switchSubtype: (InputMethodSubtype) -> Unit
) : PlatformInputMethodPort {
    private val policy = SubtypeSynchronizationPolicy()

    override suspend fun synchronize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            SubtypeManager.syncWith(queries.enabledInputMethods()
                .map(FcitxPlatformAdapter::inputMethod).toTypedArray())
        }
    }

    fun onBindInput() {
        if (!policy.consumeInitialBinding()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            currentSubtype()?.let { commands.submit(EngineCommand.ActivateInputMethod(SubtypeManager.inputMethodOf(it))) }
        }
    }

    fun onPlatformSubtypeChanged(subtype: InputMethodSubtype) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val name = SubtypeManager.inputMethodOf(subtype)
            if (policy.shouldActivate(name)) commands.submit(EngineCommand.ActivateInputMethod(name))
        }
    }

    fun onEngineInputMethodChanged(name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val subtype = SubtypeManager.subtypeOf(name) ?: return
            policy.expectingEcho(name)
            switchSubtype(subtype)
        }
    }
}
