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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.DesktopArea;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.ThemeInfo;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.PersistentColorModel;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;
import de.matthiasmann.twl.renderer.CacheContext;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.lwjgl.LWJGLRenderer;
import de.matthiasmann.twl.theme.ThemeManager;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeLoadErrorTracker;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.prefs.Preferences;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Util;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author Matthias Mann
 */
public class PreviewWidget extends Widget {

    public static final StateKey STATE_FLASHING = StateKey.get("flashing");
    
    public interface Callback {
        public void testWidgetChanged(Widget widget);
        public void errorLocationChanged(Object errorLocation);
        public void testGUIChanged(GUI testGUI);
        public void themeLoaded();
    }

    private final MessageLog messageLog;
    
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
    private boolean mouseInside;
    
    private Image whiteImage;
    private final PersistentColorModel backgroundColorModel;
    
    private Image flashImage;
    private int flashX;
    private int flashY;
    private int flashWidth;
    private int flashHeight;

    private static final MessageLog.Category CAT_INIT = new MessageLog.Category("init renderer", MessageLog.CombineMode.REPLACE, DecoratedText.ERROR);
    private static final MessageLog.Category CAT_WIDGET = new MessageLog.Category("creating widget", MessageLog.CombineMode.REPLACE, DecoratedText.ERROR);
    private static final MessageLog.Category CAT_THEME = new MessageLog.Category("theme loading", MessageLog.CombineMode.REPLACE, DecoratedText.ERROR);
    private static final MessageLog.Category CAT_EXECUTE = new MessageLog.Category("executing", MessageLog.CombineMode.REPLACE, DecoratedText.ERROR);

    private static final String KEY_BACKGROUND_COLOR = "previewWidgetBackgroundColor";
    
    public PreviewWidget(MessageLog messageLog) {
        this.messageLog = messageLog;
        
        Preferences prefs = Preferences.userNodeForPackage(PreviewWidget.class);
        backgroundColorModel = new PersistentColorModel(prefs, KEY_BACKGROUND_COLOR, Color.BLACK);
        backgroundColorModel.addCallback(new Runnable() {
            public void run() {
                applyBackgroundColor();
            }
        });
        
        setCanAcceptKeyboardFocus(true);
        setClip(true);
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

    public GUI getTestGUI() {
        return testGUI;
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

    public void flashRectangle(int x, int y, int width, int height, boolean flashing) {
        flashX = x;
        flashY = y;
        flashWidth = width;
        flashHeight = height;
        AnimationState as = getAnimationState();
        as.setAnimationState(STATE_FLASHING, flashing);
        as.resetAnimationTime(STATE_FLASHING);
    }

    public Widget selectWidgetFromMouse(int x, int y) {
        Widget widget = testGUI;
        if(widget == null) {
            return null;
        }
        
        x -= getInnerX();
        y -= getInnerY();

        for(;;) {
            Widget found = null;
            for(int i=widget.getNumChildren() ; i-->0 ;) {
                Widget c = widget.getChild(i);
                if(c.isVisible() && testWidgetInside(c, x, y) && !isTooltipWindow(c)) {
                    found = c;
                    break;
                }
            }
            if(found == null) {
                return widget;
            }
            widget = found;
        }
    }

    public void addSettingsMenuItems(Menu settingsMenu) {
        settingsMenu.add(new ColorMenuItem(backgroundColorModel, "Preview background"));
    }
    
    private static boolean isTooltipWindow(Widget widget) {
        return "de.matthiasmann.twl.GUI$TooltipWindow".equals(widget.getClass().getName());
    }

    private static boolean testWidgetInside(Widget widget, int x, int y) {
        return x >= widget.getX() && y >= widget.getY() && x < widget.getRight() && y < widget.getBottom();
    }
    
    void applyBackgroundColor() {
        if(whiteImage != null) {
            setBackground(whiteImage.createTintedVersion(backgroundColorModel.getValue()));
        } else {
            setBackground(null);
        }
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        flashImage = themeInfo.getImage("flashImage");
        whiteImage = themeInfo.getImage("whiteImage");
        applyBackgroundColor();
    }

    @Override
    protected void applyThemeBackground(ThemeInfo themeInfo) {
    }
    
    @Override
    protected void paintWidget(GUI gui) {
        if((reloadTheme || !hadThemeLoadError) && ctx != null) {
            LWJGLRenderer mainRenderer = (LWJGLRenderer)gui.getRenderer();
            ctx.installDebugHook();
            try {
                // CRITICAL REGION: GL STATE IS MODIFIED - DON'T CALL ANY APP CODE
                mainRenderer.pauseRendering();
                try {
                    executeTestEnv(gui, mainRenderer);
                    Util.checkGLError();
                } catch (Throwable ex) {
                    messageLog.add(new MessageLog.Entry(CAT_EXECUTE, "Exception while executing test widget", null, ex));
                } finally {
                    setViewport(mainRenderer);
                    mainRenderer.resumeRendering();
                    // END OF CRITICAL REGION
                }
            } finally {
                ctx.uninstallDebugHook();
            }
        }
        if(flashImage != null && flashWidth > 0 && flashHeight > 0) {
            flashImage.draw(getAnimationState(), getInnerX() + flashX, getInnerY() + flashY, flashWidth, flashHeight);
        }
    }
    
    private static void setViewport(LWJGLRenderer renderer) {
        GL11.glViewport(
                renderer.getViewportX(),
                renderer.getViewportY(),
                renderer.getWidth(),
                renderer.getHeight());
    }

    private void executeTestEnv(GUI gui, LWJGLRenderer mainRenderer) {
        if(render == null && !initRenderer()) {
            return;
        }

        render.setViewport(
            mainRenderer.getViewportX() + getInnerX(),
            mainRenderer.getViewportY() + mainRenderer.getHeight() - (getInnerY() + getInnerHeight()),
            getInnerWidth(), getInnerHeight());
        setViewport(render);

        if(testGUI == null) {
            TestWidgetContainer container = new TestWidgetContainer();
            testGUI = new GUI(container, render);
            if(callback != null) {
                callback.testGUIChanged(testGUI);
            }
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
                if(testWidget instanceof DesktopArea || MainUI.class.getName().equals(testWidget.getClass().getName())) {
                    testWidget.setSize(getInnerWidth(), getInnerHeight());
                } else {
                    testWidget.adjustSize();
                }
            } catch (Throwable ex) {
                messageLog.add(new MessageLog.Entry(CAT_WIDGET, "Exception while creating test widget", null, unwrap(ex)));
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

    private static Throwable unwrap(Throwable ex) {
        if(ex instanceof InvocationTargetException) {
            return ((InvocationTargetException)ex).getTargetException();
        }
        return ex;
    }

    private boolean initRenderer() {
        assert(render == null);

        try {
            render = new LWJGLRenderer() {
                @Override
                protected boolean isMouseInsideWindow() {
                    return mouseInside;
                }
                @Override
                protected void setNativeCursor(Cursor cursor) {
                }
            };
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
            try {
                ThemeManager newTheme = ThemeManager.createThemeManager(url, render, newCacheContext);
                testGUI.applyTheme(newTheme);

                oldCacheContext.destroy();
                if(theme != null) {
                    theme.destroy();
                }

                theme = newTheme;
                testGUI.destroy();
                messageLog.removeAll(CAT_THEME);
                
                if(callback != null) {
                    final Callback cb = callback;
                    gui.invokeLater(new Runnable() {
                        public void run() {
                            cb.themeLoaded();
                        }
                    });
                }
                
                return true;
            } catch (IOException ex) {
                hadThemeLoadError = true;
                errorLocation = tracker.findErrorLocation();
                render.setActiveCacheContext(oldCacheContext);
                newCacheContext.destroy();
                messageLog.add(new MessageLog.Entry(CAT_THEME, "Exception while loading theme", null,
                        (ex.getCause() instanceof XmlPullParserException) ? ex.getCause() : ex));
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
        if(evt.isMouseEvent()) {
            mouseInside = evt.getType() != Event.Type.MOUSE_EXITED;
        }
        if(testGUI != null) {
            boolean handled = false;
            try {
                switch (evt.getType()) {
                    case MOUSE_MOVED:
                    case MOUSE_DRAGGED:
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
                        if(evt.getKeyCode() != Event.KEY_F5) {
                            handled = true;
                            testGUI.handleKey(translateKeyCode(evt.getKeyCode()),
                                    evt.getKeyChar(), evt.getType() == Event.Type.KEY_PRESSED);
                        }
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
        if(keyCode == Event.KEY_F6) {
            return Event.KEY_TAB;
        }
        return keyCode;
    }
}
