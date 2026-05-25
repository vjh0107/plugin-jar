package kr.junhyung.pluginjar.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class NestedLibraryExtractor {

    public static final String LIBRARIES_DIR = "BOOT-INF/lib";
    public static final String LIBRARIES_PATH = "/" + LIBRARIES_DIR;

    private static final String CACHE_DIR = ".pluginjar-cache";
    private static final String TEMP_DIR_PREFIX = ".extract-";

    private NestedLibraryExtractor() {
    }

    public static List<Path> extract(Path jarPath) throws IOException {
        Path cacheDir = jarPath.toAbsolutePath().getParent()
                .resolve(CACHE_DIR)
                .resolve(jarPath.getFileName().toString());
        String hash = computeHash(jarPath);
        Path hashDir = cacheDir.resolve(hash);

        if (Files.isDirectory(hashDir)) {
            cleanStaleCacheEntries(cacheDir, hash);
            return listJars(hashDir);
        }

        Files.createDirectories(cacheDir);
        Path tempDir = Files.createTempDirectory(cacheDir, TEMP_DIR_PREFIX);
        try {
            extractLibraries(jarPath, tempDir);
            atomicMoveDirectory(tempDir, hashDir);
        } catch (IOException e) {
            deleteDirectoryQuietly(tempDir);
            throw e;
        }

        cleanStaleCacheEntries(cacheDir, hash);
        return listJars(hashDir);
    }

    private static void atomicMoveDirectory(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        } catch (FileAlreadyExistsException | DirectoryNotEmptyException e) {
            deleteDirectoryQuietly(source);
        }
    }

    private static void extractLibraries(Path jarPath, Path targetDir) throws IOException {
        try (FileSystem jarFs = openJarFileSystem(jarPath)) {
            Path librariesDir = jarFs.getPath(LIBRARIES_PATH);
            if (!Files.exists(librariesDir)) {
                return;
            }
            try (Stream<Path> entries = Files.walk(librariesDir, 1)) {
                for (Path entry : entries.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".jar"))
                        .toList()) {
                    Files.copy(entry, targetDir.resolve(entry.getFileName().toString()));
                }
            }
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

    private static String computeHash(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 not available", e);
        }
        byte[] buffer = new byte[8192];
        try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
            while (in.read(buffer) != -1) {
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static List<Path> listJars(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }

    private static void cleanStaleCacheEntries(Path cacheDir, String currentHash) throws IOException {
        try (Stream<Path> entries = Files.list(cacheDir)) {
            for (Path entry : entries.filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().equals(currentHash))
                    .filter(p -> !p.getFileName().toString().startsWith(TEMP_DIR_PREFIX))
                    .toList()) {
                deleteDirectory(entry);
            }
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void deleteDirectoryQuietly(Path directory) {
        try {
            deleteDirectory(directory);
        } catch (IOException ignored) {
        }
    }
}
