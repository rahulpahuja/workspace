import java.awt.Color
import java.awt.Font

/** Single source of colour and type for the whole UI. Components never hold their own hex values. */
object Theme {
    val ink = Color(0x1E2530)
    val paper = Color(0xF4F5F7)
    val surface = Color(0xFFFFFF)
    val rule = Color(0xDDE1E7)
    val muted = Color(0x6B7585)
    val signal = Color(0x2BA56B)
    val appBadge = Color(0x3B4A63)
    val webBadge = Color(0x2F6BD6)

    private const val SANS = "SansSerif"
    private const val MONO = "Menlo"

    val display: Font = Font(SANS, Font.BOLD, 20)
    val heading: Font = Font(SANS, Font.BOLD, 13)
    val body: Font = Font(SANS, Font.PLAIN, 13)
    val caption: Font = Font(SANS, Font.BOLD, 11)
    val mono: Font = Font(MONO, Font.PLAIN, 11)
}
