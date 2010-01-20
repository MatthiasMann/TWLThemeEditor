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
package de.matthiasmann.twlthemeeditor.properties;

import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;

/**
 *
 * @author Matthias Mann
 */
public abstract class RectProperty implements Property<Rect> {

    private final IntegerModel baseX;
    private final IntegerModel baseY;
    private final IntegerModel baseW;
    private final IntegerModel baseH;
    private final String name;

    public RectProperty(Property<String> x, Property<String> y, Property<String> width, Property<String> height, String name) {
        this.baseX = new IntegerProperty(x, 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getX() - 1;
            }
            @Override
            public void setValue(int value) {
                if(getValue() != value) {
                    baseW.setValue(Math.min(getLimit().getX() - value, baseW.getValue()));
                    super.setValue(value);
                }
            }
        };
        this.baseY = new IntegerProperty(y, 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getY() - 1;
            }
            @Override
            public void setValue(int value) {
                if(getValue() != value) {
                    baseH.setValue(Math.min(getLimit().getY() - value, baseH.getValue()));
                    super.setValue(value);
                }
            }
        };
        this.baseW = new IntegerProperty(width, 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getX();
            }
            @Override
            public void setValue(int value) {
                if(getValue() != value) {
                    baseX.setValue(Math.min(getLimit().getX() - value, baseX.getValue()));
                    super.setValue(value);
                }
            }
        };
        this.baseH = new IntegerProperty(height, 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getY();
            }
            @Override
            public void setValue(int value) {
                if(getValue() != value) {
                    baseY.setValue(Math.min(getLimit().getY() - value, baseY.getValue()));
                    super.setValue(value);
                }
            }
        };
        this.name = name;
    }

    public RectProperty(IntegerModel baseX, IntegerModel baseY, IntegerModel baseW, IntegerModel baseH, String name) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.baseW = baseW;
        this.baseH = baseH;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class<Rect> getType() {
        return Rect.class;
    }

    public boolean canBeNull() {
        return false;
    }

    public boolean isReadOnly() {
        return false;
    }

    public Rect getPropertyValue() {
        return new Rect(
                baseX.getValue(),
                baseY.getValue(),
                baseW.getValue(),
                baseH.getValue());
    }

    public void setPropertyValue(Rect value) throws IllegalArgumentException {
        baseX.setValue(value.getX());
        baseY.setValue(value.getY());
        baseW.setValue(value.getWidth());
        baseH.setValue(value.getHeight());
    }

    public void addValueChangedCallback(Runnable cb) {
        baseX.addCallback(cb);
        baseY.addCallback(cb);
        baseW.addCallback(cb);
        baseH.addCallback(cb);
    }

    public void removeValueChangedCallback(Runnable cb) {
        baseX.removeCallback(cb);
        baseY.removeCallback(cb);
        baseW.removeCallback(cb);
        baseH.removeCallback(cb);
    }

    public IntegerModel getXProperty() {
        return baseX;
    }

    public IntegerModel getYProperty() {
        return baseY;
    }

    public IntegerModel getWidthProperty() {
        return baseW;
    }

    public IntegerModel getHeightProperty() {
        return baseH;
    }

    public abstract Dimension getLimit();

}
