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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class LibraryExtractor {

    public static final String LIBRARIES_DIR = "BOOT-INF/lib";
    public static final String LIBRARIES_PATH = "/" + LIBRARIES_DIR;

    private static final String TEMP_DIR_PREFIX = "pluginjar-libs-";
    private static final String CLEANUP_THREAD_NAME = "pluginjar-TempCleanup";

    private LibraryExtractor() {
    }

    public static List<Path> extractToTempDirectory(Path jarPath, Consumer<Path> libraryConsumer) {
        try (FileSystem jarFs = openJarFileSystem(jarPath)) {
            Path librariesDir = jarFs.getPath(LIBRARIES_PATH);

            if (!Files.exists(librariesDir)) {
                return List.of();
            }

            Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
            registerShutdownHook(tempDir);

            return extractLibraries(librariesDir, tempDir, libraryConsumer);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to extract libraries from " + jarPath, e);
        }
    }

    private static FileSystem openJarFileSystem(Path jarPath) throws IOException, URISyntaxException {
        URI jarUri = new URI("jar:" + jarPath.toUri());
        return FileSystems.newFileSystem(jarUri, Map.of());
    }

    private static void registerShutdownHook(Path tempDir) {
        Thread cleanupThread = new Thread(() -> deleteDirectoryRecursively(tempDir), CLEANUP_THREAD_NAME);
        Runtime.getRuntime().addShutdownHook(cleanupThread);
    }

    private static void deleteDirectoryRecursively(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            // Ignore cleanup failures
        }
    }

    private static List<Path> extractLibraries(Path librariesDir, Path tempDir, Consumer<Path> libraryConsumer)
            throws IOException {
        List<Path> jarFiles = findJarFiles(librariesDir);

        for (Path jarPath : jarFiles) {
            Path tempJar = extractToTemp(jarPath, tempDir);
            if (libraryConsumer != null) {
                libraryConsumer.accept(tempJar);
            }
        }

        return jarFiles.stream()
                .map(p -> tempDir.resolve(p.getFileName().toString()))
                .toList();
    }

    private static List<Path> findJarFiles(Path directory) throws IOException {
        try (var paths = Files.walk(directory, 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .toList();
        }
    }

    private static Path extractToTemp(Path jarPath, Path tempDir) {
        String fileName = jarPath.getFileName().toString();
        Path tempJar = tempDir.resolve(fileName);

        try {
            Files.copy(jarPath, tempJar, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract library: " + fileName, e);
        }

        return tempJar;
    }
}
