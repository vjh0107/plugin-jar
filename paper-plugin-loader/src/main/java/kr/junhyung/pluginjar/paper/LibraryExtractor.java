package kr.junhyung.pluginjar.paper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public final class LibraryExtractor {

    public static final String LIBRARIES_DIR = "BOOT-INF/lib";
    public static final String LIBRARIES_PATH = "/" + LIBRARIES_DIR;

    private static final Logger logger = LoggerFactory.getLogger(LibraryExtractor.class);
    private static final String TEMP_DIR_PREFIX = "pluginjar-libs-";
    private static final String CLEANUP_THREAD_NAME = "pluginjar-TempCleanup";

    private static final Set<Path> CLEANUP_PATHS = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);

    private LibraryExtractor() {
    }

    public static Stream<Path> extractToTempDirectory(Path jarPath) {
        FileSystem jarFs = openJarFileSystem(jarPath);
        Path librariesDir;
        try {
            librariesDir = jarFs.getPath(LIBRARIES_PATH);
            if (!Files.exists(librariesDir)) {
                closeQuietly(jarFs);
                return Stream.empty();
            }
        } catch (RuntimeException e) {
            closeQuietly(jarFs);
            throw e;
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
        } catch (IOException e) {
            closeQuietly(jarFs);
            throw new UncheckedIOException("Failed to create temp directory for libraries", e);
        }
        registerForCleanup(tempDir);

        Stream<Path> stream;
        try {
            stream = Files.walk(librariesDir, 1)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .map(jarEntry -> extractToTemp(jarEntry, tempDir));
        } catch (IOException e) {
            closeQuietly(jarFs);
            throw new UncheckedIOException("Failed to walk " + LIBRARIES_PATH + " in " + jarPath, e);
        }
        return stream.onClose(() -> closeQuietly(jarFs));
    }

    private static FileSystem openJarFileSystem(Path jarPath) {
        try {
            URI jarUri = new URI("jar:" + jarPath.toUri());
            return FileSystems.newFileSystem(jarUri, Map.of());
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to open jar file system for " + jarPath, e);
        }
    }

    private static Path extractToTemp(Path jarEntry, Path tempDir) {
        String fileName = jarEntry.getFileName().toString();
        Path tempJar = tempDir.resolve(fileName);
        try {
            Files.copy(jarEntry, tempJar, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract library: " + fileName, e);
        }
        return tempJar;
    }

    private static void registerForCleanup(Path tempDir) {
        CLEANUP_PATHS.add(tempDir);
        if (SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            Thread cleanupThread = new Thread(LibraryExtractor::cleanupAll, CLEANUP_THREAD_NAME);
            Runtime.getRuntime().addShutdownHook(cleanupThread);
        }
    }

    private static void cleanupAll() {
        for (Path dir : CLEANUP_PATHS) {
            deleteDirectoryRecursively(dir);
        }
    }

    private static void deleteDirectoryRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            logger.warn("Failed to clean up temporary directory {}: {}", directory, e.getMessage());
        }
    }

    private static void closeQuietly(FileSystem fs) {
        try {
            fs.close();
        } catch (IOException e) {
            logger.debug("Failed to close jar file system: {}", e.getMessage());
        }
    }
}
