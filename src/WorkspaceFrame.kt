import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder

class WorkspaceFrame(
    private val config: WorkspaceConfig,
    private val store: ConfigStore,
    private val profileSource: ChromeProfileSource,
    private val launcher: Launcher,
    private val loginItem: LoginItem,
    private val configFile: File
) : JFrame("Mobile1X Workspace") {

    private val listPanel = JPanel()
    private val summary = JLabel()
    private val startupBox = JCheckBox("Open at Mac login")
    private val profileBox = JComboBox<ChromeProfile>()
    private val status = JLabel("Ready")
    private val launchButton = JButton()
    private val stopButton = JButton("Stop")

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(720, 620)
        setSize(760, 680)
        setLocationRelativeTo(null)

        val root = JPanel(BorderLayout()).apply { background = Theme.paper }
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
        status.text = "Opening ${enabled.size} item(s)…"

        enabled.filter { it.type == "app" }.forEach { launcher.openApp(it.target) }

        val profile = selectedProfile()?.directory ?: config.chromeProfile
        launcher.openUrls(enabled.filter { it.type == "url" }.map { it.target }, profile)

        status.text = "Opened ${enabled.size} item(s) in ${profileName()}"
    }

    private fun stopWorkspace() {
        // Deliberately only stops apps configured in this workspace.
        val apps = config.items.filter { it.enabled && it.type == "app" }
        apps.forEach { launcher.quitApp(it.target) }
        status.text = "Quit ${apps.size} app(s). Chrome windows stay open."
    }

    private fun header(): JPanel {
        val title = JLabel("Workspace").apply { font = Theme.display; foreground = Theme.ink }
        summary.font = Theme.body
        summary.foreground = Theme.muted

        val text = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(title, BorderLayout.NORTH)
            add(summary, BorderLayout.SOUTH)
        }

        val settings = secondaryButton("Settings") { settingsDialog() }

        return JPanel(BorderLayout()).apply {
            background = Theme.surface
            border = CompoundBorder(
                MatteBorder(0, 0, 1, 0, Theme.rule),
                EmptyBorder(16, 20, 16, 20)
            )
            add(text, BorderLayout.CENTER)
            add(settings, BorderLayout.EAST)
        }
    }

    private fun buildCenter(): JPanel {
        val wrapper = JPanel(BorderLayout()).apply { background = Theme.paper }

        val sectionLabel = JLabel("ITEMS").apply {
            font = Theme.caption
            foreground = Theme.muted
        }
        val add = secondaryButton("Add item") { addItemDialog() }
        val top = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = EmptyBorder(14, 20, 8, 20)
            add(sectionLabel, BorderLayout.WEST)
            add(add, BorderLayout.EAST)
        }
        wrapper.add(top, BorderLayout.NORTH)

        // Rows stack inside a top-anchored container so the scroll viewport always gets a real height.
        val rowsHost = JPanel(BorderLayout()).apply {
            background = Theme.surface
            add(listPanel, BorderLayout.NORTH)
        }
        listPanel.layout = BoxLayout(listPanel, BoxLayout.Y_AXIS)
        listPanel.background = Theme.surface
        val scroll = JScrollPane(rowsHost).apply {
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.unitIncrement = 16
            verticalScrollBar.blockIncrement = 96
            border = MatteBorder(1, 0, 1, 0, Theme.rule)
            viewport.background = Theme.surface
        }
        wrapper.add(scroll, BorderLayout.CENTER)
        // Side gutters keep the board from touching the window edge.
        wrapper.border = EmptyBorder(0, 20, 0, 20)
        return wrapper
    }

    private fun buildFooter(): JPanel {
        launchButton.putClientProperty("JButton.buttonType", "default")
        launchButton.font = Theme.heading
        launchButton.addActionListener { launchWorkspace() }
        stopButton.addActionListener { stopWorkspace() }

        val options = JPanel(FlowLayout(FlowLayout.LEFT, 12, 0)).apply {
            isOpaque = false
            add(startupBox)
            add(JLabel("Chrome profile").apply { font = Theme.caption; foreground = Theme.muted })
            add(profileBox)
        }
        profileBox.addActionListener {
            selectedProfile()?.let { config.chromeProfile = it.directory }
            save()
            updateSummary()
        }

        val actions = JPanel(BorderLayout(8, 0)).apply {
            isOpaque = false
            add(stopButton, BorderLayout.WEST)
            add(launchButton, BorderLayout.CENTER)
        }

        status.font = Theme.mono
        status.foreground = Theme.muted

        val body = JPanel(BorderLayout(0, 10)).apply {
            background = Theme.surface
            border = CompoundBorder(
                MatteBorder(1, 0, 0, 0, Theme.rule),
                EmptyBorder(14, 20, 14, 20)
            )
            add(options, BorderLayout.NORTH)
            add(actions, BorderLayout.CENTER)
            add(status, BorderLayout.SOUTH)
        }
        return body
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

    private fun profileName(): String = selectedProfile()?.displayName ?: config.chromeProfile

    private fun rebuildList() {
        listPanel.removeAll()

        if (config.items.isEmpty()) {
            listPanel.add(emptyState())
        }

        config.items.forEachIndexed { index, item ->
            listPanel.add(itemRow(index, item))
            listPanel.add(JSeparator().apply { foreground = Theme.rule; background = Theme.rule })
        }

        updateControlState()
        listPanel.revalidate()
        listPanel.repaint()
    }

    private fun emptyState(): JComponent = JLabel(
        "<html><div style='width:360px;padding:28px 0;color:#6B7585'>" +
                "Nothing on the board yet.<br>Add a website or a Mac app to start your workspace.</div></html>"
    ).apply {
        font = Theme.body
        alignmentX = LEFT_ALIGNMENT
    }

    private fun itemRow(index: Int, item: WorkspaceItem): JPanel {
        val row = JPanel(BorderLayout(12, 0)).apply {
            background = Theme.surface
            border = EmptyBorder(10, 4, 10, 4)
            alignmentX = LEFT_ALIGNMENT
        }

        val enabled = JCheckBox().apply {
            isSelected = item.enabled
            background = Theme.surface
            toolTipText = if (item.enabled) "Turn off for launch" else "Turn on for launch"
            accessibleContext.accessibleName = "Include ${item.name} in launch"
            addActionListener {
                item.enabled = isSelected
                save()
                rebuildList()
            }
        }

        val name = JLabel(item.name).apply {
            font = Theme.heading
            foreground = if (item.enabled) Theme.ink else Theme.muted
        }
        val target = JLabel(item.target).apply {
            font = Theme.mono
            foreground = Theme.muted
        }
        val text = JPanel(GridLayout(2, 1)).apply {
            isOpaque = false
            add(name)
            add(target)
        }

        val badge = JLabel(if (item.type == "app") "APP" else "WEB").apply {
            font = Theme.caption
            foreground = java.awt.Color.WHITE
            background = if (item.type == "app") Theme.appBadge else Theme.webBadge
            border = EmptyBorder(3, 7, 3, 7)
            isOpaque = true
        }

        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
            isOpaque = false
            add(iconButton("Move up", "↑", index > 0) { moveItem(index, index - 1) })
            add(iconButton("Move down", "↓", index < config.items.lastIndex) { moveItem(index, index + 1) })
            add(textButton("Edit") { editItemDialog(index) })
            add(iconButton("Remove", "×", true) { confirmRemove(index) })
        }

        val leading = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0)).apply {
            isOpaque = false
            add(enabled)
            add(badge)
        }

        row.add(leading, BorderLayout.WEST)
        row.add(text, BorderLayout.CENTER)
        row.add(actions, BorderLayout.EAST)
        // Measure only after children exist; a height taken from an empty panel collapses the row to 0px.
        row.maximumSize = Dimension(Int.MAX_VALUE, row.preferredSize.height)
        return row
    }

    private fun secondaryButton(label: String, action: () -> Unit) = JButton(label).apply {
        font = Theme.body
        addActionListener { action() }
    }

    private fun textButton(label: String, action: () -> Unit) = JButton(label).apply {
        putClientProperty("JButton.buttonType", "borderless")
        font = Theme.body
        addActionListener { action() }
    }

    private fun iconButton(name: String, glyph: String, enabledState: Boolean, action: () -> Unit) =
        JButton(glyph).apply {
            putClientProperty("JButton.buttonType", "borderless")
            toolTipText = name
            accessibleContext.accessibleName = name
            isEnabled = enabledState
            addActionListener { action() }
        }

    /** Launch needs at least one enabled item; stop only makes sense when apps are enabled. */
    private fun updateControlState() {
        val total = config.items.size
        val lit = config.items.count { it.enabled }
        launchButton.isEnabled = lit > 0
        launchButton.text = if (lit == 1) "Launch 1 item" else "Launch $lit items"
        stopButton.isEnabled = config.items.any { it.enabled && it.type == "app" }
        summary.text = when {
            total == 0 -> "No items yet"
            else -> "$lit of $total items open in ${profileName()}"
        }
    }

    private fun updateSummary() = updateControlState()

    private fun moveItem(from: Int, to: Int) {
        if (to !in config.items.indices) return
        val item = config.items.removeAt(from)
        config.items.add(to, item)
        save()
        rebuildList()
    }

    private fun confirmRemove(index: Int) {
        val item = config.items[index]
        val choice = JOptionPane.showConfirmDialog(
            this,
            "Remove \"${item.name}\" from the workspace?\nThe app or website itself is not affected.",
            "Remove item",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        if (choice != JOptionPane.OK_OPTION) return
        config.items.removeAt(index)
        save()
        rebuildList()
        status.text = "Removed \"${item.name}\""
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
}
