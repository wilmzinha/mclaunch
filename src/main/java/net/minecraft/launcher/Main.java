/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  joptsimple.ArgumentAcceptingOptionSpec
 *  joptsimple.NonOptionArgumentSpec
 *  joptsimple.OptionParser
 *  joptsimple.OptionSet
 *  joptsimple.OptionSpec
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package net.minecraft.launcher;

import com.mojang.launcher.OperatingSystem;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.LauncherConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) {
        LOGGER.debug("main() called!");
        Main.startLauncher(args);
    }

    private static void startLauncher(String[] args) {
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        parser.accepts("winTen");
        ArgumentAcceptingOptionSpec<String> proxyHostOption = parser.accepts("proxyHost").withRequiredArg();
        ArgumentAcceptingOptionSpec<Integer> proxyPortOption = parser.accepts("proxyPort").withRequiredArg().defaultsTo("8080", new String[0]).ofType(Integer.class);
        ArgumentAcceptingOptionSpec<File> workDirOption = parser.accepts("workDir").withRequiredArg().ofType(File.class).defaultsTo(Main.getWorkingDirectory(), new File[0]);
        NonOptionArgumentSpec<String> nonOption = parser.nonOptions();
        OptionSet optionSet = parser.parse(args);
        List<String> leftoverArgs = optionSet.valuesOf(nonOption);
        String hostName = optionSet.valueOf(proxyHostOption);
        Proxy proxy = Proxy.NO_PROXY;
        if (hostName != null) {
            try {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostName, (int)((Integer)optionSet.valueOf((OptionSpec)proxyPortOption))));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        File workingDirectory = (File)optionSet.valueOf((OptionSpec)workDirOption);
        workingDirectory.mkdirs();
        LOGGER.debug("About to create JFrame.");
        Proxy finalProxy = proxy;
        JFrame frame = new JFrame();
        frame.setTitle("Minecraft Launcher " + LauncherConstants.getVersionName() + LauncherConstants.PROPERTIES.getEnvironment().getTitle());
        frame.setPreferredSize(new Dimension(900, 580));
        try {
            InputStream in = Launcher.class.getResourceAsStream("/favicon.png");
            if (in != null) {
                frame.setIconImage(ImageIO.read(in));
            }
        }
        catch (IOException in) {
            // empty catch block
        }
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        if (optionSet.has("winTen")) {
            System.setProperty("os.name", "Windows 10");
            System.setProperty("os.version", "10.0");
        }
        LOGGER.debug("Starting up launcher.");
        Launcher launcher = new Launcher(frame, workingDirectory, finalProxy, null, leftoverArgs.toArray(new String[leftoverArgs.size()]), 100);
        if (optionSet.has("winTen")) {
            launcher.setWinTenHack();
        }
        frame.setLocationRelativeTo(null);
        LOGGER.debug("End of main.");
    }

    public static File getWorkingDirectory() {
        File workingDirectory;
        String userHome = System.getProperty("user.home", ".");
        switch (OperatingSystem.getCurrentPlatform()) {
            case LINUX: {
                workingDirectory = new File(userHome, ".minecraft/");
                break;
            }
            case WINDOWS: {
                String applicationData = System.getenv("APPDATA");
                String folder = applicationData != null ? applicationData : userHome;
                workingDirectory = new File(folder, ".minecraft/");
                break;
            }
            case OSX: {
                workingDirectory = new File(userHome, "Library/Application Support/minecraft");
                break;
            }
            default: {
                workingDirectory = new File(userHome, "minecraft/");
            }
        }
        return workingDirectory;
    }
}

