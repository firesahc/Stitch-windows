package soko.ekibun.stitch.service

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import soko.ekibun.stitch.Stitch.StitchInfo
import java.io.File

class UndoManager {
    private val stitchInfoBak = mutableListOf<StitchInfo>()
    private val selectedBak = mutableSetOf<String>()
    private var undoTag: Any? = null
    private var job: Job? = null

    fun clearUndoTag() {
        undoTag = null
    }

    @Synchronized
    fun updateUndo(
        tag: Any?,
        stitchInfo: MutableList<StitchInfo>,
        selected: MutableSet<String>,
        runBeforeSave: () -> Unit,
    ) {
        if (tag != null && tag != undoTag) {
            undoTag = tag
            selectedBak.clear()
            selectedBak.addAll(selected)
            stitchInfoBak.clear()
            stitchInfoBak.addAll(stitchInfo.map { it.clone() })
        }
        runBeforeSave()
    }

    @Synchronized
    fun undo(stitchInfo: MutableList<StitchInfo>, selected: MutableSet<String>) {
        val last = stitchInfo.map { it.clone() }
        val lastSelect = selected.map { it }
        stitchInfo.clear()
        stitchInfo.addAll(stitchInfoBak)
        selected.clear()
        selected.addAll(selectedBak)
        selectedBak.clear()
        selectedBak.addAll(lastSelect)
        stitchInfoBak.clear()
        stitchInfoBak.addAll(last)
        undoTag = null
    }

    @Synchronized
    fun save(file: File, stitchInfo: List<StitchInfo>, gson: Gson, dispatcherIO: CoroutineDispatcher) {
        job?.cancel()
        job = GlobalScope.launch(dispatcherIO) {
            try {
                val info = stitchInfo.toList()
                if (!file.exists()) {
                    if (info.isNotEmpty()) file.parentFile?.mkdirs()
                    else return@launch
                }
                file.writeText(gson.toJson(info))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
