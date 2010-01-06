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

import de.matthiasmann.twl.CallbackWithReason;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ListBox;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SplitPane;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.renderer.lwjgl.PNGDecoder;
import de.matthiasmann.twl.theme.ThemeManager;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.Textures;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeFile;
import de.matthiasmann.twlthemeeditor.gui.Context;
import de.matthiasmann.twlthemeeditor.gui.PreviewWidget;
import de.matthiasmann.twlthemeeditor.gui.PropertyPanel;
import java.beans.IntrospectionException;
import java.io.FileOutputStream;
import java.io.InputStream;
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
        URL url = Main.class.getResource("simple.xml");
        ThemeFile tf = new ThemeFile(url);

        final SimpleChangableListModel<Image> images = new SimpleChangableListModel<Image>();
        final Context ctx = new Context(images);

        final TestEnv env = new TestEnv();

        ctx.setPropertyOrder("x", "y", "width", "height", "center");
        
        for(Textures t : tf.findTextures()) {
            System.out.println(t);
            
            URL textureURL = new URL(url, t.getFile());
            InputStream textureStream = textureURL.openStream();
            try {
                PNGDecoder decoder = new PNGDecoder(textureStream);
                t.setTextureDimensions(new Dimension(decoder.getWidth(), decoder.getHeight()));
            } finally {
                textureStream.close();
            }

            env.registerFile(t.getFile(), textureURL);
            
            for(Image i : t.getImages()) {
                System.out.println("  " + i);
                images.addElement(i);
            }
        }

        env.registerFile("/theme.xml", tf.createVirtualFile());
        env.registerFile("/font.fnt", new URL(url, "font.fnt"));
        env.registerFile("/font_00.png", new URL(url, "font_00.png"));
        
        try {
            Display.setDisplayMode(new DisplayMode(800, 600));
            Display.create();
            Display.setTitle("TWL Theme Editor");
            Display.setVSyncEnabled(true);

            LWJGLRenderer renderer = new LWJGLRenderer();
            SplitPane root = new SplitPane();
            GUI gui = new GUI(root, renderer);

            final ListBox lb = new ListBox(images);
            lb.setTheme("/listbox");
            
            final ScrollPane scrollPane = new ScrollPane();
            scrollPane.setTheme("/scrollpane");
            scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

            SplitPane spTools = new SplitPane();
            spTools.setTheme("/splitpane");
            spTools.setDirection(SplitPane.Direction.VERTICAL);
            spTools.add(lb);
            spTools.add(scrollPane);

            final PreviewWidget previewWidget = new PreviewWidget(env);
            previewWidget.setTheme("/previewwidget");

            tf.addCallback(new Runnable() {
                public void run() {
                    previewWidget.reloadTheme();
                }
            });

            SplitPane sp2 = new SplitPane();
            sp2.setDirection(SplitPane.Direction.VERTICAL);
            sp2.add(new Widget());
            sp2.add(previewWidget);
            
            root.add(spTools);
            root.add(sp2);//previewWidget);

            lb.addCallback(new CallbackWithReason<ListBox.CallbackReason>() {
                public void callback(ListBox.CallbackReason reason) {
                    Object obj = images.getEntry(lb.getSelected());
                    if(obj != null) {
                        try {
                            PropertyPanel propertyPanel = new PropertyPanel(ctx, obj);
                            propertyPanel.setTheme("/propertypanel");
                            scrollPane.setContent(propertyPanel);
                        } catch (IntrospectionException ex) {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
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
            tf.writeTo(fos);
        } finally {
            fos.close();
        }
    }

}
