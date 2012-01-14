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
import de.matthiasmann.twl.DialogLayout.Gap;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @author Matthias Mann
 */
public class GapEditorFactory implements PropertyEditorFactory<Gap> {

    public Widget create(Property<Gap> property, ExternalFetaures ef) {
        ValueAdjusterInt adjusters[] = new ValueAdjusterInt[] {
            new ValueAdjusterInt(new GapIntegerModel(property) {
                public int getValue() {
                    return getGap().min;
                }
                public void setValue(int value) {
                    Gap gap = getGap();
                    setGap(value, Math.max(value, gap.preferred), Math.max(value, gap.max));
                }
            }),
            new ValueAdjusterInt(new GapIntegerModel(property) {
                public int getValue() {
                    return getGap().preferred;
                }
                public void setValue(int value) {
                    Gap gap = getGap();
                    setGap(Math.min(value, gap.min), value, Math.max(value, gap.max));
                }
            }),
            new ValueAdjusterInt(new GapIntegerModel(property) {
                public int getValue() {
                    return getGap().max;
                }
                public void setValue(int value) {
                    Gap gap = getGap();
                    setGap(Math.min(value, gap.min), Math.min(value, gap.preferred), value);
                }
            }),
        };

        adjusters[0].setDisplayPrefix("Min: ");
        adjusters[1].setDisplayPrefix("Pref: ");
        adjusters[2].setDisplayPrefix("Max: ");

        DialogLayout l = new DialogLayout();
        l.setTheme("gapeditor");
        l.setHorizontalGroup(l.createParallelGroup(adjusters));
        l.setVerticalGroup(l.createSequentialGroup().addWidgetsWithGap("adjuster", adjusters));
        return l;
    }
    
    static abstract class GapIntegerModel implements IntegerModel {
        static final Gap DEFAULT_GAP_VALUE = new Gap();
        final Property<Gap> property;

        GapIntegerModel(Property<Gap> property) {
            this.property = property;
        }
        public int getMinValue() {
            return 0;
        }
        public int getMaxValue() {
            return Short.MAX_VALUE;
        }
        final Gap getGap() {
            Gap gap = property.getPropertyValue();
            if(gap == null) {
                gap = DEFAULT_GAP_VALUE;
            }
            return gap;
        }
        final void setGap(int min, int pref, int max) {
            property.setPropertyValue(new Gap(min, pref, max));
        }
        public void addCallback(Runnable cb) {
            property.addValueChangedCallback(cb);
        }
        public void removeCallback(Runnable cb) {
            property.removeValueChangedCallback(cb);
        }
    }
}
