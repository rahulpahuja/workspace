import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder

class WorkspaceFrame(
    private val config: WorkspaceConfig,
    private val store: ConfigStore,
    private val profileSource: ChromeProfileSource,
    private val launcher: Launcher,
    private val loginItem: LoginItem,
    private val configFile: File
) : JFrame("Mobile1X Workspace") {

    private val listPanel = JPanel()
    private val startupBox = JCheckBox("Launch workspace at Mac login")
    private val profileBox = JComboBox<ChromeProfile>()
    private val status = JLabel("● Ready")

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(720, 620)
        setLocationRelativeTo(null)

        val root = JPanel(BorderLayout(12, 12)).apply { border = EmptyBorder(16, 16, 16, 16) }
        contentPane = root
        root.add(header(), BorderLayout.NORTH)
        root.add(buildCenter(), BorderLayout.CENTER)
        root.add(buildFooter(), BorderLayout.SOUTH)

        loadProfiles()
        startupBox.isSelected = config.launchAtLogin
        startupBox.addActionListener {
            config.launchAtLogin = startupBox.isSelected
            loginItem.setEnabled(startupBox.isSelected)
            save()
        }

        rebuildList()
    }

    /** Launches enabled apps and URLs. Public so the login-startup path can trigger it. */
    fun launchWorkspace() {
        save()
        val enabled = config.items.filter { it.enabled }
        status.text = "● Launching ${enabled.size} items..."

        enabled.filter { it.type == "app" }.forEach { launcher.openApp(it.target) }

        val profile = selectedProfile()?.directory ?: config.chromeProfile
        launcher.openUrls(enabled.filter { it.type == "url" }.map { it.target }, profile)

        status.text = "● Workspace launched"
    }

    private fun stopWorkspace() {
        // Deliberately only stops apps configured in this workspace.
        config.items.filter { it.enabled && it.type == "app" }.forEach { launcher.quitApp(it.target) }
        status.text = "● Workspace apps stopped"
    }

    private fun header(): JPanel {
        val p = JPanel(BorderLayout())
        p.add(
            JLabel(
                "<html><b><font size='5'>Mobile1X Workspace</font></b><br>" +
                        "<font color='#666666'>Your configurable Mac work environment</font></html>"
            ),
            BorderLayout.WEST
        )
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
        scroll.border = BorderFactory.createLineBorder(Color(220, 220, 220))
        wrapper.add(scroll, BorderLayout.CENTER)
        return wrapper
    }

    private fun buildFooter(): JPanel {
        val outer = JPanel(BorderLayout(8, 8))

        val controls = JPanel(GridLayout(1, 2, 8, 8))
        controls.add(JButton("🚀 Launch Workspace").apply { addActionListener { launchWorkspace() } })
        controls.add(JButton("■ Stop Workspace").apply { addActionListener { stopWorkspace() } })

        val options = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        options.add(startupBox)
        options.add(JLabel("Chrome profile:"))
        options.add(profileBox)
        profileBox.addActionListener {
            selectedProfile()?.let { config.chromeProfile = it.directory }
            save()
        }

        outer.add(options, BorderLayout.NORTH)
        outer.add(controls, BorderLayout.CENTER)
        outer.add(status, BorderLayout.SOUTH)
        return outer
    }

    private fun loadProfiles() {
        val profiles = profileSource.profiles().ifEmpty {
            listOf(ChromeProfile(config.chromeProfile, config.chromeProfile))
        }
        profiles.forEach { profileBox.addItem(it) }
        profileBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ) = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
                (value as? ChromeProfile)?.let { p ->
                    text = if (p.email.isBlank()) "${p.displayName}  (${p.directory})"
                    else "${p.displayName}  ·  ${p.email}  (${p.directory})"
                }
            }
        }
        profileBox.selectedItem = profiles.firstOrNull { it.directory == config.chromeProfile }
            ?: profiles.first()
    }

    private fun selectedProfile(): ChromeProfile? = profileBox.selectedItem as? ChromeProfile

    private fun rebuildList() {
        listPanel.removeAll()

        config.items.forEachIndexed { index, item ->
            val row = JPanel(BorderLayout(8, 4)).apply {
                border = EmptyBorder(7, 8, 7, 8)
                maximumSize = Dimension(Int.MAX_VALUE, 58)
            }

            val enabled = JCheckBox().apply {
                isSelected = item.enabled
                addActionListener {
                    item.enabled = isSelected
                    save()
                }
            }

            val icon = if (item.type == "app") "▣" else "◎"
            val info = JLabel(
                "<html><b>$icon ${escape(item.name)}</b><br>" +
                        "<font color='#777777'>${escape(item.target)}</font></html>"
            )

            val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
            actions.add(JButton("↑").apply {
                addActionListener { moveItem(index, index - 1) }
            })
            actions.add(JButton("↓").apply {
                addActionListener { moveItem(index, index + 1) }
            })
            actions.add(JButton("Edit").apply { addActionListener { editItemDialog(index) } })
            actions.add(JButton("×").apply {
                addActionListener {
                    config.items.removeAt(index)
                    save()
                    rebuildList()
                }
            })

            row.add(enabled, BorderLayout.WEST)
            row.add(info, BorderLayout.CENTER)
            row.add(actions, BorderLayout.EAST)
            listPanel.add(row)
            listPanel.add(JSeparator())
        }

        listPanel.revalidate()
        listPanel.repaint()
    }

    private fun moveItem(from: Int, to: Int) {
        if (to !in config.items.indices) return
        val item = config.items.removeAt(from)
        config.items.add(to, item)
        save()
        rebuildList()
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
            val chooser = JFileChooser("/Applications").apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
                dialogTitle = "Choose a macOS application (.app)"
            }
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                if (!file.name.endsWith(".app")) {
                    JOptionPane.showMessageDialog(this, "Please select a .app application.")
                    return
                }
                config.items.add(WorkspaceItem(file.nameWithoutExtension, "app", file.absolutePath))
                save()
                rebuildList()
            }
        } else {
            val url = JOptionPane.showInputDialog(this, "URL:", "https://")
            if (!url.isNullOrBlank()) {
                val name = JOptionPane.showInputDialog(this, "Name:", deriveName(url))
                if (!name.isNullOrBlank()) {
                    config.items.add(WorkspaceItem(name, "url", url.trim()))
                    save()
                    rebuildList()
                }
            }
        }
    }

    private fun editItemDialog(index: Int) {
        val item = config.items[index]
        val name = JOptionPane.showInputDialog(this, "Name:", item.name) ?: return
        if (name.isBlank()) return

        if (item.type == "url") {
            val url = JOptionPane.showInputDialog(this, "URL:", item.target) ?: return
            config.items[index] = item.copy(name = name.trim(), target = url.trim())
        } else {
            config.items[index] = item.copy(name = name.trim())
        }
        save()
        rebuildList()
    }

    private fun settingsDialog() {
        val panel = JPanel(GridLayout(0, 1, 4, 4))
        panel.add(JLabel("Chrome profiles are read from Chrome's Local State file."))
        panel.add(JLabel("Config file: ${configFile.absolutePath}"))
        JOptionPane.showMessageDialog(this, panel, "Settings", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun save() = store.save(config)

    private fun deriveName(url: String): String =
        runCatching { java.net.URI(url).host?.removePrefix("www.") ?: url }.getOrDefault(url)

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
