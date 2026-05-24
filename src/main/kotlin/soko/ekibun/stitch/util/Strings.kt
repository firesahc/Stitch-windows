package soko.ekibun.stitch.util

import java.io.InputStreamReader
import java.nio.charset.Charset
import java.text.MessageFormat
import java.util.*

object Strings {
    private val props by lazy {
        val stream = Strings::class.java.getResourceAsStream("/strings.properties")
            ?: error("strings.properties not found on classpath")
        Properties().apply {
            InputStreamReader(stream, Charset.forName("UTF-8")).use { load(it) }
        }
    }

    fun get(key: String, vararg args: Any?): String {
        val pattern = props.getProperty(key) ?: "!$key!"
        return if (args.isEmpty()) pattern
               else MessageFormat.format(pattern, *args)
    }
}
