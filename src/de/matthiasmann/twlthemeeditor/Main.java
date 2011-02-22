/*
 * Copyright (c) 2008-2011, Matthias Mann
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
import de.matthiasmann.twl.input.lwjgl.LWJGLInput;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.gui.MainUI;
import de.matthiasmann.twlthemeeditor.gui.MessageLog;
import de.matthiasmann.twlthemeeditor.gui.MessageLog.Category;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final String KEY_MAINWINDOW_X = "mainwindow.x";
    private static final String KEY_MAINWINDOW_Y = "mainwindow.y";
    private static final String KEY_MAINWINDOW_WIDTH = "mainwindow.width";
    private static final String KEY_MAINWINDOW_HEIGHT = "mainwindow.height";

    private static final int REQUIRED_LWJGL_VERSION_MAJOR = 2;
    private static final int REQUIRED_LWJGL_VERSION_MINOR = 6;
    
    public static void main(String[] args) throws Exception {
        try {
            System.setProperty("org.lwjgl.input.Mouse.allowNegativeMouseCoords", "true");
            System.setProperty("sun.java2d.noddraw", "true");
            System.setProperty("sun.java2d.opengl", "false");
        } catch (Throwable ignored) {
        }

        final int desktopWidth = Display.getDesktopDisplayMode().getWidth();
        final int desktopHeight = Display.getDesktopDisplayMode().getHeight();
        final Preferences prefs = Preferences.userNodeForPackage(Main.class);

        int width  = Math.max(400, Math.min(desktopWidth,  prefs.getInt(KEY_MAINWINDOW_WIDTH,  desktopWidth *4/5)));
        int height = Math.max(300, Math.min(desktopHeight, prefs.getInt(KEY_MAINWINDOW_HEIGHT, desktopHeight*4/5)));

        final Main main = new Main();
        main.setSize(width, height);

        String strX = prefs.get(KEY_MAINWINDOW_X, null);
        String strY = prefs.get(KEY_MAINWINDOW_Y, null);
        if(strX != null && strY != null) {
            try {
                int x = Math.max(0, Math.min(desktopWidth  - width,  Integer.parseInt(strX)));
                int y = Math.max(0, Math.min(desktopHeight - height, Integer.parseInt(strY)));
                main.setLocation(x, y);
            } catch(Throwable ex) {
                main.setLocationRelativeTo(null);
            }
        } else {
            main.setLocationRelativeTo(null);
        }

        main.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                prefs.putInt(KEY_MAINWINDOW_WIDTH, main.getWidth());
                prefs.putInt(KEY_MAINWINDOW_HEIGHT, main.getHeight());
            }
            @Override
            public void componentMoved(ComponentEvent e) {
                prefs.putInt(KEY_MAINWINDOW_X, main.getX());
                prefs.putInt(KEY_MAINWINDOW_Y, main.getY());
            }
        });

        main.setVisible(true);
        main.run();
        main.dispose();
        
        System.exit(0);
    }
    
    final Canvas canvas;
    final AtomicReference<Dimension> newCanvasSize = new AtomicReference<Dimension>();
    
    volatile boolean closeRequested;

    public Main() {
        super("TWL Theme editor");

        canvas = new Canvas();
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                newCanvasSize.set(canvas.getSize());
            }
        });

        add(canvas, BorderLayout.CENTER);
        setIconImage(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("icon16.png")));

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
                        // rethrow the original error if we can't change the system property
                        throw ex;
                    }
                } else {
                    throw ex;
                }
            }
            Display.setVSyncEnabled(true);

            LWJGLRenderer renderer = new LWJGLRenderer();
            MainUI root = new MainUI();
            GUI gui = new GUI(root, renderer, new LWJGLInput());

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

            String lwjglVersionMsg = null;
            Matcher matcher = Pattern.compile("^(\\d+)\\.(\\d+)(\\.\\d+)?").matcher(Sys.getVersion());
            if(matcher.matches()) {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                if(major < REQUIRED_LWJGL_VERSION_MAJOR || (major == REQUIRED_LWJGL_VERSION_MAJOR && minor < REQUIRED_LWJGL_VERSION_MINOR)) {
                    lwjglVersionMsg = "is too old and could result in mouse handling issues";
                }
            } else {
                lwjglVersionMsg = "could not be parsed";
            }

            if(lwjglVersionMsg != null) {
                Category cat = new MessageLog.Category("LWJGL", MessageLog.CombineMode.NONE, DecoratedText.WARNING);
                root.addMessage(new MessageLog.Entry(cat, "LWJGL version",
                        "The used version of LWJGL (" + Sys.getVersion() + ") " + lwjglVersionMsg +
                        ".\nIt is suggested to upgrade to LWJGL " + REQUIRED_LWJGL_VERSION_MAJOR +
                        "." + REQUIRED_LWJGL_VERSION_MINOR + " or later.", null));
                root.openMessagesDialog();
            }

            while(!Display.isCloseRequested() && !closeRequested && !root.isCloseRequested()) {
                Dimension newDim = newCanvasSize.getAndSet(null);
                if(newDim != null) {
                    GL11.glViewport(0, 0, newDim.width, newDim.height);
                    renderer.syncViewportSize();
                }
                
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                gui.update();
                Display.update();
            }

            gui.destroy();
            theme.destroy();
        } catch (Throwable ex) {
            showErrMsg(ex);
        }
        Display.destroy();
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
