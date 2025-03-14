/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javafx.application.Platform
 *  javafx.beans.value.ChangeListener
 *  javafx.beans.value.ObservableValue
 *  javafx.concurrent.Worker$State
 *  javafx.embed.swing.JFXPanel
 *  javafx.scene.Group
 *  javafx.scene.Parent
 *  javafx.scene.Scene
 *  javafx.scene.web.WebEngine
 *  javafx.scene.web.WebView
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package net.minecraft.launcher.ui.tabs.website;

import java.awt.Component;
import java.awt.Dimension;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import net.minecraft.launcher.ui.tabs.website.Browser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

public class JFXBrowser
implements Browser {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Object lock = new Object();
    private final JFXPanel fxPanel = new JFXPanel();
    private String urlToBrowseTo;
    private Dimension size;
    private WebView browser;
    private WebEngine webEngine;

    public JFXBrowser() {
        Platform.runLater((Runnable)new Runnable(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void run() {
                Object object;
                Group root = new Group();
                Scene scene = new Scene((Parent)root);
                JFXBrowser.this.fxPanel.setScene(scene);
                Object object2 = object = JFXBrowser.this.lock;
                synchronized (object2) {
                    JFXBrowser.this.browser = new WebView();
                    JFXBrowser.this.browser.setContextMenuEnabled(false);
                    if (JFXBrowser.this.size != null) {
                        JFXBrowser.this.resize(JFXBrowser.this.size);
                    }
                    JFXBrowser.this.webEngine = JFXBrowser.this.browser.getEngine();
                    JFXBrowser.this.webEngine.setJavaScriptEnabled(false);
                    JFXBrowser.this.webEngine.getLoadWorker().stateProperty().addListener((ChangeListener)new ChangeListener<Worker.State>(){

                        public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State oldState, Worker.State newState) {
                            if (newState == Worker.State.SUCCEEDED) {
                                EventListener listener = new EventListener(){

                                    @Override
                                    public void handleEvent(Event event) {
                                    }
                                };
                                Document doc = JFXBrowser.this.webEngine.getDocument();
                                if (doc != null) {
                                    NodeList elements = doc.getElementsByTagName("a");
                                    for (int i = 0; i < elements.getLength(); ++i) {
                                        Node item = elements.item(i);
                                        if (!(item instanceof EventTarget)) continue;
                                        ((EventTarget)((Object)item)).addEventListener("click", listener, false);
                                    }
                                }
                            }
                        }
                    });
                    if (JFXBrowser.this.urlToBrowseTo != null) {
                        JFXBrowser.this.loadUrl(JFXBrowser.this.urlToBrowseTo);
                    }
                }
                root.getChildren().add(JFXBrowser.this.browser);
            }
        });
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void loadUrl(final String url) {
        Object object;
        Object object2 = object = this.lock;
        synchronized (object2) {
            this.urlToBrowseTo = url;
            if (this.webEngine != null) {
                Platform.runLater((Runnable)new Runnable(){

                    @Override
                    public void run() {
                        JFXBrowser.this.webEngine.load(url);
                    }
                });
            }
        }
    }

    @Override
    public Component getComponent() {
        return this.fxPanel;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void resize(Dimension size) {
        Object object;
        Object object2 = object = this.lock;
        synchronized (object2) {
            this.size = size;
            if (this.browser != null) {
                this.browser.setMinSize(size.getWidth(), size.getHeight());
                this.browser.setMaxSize(size.getWidth(), size.getHeight());
                this.browser.setPrefSize(size.getWidth(), size.getHeight());
            }
        }
    }
}

