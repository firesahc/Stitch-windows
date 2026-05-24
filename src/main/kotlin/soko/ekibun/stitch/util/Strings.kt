package soko.ekibun.stitch.util

import java.io.InputStreamReader
import java.nio.charset.Charset
import java.text.MessageFormat
import java.util.*

object Strings {
    private val props = Properties().apply {
        load(InputStreamReader(
            this::class.java.getResourceAsStream("/strings.properties"),
            Charset.forName("UTF-8")
        ))
    }

    fun get(key: String, vararg args: Any?): String {
        val pattern = props.getProperty(key) ?: "!$key!"
        return if (args.isEmpty()) pattern
               else MessageFormat.format(pattern, *args)
    }
}
