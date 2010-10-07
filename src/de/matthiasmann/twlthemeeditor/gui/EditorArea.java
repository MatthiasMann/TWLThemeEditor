/*
 * Copyright (c) 2008-2010, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.CallbackWithReason;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.MenuAction;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.model.PersistentMRUListModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.DelayedAction;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.Split;
import de.matthiasmann.twlthemeeditor.datamodel.Images;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;
import java.io.File;
import java.net.MalformedURLException;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public final class EditorArea extends Widget {

    public enum Layout {
        SPLIT_HV,
        SPLIT_HHV
    }

    private static final String KEY_RECENT_CLASSPATHS = "recentClasspaths";
    private static final String KEY_CLASSPATH_FS = "userWidgetJARs";
    
    private final MessageLog messageLog;
    private final TestWidgetManager testWidgetManager;
    private final Menu testWidgetMenu;
    private final ThemeTreePane themeTreePane;
    private final PreviewWidget previewWidget;
    private final TextureViewerPane textureViewerPane;
    private final ScrollPane propertiesScrollPane;
    private final WidgetTreeModel widgetTreeModel;
    private final WidgetTree widgetTree;
    private final WidgetPropertyEditor widgetPropertyEditor;

    private final Preferences prefs;
    private final PersistentMRUListModel<String> recentClasspathsModel;
    private final Menu classpathsMenu;

    private final CallbackWithReason<ThemeTreeModel.CallbackReason> modelChangedCB;
    private final Runnable boundPropertyCB;

    private DelayedAction updatePropertyEditors;
    private Context ctx;
    private RectProperty boundRectProperty;
    private ColorProperty boundColorProperty;
    private SplitProperty boundSplitXProperty;
    private SplitProperty boundSplitYProperty;
    private Layout layout = Layout.SPLIT_HV;

    public EditorArea(MessageLog messageLog) {
        this.messageLog = messageLog;
        
        testWidgetManager = new TestWidgetManager(messageLog);
        testWidgetMenu = new Menu("Widgets");
        themeTreePane = new ThemeTreePane(messageLog);
        themeTreePane.setTheme("/themetreepane");
        previewWidget = new PreviewWidget(messageLog);
        previewWidget.setTheme("/previewwidget");
        textureViewerPane = new TextureViewerPane();
        textureViewerPane.setTheme("/textureviewerpane");
        propertiesScrollPane = new ScrollPane();
        propertiesScrollPane.setTheme("/propertyEditor");
        propertiesScrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);
        widgetTreeModel = new WidgetTreeModel();
        widgetTree = new WidgetTree(widgetTreeModel, previewWidget);
        widgetTree.setTheme("/widgetTree");
        widgetPropertyEditor = new WidgetPropertyEditor();
        widgetPropertyEditor.setTheme("/propertyEditor");

        prefs = Preferences.userNodeForPackage(EditorArea.class);
        recentClasspathsModel = new PersistentMRUListModel<String>(5, String.class, prefs, KEY_RECENT_CLASSPATHS);
        classpathsMenu = new Menu("User widgets");
        
        previewWidget.setCallback(new PreviewWidget.Callback() {
            public void testWidgetChanged(Widget widget) {
                updateTestWidget(widget);
            }
            public void errorLocationChanged(Object errorLocation) {
                updateErrorLocation(errorLocation);
            }
        });

        modelChangedCB = new CallbackWithReason<ThemeTreeModel.CallbackReason>() {
            public void callback(ThemeTreeModel.CallbackReason reason) {
                reloadTheme();
                if(reason == ThemeTreeModel.CallbackReason.STRUCTURE_CHANGED) {
                    updatePropertyEditors.run();
                }
            }
        };

        boundPropertyCB = new Runnable() {
            public void run() {
                updateTextureViewerPane();
            }
        };

        widgetTree.addSelectionChangeListener(new Runnable() {
            public void run() {
                updateTestWidgetProperties();
            }
        });

        testWidgetManager.setCallback(new Runnable() {
            public void run() {
                changeTestWidget();
            }
        });

        textureViewerPane.setListener(new TextureViewerPane.Listener() {
            public void dragEdgeTop(int y) {
                if(boundRectProperty != null) {
                    Dimension limit = boundRectProperty.getLimit();
                    Rect rect = boundRectProperty.getPropertyValue();
                    rect.set(
                        rect.getX(),
                        limit(y, 0, Math.min(limit.getY(), rect.getBottom())-1),
                        rect.getRight(),
                        rect.getBottom());
                    boundRectProperty.setPropertyValue(rect);
                }
            }
            public void dragEdgeBottom(int y) {
                if(boundRectProperty != null) {
                    Dimension limit = boundRectProperty.getLimit();
                    Rect rect = boundRectProperty.getPropertyValue();
                    rect.set(
                        rect.getX(),
                        rect.getY(),
                        rect.getRight(),
                        limit(y, Math.max(0, rect.getY())+1, limit.getY()));
                    boundRectProperty.setPropertyValue(rect);
                }
            }
            public void dragEdgeLeft(int x) {
                if(boundRectProperty != null) {
                    Dimension limit = boundRectProperty.getLimit();
                    Rect rect = boundRectProperty.getPropertyValue();
                    rect.set(
                        limit(x, 0, Math.min(limit.getX(), rect.getRight())-1),
                        rect.getY(),
                        rect.getRight(),
                        rect.getBottom());
                    boundRectProperty.setPropertyValue(rect);
                }
            }
            public void dragEdgeRight(int x) {
                if(boundRectProperty != null) {
                    Dimension limit = boundRectProperty.getLimit();
                    Rect rect = boundRectProperty.getPropertyValue();
                    rect.set(
                        rect.getX(),
                        rect.getY(),
                        limit(x, Math.max(0, rect.getX())+1, limit.getX()),
                        rect.getBottom());
                    boundRectProperty.setPropertyValue(rect);
                }
            }
            public void dragSplitX(int idx, int x) {
                dragSplit(idx, x, boundSplitXProperty, true);
            }
            public void dragSplitY(int idx, int y) {
                dragSplit(idx, y, boundSplitYProperty, false);
            }
        });

        recreateLayout();
        updateRecentClasspathsMenu();
        updateTestWidgetMenu();
        changeTestWidget();
    }

    public void setModel(ThemeTreeModel model) {
        if(ctx != null) {
            ctx.getThemeTreeModel().removeCallbacks(modelChangedCB);
        }

        if(model == null) {
            ctx = null;
            previewWidget.setURL(null, null);
        } else {
            ctx = new Context(messageLog, model);
            ctx.setThemeTreePane(themeTreePane);

            model.addCallback(modelChangedCB);

            try {
                previewWidget.setURL(ctx, model.getRootThemeFile().getVirtualURL());
            } catch(MalformedURLException ex) {
                previewWidget.setURL(null, null);
            }
        }
        
        textureViewerPane.setUrl(null);
        propertiesScrollPane.setContent(null);
        themeTreePane.setModel(model);
        widgetPropertyEditor.setContext(ctx);
    }

    public boolean setLayout(Layout layout) {
        if(this.layout != layout) {
            this.layout = layout;
            recreateLayout();
            return true;
        }
        return false;
    }

    public void addMenus(Menu menu) {
        menu.add(testWidgetMenu);
    }

    public void addSettingsMenuItems(Menu settingsMenu) {
        textureViewerPane.addSettingsMenuItems(settingsMenu);
    }

    public void reloadTheme() {
        ctx.getThemeTreeModel().setErrorLocation(null);
        previewWidget.reloadTheme();
    }
    
    void recreateLayout() {
        removeAllChildren();
        removeFromParent(themeTreePane);
        removeFromParent(previewWidget);
        removeFromParent(textureViewerPane);
        removeFromParent(propertiesScrollPane);
        removeFromParent(widgetPropertyEditor);
        removeFromParent(widgetTree);

        switch(layout) {
            case SPLIT_HV: {
                SplitPane spH1 = new SplitPane();
                SplitPane spH2 = new SplitPane();
                SplitPane spV1 = new SplitPane();
                SplitPane spV2 = new SplitPane();
                SplitPane spV3 = new SplitPane();
                spH1.setSplitPosition(300);
                spH1.add(spV1);
                spH1.add(spH2);
                spH2.setReverseSplitPosition(true);
                spH2.setSplitPosition(300);
                spH2.add(spV2);
                spH2.add(spV3);
                spV1.setDirection(SplitPane.Direction.VERTICAL);
                spV1.add(themeTreePane);
                spV1.add(propertiesScrollPane);
                spV2.setDirection(SplitPane.Direction.VERTICAL);
                spV2.add(textureViewerPane);
                spV2.add(previewWidget);
                spV3.setDirection(SplitPane.Direction.VERTICAL);
                spV3.add(widgetTree);
                spV3.add(widgetPropertyEditor);
                add(spH1);
                break;
            }

            case SPLIT_HHV: {
                SplitPane spH1 = new SplitPane();
                SplitPane spH2 = new SplitPane();
                SplitPane spH3 = new SplitPane();
                SplitPane spV1 = new SplitPane();
                SplitPane spV2 = new SplitPane();
                spH1.setSplitPosition(300);
                spH1.add(themeTreePane);
                spH1.add(spH2);
                spH2.setSplitPosition(300);
                spH2.add(propertiesScrollPane);
                spH2.add(spH3);
                spH3.setReverseSplitPosition(true);
                spH3.setSplitPosition(300);
                spH3.add(spV1);
                spH3.add(spV2);
                spV1.setDirection(SplitPane.Direction.VERTICAL);
                spV1.add(textureViewerPane);
                spV1.add(previewWidget);
                spV2.setDirection(SplitPane.Direction.VERTICAL);
                spV2.add(widgetTree);
                spV2.add(widgetPropertyEditor);
                add(spH1);
                break;
            }
        }
    }

    void updateProperties() {
        if(boundRectProperty != null) {
            boundRectProperty.removeValueChangedCallback(boundPropertyCB);
            boundRectProperty = null;
        }
        if(boundColorProperty != null) {
            boundColorProperty.removeCallback(boundPropertyCB);
            boundColorProperty = null;
        }
        if(boundSplitXProperty != null) {
            boundSplitXProperty.removeCallback(boundPropertyCB);
            boundSplitXProperty = null;
        }
        if(boundSplitYProperty != null) {
            boundSplitYProperty.removeCallback(boundPropertyCB);
            boundSplitYProperty = null;
        }
        
        Object obj = themeTreePane.getSelected();
        if(obj != null) {
            Images textures = getTextures(obj);
            try {
                textureViewerPane.setUrl((textures != null) ? textures.getTextureURL() : null);
            } catch(MalformedURLException ignored) {
                textureViewerPane.setUrl(null);
            }
            if(obj instanceof HasProperties) {
                Property<?>[] properties = ((HasProperties)obj).getProperties();
                PropertyPanel propertyPanel = new PropertyPanel(ctx, properties);
                propertiesScrollPane.setContent(propertyPanel);
                for(Property<?> property : properties) {
                    if(boundRectProperty == null && (property instanceof RectProperty)) {
                        boundRectProperty = (RectProperty)property;
                        boundRectProperty.addValueChangedCallback(boundPropertyCB);
                    }
                    if(boundColorProperty == null && (property instanceof ColorProperty)) {
                        boundColorProperty = (ColorProperty)property;
                        boundColorProperty.addValueChangedCallback(boundPropertyCB);
                    }
                    if(property instanceof SplitProperty) {
                        if(boundSplitXProperty == null && property.getName().startsWith("Split X")) {
                            boundSplitXProperty = (SplitProperty)property;
                            boundSplitXProperty.addValueChangedCallback(boundPropertyCB);
                        }
                        if(boundSplitYProperty == null && property.getName().startsWith("Split Y")) {
                            boundSplitYProperty = (SplitProperty)property;
                            boundSplitYProperty.addValueChangedCallback(boundPropertyCB);
                        }
                    }
                }
            }
        } else {
            propertiesScrollPane.setContent(null);
        }
        updateTextureViewerPane();
        textureViewerPane.scrollToRect();
    }

    void updateTextureViewerPane() {
        if(boundRectProperty != null) {
            textureViewerPane.setRect(boundRectProperty.getPropertyValue());
        } else {
            de.matthiasmann.twl.renderer.Image renderImage = null;
            Object obj = themeTreePane.getSelected();
            if(obj instanceof Image) {
                Image image = (Image)obj;
                String name = image.getName();
                while(name == null && (image.getParent() instanceof Image)) {
                    image = (Image)image.getParent();
                    name = image.getName();
                }
                if(name != null) {
                    renderImage = previewWidget.getImage(name);
                }
            }
            if(renderImage != null) {
                textureViewerPane.setImage(renderImage);
            } else {
                textureViewerPane.setRect(null);
            }
        }
        Color color = (boundColorProperty != null) ? boundColorProperty.getPropertyValue() : Color.WHITE;
        textureViewerPane.setTintColor((color != null) ? color : Color.WHITE);
        textureViewerPane.setSplitPositionsX(getSplitPos(boundSplitXProperty));
        textureViewerPane.setSplitPositionsY(getSplitPos(boundSplitYProperty));
    }

    private static int[] getSplitPos(SplitProperty splitProperty) {
        if(splitProperty != null) {
            Split split = splitProperty.getPropertyValue();
            if(split != null) {
                int size = splitProperty.getLimit();
                return new int[] {
                    split.getPoint1().convertToPX(size),
                    split.getPoint2().convertToPX(size)
                };
            }
        }
        return null;
    }

    static int limit(int what, int min, int max) {
        return Math.max(min, Math.min(what, max));
    }
    
    void dragSplit(int idx, int pos, SplitProperty splitProperty, boolean horz) {
        Split split = splitProperty.getPropertyValue();
        if(split != null) {
            int size = splitProperty.getLimit();
            Split.Point p1 = split.getPoint1();
            Split.Point p2 = split.getPoint2();
            switch (idx) {
                case 0:
                    pos = limit(pos, 0, p2.convertToPX(size));
                    p1 = p1.movePX(pos - p1.convertToPX(size));
                    break;
                case 1:
                    pos = limit(pos, p1.convertToPX(size), size);
                    p2 = p2.movePX(pos - p2.convertToPX(size));
                    break;
            }
            splitProperty.setPropertyValue(new Split(p1, p2));
        }
    }
    
    void updateErrorLocation(Object errorLocation) {
        if(ctx != null) {
            if(errorLocation instanceof ThemeTreeNode) {
                ctx.getThemeTreeModel().setErrorLocation((ThemeTreeNode)errorLocation);
            } else {
                ctx.getThemeTreeModel().setErrorLocation(null);
            }
        }
    }

    void updateTestWidget(Widget testWidget) {
        widgetTree.setRootWidget(ctx, testWidget);
        widgetPropertyEditor.setWidget(testWidget);
    }

    void updateTestWidgetProperties() {
        Widget widget = widgetTree.getSelectedWidget();
        widgetPropertyEditor.setWidget(widget);
    }
    
    void changeTestWidget() {
        previewWidget.setWidgetFactory(testWidgetManager.getCurrentTestWidgetFactory());
    }

    void recreateTestWidgets() {
        testWidgetManager.clearCache();
        changeTestWidget();
    }

    void loadClasspath() {
        LoadFileSelector lfs = new LoadFileSelector(this, prefs, KEY_CLASSPATH_FS,
                "TWL Theme Editor classpath file", ".classpath", new LoadFileSelector.Callback() {
            public void fileSelected(File file) {
                loadClasspath(file);
            }
            public void canceled() {
            }
        });
        lfs.openPopup();
    }

    void createClasspath() {
        NewClasspathDialog ncd = new NewClasspathDialog(this, new NewClasspathDialog.Callback() {
            public void classpathCreated(File file) {
                loadClasspath(file);
            }
        });
        ncd.openPopup();
    }

    void loadClasspath(File file) {
        if(testWidgetManager.loadUserWidgets(file)) {
            updateTestWidgetMenu();
            recentClasspathsModel.addEntry(file.toString());
            updateRecentClasspathsMenu();
        } else {
            int idx = Utils.find(recentClasspathsModel, file.toString());
            if(idx >= 0) {
                recentClasspathsModel.removeEntry(idx);
                updateRecentClasspathsMenu();
            }
        }
    }

    void updateRecentClasspathsMenu() {
        MenuAction maLoadClasspath = new MenuAction("Load classpath...", new Runnable() {
            public void run() {
                loadClasspath();
            }
        });
        maLoadClasspath.setTooltipContent("Load Widgets from an existing classpath file");

        MenuAction maCreateClasspath = new MenuAction("New classpath...", new Runnable() {
            public void run() {
                createClasspath();
            }
        });
        maCreateClasspath.setTooltipContent("Create a new classpath file containing your Widgets");

        classpathsMenu.clear();
        classpathsMenu.add(maLoadClasspath);
        classpathsMenu.add(maCreateClasspath);
        int numEntries = recentClasspathsModel.getNumEntries();
        if(numEntries > 0) {
            classpathsMenu.addSpacer();
            for(int i=0 ; i<numEntries ; i++) {
                final String entry = recentClasspathsModel.getEntry(i);
                classpathsMenu.add(entry, new Runnable() {
                    public void run() {
                        loadClasspath(new File(entry));
                    }
                });
            }
        }
    }

    void updateTestWidgetMenu() {
        MenuAction maRecreateTestWidgets = new MenuAction("Recreate widgets", new Runnable() {
            public void run() {
                recreateTestWidgets();
            }
        });
        maRecreateTestWidgets.setTooltipContent("Clears widget cache and recreates current widget");

        testWidgetMenu.clear();
        testWidgetManager.updateMenu(testWidgetMenu);
        testWidgetMenu.addSpacer();
        testWidgetMenu.add(maRecreateTestWidgets);
        testWidgetMenu.add(classpathsMenu);
    }
    
    private void removeFromParent(Widget w) {
        if(w.getParent() != null) {
            w.getParent().removeChild(w);
        }
    }

    private Images getTextures(Object node) {
        if(node instanceof Images) {
            return (Images)node;
        } else if(node instanceof Image) {
            return ((Image)node).getTextures();
        } else {
            return null;
        }
    }

    @Override
    protected void layout() {
        layoutChildrenFullInnerArea();
    }

    @Override
    protected void afterAddToGUI(GUI gui) {
        super.afterAddToGUI(gui);

        updatePropertyEditors = new DelayedAction(gui, new Runnable() {
            public void run() {
                updateProperties();
            }
        });
        themeTreePane.addCallback(updatePropertyEditors);
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        super.beforeRemoveFromGUI(gui);
        
        themeTreePane.removeCallback(updatePropertyEditors);
        updatePropertyEditors = null;
    }

    public class LayoutModel extends HasCallback implements BooleanModel {
        private final Layout layout;

        public LayoutModel(Layout layout) {
            this.layout = layout;
        }

        public boolean getValue() {
            return EditorArea.this.layout == layout;
        }

        public void setValue(boolean value) {
            if(setLayout(layout)) {
                doCallback();
            }
        }
    }
}
