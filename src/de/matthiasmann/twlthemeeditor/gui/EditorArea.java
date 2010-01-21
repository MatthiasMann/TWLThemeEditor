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
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twlthemeeditor.DelayedAction;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import java.net.MalformedURLException;

/**
 *
 * @author Matthias Mann
 */
public class EditorArea extends Widget {

    public enum Layout {
        SPLIT_HV,
        SPLIT_HHV
    }

    private final ThemeTreePane themeTreePane;
    private final PreviewPane previewPane;
    private final TextureViewerPane textureViewerPane;
    private final ScrollPane propertiesScrollPane;

    private final CallbackWithReason<ThemeTreeModel.CallbackReason> modelChangedCB;

    private DelayedAction updatePropertyEditors;
    private Context ctx;
    private Layout layout = Layout.SPLIT_HV;

    public EditorArea() {
        themeTreePane = new ThemeTreePane();
        themeTreePane.setTheme("/themetreepane");
        previewPane = new PreviewPane();
        previewPane.setTheme("/previewpane");
        textureViewerPane = new TextureViewerPane();
        textureViewerPane.setTheme("/textureviewerpane");
        propertiesScrollPane = new ScrollPane();
        propertiesScrollPane.setTheme("/propertyEditor");
        propertiesScrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

        previewPane.addCallback(new Runnable() {
            public void run() {
                updateErrorLocation();
            }
        });

        modelChangedCB = new CallbackWithReason<ThemeTreeModel.CallbackReason>() {
            public void callback(ThemeTreeModel.CallbackReason reason) {
                ctx.getThemeTreeModel().setErrorLocation(null);
                previewPane.reloadTheme();
                if(reason == ThemeTreeModel.CallbackReason.STRUCTURE_CHANGED) {
                    updatePropertyEditors.run();
                }
            }
        };

        recreateLayout();
    }

    public void setModel(ThemeTreeModel model) {
        if(ctx != null) {
            ctx.getThemeTreeModel().removeCallbacks(modelChangedCB);
        }

        if(model == null) {
            ctx = null;
            previewPane.setURL(null);
        } else {
            ctx = new Context(model);
            ctx.setTextureViewerPane(textureViewerPane);
            ctx.setThemeTreePane(themeTreePane);

            model.addCallback(modelChangedCB);
            
            try {
                previewPane.setURL(model.getRootThemeFile().getVirtualURL());
            } catch(MalformedURLException ex) {
                previewPane.setURL(null);
            }
        }
        
        textureViewerPane.setUrl(null);
        propertiesScrollPane.setContent(null);
        themeTreePane.setModel(model);
        previewPane.setContext(ctx);
    }

    public void setLayout(Layout layout) {
        if(this.layout != layout) {
            this.layout = layout;
            recreateLayout();
        }
    }

    void recreateLayout() {
        removeAllChildren();
        removeFromParent(themeTreePane);
        removeFromParent(previewPane);
        removeFromParent(textureViewerPane);
        removeFromParent(propertiesScrollPane);

        switch(layout) {
            case SPLIT_HV: {
                SplitPane spH = new SplitPane();
                SplitPane spV1 = new SplitPane();
                SplitPane spV2 = new SplitPane();
                spH.setSplitPosition(300);
                spH.add(spV1);
                spH.add(spV2);
                spV1.setDirection(SplitPane.Direction.VERTICAL);
                spV1.add(themeTreePane);
                spV1.add(propertiesScrollPane);
                spV2.setDirection(SplitPane.Direction.VERTICAL);
                spV2.add(textureViewerPane);
                spV2.add(previewPane);
                add(spH);
                break;
            }

            case SPLIT_HHV: {
                SplitPane spH1 = new SplitPane();
                SplitPane spH2 = new SplitPane();
                SplitPane spV = new SplitPane();
                spH1.setSplitPosition(300);
                spH1.add(themeTreePane);
                spH1.add(spH2);
                spH2.setSplitPosition(300);
                spH2.add(propertiesScrollPane);
                spH2.add(spV);
                spV.setDirection(SplitPane.Direction.VERTICAL);
                spV.add(textureViewerPane);
                spV.add(previewPane);
                add(spH1);
                break;
            }
        }
    }

    void updateProperties() {
        Object obj = themeTreePane.getSelected();
        if(obj != null) {
            if(obj instanceof Image) {
                try {
                    textureViewerPane.setUrl(((Image)obj).getTextures().getTextureURL());
                } catch(MalformedURLException ex) {
                    textureViewerPane.setUrl(null);
                }
                textureViewerPane.setRect(null);
                textureViewerPane.setTintColor(Color.WHITE);
            }
            if(obj instanceof HasProperties) {
                PropertyPanel propertyPanel = new PropertyPanel(
                        ctx, ((HasProperties)obj).getProperties());
                propertiesScrollPane.setContent(propertyPanel);
            }
        } else {
            propertiesScrollPane.setContent(null);
        }
    }

    void updateErrorLocation() {
        if(ctx != null) {
            Object loc = previewPane.getThemeLoadErrorLocation();
            if(loc instanceof ThemeTreeNode) {
                ctx.getThemeTreeModel().setErrorLocation((ThemeTreeNode)loc);
            } else {
                ctx.getThemeTreeModel().setErrorLocation(null);
            }
        }
    }

    private void removeFromParent(Widget w) {
        if(w.getParent() != null) {
            w.getParent().removeChild(w);
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
    
}
