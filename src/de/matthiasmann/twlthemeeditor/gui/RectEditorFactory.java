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

import de.matthiasmann.twlthemeeditor.properties.PropertyAccessor;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AbstractIntegerModel;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;

/**
 *
 * @author Matthias Mann
 */
public class RectEditorFactory implements PropertyEditorFactory<Rect, RectProperty> {

    private final Context ctx;

    public RectEditorFactory(Context ctx) {
        this.ctx = ctx;
    }

    public Widget create(final PropertyAccessor<Rect, RectProperty> pa) {
        return new RectEditor(ctx, pa);
    }

    static abstract class MyIntegerModel extends AbstractIntegerModel {
        protected void fireCallback() {
            super.doCallback();
        }
    }

    static class RectEditor extends DialogLayout {
        private final Context ctx;
        private final PropertyAccessor<Rect, RectProperty> pa;
        private final Dimension dim;
        private Rect rect;

        final MyIntegerModel modelX;
        final MyIntegerModel modelY;
        final MyIntegerModel modelWidth;
        final MyIntegerModel modelHeight;

        public RectEditor(Context ctx, PropertyAccessor<Rect, RectProperty> pa) {
            this.ctx = ctx;
            this.pa = pa;
            this.rect = pa.getValue(new Rect(0, 0, 1, 1));  // rect is mutable - need to create a new one
            this.dim = pa.getProperty().getLimit();

            this.modelX = new MyIntegerModel() {
                public int getMaxValue() {
                    return dim.getX() - 1;
                }
                public int getMinValue() {
                    return 0;
                }
                public int getValue() {
                    return rect.getX();
                }
                public void setValue(int x) {
                    int width = Math.min(dim.getX() - x, rect.getWidth());
                    setRect(x, rect.getY(), width, rect.getHeight());
                }
            };
            
            this.modelY = new MyIntegerModel() {
                public int getMaxValue() {
                    return dim.getY() - 1;
                }
                public int getMinValue() {
                    return 0;
                }
                public int getValue() {
                    return rect.getY();
                }
                public void setValue(int y) {
                    int height = Math.min(dim.getY() - y, rect.getHeight());
                    setRect(rect.getX(), y, rect.getWidth(), height);
                }
            };
            
            this.modelWidth = new MyIntegerModel() {
                public int getMaxValue() {
                    return dim.getX();
                }
                public int getMinValue() {
                    return 1;
                }
                public int getValue() {
                    return rect.getWidth();
                }
                public void setValue(int width) {
                    int x = Math.min(dim.getX() - width, rect.getX());
                    setRect(x, rect.getY(), width, rect.getHeight());
                }
            };

            this.modelHeight = new MyIntegerModel() {
                public int getMaxValue() {
                    return dim.getY();
                }
                public int getMinValue() {
                    return 1;
                }
                public int getValue() {
                    return rect.getHeight();
                }
                public void setValue(int height) {
                    int y = Math.min(dim.getY() - height, rect.getY());
                    setRect(rect.getX(), y, rect.getWidth(), height);
                }
            };

            ValueAdjusterInt adjusterX = new ValueAdjusterInt(modelX);
            ValueAdjusterInt adjusterY = new ValueAdjusterInt(modelY);
            ValueAdjusterInt adjusterW = new ValueAdjusterInt(modelWidth);
            ValueAdjusterInt adjusterH = new ValueAdjusterInt(modelHeight);

            adjusterX.setTooltipContent("X position");
            adjusterY.setTooltipContent("Y position");
            adjusterW.setTooltipContent("Width");
            adjusterH.setTooltipContent("Height");

            adjusterX.setDisplayPrefix("X: ");
            adjusterY.setDisplayPrefix("Y: ");
            adjusterW.setDisplayPrefix("W: ");
            adjusterH.setDisplayPrefix("H: ");

            setHorizontalGroup(createParallelGroup(adjusterX, adjusterY, adjusterW, adjusterH));
            setVerticalGroup(createSequentialGroup().addWidgetsWithGap("adjuster", adjusterX, adjusterY, adjusterW, adjusterH));
            
            updateTextureViewerPane();
        }

        void setRect(int x, int y, int width, int height) {
            rect = new Rect(x, y, width, height);
            pa.setValue(rect);
            
            modelX.fireCallback();
            modelY.fireCallback();
            modelWidth.fireCallback();
            modelHeight.fireCallback();

            updateTextureViewerPane();
        }

        void updateTextureViewerPane() {
            TextureViewerPane tvp = ctx.getTextureViewerPane();
            if(tvp != null) {
                // rect is mutable, make a copy
                tvp.setRect(new Rect(rect));
            }
        }
    }
}
