import java.io.File
import javax.swing.SwingUtilities
import javax.swing.Timer

private val APP_SUPPORT_DIR = File(System.getProperty("user.home"), "Library/Application Support/Mobile1XWorkspace")
private val CHROME_LOCAL_STATE = File(System.getProperty("user.home"), "Library/Application Support/Google/Chrome/Local State")

fun main(args: Array<String>) {
    SwingUtilities.invokeLater {
        val configFile = File(APP_SUPPORT_DIR, "workspace.properties")
        val store = PropertiesConfigStore(configFile)
        val config = store.load()

        val frame = WorkspaceFrame(
            config = config,
            store = store,
            profileSource = LocalStateProfileSource(CHROME_LOCAL_STATE),
            launcher = MacLauncher(config.chromeAppName),
            loginItem = LaunchAgentLoginItem(),
            configFile = configFile
        )
        frame.isVisible = true

        // Launch only when explicitly started by the LaunchAgent.
        if (args.contains("--startup")) {
            Timer(1500) { frame.launchWorkspace() }.apply { isRepeats = false }.start()
        }
    }
}
