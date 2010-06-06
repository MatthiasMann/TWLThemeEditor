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

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.CallbackWithReason;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.MenuAction;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.DelayedAction;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.Split;
import de.matthiasmann.twlthemeeditor.datamodel.Textures;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.gui.MainUI.ExtFilter;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;
import java.net.MalformedURLException;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class EditorArea extends Widget {

    public enum Layout {
        SPLIT_HV,
        SPLIT_HHV
    }

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

    private final CallbackWithReason<ThemeTreeModel.CallbackReason> modelChangedCB;
    private final Runnable boundPropertyCB;

    private DelayedAction updatePropertyEditors;
    private Context ctx;
    private RectProperty boundRectProperty;
    private ColorProperty boundColorProperty;
    private SplitProperty boundSplitXProperty;
    private SplitProperty boundSplitYProperty;
    private BorderProperty boundBorderProperty;
    private Layout layout = Layout.SPLIT_HV;

    public EditorArea(MessageLog messageLog) {
        this.messageLog = messageLog;
        
        testWidgetManager = new TestWidgetManager(messageLog);
        testWidgetMenu = new Menu("Widgets");
        themeTreePane = new ThemeTreePane();
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
                ctx.getThemeTreeModel().setErrorLocation(null);
                previewWidget.reloadTheme();
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

        recreateLayout();
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
        if(boundBorderProperty != null) {
            boundBorderProperty.removeCallback(boundPropertyCB);
            boundBorderProperty = null;
        }
        
        Object obj = themeTreePane.getSelected();
        if(obj != null) {
            Textures textures = getTextures(obj);
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
                    if(boundBorderProperty == null && (property instanceof BorderProperty) && property.getName().startsWith("Border")) {
                        boundBorderProperty = (BorderProperty)property;
                        boundBorderProperty.addValueChangedCallback(boundPropertyCB);
                    }
                }
            }
        } else {
            propertiesScrollPane.setContent(null);
        }
        updateTextureViewerPane();
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
        textureViewerPane.setSplitPositionsX(getSplitPos(boundSplitXProperty, boundBorderProperty, true));
        textureViewerPane.setSplitPositionsY(getSplitPos(boundSplitYProperty, boundBorderProperty, false));
    }

    private static int[] getSplitPos(SplitProperty splitProperty, BorderProperty borderProperty, boolean horz) {
        if(splitProperty != null) {
            Split split = splitProperty.getPropertyValue();
            if(split != null) {
                return new int[] { split.getSplit1(), split.getSplit2() };
            }
            if(borderProperty != null) {
                Border border = borderProperty.getPropertyValue();
                if(border != null) {
                    if(horz) {
                        return new int[] { border.getBorderLeft(), splitProperty.getLimit() - border.getBorderRight() };
                    } else {
                        return new int[] { border.getBorderTop(), splitProperty.getLimit() - border.getBorderBottom() };
                    }
                }
            }
        }
        return null;
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

    void loadUserWidget() {
        final PopupWindow popupWindow = new PopupWindow(this);
        JavaFileSystemModel fsm = new JavaFileSystemModel();
        FileSelector.NamedFileFilter filter = new FileSelector.NamedFileFilter(
                "JAR files", new ExtFilter(".jar"));
        FileSelector fileSelector = new FileSelector(
                Preferences.userNodeForPackage(EditorArea.class),
                "userWidgetJARs");
        fileSelector.setFileSystemModel(fsm);
        fileSelector.addFileFilter(FileSelector.AllFilesFilter);
        fileSelector.addFileFilter(filter);
        fileSelector.setFileFilter(filter);
        fileSelector.setAllowMultiSelection(true);
        fileSelector.addCallback(new FileSelector.Callback() {
            public void filesSelected(Object[] files) {
                if(testWidgetManager.loadUserWidgets(files)) {
                    updateTestWidgetMenu();
                }
                popupWindow.closePopup();
            }
            public void canceled() {
                popupWindow.closePopup();
            }
        });
        popupWindow.setTheme("fileselector-popup");
        popupWindow.add(fileSelector);
        popupWindow.setSize(getWidth()*4/5, getHeight()*4/5);
        popupWindow.setPosition(
                getWidth()/2 - popupWindow.getWidth()/2,
                getHeight()/2 - popupWindow.getHeight()/2);
        popupWindow.openPopup();
    }

    void updateTestWidgetMenu() {
        MenuAction maRecreateTestWidgets = new MenuAction("Recreate Widgets", new Runnable() {
            public void run() {
                recreateTestWidgets();
            }
        });
        maRecreateTestWidgets.setTooltipContent("Clears widget cache and recreates current widget");

        MenuAction maLoadUserWidget = new MenuAction("Load Widget", new Runnable() {
            public void run() {
                loadUserWidget();
            }
        });
        maLoadUserWidget.setTooltipContent("Load a Widget from a user supplied JAR file");

        testWidgetMenu.clear();
        testWidgetManager.updateMenu(testWidgetMenu);
        testWidgetMenu.addSpacer();
        testWidgetMenu.add(maRecreateTestWidgets);
        testWidgetMenu.add(maLoadUserWidget);
    }
    
    private void removeFromParent(Widget w) {
        if(w.getParent() != null) {
            w.getParent().removeChild(w);
        }
    }

    private Textures getTextures(Object node) {
        if(node instanceof Textures) {
            return (Textures)node;
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
