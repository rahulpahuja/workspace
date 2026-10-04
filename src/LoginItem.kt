import java.io.File

/** Controls whether the workspace starts when the user logs in to macOS. */
interface LoginItem {
    fun setEnabled(enabled: Boolean)
}

/** Implements login startup with a per-user LaunchAgent plist. */
class LaunchAgentLoginItem(
    private val label: String = "com.mobile1x.workspace",
    private val launchAgentsDir: File = File(System.getProperty("user.home"), "Library/LaunchAgents")
) : LoginItem {

    private val plist get() = File(launchAgentsDir, "$label.plist")

    override fun setEnabled(enabled: Boolean) {
        if (!enabled) {
            unload()
            plist.delete()
            return
        }
        launchAgentsDir.mkdirs()
        plist.writeText(plistContent())
        unload()
        runCatching {
            ProcessBuilder("launchctl", "bootstrap", "gui/${uid()}", plist.absolutePath).start().waitFor()
        }
    }

    private fun unload() {
        runCatching { ProcessBuilder("launchctl", "bootout", "gui/${uid()}", plist.absolutePath).start().waitFor() }
    }

    private fun plistContent(): String {
        val jar = File(LaunchAgentLoginItem::class.java.protectionDomain.codeSource.location.toURI()).absolutePath
        val java = "${System.getProperty("java.home")}/bin/java"
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
              "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>$label</string>
                <key>ProgramArguments</key>
                <array>
                    <string>$java</string>
                    <string>-jar</string>
                    <string>$jar</string>
                    <string>--startup</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
                <key>ProcessType</key>
                <string>Interactive</string>
            </dict>
            </plist>
        """.trimIndent()
    }

    private fun uid(): String =
        ProcessBuilder("id", "-u").start().inputStream.bufferedReader().readText().trim()
}
