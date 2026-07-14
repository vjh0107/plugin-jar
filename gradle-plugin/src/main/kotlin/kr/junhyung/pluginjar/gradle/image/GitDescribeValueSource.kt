package kr.junhyung.pluginjar.gradle.image

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Produces an immutable identifier for the current checkout via `git describe`. Falls back to
 * `nogit` when git is unavailable (e.g. tarball builds) so the value is always usable.
 */
abstract class GitDescribeValueSource : ValueSource<String, GitDescribeValueSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val projectDirectory: DirectoryProperty
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val stdout = ByteArrayOutputStream()
        return try {
            val result = execOperations.exec {
                commandLine("git", "describe", "--tags", "--always", "--dirty=-dirty")
                workingDir = parameters.projectDirectory.get().asFile
                standardOutput = stdout
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) stdout.toString().trim().ifEmpty { NO_GIT } else NO_GIT
        } catch (e: Exception) {
            NO_GIT
        }
    }

    private companion object {
        const val NO_GIT = "nogit"
    }
}
