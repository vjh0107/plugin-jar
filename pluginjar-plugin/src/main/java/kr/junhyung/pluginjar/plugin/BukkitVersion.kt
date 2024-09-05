package kr.junhyung.pluginjar.plugin

import org.gradle.api.JavaVersion

class BukkitVersion(
    private val phase: Int,
    private val major: Int,
    private val minor: Int?
) : Comparable<BukkitVersion> {

    companion object {
        private const val MINOR_API_VERSION_SUPPORTED_FROM = "1.20.5"
        private const val SNAPSHOT_SUFFIX = "-SNAPSHOT"
        private const val RELEASE_SUFFIX = "-R0.1"

        /**
         * Parses a version string into a [BukkitVersion] object.
         * The version string must be in the format of `phase.major` or `phase.major.minor`.
         * It may contain the release and snapshot suffix, but it is not required.
         *
         * @param version The version string to parse. which don't have to contain the release and snapshot suffix.
         * @return The parsed [BukkitVersion] object.
         * @throws NumberFormatException If the version string is not in the correct format.
         * @throws UnsupportedOperationException If the version is before 1.17.
         */
        fun parse(version: String): BukkitVersion {
            val parts = version
                .removeSuffix(SNAPSHOT_SUFFIX)
                .removeSuffix(RELEASE_SUFFIX)
                .split(".")
            if (parts.size !in 2..3) {
                throw NumberFormatException("Version string must have 2 or 3 parts separated by dots.")
            }
            val phase = parts[0].toInt()
            val major = parts[1].toInt()
            val minor = parts.getOrNull(2)?.toInt()
            if (major < 17) {
                throw UnsupportedOperationException("Versions before 1.17 are not supported.")
            }
            return BukkitVersion(phase, major, minor)
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
            toString()
        }
    }

    fun getVersion(): String {
        return toString() + RELEASE_SUFFIX + SNAPSHOT_SUFFIX
    }

    /**
     * Checks if this version is compatible for development with the given Java version.
     *
     * @param javaVersion The Java version to check compatibility with. Typically, the project's Java version.
     */
    fun isCompatibleWith(javaVersion: JavaVersion): Boolean {
        return javaVersion >= getMinimumCompatibleVersion()
    }

    fun getMinimumCompatibleVersion(): JavaVersion {
        return if (this < parse("1.20.5")) {
            JavaVersion.VERSION_17
        } else {
            JavaVersion.VERSION_21
        }
    }

    override fun toString(): String {
        return if (minor == null) {
            "$phase.$major"
        } else {
            "$phase.$major.$minor"
        }
    }
}