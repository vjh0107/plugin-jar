package kr.junhyung.pluginjar.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public final class LibraryExtractor {

    public static final String LIBRARIES_DIR = "BOOT-INF/lib";
    public static final String LIBRARIES_PATH = "/" + LIBRARIES_DIR;

    private static final String TEMP_DIR_PREFIX = "pluginjar-libs-";
    private static final String CLEANUP_THREAD_NAME = "pluginjar-TempCleanup";

    private static final Set<Path> CLEANUP_PATHS = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);

    private LibraryExtractor() {
    }

    public static List<Path> extractToTempDirectory(Path jarPath) throws IOException {
        Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
        registerForCleanup(tempDir);

        try (FileSystem jarFs = openJarFileSystem(jarPath)) {
            Path librariesDir = jarFs.getPath(LIBRARIES_PATH);
            if (!Files.exists(librariesDir)) {
                return List.of();
            }
            List<Path> extracted = new ArrayList<>();
            try (Stream<Path> entries = Files.walk(librariesDir, 1)) {
                for (Path entry : entries.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".jar"))
                        .toList()) {
                    extracted.add(extractToTemp(entry, tempDir));
                }
            }
            return extracted;
        }
    }

    private static FileSystem openJarFileSystem(Path jarPath) throws IOException {
        try {
            URI jarUri = new URI("jar:" + jarPath.toUri());
            return FileSystems.newFileSystem(jarUri, Map.of());
        } catch (URISyntaxException e) {
            throw new IOException("Invalid jar URI for " + jarPath, e);
        }
    }

    private static Path extractToTemp(Path jarEntry, Path tempDir) throws IOException {
        String fileName = jarEntry.getFileName().toString();
        Path tempJar = tempDir.resolve(fileName);
        Files.copy(jarEntry, tempJar, StandardCopyOption.REPLACE_EXISTING);
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
        IOException exception = null;
        for (Path dir : CLEANUP_PATHS) {
            try {
                deleteDirectoryRecursively(dir);
            } catch (IOException e) {
                if (exception == null) exception = e;
                else exception.addSuppressed(e);
            }
        }
        if (exception != null) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
