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
package de.matthiasmann.twlthemeeditor;

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;
import de.matthiasmann.twlthemeeditor.datamodel.HasProperties;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.Textures;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeFile;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.gui.Context;
import de.matthiasmann.twlthemeeditor.gui.PreviewPane;
import de.matthiasmann.twlthemeeditor.gui.PropertyPanel;
import de.matthiasmann.twlthemeeditor.gui.TextureViewerPane;
import java.beans.IntrospectionException;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author Matthias Mann
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        try {
            System.setProperty("org.lwjgl.input.Mouse.allowNegativeMouseCoords", "true");
        } catch (Throwable unused) {
        }
        
        final TestEnv env = new TestEnv();

        URL url = Main.class.getResource("gui.xml");
        ThemeTreeModel ttm = new ThemeTreeModel(env, url);

        final SimpleChangableListModel<Image> images = new SimpleChangableListModel<Image>();
        final Context ctx = new Context(images);

        ctx.setPropertyOrder("rect", "ref", "condition", "centered", "tint", "splitX", "splitY", "weightsX", "weightsY");
        
        for(Textures t : ttm.getTextures()) {
            System.out.println(t);
            
            for(Image i : t.getChildren(Image.class)) {
                System.out.println("  " + i);
                images.addElement(i);
            }
        }

        env.registerFile("/font.fnt", new URL(url, "font.fnt"));
        env.registerFile("/font_00.png", new URL(url, "font_00.png"));
        
        try {
            Display.setDisplayMode(new DisplayMode(1000, 800));
            Display.create();
            Display.setTitle("TWL Theme Editor");
            Display.setVSyncEnabled(true);

            LWJGLRenderer renderer = new LWJGLRenderer();
            SplitPane root = new SplitPane();
            GUI gui = new GUI(root, renderer);

            final TreeTable treeTable = new TreeTable(ttm);
            final TableSingleSelectionModel selectionModel = new TableSingleSelectionModel();
            treeTable.setSelectionManager(new TableRowSelectionManager(selectionModel));

            ScrollPane treeTableScrollPane = new ScrollPane(treeTable);
            treeTableScrollPane.setTheme("/elementTree");
            treeTableScrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

            final ScrollPane scrollPane = new ScrollPane();
            scrollPane.setTheme("/propertyEditor");
            scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

            SplitPane spTools = new SplitPane();
            spTools.setTheme("/splitpane");
            spTools.setDirection(SplitPane.Direction.VERTICAL);
            spTools.add(treeTableScrollPane);
            spTools.add(scrollPane);

            final PreviewPane previewPane = new PreviewPane(env);
            previewPane.setTheme("/previewpane");

            ttm.getRootThemeFile().addCallback(new Runnable() {
                public void run() {
                    previewPane.reloadTheme();
                }
            });

            TextureViewerPane tvp = new TextureViewerPane();
            tvp.setTheme("/textureviewerpane");

            SplitPane sp2 = new SplitPane();
            sp2.setDirection(SplitPane.Direction.VERTICAL);
            sp2.add(tvp);
            sp2.add(previewPane);
            
            root.add(spTools);
            root.add(sp2);
            root.setSplitPosition(300);
            root.setFocusKeyEnabled(true);

            ctx.setTextureViewerPane(tvp);
            
            selectionModel.addSelectionChangeListener(new Runnable() {
                public void run() {
                    if(selectionModel.hasSelection()) {
                        Object obj = treeTable.getNodeFromRow(selectionModel.getFirstSelected());
                        TextureViewerPane tvp = ctx.getTextureViewerPane();
                        if(tvp != null && (obj instanceof Image)) {
                            tvp.setUrl(((Image)obj).getTextures().getTextureURL());
                            tvp.setRect(null);
                            tvp.setTintColor(Color.WHITE);
                        }
                        if(obj instanceof HasProperties) {
                            try {
                                PropertyPanel propertyPanel = new PropertyPanel(
                                        ctx, ((HasProperties)obj).getProperties());
                                scrollPane.setContent(propertyPanel);
                            } catch (IntrospectionException ex) {
                                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            });

            ThemeManager theme = ThemeManager.createThemeManager(
                    Main.class.getResource("gui.xml"), renderer);
            gui.applyTheme(theme);

            while(!Display.isCloseRequested()) {
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                gui.update();
                Display.update();
            }

            gui.destroy();
            theme.destroy();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Display.destroy();

        FileOutputStream fos = new FileOutputStream("test.xml");
        try {
            ttm.getRootThemeFile().writeTo(fos);
        } finally {
            fos.close();
        }
    }

}
