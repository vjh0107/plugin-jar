package kr.junhyung.pluginjar.plugin

class BukkitVersion(
    private val phase: Int,
    private val major: Int,
    private val minor: Int?,
    @Suppress("unused") private val isSnapshotVersion: Boolean
) : Comparable<BukkitVersion> {

    companion object {
        private const val MINOR_API_VERSION_SUPPORTED_FROM = "1.20.5"
        private const val SNAPSHOT_SUFFIX = "-SNAPSHOT"
        private const val RELEASE_SUFFIX = "-R0.1"

        fun parse(version: String): BukkitVersion {
            val parts = version
                .removeSuffix(SNAPSHOT_SUFFIX)
                .removeSuffix(RELEASE_SUFFIX)
                .split(".")
            return BukkitVersion(parts[0].toInt(), parts[1].toInt(), parts.getOrNull(2)?.toInt(), version.endsWith(
                SNAPSHOT_SUFFIX
            ))
        }
    }

    override fun compareTo(other: BukkitVersion): Int {
        if (phase != other.phase) {
            return phase.compareTo(other.phase)
        }
        if (major != other.major) {
            return major.compareTo(other.major)
        }
        if (minor != null) {
            if (other.minor == null) {
                return 1
            }
            if (minor != other.minor) {
                return minor.compareTo(other.minor)
            }
        }
        return 0
    }

    fun getApiVersion(): String {
        return if (this < parse(MINOR_API_VERSION_SUPPORTED_FROM)) {
            "$phase.$major"
        } else {
            if (minor == null) {
                "$phase.$major"
            } else {
                "$phase.$major.$minor"
            }
        }
    }
}