/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.h2;

/**
 * Generates a Java class file containing constants representing the name of tables and columns in
 * an H2 SQL schema.
 */
public class H2SqlConstantsPlugin extends ConstantsPluginBase {

    @Override
    protected Class<? extends ConstantsTaskBase> getTaskClass() {
        return H2SqlConstantsTask.class;
    }

    @Override
    protected String getTaskTarget() {
        return "H2SqlConstants";
    }
}
