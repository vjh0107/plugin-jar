package kr.junhyung.pluginjar.core;

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
import java.util.ArrayList;
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

        List<Path> extracted = LibraryExtractor.extractToTempDirectory(testJarPath, null);

        assertTrue(extracted.isEmpty());
    }

    @Test
    @DisplayName("라이브러리 디렉토리의 모든 JAR을 추출한다")
    void extractsAllJars() throws Exception {
        List<String> libraryNames = List.of("lib1.jar", "lib2.jar", "lib3.jar");
        createJarWithLibraries(testJarPath, libraryNames);

        List<Path> extracted = LibraryExtractor.extractToTempDirectory(testJarPath, null);

        assertEquals(3, extracted.size());
        for (Path path : extracted) {
            assertTrue(path.toString().endsWith(".jar"));
        }
    }

    @Test
    @DisplayName("추출된 각 라이브러리에 대해 consumer를 호출한다")
    void invokesConsumerForEachLibrary() throws Exception {
        List<String> libraryNames = List.of("lib1.jar", "lib2.jar");
        createJarWithLibraries(testJarPath, libraryNames);

        List<Path> consumedPaths = new ArrayList<>();

        LibraryExtractor.extractToTempDirectory(testJarPath, consumedPaths::add);

        assertEquals(2, consumedPaths.size());
        for (Path path : consumedPaths) {
            assertTrue(Files.exists(path));
            assertTrue(path.toString().endsWith(".jar"));
        }
    }

    @Test
    @DisplayName("JAR이 아닌 파일은 무시한다")
    void ignoresNonJarFiles() throws Exception {
        createJarWithMixedFiles(testJarPath);

        List<Path> extracted = LibraryExtractor.extractToTempDirectory(testJarPath, null);

        assertEquals(1, extracted.size());
        assertTrue(extracted.getFirst().toString().endsWith(".jar"));
    }

    @Test
    @DisplayName("존재하지 않는 JAR 경로에 대해 예외를 던진다")
    void throwsExceptionForInvalidJarPath() {
        Path nonExistentJar = tempDir.resolve("non-existent.jar");

        assertThrows(IllegalStateException.class, () -> LibraryExtractor.extractToTempDirectory(nonExistentJar, null));
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
            Path libDir = fs.getPath(LibraryExtractor.LIBRARIES_PATH);
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
            Path libDir = fs.getPath(LibraryExtractor.LIBRARIES_PATH);
            Files.createDirectories(libDir);

            Files.write(libDir.resolve("actual-lib.jar"), new byte[]{0x50, 0x4B, 0x03, 0x04});
            Files.writeString(libDir.resolve("readme.txt"), "Not a jar file");
            Files.writeString(libDir.resolve("config.xml"), "<config/>");
        }
    }
}
