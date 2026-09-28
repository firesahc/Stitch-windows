package soko.ekibun.stitch.service

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import soko.ekibun.stitch.Stitch.StitchInfo
import java.io.File

/**
 * 单步快照撤销（undo 即 swap，无 redo）。
 * save 经注入的 scope 做防抖异步写，不再使用 GlobalScope。
 */
class UndoManager(
    private val externalScope: CoroutineScope? = null
) {
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
        val scope = externalScope ?: CoroutineScope(SupervisorJob() + dispatcherIO)
        job = scope.launch(dispatcherIO) {
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
