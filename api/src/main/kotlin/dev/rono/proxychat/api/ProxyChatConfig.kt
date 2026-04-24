package dev.rono.proxychat.api

/**
 * Platform-independent configuration interface
 */
interface ProxyChatConfig {
    fun getSection(path: String): ProxyChatConfig?
    fun getString(path: String): String?
    fun getBoolean(path: String): Boolean
    fun getInt(path: String): Int
    fun getList(path: String): List<String>?
    fun getKeys(): Collection<String>
    fun contains(path: String): Boolean
    fun set(path: String, value: Any?)
    fun save(file: java.io.File)
}
