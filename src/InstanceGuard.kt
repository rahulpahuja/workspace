import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit

/** Ensures only one workspace process runs at a time, using an OS-level file lock. */
class InstanceGuard(private val lockFile: File) {

    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    /** Returns true when this process now owns the lock. */
    fun tryAcquire(): Boolean {
        lockFile.parentFile.mkdirs()
        val ch = FileChannel.open(
            lockFile.toPath(),
            StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE
        )
        val acquired = runCatching { ch.tryLock() }.getOrNull()
        if (acquired == null) {
            ch.close()
            return false
        }
        channel = ch
        lock = acquired
        ch.truncate(0)
        ch.write(ByteBuffer.wrap(ProcessHandle.current().pid().toString().toByteArray()), 0)
        return true
    }

    /** PID of the process currently holding the lock, if it was recorded. */
    fun ownerPid(): Long? = runCatching { lockFile.readText().trim().toLong() }.getOrNull()

    /** Waits until the lock becomes free, for up to [timeoutSeconds]. */
    fun awaitAcquire(timeoutSeconds: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000
        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire()) return true
            Thread.sleep(200)
        }
        return false
    }
}

/** Options offered when another instance is already running. */
enum class ExistingInstanceAction(val label: String) {
    CLOSE_EXISTING("Close it"),
    RESTART_EXISTING("Re-run it"),
    DO_NOTHING("Do nothing")
}

/** Stops a running instance by PID, forcing termination if it does not exit in time. */
object InstanceTerminator {
    fun terminate(pid: Long, timeoutSeconds: Long = 5) {
        val handle = ProcessHandle.of(pid).orElse(null) ?: return
        handle.destroy()
        if (!handle.onExit().completeOnTimeout(null, timeoutSeconds, TimeUnit.SECONDS).isDone) {
            handle.destroyForcibly()
        }
        handle.onExit().get(timeoutSeconds, TimeUnit.SECONDS)
    }
}
