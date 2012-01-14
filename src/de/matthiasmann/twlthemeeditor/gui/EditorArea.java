/*
 * Copyright (c) 2008-2012, Matthias Mann
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

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.MenuAction;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.Timer;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.EnumModel;
import de.matthiasmann.twl.model.FloatModel;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.OptionEnumModel;
import de.matthiasmann.twl.model.PersistentEnumModel;
import de.matthiasmann.twl.model.PersistentMRUListModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.renderer.AnimationState;
import de.matthiasmann.twl.renderer.Gradient;
import de.matthiasmann.twl.renderer.Gradient.Type;
import de.matthiasmann.twl.renderer.Texture;
import de.matthiasmann.twlthemeeditor.DelayedAction;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.Split;
import de.matthiasmann.twlthemeeditor.datamodel.Images;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;
import de.matthiasmann.twlthemeeditor.properties.EnumProperty;
import de.matthiasmann.twlthemeeditor.properties.GradientStopModel;
import de.matthiasmann.twlthemeeditor.properties.GradientStopModel.Stop;
import de.matthiasmann.twlthemeeditor.properties.GradientStopProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty.SplitIntegerModel;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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
    private static final String KEY_LAYOUT = "layout";
    
    private final MessageLog messageLog;
    private final ProgressDialog progressDialog;
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

    private final EnumModel<Layout> layoutModel;

    private DelayedAction modelChangedCB;
    private DelayedAction updatePropertyEditors;
    private Timer checkWidgetTreeTimer;
    private Context ctx;
    private URL textureURL;
    private Long currentSelectedID;
    private PropertyPanel propertyPanel;
    private RectProperty rectProperty;
    private ColorProperty colorProperty;
    private SplitProperty splitXProperty;
    private SplitProperty splitYProperty;
    private EnumProperty<Gradient.Type> gradientTypeProperty;
    private GradientStopProperty gradientStopProperty;

    public EditorArea(MessageLog messageLog) {
        this.messageLog = messageLog;

        progressDialog = new ProgressDialog(this);
        progressDialog.setTheme("/progressdialog");
        testWidgetManager = new TestWidgetManager(messageLog);
        testWidgetMenu = new Menu("Widgets");
        themeTreePane = new ThemeTreePane(messageLog, testWidgetManager);
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

        layoutModel = new PersistentEnumModel<Layout>(prefs, KEY_LAYOUT, Layout.SPLIT_HV);
        layoutModel.addCallback(new Runnable() {
            public void run() {
                recreateLayout();
            }
        });

        themeTreePane.setFocusNameFieldCB(new Runnable() {
            public void run() {
                focusNameField();
            }
        });

        previewWidget.setCallback(new PreviewWidget.Callback() {
            public void testWidgetChanged(Widget widget) {
                updateTestWidget(widget);
            }
            public void errorLocationChanged(Object errorLocation) {
                updateErrorLocation(errorLocation);
                updateTextureViewerPane();
            }
            public void testGUIChanged(GUI testGUI) {
                updateTestGUI(testGUI);
            }
            public void themeLoaded() {
                updateTextureViewerPane();
            }
        });

        widgetTree.addSelectionChangeListener(new Runnable() {
            public void run() {
                updateTestWidgetProperties();
            }
        });

        widgetTree.addReloadButtenCallback(new Runnable() {
            public void run() {
                reloadTestWidget();
            }
        });

        testWidgetManager.setProgressDialog(progressDialog);
        testWidgetManager.setCallback(new TestWidgetManager.Callback() {
            public void testWidgetChanged() {
                changeTestWidget();
            }
            public void newWidgetsLoaded() {
                updateTestWidgetMenu();
            }
        });

        textureViewerPane.setTextureLoadedListener(new TextureViewer.TextureLoadedListener() {
            public void textureLoaded(URL url, Texture texture) {
                EditorArea.this.textureLoaded(url, texture);
            }
        });

        recreateLayout();
        updateRecentClasspathsMenu();
        updateTestWidgetMenu();
        changeTestWidget();
    }

    private void removeModelChangedCB() {
        if(ctx != null && modelChangedCB != null) {
            ctx.getThemeTreeModel().removeCallbacks(modelChangedCB);
        }
    }
    
    public void setModel(ThemeTreeModel model) {
        removeModelChangedCB();
        if(model == null) {
            ctx = null;
            previewWidget.setURL(null, null);
        } else {
            ctx = new Context(messageLog, model);
            ctx.setThemeTreePane(themeTreePane);

            if(modelChangedCB != null) {
                model.addCallback(modelChangedCB);
            }

            try {
                previewWidget.setURL(ctx, model.getRootThemeFile().getVirtualURL());
            } catch(MalformedURLException ex) {
                previewWidget.setURL(null, null);
            }
        }
        
        textureViewerPane.setContext(ctx);
        textureViewerPane.setImage(null);
        propertiesScrollPane.setContent(null);
        themeTreePane.setModel(model);
        widgetPropertyEditor.setContext(ctx);
        widgetTree.setContext(ctx);
    }

    public EnumModel<Layout> getLayoutModel() {
        return layoutModel;
    }

    public BooleanModel getLayoutBooleanModel(Layout layout) {
        return new OptionEnumModel<Layout>(layoutModel, layout);
    }

    public void addMenus(Menu menu) {
        menu.add(testWidgetMenu);
    }

    public void addSettingsMenuItems(Menu settingsMenu) {
        textureViewerPane.addSettingsMenuItems(settingsMenu);
        previewWidget.addSettingsMenuItems(settingsMenu);
    }

    public void setDemoMode(boolean demoMode) {
        testWidgetManager.setDemoMode(demoMode);
    }

    public void reloadTheme() {
        ctx.getThemeTreeModel().setErrorLocation(null);
        previewWidget.reloadTheme();
    }
    
    public void undoGotoLastSelected() {
        if(ctx != null) {
            ThemeTreeModel model = ctx.getThemeTreeModel();
            Object state = model.getUndo().getUserState();
            if(state instanceof Long) {
                Long prevSelected = (Long)state;
                if(currentSelectedID == null || currentSelectedID.longValue() != prevSelected.longValue()) {
                    ThemeTreeNode node = model.findNode(prevSelected);
                    if(node != null) {
                        themeTreePane.selectNode(node);
                    }
                }
            }
        }
    }
    
    void recreateLayout() {
        removeAllChildren();
        removeFromParent(themeTreePane);
        removeFromParent(previewWidget);
        removeFromParent(textureViewerPane);
        removeFromParent(propertiesScrollPane);
        removeFromParent(widgetPropertyEditor);
        removeFromParent(widgetTree);

        switch(layoutModel.getValue()) {
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
        textureURL = null;
        rectProperty = null;
        colorProperty = null;
        splitXProperty = null;
        splitYProperty = null;
        gradientStopProperty = null;
        gradientTypeProperty = null;
        
        TreeTableNode obj = themeTreePane.getSelected();
        if(obj != null) {
            Images images = getImages(obj);
            try {
                textureURL = (images != null) ? images.getTextureURL() : null;
            } catch(MalformedURLException ignored) {
            }
        }
        
        if(obj instanceof HasProperties) {
            Property<?>[] properties = ((HasProperties)obj).getProperties();
            propertyPanel = new PropertyPanel(ctx, properties);
            for(Property<?> property : properties) {
                if(rectProperty == null && (property instanceof RectProperty)) {
                    rectProperty = (RectProperty)property;
                }
                if(colorProperty == null && (property instanceof ColorProperty)) {
                    colorProperty = (ColorProperty)property;
                }
                if(property instanceof SplitProperty) {
                    if(splitXProperty == null && property.getName().startsWith("Split X")) {
                        splitXProperty = (SplitProperty)property;
                    }
                    if(splitYProperty == null && property.getName().startsWith("Split Y")) {
                        splitYProperty = (SplitProperty)property;
                    }
                }
                if(gradientStopProperty == null && (property instanceof GradientStopProperty)) {
                    gradientStopProperty = (GradientStopProperty)property;
                }
                if(property instanceof EnumProperty<?>) {
                    EnumProperty<?> enumProperty = (EnumProperty<?>)property;
                    if(gradientTypeProperty == null && enumProperty.getType() == Gradient.Type.class) {
                        @SuppressWarnings("unchecked")
                        EnumProperty<Gradient.Type> gtp = (EnumProperty<Type>)enumProperty;
                        gradientTypeProperty = gtp;
                    }
                }
            }
        } else {
            propertyPanel = null;
        }

        currentSelectedID = null;
        if(ctx != null) {
            if(obj instanceof ThemeTreeNode) {
                currentSelectedID = ((ThemeTreeNode)obj).getDOMElement().getID();
            }
            ctx.getThemeTreeModel().getUndo().setUserState(currentSelectedID);
        }
        
        propertiesScrollPane.setContent(propertyPanel);
        updateTextureViewerPane();
        textureViewerPane.scrollToRect();
    }

    void focusNameField() {
        if(propertyPanel != null) {
            propertyPanel.focusWidget("Name");
        }
    }

    void checkWidgetTree() {
        if(ctx != null && ctx.checkLayoutValidated()) {
            widgetTreeModel.refreshTree();
        }
    }

    void updateTextureViewerPane() {
        if(rectProperty != null) {
            textureViewerPane.setImage(textureURL, rectProperty);
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
                if(gradientTypeProperty != null &&
                        (obj instanceof de.matthiasmann.twlthemeeditor.datamodel.images.Gradient)) {
                    switch(gradientTypeProperty.getPropertyValue()) {
                        case HORIZONTAL:
                            renderImage = new AdjustImageSize(renderImage, -1, 10);
                            break;
                        case VERTICAL:
                            renderImage = new AdjustImageSize(renderImage, 10, -1);
                            break;
                    }
                }
                textureViewerPane.setImage(renderImage);
            } else {
                textureViewerPane.setImage(null);
            }
        }
        Color color = (colorProperty != null) ? colorProperty.getPropertyValue() : Color.WHITE;
        textureViewerPane.setTintColor((color != null) ? color : Color.WHITE);
        if(gradientStopProperty != null && gradientTypeProperty != null) {
            GradientStopModel model = gradientStopProperty.getPropertyValue();
            switch(gradientTypeProperty.getPropertyValue()) {
                case HORIZONTAL:
                    textureViewerPane.setSplitPositions(getSplitPos(model), null);
                    break;
                case VERTICAL:
                    textureViewerPane.setSplitPositions(null, getSplitPos(model));
                    break;
                default:
                    textureViewerPane.setSplitPositions((FloatModel[])null, null);
                    break;
            }
        } else {
            textureViewerPane.setSplitPositions(
                    getSplitPos(splitXProperty),
                    getSplitPos(splitYProperty));
        }
    }

    void textureLoaded(URL url, Texture texture) {
        Object obj = themeTreePane.getSelected();
        if(obj != null) {
            Images images = getImages(obj);
            if(images != null) {
                images.updateTextureDimension(url, texture.getWidth(), texture.getHeight());
            }
        }
    }

    private static IntegerModel[] getSplitPos(SplitProperty splitProperty) {
        if(splitProperty != null && splitProperty.isPresent()) {
            return new IntegerModel[] {
                new SplitIntegerModel(splitProperty, 0, true),
                new SplitIntegerModel(splitProperty, 1, true),
            };
        }
        return null;
    }
    
    private static FloatModel[] getSplitPos(GradientStopModel model) {
        FloatModel[] result = new FloatModel[model.getNumEntries() - 1];
        for(int i=0 ; i<result.length ; i++) {
            result[i] = model.getEntry(i).getPosModel();
        }
        return result;
    }

    static int limit(int what, int min, int max) {
        return Math.max(min, Math.min(what, max));
    }

    static float limit(float what, float min, float max) {
        return Math.max(min, Math.min(what, max));
    }
    
    void dragSplit(int idx, int pos, SplitProperty splitProperty, boolean horz) {
        if(gradientStopProperty != null && gradientTypeProperty != null) {
            GradientStopModel model = gradientStopProperty.getPropertyValue();
            if(idx >= 0 && idx < model.getNumEntries()) {
                Stop stop = model.getEntry(idx);
                if(!stop.isSpecial()) {
                    FloatModel posModel = stop.getPosModel();
                    float newPos = limit(pos, posModel.getMinValue(), posModel.getMaxValue());
                    posModel.setValue(newPos);
                }
            }
            return;
        }
        
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
            updateTextureViewerPane();
        }
    }

    void updateTestGUI(GUI testGUI) {
        widgetTree.setTestGUI(ctx, testGUI);
    }

    void updateTestWidget(Widget testWidget) {
        widgetPropertyEditor.setWidget(testWidget);
    }

    void updateTestWidgetProperties() {
        Widget widget = widgetTree.getSelectedWidget();
        widgetPropertyEditor.setWidget(widget);
    }

    void reloadTestWidget() {
        testWidgetManager.reloadCurrentWidget();
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
        testWidgetMenu.add(classpathsMenu);
        testWidgetMenu.add(maRecreateTestWidgets);
    }
    
    private void removeFromParent(Widget w) {
        if(w.getParent() != null) {
            w.getParent().removeChild(w);
        }
    }

    private Images getImages(Object node) {
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
        
        modelChangedCB = new DelayedAction(gui, new Runnable() {
            public void run() {
                reloadTheme();
            }
        });
        if(ctx != null) {
            ctx.getThemeTreeModel().addCallback(modelChangedCB);
        }
        
        checkWidgetTreeTimer = gui.createTimer();
        checkWidgetTreeTimer.setContinuous(true);
        checkWidgetTreeTimer.setDelay(250);
        checkWidgetTreeTimer.setCallback(new Runnable() {
            public void run() {
                checkWidgetTree();
            }
        });
        checkWidgetTreeTimer.start();
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        super.beforeRemoveFromGUI(gui);

        removeModelChangedCB();
        checkWidgetTreeTimer.stop();
        checkWidgetTreeTimer = null;
        themeTreePane.removeCallback(updatePropertyEditors);
        updatePropertyEditors = null;
    }
    
    static class AdjustImageSize implements de.matthiasmann.twl.renderer.Image {
        private final de.matthiasmann.twl.renderer.Image img;
        private final int width;
        private final int height;

        public AdjustImageSize(de.matthiasmann.twl.renderer.Image img, int width, int height) {
            this.img = img;
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return (width >= 0) ? width : img.getWidth();
        }

        public int getHeight() {
            return (height >= 0) ? height : img.getHeight();
        }

        public void draw(AnimationState as, int x, int y) {
            img.draw(as, x, y, getWidth(), getHeight());
        }

        public void draw(AnimationState as, int x, int y, int width, int height) {
            img.draw(as, x, y, width, height);
        }
        
        public de.matthiasmann.twl.renderer.Image createTintedVersion(Color color) {
            return new AdjustImageSize(img.createTintedVersion(color), width, height);
        }
    }
}
