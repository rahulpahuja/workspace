import java.io.File
import java.util.Properties

/** Abstraction over wherever the workspace configuration is persisted. */
interface ConfigStore {
    fun load(): WorkspaceConfig
    fun save(config: WorkspaceConfig)
}

/** Stores the configuration as a Java .properties file. */
class PropertiesConfigStore(private val file: File) : ConfigStore {

    override fun load(): WorkspaceConfig {
        if (!file.exists()) return WorkspaceConfig()
        val p = Properties()
        file.inputStream().use { p.load(it) }

        val items = mutableListOf<WorkspaceItem>()
        var i = 0
        while (p.getProperty("item.$i.name") != null) {
            items.add(
                WorkspaceItem(
                    name = p.getProperty("item.$i.name"),
                    type = p.getProperty("item.$i.type", "app"),
                    target = p.getProperty("item.$i.target", ""),
                    enabled = p.getProperty("item.$i.enabled", "true").toBoolean()
                )
            )
            i++
        }

        return WorkspaceConfig(
            items = items,
            launchAtLogin = p.getProperty("launchAtLogin", "false").toBoolean(),
            // "chromeProfile" is the key written by earlier versions.
            chromeProfile = p.getProperty("chrome.profile") ?: p.getProperty("chromeProfile", "Default"),
            chromeAppName = p.getProperty("chrome.appName", "Google Chrome")
        )
    }

    override fun save(config: WorkspaceConfig) {
        file.parentFile.mkdirs()
        val text = buildString {
            appendLine("launchAtLogin=${config.launchAtLogin}")
            appendLine("chrome.profile=${escape(config.chromeProfile)}")
            appendLine("chrome.appName=${escape(config.chromeAppName)}")
            config.items.forEachIndexed { i, item ->
                appendLine("item.$i.name=${escape(item.name)}")
                appendLine("item.$i.type=${item.type}")
                appendLine("item.$i.target=${escape(item.target)}")
                appendLine("item.$i.enabled=${item.enabled}")
            }
        }
        file.writeText(text, Charsets.UTF_8)
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")
}
