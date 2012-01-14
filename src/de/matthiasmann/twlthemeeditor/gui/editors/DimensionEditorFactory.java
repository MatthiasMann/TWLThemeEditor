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
package de.matthiasmann.twlthemeeditor.gui.editors;

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @author Matthias Mann
 */
public class DimensionEditorFactory implements PropertyEditorFactory<Dimension> {

    public Widget create(Property<Dimension> property, ExternalFetaures ef) {
        ValueAdjusterInt adjusters[] = new ValueAdjusterInt[] {
            new ValueAdjusterInt(new DimAxisModel(property, true)),
            new ValueAdjusterInt(new DimAxisModel(property, false))
        };

        adjusters[0].setDisplayPrefix("X: ");
        adjusters[1].setDisplayPrefix("Y: ");

        DialogLayout l = new DialogLayout();
        l.setTheme("dimensioneditor");
        l.setHorizontalGroup(l.createParallelGroup(adjusters));
        l.setVerticalGroup(l.createSequentialGroup().addWidgetsWithGap("adjuster", adjusters));
        return l;
    }

    static class DimAxisModel implements IntegerModel {
        private final Property<Dimension> property;
        private final boolean xAxis;

        public DimAxisModel(Property<Dimension> property, boolean xAxis) {
            this.property = property;
            this.xAxis = xAxis;
        }

        public int getMinValue() {
            return 0;
        }

        public int getMaxValue() {
            return Short.MAX_VALUE;
        }

        public int getValue() {
            Dimension dim = property.getPropertyValue();
            return (dim == null) ? 0 : (xAxis ? dim.getX() : dim.getY());
        }

        public void setValue(int value) {
            Dimension dim = property.getPropertyValue();
            int x, y;
            if(dim == null) {
                x = y = 0;
            } else {
                x = dim.getX();
                y = dim.getY();
            }
            if(xAxis) {
                x = value;
            } else {
                y = value;
            }
            if(dim == null || dim.getX() != x || dim.getY() != y) {
                property.setPropertyValue(new Dimension(x, y));
            }
        }

        public void addCallback(Runnable cb) {
            property.addValueChangedCallback(cb);
        }

        public void removeCallback(Runnable cb) {
            property.removeValueChangedCallback(cb);
        }
        
    }
}
