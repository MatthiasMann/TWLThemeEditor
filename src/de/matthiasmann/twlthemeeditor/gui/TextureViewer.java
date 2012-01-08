/*
 * Copyright (c) 2008-2012, Matthias Mann
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
import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.DraggableButton;
import de.matthiasmann.twl.DraggableButton.DragListener;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.ThemeInfo;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;
import de.matthiasmann.twl.renderer.CacheContext;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.renderer.OffscreenRenderer;
import de.matthiasmann.twl.renderer.OffscreenSurface;
import de.matthiasmann.twl.renderer.Renderer;
import de.matthiasmann.twl.renderer.Texture;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twleffects.lwjgl.LWJGLOffscreenSurface;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author Matthias Mann
 */
public class TextureViewer extends Widget {

    public interface MouseOverListener {
        public void mousePosition(int x, int y);
        public void mouseExited();
    }

    public interface PositionBarDragListener {
        public void dragStarted(int posBarHorz, int posBarVert);
        public void dragged(int deltaX, int deltaY);
        public void dragStopped();
    }

    public interface TextureLoadedListener {
        public void textureLoaded(URL url, Texture texture);
    }
    
    private enum DragMode {
        NONE,
        IMAGE,
        BARS
    }

    private enum Cursors {
        NONE("mouseCursor"),
        HORZ("mouseCursor.horz"),
        VERT("mouseCursor.vert"),
        BOTH("mouseCursor.both");

        final String cursorName;
        Cursors(String cursorName) {
            this.cursorName = cursorName;
        }
    }

    private final EnumMap<Cursors, MouseCursor> cursors;
    private final AnimationState animationState;
    
    private URL url;
    private Rect rect;
    private Color tintColor = Color.WHITE;
    private float zoomX;
    private float zoomY;
    private int[] positionBarsHorz;
    private int[] positionBarsVert;
    private Runnable[] exceptionCallbacks;
    private MouseOverListener mouseOverListener;
    private DraggableButton.DragListener imageDragListener;
    private PositionBarDragListener positionBarDragListener;
    private TextureLoadedListener textureLoadedListener;

    private long lastModified;
    private CacheContext cacheContext;
    private OffscreenSurface surface;
    private Texture texture;
    private Image image;
    private Image specialImage;
    private IOException loadException;
    private int[] maxNegInset;
    
    private Image imagePositionBarHorz;
    private Image imagePositionBarVert;

    private boolean reloadTexture;
    private boolean changeImage;
    private boolean mouseInside;

    private DragMode dragMode = DragMode.NONE;
    private int dragPosBarHorz;
    private int dragPosBarVert;
    private int dragStartX;
    private int dragStartY;

    public TextureViewer() {
        cursors = new EnumMap<Cursors, MouseCursor>(Cursors.class);
        animationState = new AnimationState();
        maxNegInset = new int[4];
    }

    public AnimationState getTextureAnimationState() {
        return animationState;
    }
    
    public URL getUrl() {
        return url;
    }

    public void setImage(URL url, Rect rect) {
        if(!Utils.equals(this.url, url) || checkModified()) {
            this.url = url;
            reloadTexture = true;
        }
        this.rect = rect;
        this.specialImage = null;
        this.maxNegInset = new int[4];
        changeImage = true;
    }

    public void setImage(Image image) {
        this.specialImage = image;
        this.image = image;
        this.rect = (image != null) ? new Rect(0, 0, image.getWidth(), image.getHeight()) : null;
        this.url = null;
        this.reloadTexture = false;
        this.changeImage = false;
        this.maxNegInset = new int[4];
        
        if(image != null) {
            ArrayList<Image> allImages = new ArrayList<Image>();
            ImageUtil.getAllImages(image, allImages);
            
            BitSet allStateKeys = new BitSet();
            for(Image img : allImages) {
                ImageUtil.getAllStateKeys(img, allStateKeys);
                Border inset = ImageUtil.getInset(img);
                if(inset != null) {
                    maxNegInset[0] = Math.max(maxNegInset[0], Math.max(0, -inset.getTop()));
                    maxNegInset[1] = Math.max(maxNegInset[1], Math.max(0, -inset.getLeft()));
                    maxNegInset[2] = Math.max(maxNegInset[2], Math.max(0, -inset.getRight()));
                    maxNegInset[3] = Math.max(maxNegInset[3], Math.max(0, -inset.getBottom()));
                }
            }
            
            for(int i=-1 ; (i=allStateKeys.nextSetBit(i+1))>=0 ;) {
                StateKey sk = StateKey.get(i);
                if(!animationState.getAnimationState(sk)) {
                    // setting to false causes the state entry to be created
                    animationState.setAnimationState(sk, false);
                }
            }
                
        }
        invalidateLayout();
    }

    public void setTintColor(Color tintColor) {
        if(tintColor == null) {
            throw new NullPointerException();
        }
        this.tintColor = tintColor;
        if(specialImage == null) {
            changeImage = true;
        }
    }

    public Rect getTextureRect() {
        if(texture == null) {
            return new Rect();
        }
        return new Rect(0, 0, texture.getWidth(), texture.getHeight());
    }

    public void setZoom(float zoomX, float zoomY) {
        this.zoomX = zoomX;
        this.zoomY = zoomY;
        invalidateLayout();
    }

    public void setPositionBars(int[] positionBarsHorz, int[] positionBarsVert) {
        this.positionBarsHorz = positionBarsHorz;
        this.positionBarsVert = positionBarsVert;
    }

    public IOException getLoadException() {
        return loadException;
    }

    public void addExceptionCallback(Runnable cb) {
        exceptionCallbacks = CallbackSupport.addCallbackToList(exceptionCallbacks, cb, Runnable.class);
    }

    public void removeExceptionCallback(Runnable cb) {
        exceptionCallbacks = CallbackSupport.removeCallbackFromList(exceptionCallbacks, cb);
    }

    public void setMouseOverListener(MouseOverListener mouseOverListener) {
        this.mouseOverListener = mouseOverListener;
    }

    public void setImageDragListener(DragListener imageDragListener) {
        this.imageDragListener = imageDragListener;
    }

    public void setPositionBarDragListener(PositionBarDragListener positionBarDragListener) {
        this.positionBarDragListener = positionBarDragListener;
    }

    public void setTextureLoadedListener(TextureLoadedListener textureLoadedListener) {
        this.textureLoadedListener = textureLoadedListener;
    }

    @Override
    public void destroy() {
        super.destroy();
        if(texture != null) {
            texture.destroy();
            texture = null;
        }
        if(cacheContext != null) {
            cacheContext.destroy();
            cacheContext = null;
        }
        if(surface != null) {
            surface.destroy();
            surface = null;
        }
        image = null;
        reloadTexture = true;
        loadException = null;
    }

    @Override
    public int getPreferredInnerWidth() {
        int width = (image != null) ? image.getWidth() : 0;
        width += maxNegInset[1] + maxNegInset[3];
        return (int)(width * zoomX);
    }

    @Override
    public int getPreferredInnerHeight() {
        int height = (image != null) ? image.getHeight() : 0;
        height += maxNegInset[0] + maxNegInset[2];
        return (int)(height * zoomY);
    }

    public void validateImage() {
        if(reloadTexture) {
            destroy();

            GUI gui = getGUI();
            if(url != null && gui != null) {
                try {
                    loadTexture(gui.getRenderer());
                } catch (IOException ex) {
                    loadException = ex;
                }
            }

            reloadTexture = false;
            changeImage = true;
            CallbackSupport.fireCallbacks(exceptionCallbacks);
        }
        
        if(changeImage) {
            if(texture != null) {
                if(rect == null) {
                    rect = getTextureRect();
                } else {
                    rect = new Rect(rect);
                    rect.intersect(getTextureRect());
                }
                try {
                    image = texture.getImage(rect.getX(), rect.getY(),
                            rect.getWidth(), rect.getHeight(), tintColor,
                            false, Texture.Rotation.NONE);
                } catch (IllegalArgumentException ex) {
                    image = null;
                }
            } else {
                image = null;
            }

            invalidateLayout();
            changeImage = false;
        }
    }

    @Override
    protected void paintWidget(GUI gui) {
        // render the previous image if the rectangle changes to give layout time to update
        // this prevents jumpy image scaling when the rectangle size is changed
        Image prevImage = image;

        validateImage();
        
        if(prevImage == null) {
            prevImage = image;
        }

        if(prevImage != null) {
            if(prevImage != specialImage || !drawOffscreen(prevImage, gui.getRenderer().getOffscreenRenderer())) {
                prevImage.draw(animationState, getInnerX(), getInnerY(), getInnerWidth(), getInnerHeight());
            }
        }

        if(positionBarsVert != null && imagePositionBarVert != null) {
            for(int x : positionBarsVert) {
                imagePositionBarVert.draw(getAnimationState(),
                        getInnerX() + (int)(x*zoomX), getInnerY(), 1, getInnerHeight());
            }
        }
        if(positionBarsHorz != null && imagePositionBarHorz != null) {
            for(int y : positionBarsHorz) {
                imagePositionBarHorz.draw(getAnimationState(),
                        getInnerX(), getInnerY() + (int)(y*zoomY), getInnerWidth(), 1);
            }
        }
    }
    
    private boolean drawOffscreen(Image img, OffscreenRenderer renderer) {
        if(renderer == null) {
            return false;
        }
        
        int width = img.getWidth() + maxNegInset[1] + maxNegInset[3];
        int height = img.getHeight() + maxNegInset[0] + maxNegInset[2];
        if(width <= 0 || height <= 0) {
            return false;
        }
        
        surface = renderer.startOffscreenRendering(surface, 0, 0, width, height);
        if(surface == null) {
            return false;
        }
        
        try {
            img.draw(animationState, maxNegInset[1], maxNegInset[0]);
        } finally {
            renderer.endOffscreenRendering();
        }
        
        if(surface instanceof LWJGLOffscreenSurface) {
            ((LWJGLOffscreenSurface)surface).setGLFilter(GL11.GL_NEAREST, GL11.GL_NEAREST);
        }
        
        surface.draw(null, getInnerX(), getInnerY(), getInnerWidth(), getInnerHeight());
        return true;
    }

    protected void loadTexture(Renderer renderer) throws IOException {
        try {
            lastModified = getLastModified();
        } catch(IOException ex) {
            lastModified = System.currentTimeMillis();
            Logger.getLogger(TextureViewer.class.getName()).log(Level.SEVERE,
                    "Can't determine last modified date", ex);
        }
        CacheContext prevCacheContext = renderer.getActiveCacheContext();
        cacheContext = renderer.createNewCacheContext();
        renderer.setActiveCacheContext(cacheContext);
        try {
            texture = renderer.loadTexture(url, "RGBA", "NEAREST");
        } finally {
            renderer.setActiveCacheContext(prevCacheContext);
        }
        if(textureLoadedListener != null && texture != null) {
            textureLoadedListener.textureLoaded(url, texture);
        }
    }

    protected long getLastModified() throws IOException {
        URLConnection connection = url.openConnection();
        connection.setAllowUserInteraction(false);
        return connection.getLastModified();
    }

    protected boolean checkModified() {
        try {
            return (url != null) && lastModified != getLastModified();
        } catch(IOException ignore) {
            return false;
        }
    }

    @Override
    public boolean handleEvent(Event evt) {
        if(mouseOverListener != null && evt.isMouseEvent()) {
            boolean isInside = false;
            int x = 0;
            int y = 0;

            if(image != null && evt.getType() != Event.Type.MOUSE_EXITED) {
                int mouseX = evt.getMouseX() - getInnerX();
                int mouseY = evt.getMouseY() - getInnerY();

                if(mouseX >= 0 && mouseY >= 0) {
                    x = (int)(mouseX / zoomX);
                    y = (int)(mouseY / zoomY);
                    isInside = (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight());
                }
            }

            if(isInside) {
                if(rect != null) {
                    x += rect.getX();
                    y += rect.getY();
                }
                mouseOverListener.mousePosition(x, y);
                mouseInside = true;
            } else if(mouseInside) {
                mouseOverListener.mouseExited();
                mouseInside = false;
            }
        }

        if(evt.isMouseEvent() && dragMode != DragMode.NONE) {
            if(evt.getType() == Event.Type.MOUSE_DRAGGED) {
                if(dragMode == DragMode.IMAGE && imageDragListener != null) {
                    imageDragListener.dragged(evt.getMouseX()-dragStartX, evt.getMouseY()-dragStartY);
                }
                if(dragMode == DragMode.BARS && positionBarDragListener != null) {
                    positionBarDragListener.dragged(
                            (int)((evt.getMouseX()-dragStartX) / zoomX),
                            (int)((evt.getMouseY()-dragStartY) / zoomY));
                }
            }
            if(evt.isMouseDragEnd()) {
                if(dragMode == DragMode.IMAGE && imageDragListener != null) {
                    imageDragListener.dragStopped();
                }
                if(dragMode == DragMode.BARS && positionBarDragListener != null) {
                    positionBarDragListener.dragStopped();
                }
                dragMode = DragMode.NONE;
            }
            return true;
        }

        if(super.handleEvent(evt)) {
            return true;
        }

        switch (evt.getType()) {
        case MOUSE_BTNDOWN:
            dragStartX = evt.getMouseX();
            dragStartY = evt.getMouseY();
            break;
        case MOUSE_DRAGGED:
            startDrag();
            return true;
        case MOUSE_MOVED:
            setMouseCursor(cursors.get(selectMouseCursor(evt.getMouseX() - getInnerX(), evt.getMouseY() - getInnerY())));
            return true;
        }

        return evt.isMouseEventNoWheel();
    }

    @Override
    protected void applyTheme(ThemeInfo themeInfo) {
        super.applyTheme(themeInfo);
        imagePositionBarHorz = themeInfo.getImage("positionBarHorz");
        imagePositionBarVert = themeInfo.getImage("positionBarVert");
        for(Cursors cursor : Cursors.values()) {
            cursors.put(cursor, themeInfo.getMouseCursor(cursor.cursorName));
        }
    }

    @Override
    protected void afterAddToGUI(GUI gui) {
        super.afterAddToGUI(gui);
        animationState.setGUI(gui);
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        animationState.setGUI(null);
        super.beforeRemoveFromGUI(gui);
    }

    private void startDrag() {
        assert dragMode == DragMode.NONE;

        dragPosBarHorz = findNearestPosBar(positionBarsHorz, dragStartY - getInnerY(), zoomY);
        dragPosBarVert = findNearestPosBar(positionBarsVert, dragStartX - getInnerX(), zoomX);

        if(dragPosBarHorz >= 0 || dragPosBarVert >= 0) {
            dragMode = DragMode.BARS;
            if(positionBarDragListener != null) {
                positionBarDragListener.dragStarted(dragPosBarHorz, dragPosBarVert);
            }
        } else {
            dragMode = DragMode.IMAGE;
            setMouseCursor(cursors.get(Cursors.BOTH));
            if(imageDragListener != null) {
                imageDragListener.dragStarted();
            }
        }
    }

    private Cursors selectMouseCursor(int x, int y) {
        int barHorz = findNearestPosBar(positionBarsHorz, y, zoomY);
        int barVert = findNearestPosBar(positionBarsVert, x, zoomX);
        if(barHorz >= 0 && barVert >= 0) {
            return Cursors.BOTH;
        } else if(barHorz >= 0) {
            return Cursors.HORZ;
        } else if(barVert >= 0) {
            return Cursors.VERT;
        } else {
            return Cursors.NONE;
        }
    }

    private int findNearestPosBar(int[] posBars, int pos, float zoom) {
        int bestIdx = -1;
        int bestDist = 10;

        if(posBars != null) {
            for(int idx=0 ; idx<posBars.length ; idx++) {
                int dist = Math.abs((int)(posBars[idx] * zoom) - pos);
                if(dist < bestDist) {
                    bestDist = dist;
                    bestIdx  = idx;
                }
            }
        }

        return bestIdx;
    }
}
