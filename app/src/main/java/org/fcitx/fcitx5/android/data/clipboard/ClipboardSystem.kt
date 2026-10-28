/* SPDX-License-Identifier: LGPL-2.1-or-later */
package org.fcitx.fcitx5.android.data.clipboard

import android.content.Context
import androidx.annotation.Keep
import androidx.paging.PagingSource
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardDatabase
import org.fcitx.fcitx5.android.data.clipboard.db.ClipboardEntry
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.data.prefs.ManagedPreference
import org.fcitx.fcitx5.android.utils.WeakHashSet
import org.fcitx.fcitx5.android.utils.clipboardManager
import timber.log.Timber
import java.util.Collections

/** Composition root for clipboard collection, persistence and observations. */
object ClipboardSystem {
    fun interface UpdateListener { fun onUpdate(entry: ClipboardEntry) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val listeners = Collections.synchronizedSet(WeakHashSet<UpdateListener>())
    private lateinit var repository: ClipboardRepository
    private lateinit var source: PlatformClipboardSource
    private val prefs by lazy { AppPrefs.getInstance().clipboard }

    @Volatile var itemCount: Int = 0
        private set
    @Volatile var lastEntry: ClipboardEntry? = null
        private set
    @Volatile var transformer: ((String) -> String)? = null

    @Keep
    private val enabledListener = ManagedPreference.OnChangeListener<Boolean> { _, enabled ->
        if (enabled) source.start(::collect) else source.stop()
    }

    @Keep
    private val limitListener = ManagedPreference.OnChangeListener<Int> { _, limit ->
        scope.launch { itemCount = repository.trimTo(limit) }
    }

    fun init(context: Context) {
        val database = Room.databaseBuilder(context, ClipboardDatabase::class.java, "clbdb")
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
        repository = ClipboardRepository(database, database.clipboardDao(), scope.coroutineContext)
        source = PlatformClipboardSource(context.clipboardManager)
        prefs.clipboardListening.registerOnChangeListener(enabledListener)
        prefs.clipboardHistoryLimit.registerOnChangeListener(limitListener)
        enabledListener.onChange(prefs.clipboardListening.key, prefs.clipboardListening.getValue())
        limitListener.onChange(prefs.clipboardHistoryLimit.key, prefs.clipboardHistoryLimit.getValue())
    }

    fun addUpdateListener(listener: UpdateListener) { listeners.add(listener) }
    fun removeUpdateListener(listener: UpdateListener) { listeners.remove(listener) }
    suspend fun get(id: Int) = repository.get(id)
    suspend fun haveUnpinned() = repository.haveUnpinned()
    fun allEntries(): PagingSource<Int, ClipboardEntry> = repository.allEntries()
    suspend fun pin(id: Int) = repository.pin(id, true)
    suspend fun unpin(id: Int) = repository.pin(id, false)

    suspend fun updateText(id: Int, text: String) {
        repository.updateText(id, text)
        lastEntry?.takeIf { it.id == id }?.let { publish(it.copy(text = text)) }
    }

    suspend fun delete(id: Int) { itemCount = repository.markDeleted(id) }
    suspend fun deleteAll(skipPinned: Boolean = true): IntArray {
        val (ids, count) = repository.markAllDeleted(skipPinned)
        itemCount = count
        return ids
    }
    suspend fun undoDelete(vararg ids: Int) { itemCount = repository.restore(ids) }
    suspend fun finalizeDeleted(ids: IntArray) = repository.finalizeDeleted(ids)
    suspend fun nukeTable() { itemCount = repository.clear() }

    private fun collect(clip: android.content.ClipData) {
        scope.launch {
            mutex.withLock {
                val entry = ClipboardEntry.fromClipData(clip, transformer) ?: return@withLock
                if (entry.text.isBlank()) return@withLock
                try {
                    publish(repository.store(entry, prefs.clipboardHistoryLimit.getValue()))
                    itemCount = repository.itemCount()
                } catch (exception: Exception) {
                    Timber.w("Failed to update clipboard database: $exception")
                    publish(entry)
                }
            }
        }
    }

    private fun publish(entry: ClipboardEntry) {
        lastEntry = entry
        val snapshot = synchronized(listeners) { listeners.toList() }
        snapshot.forEach { it.onUpdate(entry) }
    }
}
