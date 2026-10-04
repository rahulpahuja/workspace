import javax.swing.JOptionPane

/** Asks the user what to do when another workspace instance is already running. */
object ExistingInstanceDialog {
    private val actions = ExistingInstanceAction.entries

    fun ask(pid: Long?): ExistingInstanceAction {
        val choice = JOptionPane.showOptionDialog(
            null,
            "Mobile1X Workspace is already running" +
                    (pid?.let { " (PID $it)" } ?: "") +
                    ".\n\nWhat would you like to do?",
            "Mobile1X Workspace",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            actions.map { it.label }.toTypedArray(),
            ExistingInstanceAction.DO_NOTHING.label
        )
        return actions.getOrElse(choice) { ExistingInstanceAction.DO_NOTHING }
    }
}
