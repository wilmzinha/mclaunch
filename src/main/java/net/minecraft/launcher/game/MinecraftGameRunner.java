/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Objects
 *  com.google.common.base.Predicate
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  org.apache.commons.io.Charsets
 *  org.apache.commons.io.FileUtils
 *  org.apache.commons.io.filefilter.FileFilterUtils
 *  org.apache.commons.io.filefilter.IOFileFilter
 *  org.apache.commons.io.filefilter.TrueFileFilter
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.commons.lang3.text.StrSubstitutor
 */
package net.minecraft.launcher.game;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.launcher.LegacyPropertyMapSerializer;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.game.GameInstanceStatus;
import com.mojang.launcher.game.process.GameProcess;
import com.mojang.launcher.game.process.GameProcessBuilder;
import com.mojang.launcher.game.process.GameProcessFactory;
import com.mojang.launcher.game.process.GameProcessRunnable;
import com.mojang.launcher.game.process.direct.DirectGameProcessFactory;
import com.mojang.launcher.game.runner.AbstractGameRunner;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.ExtractRules;
import com.mojang.util.UUIDTypeAdapter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.launcher.CompatibilityRule;
import net.minecraft.launcher.CurrentLaunchFeatureMatcher;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.profile.LauncherVisibilityRule;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.updater.ArgumentType;
import net.minecraft.launcher.updater.CompleteMinecraftVersion;
import net.minecraft.launcher.updater.Library;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

public class MinecraftGameRunner
extends AbstractGameRunner
implements GameProcessRunnable {
    private static final String CRASH_IDENTIFIER_MAGIC = "#@!@#";
    private final Gson gson = new Gson();
    private final DateTypeAdapter dateAdapter = new DateTypeAdapter();
    private final Launcher minecraftLauncher;
    private final String[] additionalLaunchArgs;
    private final GameProcessFactory processFactory = new DirectGameProcessFactory();
    private File nativeDir;
    private LauncherVisibilityRule visibilityRule = LauncherVisibilityRule.CLOSE_LAUNCHER;
    private UserAuthentication auth;
    private Profile selectedProfile;

    public MinecraftGameRunner(Launcher minecraftLauncher, String[] additionalLaunchArgs) {
        this.minecraftLauncher = minecraftLauncher;
        this.additionalLaunchArgs = additionalLaunchArgs;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void setStatus(GameInstanceStatus status) {
        Object object;
        Object object2 = object = this.lock;
        synchronized (object2) {
            if (this.nativeDir != null && status == GameInstanceStatus.IDLE) {
                LOGGER.info("Deleting " + this.nativeDir);
                if (!this.nativeDir.isDirectory() || FileUtils.deleteQuietly((File)this.nativeDir)) {
                    this.nativeDir = null;
                } else {
                    LOGGER.warn("Couldn't delete " + this.nativeDir + " - scheduling for deletion upon exit");
                    try {
                        FileUtils.forceDeleteOnExit((File)this.nativeDir);
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
            super.setStatus(status);
        }
    }

    @Override
    protected com.mojang.launcher.Launcher getLauncher() {
        return this.minecraftLauncher.getLauncher();
    }

    @Override
    protected void downloadRequiredFiles(VersionSyncInfo syncInfo) {
        this.migrateOldAssets();
        super.downloadRequiredFiles(syncInfo);
    }

    @Override
    protected void launchGame() throws IOException {
        File serverResourcePacksDir;
        File assetsDir;
        LOGGER.info("Launching game");
        this.selectedProfile = this.minecraftLauncher.getProfileManager().getSelectedProfile();
        this.auth = this.minecraftLauncher.getProfileManager().getAuthDatabase().getByUUID(this.minecraftLauncher.getProfileManager().getSelectedUser());
        if (this.getVersion() == null) {
            LOGGER.error("Aborting launch; version is null?");
            return;
        }
        this.nativeDir = new File(this.getLauncher().getWorkingDirectory(), "versions/" + this.getVersion().getId() + "/" + this.getVersion().getId() + "-natives-" + System.nanoTime());
        if (!this.nativeDir.isDirectory()) {
            this.nativeDir.mkdirs();
        }
        LOGGER.info("Unpacking natives to " + this.nativeDir);
        try {
            this.unpackNatives(this.nativeDir);
        }
        catch (IOException e) {
            LOGGER.error("Couldn't unpack natives!", (Throwable)e);
            return;
        }
        try {
            assetsDir = this.reconstructAssets();
        }
        catch (IOException e) {
            LOGGER.error("Couldn't unpack natives!", (Throwable)e);
            return;
        }
        File gameDirectory = this.selectedProfile.getGameDir() == null ? this.getLauncher().getWorkingDirectory() : this.selectedProfile.getGameDir();
        LOGGER.info("Launching in " + gameDirectory);
        if (!gameDirectory.exists()) {
            if (!gameDirectory.mkdirs()) {
                LOGGER.error("Aborting launch; couldn't create game directory");
                return;
            }
        } else if (!gameDirectory.isDirectory()) {
            LOGGER.error("Aborting launch; game directory is not actually a directory");
            return;
        }
        if (!(serverResourcePacksDir = new File(gameDirectory, "server-resource-packs")).exists()) {
            serverResourcePacksDir.mkdirs();
        }
        GameProcessBuilder processBuilder = new GameProcessBuilder((String)Objects.firstNonNull((Object)this.selectedProfile.getJavaPath(), (Object)OperatingSystem.getCurrentPlatform().getJavaDir()));
        processBuilder.withSysOutFilter(new Predicate<String>(){

            public boolean apply(String input) {
                return input.contains(MinecraftGameRunner.CRASH_IDENTIFIER_MAGIC);
            }
        });
        processBuilder.directory(gameDirectory);
        processBuilder.withLogProcessor(this.minecraftLauncher.getUserInterface().showGameOutputTab(this));
        String profileArgs = this.selectedProfile.getJavaArgs();
        if (profileArgs != null) {
            processBuilder.withArguments(profileArgs.split(" "));
        } else {
            boolean is32Bit = "32".equals(System.getProperty("sun.arch.data.model"));
            String defaultArgument = is32Bit ? "-Xmx512M -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:-UseAdaptiveSizePolicy -Xmn128M" : "-Xmx1G -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:-UseAdaptiveSizePolicy -Xmn128M";
            processBuilder.withArguments(defaultArgument.split(" "));
        }
        CompatibilityRule.FeatureMatcher featureMatcher = this.createFeatureMatcher();
        StrSubstitutor argumentsSubstitutor = this.createArgumentsSubstitutor(this.getVersion(), this.selectedProfile, gameDirectory, assetsDir, this.auth);
        this.getVersion().addArguments(ArgumentType.JVM, featureMatcher, processBuilder, argumentsSubstitutor);
        processBuilder.withArguments(this.getVersion().getMainClass());
        LOGGER.info("Half command: " + StringUtils.join(processBuilder.getFullCommands(), (String)" "));
        this.getVersion().addArguments(ArgumentType.GAME, featureMatcher, processBuilder, argumentsSubstitutor);
        Proxy proxy = this.getLauncher().getProxy();
        PasswordAuthentication proxyAuth = this.getLauncher().getProxyAuth();
        if (!proxy.equals(Proxy.NO_PROXY)) {
            InetSocketAddress address = (InetSocketAddress)proxy.address();
            processBuilder.withArguments("--proxyHost", address.getHostName());
            processBuilder.withArguments("--proxyPort", Integer.toString(address.getPort()));
            if (proxyAuth != null) {
                processBuilder.withArguments("--proxyUser", proxyAuth.getUserName());
                processBuilder.withArguments("--proxyPass", new String(proxyAuth.getPassword()));
            }
        }
        processBuilder.withArguments(this.additionalLaunchArgs);
        try {
            LOGGER.debug("Running " + StringUtils.join(processBuilder.getFullCommands(), (String)" "));
            GameProcess process = this.processFactory.startGame(processBuilder);
            process.setExitRunnable(this);
            this.setStatus(GameInstanceStatus.PLAYING);
            if (this.visibilityRule != LauncherVisibilityRule.DO_NOTHING) {
                this.minecraftLauncher.getUserInterface().setVisible(false);
            }
        }
        catch (IOException e) {
            LOGGER.error("Couldn't launch game", (Throwable)e);
            this.setStatus(GameInstanceStatus.IDLE);
            return;
        }
        this.minecraftLauncher.performCleanups();
    }

    protected CompleteMinecraftVersion getVersion() {
        return (CompleteMinecraftVersion)this.version;
    }

    private AssetIndex getAssetIndex() throws IOException {
        String assetVersion = this.getVersion().getAssetIndex().getId();
        File indexFile = new File(new File(this.getAssetsDir(), "indexes"), assetVersion + ".json");
        return (AssetIndex)this.gson.fromJson(FileUtils.readFileToString((File)indexFile, (Charset)Charsets.UTF_8), AssetIndex.class);
    }

    private File getAssetsDir() {
        return new File(this.getLauncher().getWorkingDirectory(), "assets");
    }

    private File reconstructAssets() throws IOException {
        File assetsDir = this.getAssetsDir();
        File indexDir = new File(assetsDir, "indexes");
        File objectDir = new File(assetsDir, "objects");
        String assetVersion = this.getVersion().getAssetIndex().getId();
        File indexFile = new File(indexDir, assetVersion + ".json");
        File virtualRoot = new File(new File(assetsDir, "virtual"), assetVersion);
        if (!indexFile.isFile()) {
            LOGGER.warn("No assets index file " + virtualRoot + "; can't reconstruct assets");
            return virtualRoot;
        }
        AssetIndex index = (AssetIndex)this.gson.fromJson(FileUtils.readFileToString((File)indexFile, (Charset)Charsets.UTF_8), AssetIndex.class);
        if (assetVersion.equals("legacy") || assetVersion.equals("pre-1.6") || index.isVirtual()) {
            LOGGER.info("Reconstructing virtual assets folder at " + virtualRoot);
            for (Map.Entry<String, AssetIndex.AssetObject> entry : index.getFileMap().entrySet()) {
                File target = new File(virtualRoot, entry.getKey());
                File original = new File(new File(objectDir, entry.getValue().getHash().substring(0, 2)), entry.getValue().getHash());
                if (target.isFile()) continue;
                FileUtils.copyFile((File)original, (File)target, (boolean)false);
            }
            FileUtils.writeStringToFile((File)new File(virtualRoot, ".lastused"), (String)this.dateAdapter.serializeToString(new Date()));
            File resourcesFolder = new File(this.getAssetsDir().getParentFile(), "resources");
            FileUtils.deleteDirectory((File)resourcesFolder);
            FileUtils.copyDirectory((File)virtualRoot, (File)resourcesFolder);
        }
        return virtualRoot;
    }

    public StrSubstitutor createArgumentsSubstitutor(CompleteMinecraftVersion version, Profile selectedProfile, File gameDirectory, File assetsDirectory, UserAuthentication authentication) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("auth_access_token", authentication.getAuthenticatedToken());
        map.put("user_properties", new GsonBuilder().registerTypeAdapter((Type)((Object)PropertyMap.class), (Object)new LegacyPropertyMapSerializer()).create().toJson((Object)authentication.getUserProperties()));
        map.put("user_property_map", new GsonBuilder().registerTypeAdapter((Type)((Object)PropertyMap.class), (Object)new PropertyMap.Serializer()).create().toJson((Object)authentication.getUserProperties()));
        if (authentication.isLoggedIn() && authentication.canPlayOnline()) {
            if (authentication instanceof YggdrasilUserAuthentication) {
                map.put("auth_session", String.format("token:%s:%s", authentication.getAuthenticatedToken(), UUIDTypeAdapter.fromUUID(authentication.getSelectedProfile().getId())));
            } else {
                map.put("auth_session", authentication.getAuthenticatedToken());
            }
        } else {
            map.put("auth_session", "-");
        }
        if (authentication.getSelectedProfile() != null) {
            map.put("auth_player_name", authentication.getSelectedProfile().getName());
            map.put("auth_uuid", UUIDTypeAdapter.fromUUID(authentication.getSelectedProfile().getId()));
            map.put("user_type", authentication.getUserType().getName());
        } else {
            String user = Launcher.getCurrentInstance().getProfileManager().getSelectedUser();
            if (user.startsWith("demo-")) {
                user = user.substring(user.indexOf("-") + 1);
            }
            map.put("auth_player_name", user);
            map.put("auth_uuid", UUIDTypeAdapter.fromUUID(UUID.nameUUIDFromBytes(("minecraft:" + user).getBytes())));
            map.put("user_type", UserType.LEGACY.getName());
        }
        map.put("profile_name", selectedProfile.getName());
        map.put("version_name", version.getId());
        map.put("game_directory", gameDirectory.getAbsolutePath());
        map.put("game_assets", assetsDirectory.getAbsolutePath());
        map.put("assets_root", this.getAssetsDir().getAbsolutePath());
        map.put("assets_index_name", this.getVersion().getAssetIndex().getId());
        map.put("version_type", this.getVersion().getType().getName());
        if (selectedProfile.getResolution() != null) {
            map.put("resolution_width", String.valueOf(selectedProfile.getResolution().getWidth()));
            map.put("resolution_height", String.valueOf(selectedProfile.getResolution().getHeight()));
        } else {
            map.put("resolution_width", "");
            map.put("resolution_height", "");
        }
        map.put("language", "en-us");
        try {
            AssetIndex assetIndex = this.getAssetIndex();
            for (Map.Entry<String, AssetIndex.AssetObject> entry : assetIndex.getFileMap().entrySet()) {
                String hash = entry.getValue().getHash();
                String path = new File(new File(this.getAssetsDir(), "objects"), hash.substring(0, 2) + "/" + hash).getAbsolutePath();
                map.put("asset=" + entry.getKey(), path);
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        map.put("launcher_name", "java-minecraft-launcher");
        map.put("launcher_version", LauncherConstants.getVersionName());
        map.put("natives_directory", this.nativeDir.getAbsolutePath());
        map.put("classpath", this.constructClassPath(this.getVersion()));
        map.put("classpath_separator", System.getProperty("path.separator"));
        map.put("primary_jar", new File(this.getLauncher().getWorkingDirectory(), "versions/" + this.getVersion().getJar() + "/" + this.getVersion().getJar() + ".jar").getAbsolutePath());
        return new StrSubstitutor(map);
    }

    private void migrateOldAssets() {
        File sourceDir = this.getAssetsDir();
        File objectsDir = new File(sourceDir, "objects");
        if (!sourceDir.isDirectory()) {
            return;
        }
        IOFileFilter migratableFilter = FileFilterUtils.notFileFilter((IOFileFilter)FileFilterUtils.or((IOFileFilter[])new IOFileFilter[]{FileFilterUtils.nameFileFilter((String)"indexes"), FileFilterUtils.nameFileFilter((String)"objects"), FileFilterUtils.nameFileFilter((String)"virtual"), FileFilterUtils.nameFileFilter((String)"skins")}));
        for (File file : FileUtils.listFiles(sourceDir, TrueFileFilter.TRUE, migratableFilter)) {
            String hash = Downloadable.getDigest(file, "SHA-1", 40);
            File destinationFile = new File(objectsDir, hash.substring(0, 2) + "/" + hash);
            if (!destinationFile.exists()) {
                LOGGER.info("Migrated old asset {} into {}", (Object)file, (Object)destinationFile);
                try {
                    FileUtils.copyFile((File)file, (File)destinationFile);
                }
                catch (IOException e) {
                    LOGGER.error("Couldn't migrate old asset", (Throwable)e);
                }
            }
            FileUtils.deleteQuietly((File)file);
        }
        File[] assets = sourceDir.listFiles();
        if (assets != null) {
            for (File file : assets) {
                if (file.getName().equals("indexes") || file.getName().equals("objects") || file.getName().equals("virtual") || file.getName().equals("skins")) continue;
                LOGGER.info("Cleaning up old assets directory {} after migration", (Object)file);
                FileUtils.deleteQuietly((File)file);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void unpackNatives(File targetDir) throws IOException {
        OperatingSystem os = OperatingSystem.getCurrentPlatform();
        Collection<Library> libraries = this.getVersion().getRelevantLibraries(this.createFeatureMatcher());
        for (Library library : libraries) {
            Map<OperatingSystem, String> nativesPerOs = library.getNatives();
            if (nativesPerOs == null || nativesPerOs.get((Object)os) == null) continue;
            File file = new File(this.getLauncher().getWorkingDirectory(), "libraries/" + library.getArtifactPath(nativesPerOs.get((Object)os)));
            ExtractRules extractRules = library.getExtractRules();
            try (ZipFile zip = new ZipFile(file);){
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (extractRules != null && !extractRules.shouldExtract(entry.getName())) continue;
                    File targetFile = new File(targetDir, entry.getName());
                    if (targetFile.getParentFile() != null) {
                        targetFile.getParentFile().mkdirs();
                    }
                    if (entry.isDirectory()) continue;
                    BufferedInputStream inputStream = new BufferedInputStream(zip.getInputStream(entry));
                    byte[] buffer = new byte[2048];
                    FileOutputStream outputStream = new FileOutputStream(targetFile);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                    try {
                        int length;
                        while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                            bufferedOutputStream.write(buffer, 0, length);
                        }
                    }
                    finally {
                        Downloadable.closeSilently(bufferedOutputStream);
                        Downloadable.closeSilently(outputStream);
                        Downloadable.closeSilently(inputStream);
                    }
                }
            }
        }
    }

    private CompatibilityRule.FeatureMatcher createFeatureMatcher() {
        return new CurrentLaunchFeatureMatcher(this.selectedProfile, this.getVersion(), this.minecraftLauncher.getProfileManager().getAuthDatabase().getByUUID(this.minecraftLauncher.getProfileManager().getSelectedUser()));
    }

    private String constructClassPath(CompleteMinecraftVersion version) {
        StringBuilder result = new StringBuilder();
        Collection<File> classPath = version.getClassPath(OperatingSystem.getCurrentPlatform(), this.getLauncher().getWorkingDirectory(), this.createFeatureMatcher());
        String separator = System.getProperty("path.separator");
        for (File file : classPath) {
            if (!file.isFile()) {
                throw new RuntimeException("Classpath file not found: " + file);
            }
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(file.getAbsolutePath());
        }
        return result.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void onGameProcessEnded(GameProcess process) {
        block10: {
            String errorText;
            block12: {
                int exitCode;
                block11: {
                    exitCode = process.getExitCode();
                    if (exitCode != 0) break block11;
                    LOGGER.info("Game ended with no troubles detected (exit code " + exitCode + ")");
                    if (this.visibilityRule == LauncherVisibilityRule.CLOSE_LAUNCHER) {
                        LOGGER.info("Following visibility rule and exiting launcher as the game has ended");
                        this.getLauncher().shutdownLauncher();
                    } else if (this.visibilityRule == LauncherVisibilityRule.HIDE_LAUNCHER) {
                        LOGGER.info("Following visibility rule and showing launcher as the game has ended");
                        this.minecraftLauncher.getUserInterface().setVisible(true);
                    }
                    break block10;
                }
                LOGGER.error("Game ended with bad state (exit code " + exitCode + ")");
                LOGGER.info("Ignoring visibility rule and showing launcher due to a game crash");
                this.minecraftLauncher.getUserInterface().setVisible(true);
                errorText = null;
                Collection<String> sysOutLines = process.getSysOutLines();
                String[] sysOut = sysOutLines.toArray(new String[sysOutLines.size()]);
                for (int i = sysOut.length - 1; i >= 0; --i) {
                    String line = sysOut[i];
                    int pos = line.lastIndexOf(CRASH_IDENTIFIER_MAGIC);
                    if (pos < 0 || pos >= line.length() - CRASH_IDENTIFIER_MAGIC.length() - 1) continue;
                    errorText = line.substring(pos + CRASH_IDENTIFIER_MAGIC.length()).trim();
                    break;
                }
                if (errorText == null) break block10;
                File file = new File(errorText);
                if (!file.isFile()) break block12;
                LOGGER.info("Crash report detected, opening: " + errorText);
                FileInputStream inputStream = null;
                try {
                    String line2;
                    inputStream = new FileInputStream(file);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder result = new StringBuilder();
                    while ((line2 = reader.readLine()) != null) {
                        if (result.length() > 0) {
                            result.append("\n");
                        }
                        result.append(line2);
                    }
                    reader.close();
                    this.minecraftLauncher.getUserInterface().showCrashReport(this.getVersion(), file, result.toString());
                }
                catch (IOException e) {
                    try {
                        LOGGER.error("Couldn't open crash report", (Throwable)e);
                    }
                    catch (Throwable throwable) {
                        Downloadable.closeSilently(inputStream);
                        throw throwable;
                    }
                    Downloadable.closeSilently(inputStream);
                    break block10;
                }
                Downloadable.closeSilently(inputStream);
                break block10;
            }
            LOGGER.error("Crash report detected, but unknown format: " + errorText);
        }
        this.setStatus(GameInstanceStatus.IDLE);
    }

    public void setVisibility(LauncherVisibilityRule visibility) {
        this.visibilityRule = visibility;
    }

    public UserAuthentication getAuth() {
        return this.auth;
    }

    public Profile getSelectedProfile() {
        return this.selectedProfile;
    }
}

