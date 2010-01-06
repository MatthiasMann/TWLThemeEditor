/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import de.matthiasmann.twlthemeeditor.TestEnv;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author MannMat
 */
public class PreviewWidget extends Widget {

    private TestEnv testEnv;
    private LWJGLRenderer render;
    private ThemeManager theme;
    private GUI testGUI;
    private boolean reloadTheme;
    private ResizableFrame frame;

    public PreviewWidget(TestEnv testEnv) throws LWJGLException {
        this.testEnv = testEnv;
    }

    public void reloadTheme() {
        reloadTheme = true;
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
        GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
        GL11.glViewport(getInnerX(), Display.getDisplayMode().getHeight() -
                (getInnerY() + getInnerHeight()), getInnerWidth(), getInnerHeight());

        try {
            if(render == null) {
                try {
                    render = new LWJGLRenderer();
                    render.setUseSWMouseCursors(true);
                } catch (LWJGLException ex) {
                    Logger.getLogger(PreviewWidget.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            }
            render.syncViewportSize();
            if(testGUI == null) {
                testGUI = new GUI(createRootPane(), render);
            }
            if(theme == null || reloadTheme) {
                CacheContext oldCacheContext = render.getActiveCacheContext();
                render.setActiveCacheContext(render.createNewCacheContext());
                try {
                    ThemeManager newTheme = ThemeManager.createThemeManager(testEnv.getURL("/theme.xml"), render);
                    testGUI.applyTheme(newTheme);

                    oldCacheContext.destroy();
                    if(theme != null) {
                        theme.destroy();
                    }
                    theme = newTheme;
                    testGUI.destroy();
                    reloadTheme = false;
                } catch (IOException ex) {
                    Logger.getLogger(PreviewWidget.class.getName()).log(Level.SEVERE, null, ex);
                }

                if(frame != null) {
                    frame.adjustSize();
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
        } finally {
            GL11.glPopAttrib();
        }
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
                    testGUI.handleKey(evt.getKeyCode(), evt.getKeyChar(), false);
                    return true;

                case KEY_RELEASED:
                    testGUI.handleKey(evt.getKeyCode(), evt.getKeyChar(), true);
                    return true;
            }
        }
        return super.handleEvent(evt);
    }

}
