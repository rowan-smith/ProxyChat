package dev.rono.proxychat.core.config

import dev.rono.proxychat.api.ProxyChatConfig
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

class YamlProxyChatConfig(private val data: MutableMap<String, Any>) : ProxyChatConfig {

    override fun getSection(path: String): ProxyChatConfig? {
        val value = getRaw(path)
        if (value is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return YamlProxyChatConfig(value as MutableMap<String, Any>)
        }
        return null
    }

    override fun getString(path: String): String? = getRaw(path)?.toString()

    override fun getBoolean(path: String): Boolean = getRaw(path) as? Boolean ?: false

    override fun getInt(path: String): Int = (getRaw(path) as? Number)?.toInt() ?: 0

    override fun getList(path: String): List<String>? {
        val value = getRaw(path)
        if (value is List<*>) {
            return value.map { it.toString() }
        }
        return null
    }

    override fun getKeys(): Collection<String> = data.keys

    override fun contains(path: String): Boolean = getRaw(path) != null

    override fun set(path: String, value: Any?) {
        val keys = path.split(".")
        var current = data
        for (i in 0 until keys.size - 1) {
            val key = keys[i]
            val next = current[key]
            if (next is MutableMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                current = next as MutableMap<String, Any>
            } else {
                val newMap = mutableMapOf<String, Any>()
                current[key] = newMap
                current = newMap
            }
        }
        if (value == null) {
            current.remove(keys.last())
        } else {
            current[keys.last()] = value
        }
    }

    override fun save(file: java.io.File) {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        val yaml = Yaml(options)
        FileWriter(file).use { writer ->
            yaml.dump(data, writer)
        }
    }

    private fun getRaw(path: String): Any? {
        if (path.isEmpty()) return data
        val keys = path.split(".")
        var current: Any? = data
        for (key in keys) {
            if (current is Map<*, *>) {
                current = current[key]
            } else {
                return null
            }
        }
        return current
    }

    companion object {
        fun load(file: File): YamlProxyChatConfig {
            val yaml = Yaml()
            val data = if (file.exists()) {
                FileInputStream(file).use { input ->
                    val loaded = yaml.load<Any>(input)
                    if (loaded is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        (loaded as Map<String, Any>).toMutableMap()
                    } else {
                        mutableMapOf()
                    }
                }
            } else {
                mutableMapOf()
            }
            return YamlProxyChatConfig(data)
        }
    }
}
