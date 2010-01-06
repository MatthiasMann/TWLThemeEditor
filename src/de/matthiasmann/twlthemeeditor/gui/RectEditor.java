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

import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.model.AbstractIntegerModel;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twlthemeeditor.datamodel.HasTextureDimensions;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class RectEditor implements PropertyEditorFactory {

    public void create(PropertyPanel panel, Group vert, Object obj, PropertyDescriptor pd, ToggleButton btnActive) {
        RectModifier rm = new RectModifier(obj, pd, ((HasTextureDimensions)obj).getTextureDimensions());

        Label labelX = new Label("X");
        Label labelY = new Label("Y");
        Label labelW = new Label("Width");
        Label labelH = new Label("Height");

        ValueAdjusterInt adjusterX = new ValueAdjusterInt(rm.modelX);
        ValueAdjusterInt adjusterY = new ValueAdjusterInt(rm.modelY);
        ValueAdjusterInt adjusterW = new ValueAdjusterInt(rm.modelWidth);
        ValueAdjusterInt adjusterH = new ValueAdjusterInt(rm.modelHeight);

        labelX.setLabelFor(adjusterX);
        labelY.setLabelFor(adjusterY);
        labelW.setLabelFor(adjusterW);
        labelH.setLabelFor(adjusterH);

        vert.addGroup(panel.createSequentialGroup(
                panel.createParallelGroup(labelX, adjusterX),
                panel.createParallelGroup(labelY, adjusterY),
                panel.createParallelGroup(labelW, adjusterW),
                panel.createParallelGroup(labelH, adjusterH)));
        panel.horzColumns[0].addWidgets(labelX, labelY, labelW, labelH);
        panel.horzColumns[1].addWidgets(adjusterX, adjusterY, adjusterW, adjusterH);
    }

    static abstract class MyIntegerModel extends AbstractIntegerModel {
        protected void fireCallback() {
            super.doCallback();
        }
    }
    
    static class RectModifier extends HasCallback {
        private final Object obj;
        private final PropertyDescriptor pd;
        private final Dimension dim;
        private Rect rect;

        final MyIntegerModel modelX;
        final MyIntegerModel modelY;
        final MyIntegerModel modelWidth;
        final MyIntegerModel modelHeight;
        
        public RectModifier(Object obj, PropertyDescriptor pd, Dimension textureDim) {
            this.obj = obj;
            this.pd = pd;
            this.dim = textureDim;
            try {
                this.rect = (Rect) pd.getReadMethod().invoke(obj);
            } catch (Exception ex) {
                Logger.getLogger(RectEditor.class.getName()).log(Level.SEVERE, null, ex);
            }

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
                    setRect(rect.getX(), rect.getY(), width, rect.getHeight());
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
                    setRect(rect.getX(), rect.getY(), rect.getWidth(), height);
                }
            };
        }

        void setRect(int x, int y, int width, int height) {
            rect = new Rect(x, y, width, height);
            try {
                pd.getWriteMethod().invoke(obj, rect);
            } catch (Exception ex) {
                Logger.getLogger(RectEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
            doCallback();
            modelX.fireCallback();
            modelY.fireCallback();
            modelWidth.fireCallback();
            modelHeight.fireCallback();
        }
    }
}
