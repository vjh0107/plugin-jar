package kr.junhyung.pluginjar.paper;

import org.junit.jupiter.api.AfterEach;
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

class LibraryExtractorTest {

    @TempDir
    Path tempDir;

    private Path testJarPath;

    @BeforeEach
    void setup() {
        testJarPath = tempDir.resolve("test-plugin.jar");
    }

    @AfterEach
    void teardown() throws IOException {
        if (testJarPath != null && Files.exists(testJarPath)) {
            Files.deleteIfExists(testJarPath);
        }
    }

    @Test
    @DisplayName("라이브러리 디렉토리가 없으면 빈 리스트를 반환한다")
    void returnsEmptyListWhenNoLibrariesDir() throws Exception {
        createEmptyJar(testJarPath);

        List<Path> extracted = kr.junhyung.pluginjar.core.LibraryExtractor.extractToTempDirectory(testJarPath);
        assertEquals(0, extracted.size());
    }

    @Test
    @DisplayName("라이브러리 디렉토리의 모든 JAR을 추출한다")
    void extractsAllJars() throws Exception {
        createJarWithLibraries(testJarPath, List.of("lib1.jar", "lib2.jar", "lib3.jar"));

        List<Path> paths = kr.junhyung.pluginjar.core.LibraryExtractor.extractToTempDirectory(testJarPath);
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

        List<Path> paths = kr.junhyung.pluginjar.core.LibraryExtractor.extractToTempDirectory(testJarPath);
        assertEquals(1, paths.size());
        assertTrue(paths.getFirst().toString().endsWith(".jar"));
    }

    @Test
    @DisplayName("존재하지 않는 JAR 경로에 대해 예외를 던진다")
    void throwsExceptionForInvalidJarPath() {
        Path nonExistentJar = tempDir.resolve("non-existent.jar");

        assertThrows(IOException.class, () -> kr.junhyung.pluginjar.core.LibraryExtractor.extractToTempDirectory(nonExistentJar));
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
        URI jarUri = URI.create("jar:" + jarPath.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of("create", "true"))) {
            Path libDir = fs.getPath(kr.junhyung.pluginjar.core.LibraryExtractor.LIBRARIES_PATH);
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
            Path libDir = fs.getPath(kr.junhyung.pluginjar.core.LibraryExtractor.LIBRARIES_PATH);
            Files.createDirectories(libDir);

            Files.write(libDir.resolve("actual-lib.jar"), new byte[]{0x50, 0x4B, 0x03, 0x04});
            Files.writeString(libDir.resolve("readme.txt"), "Not a jar file");
            Files.writeString(libDir.resolve("config.xml"), "<config/>");
        }
    }
}
