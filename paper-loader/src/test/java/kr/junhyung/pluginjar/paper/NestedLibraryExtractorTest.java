package kr.junhyung.pluginjar.paper;

import kr.junhyung.pluginjar.core.NestedLibraryExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NestedLibraryExtractorTest {

    @TempDir
    Path tempDir;

    private Path testJarPath;

    @BeforeEach
    void setup() {
        testJarPath = tempDir.resolve("test-plugin.jar");
    }

    @Test
    @DisplayName("라이브러리 디렉토리가 없으면 빈 리스트를 반환한다")
    void returnsEmptyListWhenNoLibrariesDir() throws Exception {
        createEmptyJar(testJarPath);

        List<Path> extracted = NestedLibraryExtractor.extract(testJarPath);
        assertEquals(0, extracted.size());
    }

    @Test
    @DisplayName("라이브러리 디렉토리의 모든 JAR을 추출한다")
    void extractsAllJars() throws Exception {
        createJarWithLibraries(testJarPath, List.of("lib1.jar", "lib2.jar", "lib3.jar"));

        List<Path> paths = NestedLibraryExtractor.extract(testJarPath);
        assertEquals(3, paths.size());
        for (Path path : paths) {
            assertTrue(Files.exists(path), "extracted file should exist: " + path);
            assertTrue(path.toString().endsWith(".jar"));
        }
    }

    @Test
    @DisplayName("JAR이 아닌 파일은 무시한다")
    void ignoresNonJarFiles() throws Exception {
        createJarWithMixedFiles(testJarPath);

        List<Path> paths = NestedLibraryExtractor.extract(testJarPath);
        assertEquals(1, paths.size());
        assertTrue(paths.getFirst().toString().endsWith(".jar"));
    }

    @Test
    @DisplayName("존재하지 않는 JAR 경로에 대해 예외를 던진다")
    void throwsExceptionForInvalidJarPath() {
        Path nonExistentJar = tempDir.resolve("non-existent.jar");

        assertThrows(IOException.class, () -> NestedLibraryExtractor.extract(nonExistentJar));
    }

    @Test
    @DisplayName("두 번째 호출은 캐시에서 반환한다")
    void secondCallReturnsCached() throws Exception {
        createJarWithLibraries(testJarPath, List.of("lib1.jar", "lib2.jar"));

        List<Path> first = NestedLibraryExtractor.extract(testJarPath);
        List<Path> second = NestedLibraryExtractor.extract(testJarPath);

        assertEquals(first, second);
    }

    @Test
    @DisplayName("JAR이 변경되면 새로 추출하고 이전 캐시를 정리한다")
    void reExtractsWhenJarChanges() throws Exception {
        createJarWithLibraries(testJarPath, List.of("lib1.jar"));
        NestedLibraryExtractor.extract(testJarPath);

        createJarWithLibraries(testJarPath, List.of("lib1.jar", "lib2.jar"));
        List<Path> paths = NestedLibraryExtractor.extract(testJarPath);

        assertEquals(2, paths.size());

        Path cacheDir = tempDir.resolve(".pluginjar-cache").resolve(testJarPath.getFileName().toString());
        try (var entries = Files.list(cacheDir)) {
            long hashDirs = entries.filter(Files::isDirectory).count();
            assertEquals(1, hashDirs, "stale cache entry should be cleaned up");
        }
    }

    private void createEmptyJar(Path jarPath) throws IOException {
        URI jarUri = URI.create("jar:" + jarPath.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of("create", "true"))) {
            Path manifest = fs.getPath("/META-INF/MANIFEST.MF");
            Files.createDirectories(manifest.getParent());
            Files.writeString(manifest, "Manifest-Version: 1.0\n");
        }
    }

    private void createJarWithLibraries(Path jarPath, List<String> libraryNames) throws IOException {
        Files.deleteIfExists(jarPath);
        URI jarUri = URI.create("jar:" + jarPath.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of("create", "true"))) {
            Path libDir = fs.getPath(NestedLibraryExtractor.LIBRARIES_PATH);
            Files.createDirectories(libDir);

            for (String libName : libraryNames) {
                Path libPath = libDir.resolve(libName);
                Files.write(libPath, new byte[]{0x50, 0x4B, 0x03, 0x04});
            }
        }
    }

    private void createJarWithMixedFiles(Path jarPath) throws IOException {
        URI jarUri = URI.create("jar:" + jarPath.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of("create", "true"))) {
            Path libDir = fs.getPath(NestedLibraryExtractor.LIBRARIES_PATH);
            Files.createDirectories(libDir);

            Files.write(libDir.resolve("actual-lib.jar"), new byte[]{0x50, 0x4B, 0x03, 0x04});
            Files.writeString(libDir.resolve("readme.txt"), "Not a jar file");
            Files.writeString(libDir.resolve("config.xml"), "<config/>");
        }
    }
}
