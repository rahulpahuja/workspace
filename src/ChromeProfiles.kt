import java.io.File

/** Discovers the Chrome profiles that exist on this machine. */
interface ChromeProfileSource {
    fun profiles(): List<ChromeProfile>
}

/** Reads profile metadata from Chrome's own "Local State" file. */
class LocalStateProfileSource(private val localState: File) : ChromeProfileSource {

    override fun profiles(): List<ChromeProfile> {
        if (!localState.exists()) return emptyList()
        val root = runCatching { JsonParser.parse(localState.readText()) as? Map<*, *> }
            .getOrNull() ?: return emptyList()
        val infoCache = (root["profile"] as? Map<*, *>)?.get("info_cache") as? Map<*, *> ?: return emptyList()

        return infoCache.map { (directory, info) ->
            val fields = info as? Map<*, *> ?: emptyMap<String, Any?>()
            ChromeProfile(
                directory = directory as String,
                displayName = fields["name"] as? String ?: directory,
                email = fields["user_name"] as? String ?: ""
            )
        }.sortedWith(compareBy({ it.directory != "Default" }, { it.displayName.lowercase() }))
    }
}
