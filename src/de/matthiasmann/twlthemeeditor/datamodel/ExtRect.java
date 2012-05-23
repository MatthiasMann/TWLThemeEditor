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
package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.utils.WithRunnableCallback;

/**
 *
 * @author Matthias Mann
 */
public final class ExtRect {
    
    public final int x;
    public final int y;
    public final int width;
    public final int height;
    public final boolean wholeArea;
    public final boolean flipX;
    public final boolean flipY;

    public ExtRect(int x, int y, int width, int height, boolean wholeArea, boolean flipX, boolean flipY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.wholeArea = wholeArea;
        this.flipX = flipX;
        this.flipY = flipY;
    }

    public ExtRect(int x, int y, int width, int height) {
        this(x, y, width, height, false, false, false);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ExtRect)) {
            return false;
        }
        final ExtRect other = (ExtRect)obj;
        return (this.x == other.x) && (this.y == other.y) &&
                (this.width == other.width) &&
                (this.height == other.height) &&
                (this.wholeArea == other.wholeArea) &&
                (this.flipX == other.flipX) &&
                (this.flipY == other.flipY);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.x;
        hash = 53 * hash + this.y;
        hash = 53 * hash + this.width;
        hash = 53 * hash + this.height;
        hash = 53 * hash + (this.wholeArea ? 1 : 0);
        hash = 53 * hash + (this.flipX ? 1 : 0);
        hash = 53 * hash + (this.flipY ? 1 : 0);
        return hash;
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }
    
    public int getRight() {
        return x + width;
    }
    
    public int getBottom() {
        return y + height;
    }

    public int getCenterX() {
        return x + width/2;
    }
    
    public int getCenterY() {
        return y + height/2;
    }
    
    public abstract static class AbstractAction implements Runnable {
        private final String name;
        private final String tooltip;

        public AbstractAction(String name) {
            this.name = name;
            this.tooltip = null;
        }

        public AbstractAction(String name, String tooltip) {
            this.name = name;
            this.tooltip = tooltip;
        }

        public String getName() {
            return name;
        }

        public String getTooltip() {
            return tooltip;
        }
    }
    
    public interface ExtRectProperty {
        public Dimension getLimit();
        public Dimension getOffset();
        public boolean supportsWholeArea();
        public boolean supportsFlipping();
        public AbstractAction[] getActions();
    }
    
    public abstract static class M implements WithRunnableCallback {
        static final Dimension NO_LIMIT = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
        final Property<ExtRect> property;
        protected M(Property<ExtRect> property) {
            this.property = property;
        }
        public final ExtRect getRect() {
            return property.getPropertyValue();
        }
        public final void setRect(int x, int y, int width, int height, boolean wholeArea, boolean flipX, boolean flipY) {
            property.setPropertyValue(new ExtRect(x, y, width, height, wholeArea, flipX, flipY));
        }
        public final Dimension getLimit() {
            if(property instanceof ExtRectProperty) {
                Dimension dim = ((ExtRectProperty)property).getLimit();
                if(dim != null) {
                    return dim;
                }
            }
            return NO_LIMIT;
        }
        public final Dimension getOffset() {
            if(property instanceof ExtRectProperty) {
                Dimension off = ((ExtRectProperty)property).getOffset();
                if(off != null) {
                    return off;
                }
            }
            return Dimension.ZERO;
        }
        public final int getMaxX() {
            return getOffset().getX() + getLimit().getX();
        }
        public final int getMaxY() {
            return getOffset().getY() + getLimit().getY();
        }
        public void addCallback(Runnable cb) {
            property.addValueChangedCallback(cb);
        }
        public void removeCallback(Runnable cb) {
            property.removeValueChangedCallback(cb);
        }
    }
    public abstract static class IM extends M implements IntegerModel {
        protected IM(Property<ExtRect> property) {
            super(property);
        }
        public int getMinValue() {
            return 0;
        }
    }
    public abstract static class BM extends M implements BooleanModel {
        protected BM(Property<ExtRect> property) {
            super(property);
        }
    }
}
