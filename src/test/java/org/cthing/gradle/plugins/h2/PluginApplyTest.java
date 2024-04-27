/*
 * Copyright 2024 C Thing Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cthing.gradle.plugins.h2;

import java.io.File;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class PluginApplyTest {

    @Test
    public void testApplySql(@TempDir final File projectDir) {
        final Project project = ProjectBuilder.builder().withName("testProject").withProjectDir(projectDir).build();
        project.getPluginManager().apply("org.cthing.h2-sql-constants");

        final Task mainTask = project.getTasks().findByName("generateH2SqlConstants");
        assertThat(mainTask).isNotNull().isInstanceOf(H2SqlConstantsTask.class);

        final H2SqlConstantsTask task = (H2SqlConstantsTask)mainTask;
        assertThat(task.getGroup()).isEqualTo("Generate Constants");
        assertThat(task.getOutputDirectory().get().getAsFile().getPath()).endsWith("build/generated-src/h2-constants/main");
        assertThat(task.getClassname().isPresent()).isFalse();
        assertThat(task.getSizesOnly().get()).isFalse();
        assertThat(task.getPrefixWithSchema().get()).isTrue();
        assertThat(task.getFilePerSchema().get()).isFalse();
        assertThat(task.getSourceAccess().get()).isEqualTo(SourceAccess.PUBLIC);
    }

    @Test
    public void testApplyFlyway(@TempDir final File projectDir) {
        final Project project = ProjectBuilder.builder().withName("testProject").withProjectDir(projectDir).build();
        project.getPluginManager().apply("org.cthing.h2-flyway-constants");

        final Task mainTask = project.getTasks().findByName("generateH2FlywayConstants");
        assertThat(mainTask).isNotNull().isInstanceOf(H2FlywayConstantsTask.class);

        final H2FlywayConstantsTask task = (H2FlywayConstantsTask)mainTask;
        assertThat(task.getGroup()).isEqualTo("Generate Constants");
        assertThat(task.getOutputDirectory().get().getAsFile().getPath()).endsWith("build/generated-src/h2-constants/main");
        assertThat(task.getClassname().isPresent()).isFalse();
        assertThat(task.getSizesOnly().get()).isFalse();
        assertThat(task.getPrefixWithSchema().get()).isTrue();
        assertThat(task.getFilePerSchema().get()).isFalse();
        assertThat(task.getSourceAccess().get()).isEqualTo(SourceAccess.PUBLIC);
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
        assertThat(AbstractConstantsTask.toCamelCase(original)).isEqualTo(expected);
    }
}
