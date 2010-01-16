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

import de.matthiasmann.twl.DesktopArea;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ResizableFrame;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.CacheContext;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeLoadErrorTracker;
import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author Matthias Mann
 */
public class PreviewWidget extends Widget {

    private final IntBuffer viewPortBuffer;

    private URL url;
    private LWJGLRenderer render;
    private ThemeManager theme;
    private GUI testGUI;
    private boolean reloadTheme;
    private boolean adjustSize;
    private ResizableFrame frame;
    private Runnable[] callbacks;

    private LWJGLException initException;
    private Throwable executeException;
    private IOException themeLoadException;
    private Object themeLoadErrorLocation;

    public PreviewWidget(URL url) {
        this.viewPortBuffer = BufferUtils.createIntBuffer(16);
        this.url = url;
        setCanAcceptKeyboardFocus(true);
    }

    public void reloadTheme() {
        reloadTheme = true;
    }

    public Throwable getExecuteException() {
        return executeException;
    }

    public LWJGLException getInitException() {
        return initException;
    }

    public IOException getThemeLoadException() {
        return themeLoadException;
    }

    public Object getThemeLoadErrorLocation() {
        return themeLoadErrorLocation;
    }

    public void clearThemeException() {
        themeLoadException = null;
        themeLoadErrorLocation = null;
        reloadTheme = true;
    }

    public void clearExecuteException() {
        executeException = null;
    }

    public void addCallback(Runnable cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Runnable.class);
    }

    public void removeCallback(Runnable cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    private void fireCallbacks() {
        CallbackSupport.fireCallbacks(callbacks);
    }

    protected Widget createRootPane() {
        DesktopArea area = new DesktopArea();
        area.setTheme("");

        PreviewWidgets previewWidgets = new PreviewWidgets();
        previewWidgets.setTheme("/previewwidgets");

        frame = new ResizableFrame();
        frame.add(previewWidgets);
        frame.setTitle("Test");
        frame.setTheme("resizableframe-resizeHandle");
        
        area.add(frame);

        return area;
    }
    
    @Override
    protected void paintWidget(GUI gui) {
        if(executeException == null && initException == null) {
            IOException prevThemeLoadException = themeLoadException;

            GL11.glGetInteger(GL11.GL_VIEWPORT, viewPortBuffer);
            int viewPortTop = viewPortBuffer.get(1) + viewPortBuffer.get(3);

            GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
            GL11.glViewport(getInnerX(),
                    viewPortTop - (getInnerY() + getInnerHeight()),
                    getInnerWidth(), getInnerHeight());

            // CRITICAL REGION: GL STATE IS MODIFIED - DON'T CALL ANY APP CODE
            try {
                executeTestEnv();
            } catch (Throwable ex) {
                // don't let anything escape !
                executeException = ex;
            } finally {
                GL11.glPopAttrib();
                // END OF CRITICAL REGION
            }

            if(initException != null ||
                    executeException != null ||
                    prevThemeLoadException != themeLoadException) {
                // make sure to call this outside the push/pop attrib region
                fireCallbacks();
            }
        }
    }

    private void executeTestEnv() {
        if(render == null && !initRenderer()) {
            return;
        }

        render.syncViewportSize();

        if(testGUI == null) {
            testGUI = new GUI(createRootPane(), render);
        }

        if((theme == null || reloadTheme) && !loadTheme()) {
            return;
        }

        if(adjustSize && frame != null) {
            frame.adjustSize();
        }

        testGUI.setSize();
        testGUI.updateTime();
        testGUI.updateTimers();
        testGUI.handleKeyRepeat();
        testGUI.handleTooltips();
        testGUI.invokeRunables();
        testGUI.validateLayout();
        testGUI.draw();
        testGUI.setCursor();
    }

    private boolean initRenderer() {
        assert(render == null);
        assert(initException == null);

        try {
            render = new LWJGLRenderer();
            render.setUseSWMouseCursors(true);
            return true;
        } catch(LWJGLException ex) {
            initException = ex;
            render = null;
            return false;
        }
    }

    private boolean loadTheme() {
        reloadTheme = false;
        if(themeLoadException == null) {
            ThemeLoadErrorTracker tracker = new ThemeLoadErrorTracker();
            ThemeLoadErrorTracker.push(tracker);

            CacheContext oldCacheContext = render.getActiveCacheContext();
            CacheContext newCacheContext = render.createNewCacheContext();
            render.setActiveCacheContext(newCacheContext);
            try {
                ThemeManager newTheme = ThemeManager.createThemeManager(url, render);
                testGUI.applyTheme(newTheme);

                oldCacheContext.destroy();
                if(theme != null) {
                    theme.destroy();
                }

                theme = newTheme;
                testGUI.destroy();
                return true;
            } catch (IOException ex) {
                themeLoadException = ex;
                render.setActiveCacheContext(oldCacheContext);
                newCacheContext.destroy();
                themeLoadErrorLocation = tracker.findErrorLocation();
            } finally {
                if(ThemeLoadErrorTracker.pop() != tracker) {
                    throw new IllegalStateException("Wrong error tracker");
                }
            }
        }
        return false;
    }

    @Override
    protected boolean handleEvent(Event evt) {
        if(testGUI != null) {
            switch (evt.getType()) {
                case MOUSE_MOVED:
                case MOUSE_DRAGED:
                case MOUSE_ENTERED:
                case MOUSE_EXITED:
                    testGUI.handleMouse(evt.getMouseX() - getInnerX(),
                            evt.getMouseY() - getInnerY(), -1, false);
                    return true;

                case MOUSE_BTNDOWN:
                case MOUSE_BTNUP:
                    testGUI.handleMouse(evt.getMouseX() - getInnerX(),
                            evt.getMouseY() - getInnerY(), evt.getMouseButton(),
                            evt.getType() == Event.Type.MOUSE_BTNDOWN);
                    return true;

                case KEY_PRESSED:
                case KEY_RELEASED:
                    testGUI.handleKey(translateKeyCode(evt.getKeyCode()),
                            evt.getKeyChar(), evt.getType() == Event.Type.KEY_PRESSED);
                    return true;
            }
        }
        return super.handleEvent(evt);
    }

    protected int translateKeyCode(int keyCode) {
        if(keyCode == Keyboard.KEY_F6) {
            return Keyboard.KEY_TAB;
        }
        return keyCode;
    }
}
