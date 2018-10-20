data class MemoryRegion(val start: Long, val size: Long, val permissions: Int, val detail: String) {
    val canRead: Boolean = permissions and 1 == 1
    val canWrite: Boolean = permissions and 2 == 2
    val canExecute: Boolean = permissions and 4 == 4
}