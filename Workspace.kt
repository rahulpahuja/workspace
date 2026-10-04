import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.Insets
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.prefs.Preferences
import javax.swing.*
import javax.swing.border.EmptyBorder

data class WorkspaceItem(
    val name: String,
    val type: String, // "app" or "url"
    val target: String,
    var enabled: Boolean = true
)

class Mobile1XWorkspace : JFrame("Mobile1X Workspace") {
    private val prefs = Preferences.userRoot().node("mobile1x-workspace")
    private val items = mutableListOf<WorkspaceItem>()
    private val listPanel = JPanel()
    private val startupBox = JCheckBox("Launch workspace at Mac login")
    private val profileField = JTextField(prefs.get("chromeProfile", "Default"), 14)
    private val status = JLabel("● Ready")
    private val configFile = File(
        System.getProperty("user.home"),
        "Library/Application Support/Mobile1XWorkspace/workspace.properties"
    )

    init {
        load()
        if (items.isEmpty()) seedDefaults()

        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = java.awt.Dimension(720, 620)
        setLocationRelativeTo(null)

        val root = JPanel(BorderLayout(12, 12)).apply {
            border = EmptyBorder(16, 16, 16, 16)
        }
        contentPane = root

        root.add(header(), BorderLayout.NORTH)
        root.add(buildCenter(), BorderLayout.CENTER)
        root.add(buildFooter(), BorderLayout.SOUTH)

        startupBox.isSelected = prefs.getBoolean("launchAtLogin", false)
        startupBox.addActionListener {
            prefs.putBoolean("launchAtLogin", startupBox.isSelected)
            setLaunchAtLogin(startupBox.isSelected)
            save()
        }

        rebuildList()
    }

    private fun header(): JPanel {
        val p = JPanel(BorderLayout())
        val title = JLabel("<html><b><font size='5'>Mobile1X Workspace</font></b><br>" +
                "<font color='#666666'>Your configurable Mac work environment</font></html>")
        p.add(title, BorderLayout.WEST)

        val settings = JButton("⚙ Settings")
        settings.addActionListener { settingsDialog() }
        p.add(settings, BorderLayout.EAST)
        return p
    }

    private fun buildCenter(): JPanel {
        val wrapper = JPanel(BorderLayout(8, 8))

        val top = JPanel(BorderLayout())
        top.add(JLabel("<html><b>MY WORKSPACE</b></html>"), BorderLayout.WEST)

        val add = JButton("+ Add")
        add.addActionListener { addItemDialog() }
        top.add(add, BorderLayout.EAST)
        wrapper.add(top, BorderLayout.NORTH)

        listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)
        val scroll = JScrollPane(listPanel)
        scroll.border = BorderFactory.createLineBorder(java.awt.Color(220, 220, 220))
        wrapper.add(scroll, BorderLayout.CENTER)

        return wrapper
    }

    private fun buildFooter(): JPanel {
        val outer = JPanel(BorderLayout(8, 8))

        val controls = JPanel(GridLayout(1, 2, 8, 8))
        val launch = JButton("🚀 Launch Workspace")
        launch.addActionListener { launchWorkspace() }
        val stop = JButton("■ Stop Workspace")
        stop.addActionListener { stopWorkspace() }
        controls.add(launch)
        controls.add(stop)

        val options = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        options.add(startupBox)
        options.add(JLabel("Chrome profile:"))
        options.add(profileField)
        profileField.addActionListener {
            prefs.put("chromeProfile", profileField.text.trim().ifBlank { "Default" })
        }

        outer.add(options, BorderLayout.NORTH)
        outer.add(controls, BorderLayout.CENTER)
        outer.add(status, BorderLayout.SOUTH)
        return outer
    }

    private fun rebuildList() {
        listPanel.removeAll()

        items.forEachIndexed { index, item ->
            val row = JPanel(BorderLayout(8, 4)).apply {
                border = EmptyBorder(7, 8, 7, 8)
                maximumSize = java.awt.Dimension(Int.MAX_VALUE, 58)
            }

            val enabled = JCheckBox()
            enabled.isSelected = item.enabled
            enabled.addActionListener {
                item.enabled = enabled.isSelected
                save()
            }

            val icon = if (item.type == "app") "▣" else "◎"
            val info = JLabel(
                "<html><b>$icon ${escape(item.name)}</b><br>" +
                        "<font color='#777777'>${escape(item.target)}</font></html>"
            )

            val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
            val up = JButton("↑")
            val down = JButton("↓")
            val edit = JButton("Edit")
            val remove = JButton("×")

            up.addActionListener {
                if (index > 0) {
                    val x = items.removeAt(index)
                    items.add(index - 1, x)
                    save()
                    rebuildList()
                }
            }
            down.addActionListener {
                if (index < items.lastIndex) {
                    val x = items.removeAt(index)
                    items.add(index + 1, x)
                    save()
                    rebuildList()
                }
            }
            edit.addActionListener { editItemDialog(index) }
            remove.addActionListener {
                items.removeAt(index)
                save()
                rebuildList()
            }

            actions.add(up)
            actions.add(down)
            actions.add(edit)
            actions.add(remove)

            row.add(enabled, BorderLayout.WEST)
            row.add(info, BorderLayout.CENTER)
            row.add(actions, BorderLayout.EAST)
            listPanel.add(row)
            listPanel.add(JSeparator())
        }

        listPanel.revalidate()
        listPanel.repaint()
    }

    private fun addItemDialog() {
        val type = JOptionPane.showInputDialog(
            this,
            "What do you want to add?",
            "Add to Workspace",
            JOptionPane.QUESTION_MESSAGE,
            null,
            arrayOf("Mac Application", "Website / URL"),
            "Mac Application"
        ) ?: return

        if (type == "Mac Application") {
            val chooser = JFileChooser("/Applications")
            chooser.fileSelectionMode = JFileChooser.FILES_ONLY
            chooser.dialogTitle = "Choose a macOS application (.app)"
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                if (!file.name.endsWith(".app")) {
                    JOptionPane.showMessageDialog(this, "Please select a .app application.")
                    return
                }
                items.add(WorkspaceItem(file.nameWithoutExtension, "app", file.absolutePath))
                save()
                rebuildList()
            }
        } else {
            val url = JOptionPane.showInputDialog(this, "URL:", "https://")
            if (!url.isNullOrBlank()) {
                val name = JOptionPane.showInputDialog(this, "Name:", deriveName(url))
                if (!name.isNullOrBlank()) {
                    items.add(WorkspaceItem(name, "url", url.trim()))
                    save()
                    rebuildList()
                }
            }
        }
    }

    private fun editItemDialog(index: Int) {
        val item = items[index]
        val name = JOptionPane.showInputDialog(this, "Name:", item.name) ?: return
        if (name.isBlank()) return

        if (item.type == "url") {
            val url = JOptionPane.showInputDialog(this, "URL:", item.target) ?: return
            items[index] = item.copy(name = name.trim(), target = url.trim())
        } else {
            items[index] = item.copy(name = name.trim())
        }
        save()
        rebuildList()
    }

    private fun settingsDialog() {
        val panel = JPanel(GridLayout(0, 2, 8, 8))
        panel.add(JLabel("Chrome profile directory:"))
        panel.add(profileField)

        panel.add(JLabel("Config file:"))
        panel.add(JLabel(configFile.absolutePath))

        JOptionPane.showMessageDialog(
            this,
            panel,
            "Settings",
            JOptionPane.INFORMATION_MESSAGE
        )
        prefs.put("chromeProfile", profileField.text.trim().ifBlank { "Default" })
        save()
    }

    private fun launchWorkspace() {
        save()
        val enabled = items.filter { it.enabled }
        status.text = "● Launching ${enabled.size} items..."

        // Launch native apps first.
        enabled.filter { it.type == "app" }.forEach {
            runCatching { ProcessBuilder("open", it.target).start() }
        }

        // Open URLs through the selected Chrome profile.
        val urls = enabled.filter { it.type == "url" }.map { it.target }
        if (urls.isNotEmpty()) {
            val profile = profileField.text.trim().ifBlank { "Default" }
            prefs.put("chromeProfile", profile)

            val args = mutableListOf(
                "open", "-na", "Google Chrome",
                "--args",
                "--profile-directory=$profile"
            )
            args.addAll(urls)
            runCatching { ProcessBuilder(args).start() }
        }

        status.text = "● Workspace launched"
    }

    private fun stopWorkspace() {
        // Deliberately only stops apps configured in this workspace.
        items.filter { it.enabled && it.type == "app" }.forEach { item ->
            val appName = File(item.target).name.removeSuffix(".app")
            runCatching { ProcessBuilder("osascript", "-e", "tell application \"$appName\" to quit").start() }
        }

        // Close Chrome windows/tabs is intentionally not forced because Chrome may contain
        // unrelated windows. User can close the Mobile1X Chrome window normally.
        status.text = "● Workspace apps stopped"
    }

    private fun setLaunchAtLogin(enabled: Boolean) {
        val launchAgents = File(System.getProperty("user.home"), "Library/LaunchAgents")
        launchAgents.mkdirs()
        val plist = File(launchAgents, "com.mobile1x.workspace.plist")

        if (!enabled) {
            runCatching { ProcessBuilder("launchctl", "bootout", "gui/${uid()}", plist.absolutePath).start().waitFor() }
            plist.delete()
            return
        }

        val jar = File(
            Mobile1XWorkspace::class.java.protectionDomain.codeSource.location.toURI()
        ).absolutePath

        val java = "${System.getProperty("java.home")}/bin/java"
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
              "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>com.mobile1x.workspace</string>
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

        plist.writeText(content)
        runCatching {
            ProcessBuilder("launchctl", "bootout", "gui/${uid()}", plist.absolutePath).start().waitFor()
            ProcessBuilder("launchctl", "bootstrap", "gui/${uid()}", plist.absolutePath).start().waitFor()
        }
    }

    private fun uid(): String {
        return ProcessBuilder("id", "-u").start().inputStream.bufferedReader().readText().trim()
    }

    private fun save() {
        configFile.parentFile.mkdirs()
        val lines = buildString {
            appendLine("launchAtLogin=${startupBox.isSelected}")
            appendLine("chromeProfile=${escapeProperty(profileField.text.trim().ifBlank { "Default" })}")
            items.forEachIndexed { i, item ->
                appendLine("item.$i.name=${escapeProperty(item.name)}")
                appendLine("item.$i.type=${item.type}")
                appendLine("item.$i.target=${escapeProperty(item.target)}")
                appendLine("item.$i.enabled=${item.enabled}")
            }
        }
        configFile.writeText(lines, Charsets.UTF_8)
    }

    private fun load() {
        if (!configFile.exists()) return
        val p = java.util.Properties()
        configFile.inputStream().use { p.load(it) }

        prefs.putBoolean("launchAtLogin", p.getProperty("launchAtLogin", "false").toBoolean())
        prefs.put("chromeProfile", p.getProperty("chromeProfile", "Default"))

        var i = 0
        while (p.getProperty("item.$i.name") != null) {
            items.add(
                WorkspaceItem(
                    p.getProperty("item.$i.name"),
                    p.getProperty("item.$i.type", "app"),
                    p.getProperty("item.$i.target", ""),
                    p.getProperty("item.$i.enabled", "true").toBoolean()
                )
            )
            i++
        }
    }

    private fun seedDefaults() {
        items.add(WorkspaceItem("Firebase", "url", "https://console.firebase.google.com/"))
        items.add(WorkspaceItem("Google Analytics", "url", "https://analytics.google.com/"))
        items.add(WorkspaceItem("Amplitude", "url", "https://app.amplitude.com/"))
        items.add(WorkspaceItem("ChatGPT", "url", "https://chatgpt.com/"))
        items.add(WorkspaceItem("Claude", "url", "https://claude.ai/"))
        items.add(WorkspaceItem("Mobile1X GitHub", "url", "https://github.com/Mobile1X"))
        save()
    }

    private fun deriveName(url: String): String {
        return runCatching {
            java.net.URI(url).host?.removePrefix("www.") ?: url
        }.getOrDefault(url)
    }

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun escapeProperty(s: String): String =
        s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                val app = Mobile1XWorkspace()
                app.isVisible = true

                // Launch only when explicitly started by the LaunchAgent.
                if (args.contains("--startup")) {
                    Timer(1500) {
                        app.launchWorkspace()
                    }.apply { isRepeats = false }.start()
                }
            }
        }
    }
}
