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

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.DraggableButton;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.renderer.CacheContext;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.renderer.Renderer;
import de.matthiasmann.twl.renderer.Texture;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class TextureViewer extends DraggableButton {

    public interface MouseOverListener {
        public void mousePosition(int x, int y);
        public void mouseExited();
    }
    
    private URL url;
    private Rect rect;
    private Color tintColor = Color.WHITE;
    private float zoomX;
    private float zoomY;
    private Runnable[] exceptionCallbacks;
    private MouseOverListener mouseOverListener;

    private long lastModified;
    private CacheContext cacheContext;
    private Texture texture;
    private Image image;
    private Image specialImage;
    private IOException loadException;

    private boolean reloadTexture;
    private boolean changeImage;
    private boolean mouseInside;

    public TextureViewer() {
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        if(!Utils.equals(this.url, url) || checkModified()) {
            this.url = url;
            reloadTexture = true;
        }
    }

    public void setRect(Rect rect) {
        this.rect = rect;
        this.specialImage = null;
        changeImage = true;
    }

    public void setTintColor(Color tintColor) {
        if(tintColor == null) {
            throw new NullPointerException();
        }
        this.tintColor = tintColor;
        changeImage = true;
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
        invalidateLayoutTree();
    }

    public void setImage(Image image) {
        this.specialImage = image;
        this.image = image;
        this.rect = (image != null) ? new Rect(0, 0, image.getWidth(), image.getHeight()) : null;
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
        image = null;
        reloadTexture = true;
        loadException = null;
    }

    @Override
    public int getPreferredInnerWidth() {
        return (image != null) ? (int)(image.getWidth() * zoomX) : 0;
    }

    @Override
    public int getPreferredInnerHeight() {
        return (image != null) ? (int)(image.getHeight() * zoomY) : 0;
    }

    @Override
    protected void paintWidget(GUI gui) {
        if(reloadTexture) {
            destroy();

            if(url != null) {
                try {
                    loadTexture(gui.getRenderer());
                } catch (IOException ex) {
                    loadException = ex;
                }
            }

            reloadTexture = false;
            CallbackSupport.fireCallbacks(exceptionCallbacks);
        }

        // render the previous image if the rectangle changes to give layout time to update
        // this prevents jumpy image scaling when the rectangle size is changed
        Image prevImage = image;

        if(specialImage != null) {
            if(image != specialImage) {
                image = specialImage;
                invalidateLayoutTree();
            }
        } else if(texture != null && (image == null || changeImage)) {
            if(rect == null) {
                rect = getTextureRect();
            } else {
                rect = new Rect(rect);
                rect.intersect(getTextureRect());
            }
            image = texture.getImage(rect.getX(), rect.getY(),
                    rect.getWidth(), rect.getHeight(), tintColor, false);

            invalidateLayoutTree();
            changeImage = false;
        } else if(texture == null) {
            invalidateLayoutTree();
        }

        if(prevImage == null) {
            prevImage = image;
        }

        if(prevImage != null) {
            prevImage.draw(getAnimationState(), getInnerX(), getInnerY(), getInnerWidth(), getInnerHeight());
        }
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
                if(evt.getMouseX() >= getInnerX() && evt.getMouseY() >= getInnerY()) {
                    x = (int)((evt.getMouseX() - getInnerX()) / zoomX);
                    y = (int)((evt.getMouseY() - getInnerY()) / zoomY);
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
        
        return super.handleEvent(evt);
    }

}
