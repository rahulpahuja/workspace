import java.io.File

/** Opens and closes workspace items on the Mac. */
interface Launcher {
    fun openApp(path: String)
    fun quitApp(path: String)
    fun openUrls(urls: List<String>, profileDirectory: String)
}

class MacLauncher(private val chromeAppName: String) : Launcher {

    override fun openApp(path: String) {
        runCatching { ProcessBuilder("open", path).start() }
    }

    override fun quitApp(path: String) {
        val appName = File(path).name.removeSuffix(".app")
        runCatching { ProcessBuilder("osascript", "-e", "tell application \"$appName\" to quit").start() }
    }

    override fun openUrls(urls: List<String>, profileDirectory: String) {
        if (urls.isEmpty()) return
        val args = mutableListOf(
            "open", "-na", chromeAppName,
            "--args",
            "--profile-directory=$profileDirectory"
        )
        args.addAll(urls)
        runCatching { ProcessBuilder(args).start() }
    }
}
