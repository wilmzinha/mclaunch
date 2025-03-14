/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Objects
 *  com.google.common.base.Predicate
 *  com.google.common.base.Predicates
 *  com.google.common.collect.Lists
 */
package com.mojang.launcher.game.process;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.GameOutputLogProcessor;
import com.mojang.launcher.game.process.GameProcess;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameProcessBuilder {
    private final String processPath;
    private final List<String> arguments = Lists.newArrayList();
    private Predicate<String> sysOutFilter = Predicates.alwaysTrue();
    private GameOutputLogProcessor logProcessor = new GameOutputLogProcessor(){

        @Override
        public void onGameOutput(GameProcess process, String logLine) {
        }
    };
    private File directory;

    public GameProcessBuilder(String processPath) {
        if (processPath == null) {
            processPath = OperatingSystem.getCurrentPlatform().getJavaDir();
        }
        this.processPath = processPath;
    }

    public List<String> getFullCommands() {
        ArrayList<String> result = new ArrayList<String>(this.arguments);
        result.add(0, this.getProcessPath());
        return result;
    }

    public GameProcessBuilder withArguments(String ... commands) {
        this.arguments.addAll(Arrays.asList(commands));
        return this;
    }

    public List<String> getArguments() {
        return this.arguments;
    }

    public GameProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }

    public File getDirectory() {
        return this.directory;
    }

    public GameProcessBuilder withSysOutFilter(Predicate<String> predicate) {
        this.sysOutFilter = predicate;
        return this;
    }

    public GameProcessBuilder withLogProcessor(GameOutputLogProcessor logProcessor) {
        this.logProcessor = logProcessor;
        return this;
    }

    public Predicate<String> getSysOutFilter() {
        return this.sysOutFilter;
    }

    protected String getProcessPath() {
        return this.processPath;
    }

    public GameOutputLogProcessor getLogProcessor() {
        return this.logProcessor;
    }

    public String toString() {
        return Objects.toStringHelper((Object)this).add("processPath", (Object)this.processPath).add("arguments", this.arguments).add("sysOutFilter", this.sysOutFilter).add("directory", (Object)this.directory).add("logProcessor", (Object)this.logProcessor).toString();
    }
}

