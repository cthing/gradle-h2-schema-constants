/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.h2;

import java.io.File;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cthing.assertj.gradle.GradleAssertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class PluginApplyTest {

    @Test
    public void testApplySql(@TempDir final File projectDir) {
        final Project project = ProjectBuilder.builder().withName("testProject").withProjectDir(projectDir).build();
        project.getPluginManager().apply("org.cthing.h2-sql-constants");

        assertThat(project).hasTaskSatisfying("generateH2SqlConstants", H2SqlConstantsTask.class, task -> {
            assertThat(task.getGroup()).isEqualTo("Generate Constants");
            assertThat(task.getOutputDirectory()).getString().endsWith("build/generated-src/h2-constants/main");
            assertThat(task.getClassname()).isEmpty();
            assertThat(task.getSizesOnly()).contains(false);
            assertThat(task.getPrefixWithSchema()).contains(true);
            assertThat(task.getFilePerSchema()).contains(false);
            assertThat(task.getSourceAccess()).contains(SourceAccess.PUBLIC);
        });
    }

    @Test
    public void testApplyFlyway(@TempDir final File projectDir) {
        final Project project = ProjectBuilder.builder().withName("testProject").withProjectDir(projectDir).build();
        project.getPluginManager().apply("org.cthing.h2-flyway-constants");

        assertThat(project).hasTaskSatisfying("generateH2FlywayConstants", H2FlywayConstantsTask.class, task -> {
            assertThat(task.getGroup()).isEqualTo("Generate Constants");
            assertThat(task.getOutputDirectory()).getString().endsWith("build/generated-src/h2-constants/main");
            assertThat(task.getClassname()).isEmpty();
            assertThat(task.getSizesOnly()).contains(false);
            assertThat(task.getPrefixWithSchema()).contains(true);
            assertThat(task.getFilePerSchema()).contains(false);
            assertThat(task.getSourceAccess()).contains(SourceAccess.PUBLIC);
        });
    }

    public static Stream<Arguments> camelCaseProvider() {
        return Stream.of(
                arguments("", ""),
                arguments("h", "H"),
                arguments("hello", "Hello"),
                arguments("Hello", "Hello"),
                arguments("HELLO", "Hello"),
                arguments("hello_world", "HelloWorld"),
                arguments("Hello_World", "HelloWorld"),
                arguments("hello-world", "HelloWorld"),
                arguments("hello.world", "HelloWorld"),
                arguments("hello__world", "HelloWorld"),
                arguments("HELLO_WORLD", "HelloWorld"),
                arguments("HelloWorld", "HelloWorld"),
                arguments("-", ""),
                arguments("_", "")
        );
    }

    @ParameterizedTest
    @MethodSource("camelCaseProvider")
    public void testToCamelCase(final String original, final String expected) {
        assertThat(ConstantsTaskBase.toCamelCase(original)).isEqualTo(expected);
    }
}
