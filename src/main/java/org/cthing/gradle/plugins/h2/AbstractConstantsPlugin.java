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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;


/**
 * Base class for the H2 constants plugins.
 */
public abstract class AbstractConstantsPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        // Create a task that generates constants from the database for the main source set.
        final SourceSet sourceSet = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
                                           .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final String taskName = sourceSet.getTaskName("generate", getTaskTarget());
        final Provider<Directory> taskOutputDirectory =
                project.getLayout().getBuildDirectory().dir("generated-src/h2-constants/" + sourceSet.getName());

        final TaskProvider<? extends AbstractConstantsTask> h2TaskProvider =
                project.getTasks().register(taskName, getTaskClass(), h2Task -> {
                    h2Task.setDescription(String.format("Generates constants for %s H2 database",
                                                        sourceSet.getName()));

                    // Configure the output directory based on the source set name.
                    h2Task.getOutputDirectory().set(taskOutputDirectory);
                });

        // Include the generated source file(s) in the Java compilation.
        project.getTasks()
               .named(sourceSet.getCompileJavaTaskName())
               .configure(compileTask -> compileTask.dependsOn(h2TaskProvider));

        sourceSet.getJava().srcDir(taskOutputDirectory);
    }

    /**
     * Provides the class of the constants task.
     *
     * @return Class of constants task.
     */
    protected abstract Class<? extends AbstractConstantsTask> getTaskClass();

    /**
     * Provides the target portion of the preconfigured per source set constants task name.
     *
     * @return Name for the per source set constant tasks.
     */
    protected abstract String getTaskTarget();
}
