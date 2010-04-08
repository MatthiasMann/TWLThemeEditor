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
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
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

    public interface Callback {
        public void testWidgetChanged(Widget widget);
        public void errorLocationChanged(Object errorLocation);
    }

    private final MessageLog messageLog;
    private final IntBuffer viewPortBuffer;

    private Context ctx;
    private URL url;
    private LWJGLRenderer render;
    private ThemeManager theme;
    private GUI testGUI;
    private boolean reloadTheme;
    private boolean hadThemeLoadError;
    private TestWidgetFactory widgetFactory;
    private Widget testWidget;
    private Callback callback;

    private static final MessageLog.Category CAT_INIT = new MessageLog.Category("init renderer", MessageLog.CombineMode.REPLACE, DecoratedText.ERROR);
    private static final MessageLog.Category CAT_WIDGET = new MessageLog.Category("creating widget", MessageLog.CombineMode.REPLACE, DecoratedText.ERROR);
    private static final MessageLog.Category CAT_THEME = new MessageLog.Category("theme loading", MessageLog.CombineMode.REPLACE, DecoratedText.ERROR);
    private static final MessageLog.Category CAT_EXECUTE = new MessageLog.Category("executing", MessageLog.CombineMode.REPLACE, DecoratedText.ERROR);

    public PreviewWidget(MessageLog messageLog) {
        this.messageLog = messageLog;
        this.viewPortBuffer = BufferUtils.createIntBuffer(16);
        setCanAcceptKeyboardFocus(true);
    }

    public void setURL(Context ctx, URL url) {
        this.ctx = ctx;
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

    public void reloadTheme() {
        reloadTheme = true;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public Image getImage(String name) {
        return (theme != null) ? theme.getImageNoWarning(name) : null;
    }
    
    @Override
    protected void paintWidget(GUI gui) {
        if((reloadTheme || !hadThemeLoadError) && ctx != null) {
            ctx.installDebugHook();

            GL11.glGetInteger(GL11.GL_VIEWPORT, viewPortBuffer);
            int viewPortTop = viewPortBuffer.get(1) + viewPortBuffer.get(3);

            GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
            GL11.glViewport(
                    viewPortBuffer.get(0) + getInnerX(),
                    viewPortTop - (getInnerY() + getInnerHeight()),
                    getInnerWidth(), getInnerHeight());

            // CRITICAL REGION: GL STATE IS MODIFIED - DON'T CALL ANY APP CODE
            try {
                executeTestEnv(gui);
            } catch (Throwable ex) {
                messageLog.add(new MessageLog.Entry(CAT_EXECUTE, "Exception while executing test widget", null, ex));
            } finally {
                GL11.glPopAttrib();
                // END OF CRITICAL REGION
            }

            ctx.uninstallDebugHook();
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

        if((theme == null || reloadTheme) && !loadTheme(gui)) {
            return;
        }

        if(testWidget == null && widgetFactory != null) {
            testGUI.getRootPane().removeAllChildren();
            try {
                ctx.clearWidgetMessages();
                testWidget = widgetFactory.getOrCreate();
                testGUI.getRootPane().add(testWidget);
                testWidget.adjustSize();
            } catch (Throwable ex) {
                messageLog.add(new MessageLog.Entry(CAT_WIDGET, "Exception while creating test widget", null, ex));
            }

            if(callback != null) {
                final Widget widget = testWidget;
                final Callback cb = callback;
                gui.invokeLater(new Runnable() {
                    public void run() {
                        cb.testWidgetChanged(widget);
                    }
                });
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
            messageLog.add(new MessageLog.Entry(CAT_INIT, "Exception while creating renderer", null, ex));
            render = null;
            return false;
        }
    }

    private boolean loadTheme(GUI gui) {
        reloadTheme = false;
        hadThemeLoadError = false;
        if(url != null) {
            ThemeLoadErrorTracker tracker = new ThemeLoadErrorTracker();
            ThemeLoadErrorTracker.push(tracker);
            Object errorLocation = null;

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
                hadThemeLoadError = true;
                errorLocation = tracker.findErrorLocation();
                render.setActiveCacheContext(oldCacheContext);
                newCacheContext.destroy();
                messageLog.add(new MessageLog.Entry(CAT_THEME, "Exception while loading theme", null, ex));
            } finally {
                if(ThemeLoadErrorTracker.pop() != tracker) {
                    throw new IllegalStateException("Wrong error tracker");
                }
            }

            fireErrorLocationChanged(gui, errorLocation);
        }
        return false;
    }

    private void fireErrorLocationChanged(GUI gui, final Object errorLocation) {
        if(callback != null) {
            final Callback cb = callback;
            gui.invokeLater(new Runnable() {
                public void run() {
                    cb.errorLocationChanged(errorLocation);
                }
            });
        }
    }

    @Override
    protected boolean handleEvent(Event evt) {
        if(testGUI != null) {
            boolean handled = false;
            try {
                switch (evt.getType()) {
                    case MOUSE_MOVED:
                    case MOUSE_DRAGED:
                    case MOUSE_ENTERED:
                    case MOUSE_EXITED:
                        handled = true;
                        testGUI.handleMouse(evt.getMouseX() - getInnerX(),
                                evt.getMouseY() - getInnerY(), -1, false);
                        break;

                    case MOUSE_BTNDOWN:
                    case MOUSE_BTNUP:
                        handled = true;
                        testGUI.handleMouse(evt.getMouseX() - getInnerX(),
                                evt.getMouseY() - getInnerY(), evt.getMouseButton(),
                                evt.getType() == Event.Type.MOUSE_BTNDOWN);
                        break;

                    case MOUSE_WHEEL:
                        handled = true;
                        testGUI.handleMouseWheel(evt.getMouseWheelDelta());
                        break;

                    case KEY_PRESSED:
                    case KEY_RELEASED:
                        handled = true;
                        testGUI.handleKey(translateKeyCode(evt.getKeyCode()),
                                evt.getKeyChar(), evt.getType() == Event.Type.KEY_PRESSED);
                        break;
                }
            } catch(Throwable ex) {
                messageLog.add(new MessageLog.Entry(CAT_EXECUTE, "Exception while handling events", null, ex));
            }
            return handled;
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
