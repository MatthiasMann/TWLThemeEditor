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
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.CacheContext;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;
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
    private TestWidgetFactory widgetFactory;
    private Widget testWidget;
    private Runnable testWidgetChangedCB;

    private final ExceptionHolder exceptionHolder;
    private Object themeLoadErrorLocation;

    private static final int EXCEPTION_INIT    = 0;
    private static final int EXCEPTION_WIDGET  = 1;
    private static final int EXCEPTION_THEME   = 2;
    private static final int EXCEPTION_EXECUTE = 3;
    
    public PreviewWidget() {
        this.viewPortBuffer = BufferUtils.createIntBuffer(16);
        this.exceptionHolder = new ExceptionHolder(
                "Initialization",
                "Widget creation",
                "Theme load",
                "Execution");
        setCanAcceptKeyboardFocus(true);
    }

    public void setURL(URL url) {
        this.url = url;
        this.reloadTheme = true;
    }

    public void setWidgetFactory(TestWidgetFactory factory) {
        this.widgetFactory = factory;
        this.testWidget = null;
    }

    public Widget getTestWidget() {
        return testWidget;
    }

    public void setTestWidgetChangedCB(Runnable testWidgetChangedCB) {
        this.testWidgetChangedCB = testWidgetChangedCB;
    }

    public void reloadTheme() {
        reloadTheme = true;
    }

    public ExceptionHolder getExceptionHolder() {
        return exceptionHolder;
    }

    public Object getThemeLoadErrorLocation() {
        return themeLoadErrorLocation;
    }

    public void clearException(int nr) {
        if(nr == EXCEPTION_THEME) {
            themeLoadErrorLocation = null;
            reloadTheme = true;
        }
        exceptionHolder.setException(nr, null);
    }

    public Image getImage(String name) {
        return (theme != null) ? theme.getImageNoWarning(name) : null;
    }
    
    @Override
    protected void paintWidget(GUI gui) {
        if(!exceptionHolder.hasException()) {
            // don't execute callbacks in critical section
            exceptionHolder.setDeferCallbacks(true);

            GL11.glGetInteger(GL11.GL_VIEWPORT, viewPortBuffer);
            int viewPortTop = viewPortBuffer.get(1) + viewPortBuffer.get(3);

            GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
            GL11.glViewport(getInnerX(),
                    viewPortTop - (getInnerY() + getInnerHeight()),
                    getInnerWidth(), getInnerHeight());

            // CRITICAL REGION: GL STATE IS MODIFIED - DON'T CALL ANY APP CODE
            try {
                executeTestEnv(gui);
            } catch (Throwable ex) {
                // don't let anything escape !
                exceptionHolder.setException(EXCEPTION_EXECUTE, ex);
            } finally {
                GL11.glPopAttrib();
                // END OF CRITICAL REGION
            }

            exceptionHolder.setDeferCallbacks(false);
        }
    }

    private void executeTestEnv(GUI gui) {
        if(render == null && !initRenderer()) {
            return;
        }

        render.syncViewportSize();

        if(testGUI == null) {
            DesktopArea area = new DesktopArea() {
                @Override
                protected void restrictChildrenToInnerArea() {
                    final int top = getInnerY();
                    final int left = getInnerX();
                    final int right = getInnerRight();
                    final int bottom = getInnerBottom();
                    final int width = Math.max(0, right-left);
                    final int height = Math.max(0, bottom-top);

                    for(int i=0,n=getNumChildren() ; i<n ; i++) {
                        Widget w = getChild(i);
                        int minWidth = w.getMinWidth();
                        int minHeight = w.getMinHeight();
                        w.setSize(
                                Math.min(Math.max(width, minWidth), Math.max(w.getWidth(), minWidth)),
                                Math.min(Math.max(height, minHeight), Math.max(w.getHeight(), minHeight)));
                        w.setPosition(
                                Math.max(left, Math.min(right - w.getWidth(), w.getX())),
                                Math.max(top, Math.min(bottom - w.getHeight(), w.getY())));
                    }
                }
            };
            area.setTheme("");
            testGUI = new GUI(area, render);
        }

        if((theme == null || reloadTheme) && !loadTheme()) {
            return;
        }

        if(testWidget == null && widgetFactory != null) {
            testGUI.getRootPane().removeAllChildren();
            try {
                testWidget = widgetFactory.getOrCreate();
                testGUI.getRootPane().add(testWidget);

                if(testWidgetChangedCB != null) {
                    gui.invokeLater(testWidgetChangedCB);
                }
                testWidget.adjustSize();
            } catch (Throwable ex) {
                exceptionHolder.setException(EXCEPTION_WIDGET, ex);
            }
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

        try {
            render = new LWJGLRenderer();
            render.setUseSWMouseCursors(true);
            return true;
        } catch(LWJGLException ex) {
            exceptionHolder.setException(EXCEPTION_INIT, ex);
            render = null;
            return false;
        }
    }

    private boolean loadTheme() {
        reloadTheme = false;
        if(url != null) {
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
                exceptionHolder.setException(EXCEPTION_THEME, ex);
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
