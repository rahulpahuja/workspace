data class WorkspaceItem(
    val name: String,
    val type: String, // "app" or "url"
    val target: String,
    var enabled: Boolean = true
)

data class ChromeProfile(
    val directory: String,
    val displayName: String,
    val email: String = ""
)

data class WorkspaceConfig(
    val items: MutableList<WorkspaceItem> = mutableListOf(),
    var launchAtLogin: Boolean = false,
    var chromeProfile: String = "Default",
    val chromeAppName: String = "Google Chrome"
)
