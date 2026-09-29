package soko.ekibun.stitch.domain

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.util.Log
import java.io.File

/**
 * 项目持久化仓储。
 *
 * 原先读写 JSON 分散在 StitchProject.lazy + UndoManager.save() 两处，
 * 收敛至此：load 纯同步读取（调用方决定线程），save 防抖异步写由 UndoManager 触发时传入。
 */
class ProjectRepository(
    private val gson: Gson = Gson(),
    private val dispatcherIO: CoroutineDispatcher? = null
) {
    private val listType = object : TypeToken<ArrayList<Stitch.StitchInfo>>() {}.type

    fun load(file: File): MutableList<Stitch.StitchInfo> {
        val list = mutableListOf<Stitch.StitchInfo>()
        if (!file.exists()) return list
        try {
            val text = if (dispatcherIO != null) {
                runBlocking(dispatcherIO) { file.readText() }
            } else {
                file.readText()
            }
            list.addAll(gson.fromJson<ArrayList<Stitch.StitchInfo>>(text, listType) ?: arrayListOf())
        } catch (e: Exception) {
            Log.e("ProjectRepository", e)
        }
        return list
    }

    fun snapshot(infos: List<Stitch.StitchInfo>): String = gson.toJson(infos)

    fun writeText(file: File, json: String) {
        try {
            if (!file.exists()) {
                if (json.isEmpty()) return
                file.parentFile?.mkdirs()
            }
            file.writeText(json)
        } catch (e: Exception) {
            Log.e("ProjectRepository", e)
        }
    }

    fun toGson(): Gson = gson
}
