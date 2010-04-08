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
import de.matthiasmann.twlthemeeditor.gui.MainUI;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author Matthias Mann
 */
public class Main extends Frame implements WindowFocusListener, WindowListener, ComponentListener {

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

    private final Canvas canvas;
    
    private volatile boolean closeRequested;
    private volatile boolean canvasSizeChanged;

    public Main() {
        super("TWL Theme editor");

        canvas = new Canvas();
        canvas.addComponentListener(this);

        add(canvas, BorderLayout.CENTER);

        // on Windows we need to transfer focus to the Canvas
        // otherwise keyboard input does not work when using alt-tab
        if(LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_WINDOWS) {
            addWindowFocusListener(this);
        }
        
        addWindowListener(this);
    }

    public void run() {
        try {
            Display.setParent(canvas);
            Display.create();
            Display.setVSyncEnabled(true);

            LWJGLRenderer renderer = new LWJGLRenderer();
            MainUI root = new MainUI();
            GUI gui = new GUI(root, renderer);

            root.setFocusKeyEnabled(true);

            ThemeManager theme = ThemeManager.createThemeManager(
                    Main.class.getResource("gui.xml"), renderer);
            gui.applyTheme(theme);

            while(!Display.isCloseRequested() && !closeRequested && !root.isCloseRequested()) {
                if(canvasSizeChanged) {
                    canvasSizeChanged = false;
                    GL11.glViewport(0, 0, canvas.getWidth(), canvas.getHeight());
                    renderer.syncViewportSize();
                }
                
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                gui.update();
                Display.update();
                reduceInputLag();
            }

            gui.destroy();
            theme.destroy();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Display.destroy();
    }

    /**
     * reduce input lag by polling input devices after waiting for vsync
     *
     * Call after Display.update()
     */
    private static void reduceInputLag() {
        GL11.glGetError();          // this call will burn the time between vsyncs
        Display.processMessages();  // process new native messages since Display.update();
        Mouse.poll();               // now update Mouse events
        Keyboard.poll();            // and Keyboard too
    }

    @Override
    public void windowClosing(WindowEvent e) {
        closeRequested = true;
    }
    
    @Override
    public void windowGainedFocus(WindowEvent e) {
        canvas.requestFocusInWindow();
    }

    public void windowLostFocus(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        canvasSizeChanged = true;
    }

    public void componentShown(ComponentEvent e) {
    }

}
