/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.h2;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.file.PathUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class PluginIntegTest {

    private Path projectDir;

    public static Stream<Arguments> gradleVersionProvider() {
        return Stream.of(
                arguments("8.3"),
                arguments(GradleVersion.current().getVersion())
        );
    }

    @BeforeEach
    public void setup() throws IOException {
        final Path baseDir = Path.of(System.getProperty("buildDir"), "integTest");
        Files.createDirectories(baseDir);
        this.projectDir = Files.createTempDirectory(baseDir, null);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testNoSchema(final String gradleVersion) throws IOException {
        copyProject("no-schema");

        final BuildResult result = createGradleRunner(gradleVersion, "generateH2SqlConstants").build();
        verifyBuild(result, NO_SOURCE, "generateH2SqlConstants");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testDefaults(final String gradleVersion) throws IOException {
        copyProject("use-defaults");

        final BuildResult result = createGradleRunner(gradleVersion, "generateH2SqlConstants").build();
        verifyBuild(result, SUCCESS, "generateH2SqlConstants");

        final Class<?> cls1 = loadClass("org.cthing.test.DBConstants");
        assertThat(cls1).isPublic().isFinal();
        verifyConstant(cls1, "TBL_COMMON_TABLE1", "COMMON.TABLE1", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_COMMON_TABLE2", "COMMON.TABLE2", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_COMMON_TABLE3", "COMMON.TABLE3", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_COMMON_TABLE4", "COMMON.TABLE4", SourceAccess.PUBLIC);

        final Class<?> cls2 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE1");
        verifyConstant(cls2, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL", "VAL", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL_SIZE", 128, SourceAccess.PUBLIC);

        final Class<?> cls3 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE2");
        verifyConstant(cls3, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG", "MSG", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG_SIZE", 40, SourceAccess.PUBLIC);

        final Class<?> cls4 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE3");
        verifyConstant(cls4, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR", "STR", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR_SIZE", 20, SourceAccess.PUBLIC);

        final Class<?> cls5 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE4");
        verifyConstant(cls5, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE", "TITLE", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE_SIZE", 60, SourceAccess.PUBLIC);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testPackagePrivate(final String gradleVersion) throws IOException {
        copyProject("package-private");

        final BuildResult result = createGradleRunner(gradleVersion, "generateH2SqlConstants").build();
        verifyBuild(result, SUCCESS, "generateH2SqlConstants");

        final Class<?> cls1 = loadClass("org.cthing.test.DBConstants");
        assertThat(cls1).isPackagePrivate().isFinal();
        verifyConstant(cls1, "TBL_COMMON_TABLE1", "COMMON.TABLE1", SourceAccess.PACKAGE);
        verifyConstant(cls1, "TBL_COMMON_TABLE2", "COMMON.TABLE2", SourceAccess.PACKAGE);
        verifyConstant(cls1, "TBL_COMMON_TABLE3", "COMMON.TABLE3", SourceAccess.PACKAGE);
        verifyConstant(cls1, "TBL_COMMON_TABLE4", "COMMON.TABLE4", SourceAccess.PACKAGE);

        final Class<?> cls2 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE1");
        verifyConstant(cls2, "SCHEMA", "COMMON", SourceAccess.PACKAGE);
        verifyConstant(cls2, "COL_ID", "ID", SourceAccess.PACKAGE);
        verifyConstant(cls2, "COL_ID_NULLABLE", false, SourceAccess.PACKAGE);
        verifyConstant(cls2, "COL_VAL", "VAL", SourceAccess.PACKAGE);
        verifyConstant(cls2, "COL_VAL_NULLABLE", true, SourceAccess.PACKAGE);
        verifyConstant(cls2, "COL_VAL_SIZE", 128, SourceAccess.PACKAGE);

        final Class<?> cls3 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE2");
        verifyConstant(cls3, "SCHEMA", "COMMON", SourceAccess.PACKAGE);
        verifyConstant(cls3, "COL_ID", "ID", SourceAccess.PACKAGE);
        verifyConstant(cls3, "COL_ID_NULLABLE", false, SourceAccess.PACKAGE);
        verifyConstant(cls3, "COL_MSG", "MSG", SourceAccess.PACKAGE);
        verifyConstant(cls3, "COL_MSG_NULLABLE", true, SourceAccess.PACKAGE);
        verifyConstant(cls3, "COL_MSG_SIZE", 40, SourceAccess.PACKAGE);

        final Class<?> cls4 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE3");
        verifyConstant(cls4, "SCHEMA", "COMMON", SourceAccess.PACKAGE);
        verifyConstant(cls4, "COL_ID", "ID", SourceAccess.PACKAGE);
        verifyConstant(cls4, "COL_ID_NULLABLE", false, SourceAccess.PACKAGE);
        verifyConstant(cls4, "COL_STR", "STR", SourceAccess.PACKAGE);
        verifyConstant(cls4, "COL_STR_NULLABLE", true, SourceAccess.PACKAGE);
        verifyConstant(cls4, "COL_STR_SIZE", 20, SourceAccess.PACKAGE);

        final Class<?> cls5 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE4");
        verifyConstant(cls5, "SCHEMA", "COMMON", SourceAccess.PACKAGE);
        verifyConstant(cls5, "COL_ID", "ID", SourceAccess.PACKAGE);
        verifyConstant(cls5, "COL_ID_NULLABLE", false, SourceAccess.PACKAGE);
        verifyConstant(cls5, "COL_TITLE", "TITLE", SourceAccess.PACKAGE);
        verifyConstant(cls5, "COL_TITLE_NULLABLE", true, SourceAccess.PACKAGE);
        verifyConstant(cls5, "COL_TITLE_SIZE", 60, SourceAccess.PACKAGE);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testNoSchemaPrefix(final String gradleVersion) throws IOException {
        copyProject("no-schema-prefix");

        final BuildResult result = createGradleRunner(gradleVersion, "generateH2SqlConstants").build();
        verifyBuild(result, SUCCESS, "generateH2SqlConstants");

        final Class<?> cls1 = loadClass("org.cthing.test.DBConstants");
        assertThat(cls1).isPublic().isFinal();
        verifyConstant(cls1, "TBL_TABLE1", "TABLE1", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_TABLE2", "TABLE2", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_TABLE3", "TABLE3", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_TABLE4", "TABLE4", SourceAccess.PUBLIC);

        final Class<?> cls2 = loadClass("org.cthing.test.DBConstants$TABLE1");
        verifyConstant(cls2, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL", "VAL", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL_SIZE", 128, SourceAccess.PUBLIC);

        final Class<?> cls3 = loadClass("org.cthing.test.DBConstants$TABLE2");
        verifyConstant(cls3, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG", "MSG", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG_SIZE", 40, SourceAccess.PUBLIC);

        final Class<?> cls4 = loadClass("org.cthing.test.DBConstants$TABLE3");
        verifyConstant(cls4, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR", "STR", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR_SIZE", 20, SourceAccess.PUBLIC);

        final Class<?> cls5 = loadClass("org.cthing.test.DBConstants$TABLE4");
        verifyConstant(cls5, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE", "TITLE", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE_SIZE", 60, SourceAccess.PUBLIC);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testFilePerSchema(final String gradleVersion) throws IOException {
        copyProject("file-per-schema");

        final BuildResult result = createGradleRunner(gradleVersion, "generateH2SqlConstants").build();
        verifyBuild(result, SUCCESS, "generateH2SqlConstants", "DBConstantsCommon", "DBConstantsOther");

        final Class<?> cls1 = loadClass("org.cthing.test.DBConstantsCommon");
        assertThat(cls1).isPublic().isFinal();
        verifyConstant(cls1, "TBL_COMMON_TABLE1", "COMMON.TABLE1", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_COMMON_TABLE2", "COMMON.TABLE2", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_COMMON_TABLE3", "COMMON.TABLE3", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_COMMON_TABLE4", "COMMON.TABLE4", SourceAccess.PUBLIC);

        final Class<?> cls2 = loadClass("org.cthing.test.DBConstantsCommon$COMMON_TABLE1");
        verifyConstant(cls2, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL", "VAL", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL_SIZE", 128, SourceAccess.PUBLIC);

        final Class<?> cls3 = loadClass("org.cthing.test.DBConstantsCommon$COMMON_TABLE2");
        verifyConstant(cls3, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG", "MSG", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG_SIZE", 40, SourceAccess.PUBLIC);

        final Class<?> cls4 = loadClass("org.cthing.test.DBConstantsCommon$COMMON_TABLE3");
        verifyConstant(cls4, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR", "STR", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR_SIZE", 20, SourceAccess.PUBLIC);

        final Class<?> cls5 = loadClass("org.cthing.test.DBConstantsCommon$COMMON_TABLE4");
        verifyConstant(cls5, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE", "TITLE", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE_SIZE", 60, SourceAccess.PUBLIC);

        final Class<?> cls6 = loadClass("org.cthing.test.DBConstantsOther");
        assertThat(cls6).isPublic().isFinal();
        verifyConstant(cls6, "TBL_OTHER_TABLE1", "OTHER.TABLE1", SourceAccess.PUBLIC);
        verifyConstant(cls6, "TBL_OTHER_TABLE2", "OTHER.TABLE2", SourceAccess.PUBLIC);

        final Class<?> cls7 = loadClass("org.cthing.test.DBConstantsOther$OTHER_TABLE1");
        verifyConstant(cls7, "SCHEMA", "OTHER", SourceAccess.PUBLIC);
        verifyConstant(cls7, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls7, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls7, "COL_VAL", "VAL", SourceAccess.PUBLIC);
        verifyConstant(cls7, "COL_VAL_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls7, "COL_VAL_SIZE", 128, SourceAccess.PUBLIC);

        final Class<?> cls8 = loadClass("org.cthing.test.DBConstantsOther$OTHER_TABLE2");
        verifyConstant(cls8, "SCHEMA", "OTHER", SourceAccess.PUBLIC);
        verifyConstant(cls8, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls8, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls8, "COL_MSG", "MSG", SourceAccess.PUBLIC);
        verifyConstant(cls8, "COL_MSG_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls8, "COL_MSG_SIZE", 40, SourceAccess.PUBLIC);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testAllSchema(final String gradleVersion) throws IOException {
        copyProject("all-schema");

        final BuildResult result = createGradleRunner(gradleVersion, "generateH2SqlConstants").build();
        verifyBuild(result, SUCCESS, "generateH2SqlConstants");

        final Class<?> cls = loadClass("org.cthing.test.DBConstants");
        assertThat(cls).isPublic().isFinal();
        verifyConstant(cls, "TBL_INFORMATION_SCHEMA_TABLES", "INFORMATION_SCHEMA.TABLES", SourceAccess.PUBLIC);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testSizesOnly(final String gradleVersion) throws IOException {
        copyProject("sizes-only");

        final BuildResult result = createGradleRunner(gradleVersion, "generateH2SqlConstants").build();
        verifyBuild(result, SUCCESS, "generateH2SqlConstants");

        final Class<?> cls1 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE1");
        assertThat(cls1).isPublic().isFinal();
        verifyConstant(cls1, "COL_VAL_SIZE", 128, SourceAccess.PUBLIC);

        final Class<?> cls2 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE2");
        verifyConstant(cls2, "COL_MSG_SIZE", 40, SourceAccess.PUBLIC);

        final Class<?> cls3 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE3");
        verifyConstant(cls3, "COL_STR_SIZE", 20, SourceAccess.PUBLIC);

        final Class<?> cls4 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE4");
        verifyConstant(cls4, "COL_TITLE_SIZE", 60, SourceAccess.PUBLIC);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testNoSchemaPrefixSizesOnly(final String gradleVersion) throws IOException {
        copyProject("no-schema-prefix-sizes-only");

        final BuildResult result = createGradleRunner(gradleVersion, "generateH2SqlConstants").build();
        verifyBuild(result, SUCCESS, "generateH2SqlConstants");

        final Class<?> cls1 = loadClass("org.cthing.test.DBConstants$TABLE1");
        assertThat(cls1).isPublic().isFinal();
        verifyConstant(cls1, "COL_VAL_SIZE", 128, SourceAccess.PUBLIC);

        final Class<?> cls2 = loadClass("org.cthing.test.DBConstants$TABLE2");
        verifyConstant(cls2, "COL_MSG_SIZE", 40, SourceAccess.PUBLIC);

        final Class<?> cls3 = loadClass("org.cthing.test.DBConstants$TABLE3");
        verifyConstant(cls3, "COL_STR_SIZE", 20, SourceAccess.PUBLIC);

        final Class<?> cls4 = loadClass("org.cthing.test.DBConstants$TABLE4");
        verifyConstant(cls4, "COL_TITLE_SIZE", 60, SourceAccess.PUBLIC);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testFlyway(final String gradleVersion) throws IOException {
        copyProject("flyway");

        final BuildResult result = createGradleRunner(gradleVersion, "generateH2FlywayConstants").build();
        verifyBuild(result, SUCCESS, "generateH2FlywayConstants");

        final Class<?> cls1 = loadClass("org.cthing.test.DBConstants");
        assertThat(cls1).isPublic().isFinal();
        verifyConstant(cls1, "TBL_COMMON_TABLE1", "COMMON.TABLE1", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_COMMON_TABLE2", "COMMON.TABLE2", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_COMMON_TABLE3", "COMMON.TABLE3", SourceAccess.PUBLIC);
        verifyConstant(cls1, "TBL_COMMON_TABLE4", "COMMON.TABLE4", SourceAccess.PUBLIC);

        final Class<?> cls2 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE1");
        verifyConstant(cls2, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL", "VAL", SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls2, "COL_VAL_SIZE", 128, SourceAccess.PUBLIC);

        final Class<?> cls3 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE2");
        verifyConstant(cls3, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG", "MSG", SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls3, "COL_MSG_SIZE", 40, SourceAccess.PUBLIC);

        final Class<?> cls4 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE3");
        verifyConstant(cls4, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR", "STR", SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls4, "COL_STR_SIZE", 20, SourceAccess.PUBLIC);

        final Class<?> cls5 = loadClass("org.cthing.test.DBConstants$COMMON_TABLE4");
        verifyConstant(cls5, "SCHEMA", "COMMON", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_ID", "ID", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_ID_NULLABLE", false, SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE", "TITLE", SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE_NULLABLE", true, SourceAccess.PUBLIC);
        verifyConstant(cls5, "COL_TITLE_SIZE", 60, SourceAccess.PUBLIC);
    }

    private void copyProject(final String projectName) throws IOException {
        final URL projectUrl = getClass().getResource("/" + projectName);
        assertThat(projectUrl).isNotNull();
        PathUtils.copyDirectory(Path.of(projectUrl.getPath()), this.projectDir);

        for (final String propName : List.of("schema1", "schema2", "schema3", "schema4")) {
            final URL propUrl = getClass().getResource("/" + propName + ".sql");
            assertThat(propUrl).isNotNull();
            PathUtils.copyFileToDirectory(propUrl, this.projectDir);
        }
    }

    private GradleRunner createGradleRunner(final String gradleVersion, final String taskName) {
        return GradleRunner.create()
                           .withProjectDir(this.projectDir.toFile())
                           .withArguments(taskName, "build")
                           .withPluginClasspath()
                           .withGradleVersion(gradleVersion);
    }

    private void verifyBuild(final BuildResult result, final TaskOutcome outcome, final String taskName,
                             final String... filenames) {
        final BuildTask genTask = result.task(":" + taskName);
        assertThat(genTask).isNotNull();
        assertThat(genTask.getOutcome()).as(result.getOutput()).isEqualTo(outcome);

        if (outcome != SUCCESS) {
            return;
        }

        final BuildTask buildTask = result.task(":build");
        assertThat(buildTask).isNotNull();
        assertThat(buildTask.getOutcome()).as(result.getOutput()).isEqualTo(SUCCESS);

        final String[] fnames = filenames.length == 0 ? new String[] { "DBConstants" } : filenames;
        for (final String filename : fnames) {
            final Path actualSource =
                    this.projectDir.resolve("build/generated-src/h2-constants/main/org/cthing/test/" + filename + ".java");
            assertThat(actualSource).isRegularFile();

            final Path expectedSource = this.projectDir.resolve(filename + ".java");
            assertThat(actualSource).hasSameTextualContentAs(expectedSource, StandardCharsets.UTF_8);

            final Path classFile =
                    this.projectDir.resolve("build/classes/java/main/org/cthing/test/" + filename + ".class");
            assertThat(classFile).isRegularFile();
        }
    }

    private void verifyConstant(final Class<?> cls, final String fieldName, final Object fieldValue,
                                final SourceAccess access) throws IOException {
        try {
            assertThat(cls).hasDeclaredFields(fieldName);

            final Field field = cls.getDeclaredField(fieldName);
            assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
            assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
            if (access == SourceAccess.PUBLIC) {
                assertThat(Modifier.isPublic(field.getModifiers())).isTrue();
            } else {
                assertThat(Modifier.isPublic(field.getModifiers())).isFalse();

                field.setAccessible(true);
            }
            if (fieldValue instanceof String) {
                assertThat((String)field.get(null)).isEqualTo(fieldValue);
            } else if (fieldValue instanceof Boolean) {
                assertThat(field.getBoolean(null)).isEqualTo(fieldValue);
            } else {
                assertThat(field.getInt(null)).isEqualTo(fieldValue);
            }
        } catch (final IllegalAccessException | NoSuchFieldException ex) {
            throw new IOException(ex);
        }
    }

    private Class<?> loadClass(final String classname) throws IOException {
        final Path classesDir = this.projectDir.resolve("build/classes/java/main");
        try (URLClassLoader loader = new URLClassLoader(new URL[] { classesDir.toUri().toURL() })) {
            return loader.loadClass(classname);
        } catch (final ClassNotFoundException ex) {
            throw new IOException(ex);
        }
    }
}
