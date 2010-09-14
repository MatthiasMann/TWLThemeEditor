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
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.gui.MainUI;
import de.matthiasmann.twlthemeeditor.gui.MessageLog;
import de.matthiasmann.twlthemeeditor.gui.MessageLog.Category;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author Matthias Mann
 */
public class Main extends Frame {

    public static void main(String[] args) throws Exception {
        try {
            System.setProperty("org.lwjgl.input.Mouse.allowNegativeMouseCoords", "true");
        } catch (Throwable ignored) {
        }

        Main main = new Main();
        main.setSize(
                Display.getDesktopDisplayMode().getWidth()*4/5,
                Display.getDesktopDisplayMode().getHeight()*4/5);
        main.setLocationRelativeTo(null);
        main.setVisible(true);
        main.run();
        main.dispose();
        
        System.exit(0);
    }

    final Canvas canvas;
    
    volatile boolean closeRequested;
    volatile boolean canvasSizeChanged;

    public Main() {
        super("TWL Theme editor");

        canvas = new Canvas();
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                canvasSizeChanged = true;
            }
        });

        add(canvas, BorderLayout.CENTER);

        // on Windows we need to transfer focus to the Canvas
        // otherwise keyboard input does not work when using alt-tab
        if(LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_WINDOWS) {
            addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowGainedFocus(WindowEvent e) {
                    canvas.requestFocusInWindow();
                }
            });
        }
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeRequested = true;
            }
        });
    }

    public void run() {
        try {
            boolean usesSWGL = false;
            
            Display.setParent(canvas);
            try {
                Display.create();
            } catch (LWJGLException ex) {
                if(ex.getMessage().equals("Pixel format not accelerated")) {
                    try {
                        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");
                        Display.create();
                        usesSWGL = true;
                    } catch (LWJGLException ex2) {
                        throw ex2;
                    } catch (SecurityException ex2) {
                        throw ex;
                    }
                }
            }
            Display.setVSyncEnabled(true);

            LWJGLRenderer renderer = new LWJGLRenderer();
            MainUI root = new MainUI();
            GUI gui = new GUI(root, renderer);

            root.setFocusKeyEnabled(true);

            ThemeManager theme = ThemeManager.createThemeManager(
                    Main.class.getResource("gui.xml"), renderer);
            gui.applyTheme(theme);

            if(usesSWGL) {
                Category cat = new MessageLog.Category("OpenGL", MessageLog.CombineMode.NONE, DecoratedText.WARNING);
                root.addMessage(new MessageLog.Entry(cat, "Software OpenGL rendering in use",
                        "Software OpenGL rendering offers poor performance which could make this editor hard to use." +
                        "It is suggested to check that the latest graphics driver from the graphic card vendor is installed.", null));
                root.openMessagesDialog();
            }

            while(!Display.isCloseRequested() && !closeRequested && !root.isCloseRequested()) {
                if(canvasSizeChanged) {
                    canvasSizeChanged = false;
                    GL11.glViewport(0, 0, canvas.getWidth(), canvas.getHeight());
                    renderer.syncViewportSize();
                }
                
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                if(!Display.isActive()) {
                    gui.clearKeyboardState();
                    gui.clearMouseState();
                }
                
                gui.update();
                //Display.update(false);
                //reduceInputLag();
                Display.update();
            }

            gui.destroy();
            theme.destroy();
        } catch (Throwable ex) {
            showErrMsg(ex);
        }
        Display.destroy();
    }

    /**
     * reduce input lag by polling input devices after waiting for vsync
     *
     * Call after Display.update()
     */
    private static void reduceInputLag() {
        // calling glGetError() will cause high CPU usage - so disable it for now
        //GL11.glGetError();          // this call will burn the time between vsyncs
        Display.processMessages();  // process new native messages since Display.update();
        // *.poll() is called already by processMessages() above
        //Mouse.poll();               // now update Mouse events
        //Keyboard.poll();            // and Keyboard too
    }

    @SuppressWarnings("CallToThreadDumpStack")
    public static void showErrMsg(Throwable ex) {
        ex.printStackTrace();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.flush();
        Sys.alert("TWL Theme Editor - unhandled exception", sw.toString());
    }
}
