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

import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;
import de.matthiasmann.twlthemeeditor.datamodel.Include;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.gui.EditorArea;
import java.io.FileOutputStream;
import java.net.URL;
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
        
        URL url = Main.class.getResource("gui.xml");
        final ThemeTreeModel ttm = new ThemeTreeModel(url);

        try {
            Display.setDisplayMode(new DisplayMode(1000, 800));
            Display.create();
            Display.setTitle("TWL Theme Editor");
            Display.setVSyncEnabled(true);

            LWJGLRenderer renderer = new LWJGLRenderer();
            EditorArea root = new EditorArea();
            GUI gui = new GUI(root, renderer);

            root.setFocusKeyEnabled(true);
            root.setModel(ttm);

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

        int i=1;
        for(Include inc : ttm.getTopLevelNodes(Include.class)) {
            fos = new FileOutputStream("test"+i+".xml");
            try {
                inc.getIncludedThemeFile().writeTo(fos);
            } finally {
                fos.close();
            }
            i++;
        }
    }

}
