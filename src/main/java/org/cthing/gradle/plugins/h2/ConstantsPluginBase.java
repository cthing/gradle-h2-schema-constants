/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
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
public abstract class ConstantsPluginBase implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        // Create a task that generates constants from the database for the main source set.
        final SourceSet sourceSet = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
                                           .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final String taskName = sourceSet.getTaskName("generate", getTaskTarget());
        final Provider<Directory> taskOutputDirectory =
                project.getLayout().getBuildDirectory().dir("generated-src/h2-constants/" + sourceSet.getName());

        final TaskProvider<? extends ConstantsTaskBase> h2TaskProvider =
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
    protected abstract Class<? extends ConstantsTaskBase> getTaskClass();

    /**
     * Provides the target portion of the preconfigured per source set constants task name.
     *
     * @return Name for the per source set constant tasks.
     */
    protected abstract String getTaskTarget();
}
