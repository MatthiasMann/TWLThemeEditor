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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.DialogLayout.Gap;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AbstractIntegerModel;
import de.matthiasmann.twlthemeeditor.properties.GapProperty;

/**
 *
 * @author Matthias Mann
 */
public class GapEditorFactory implements PropertyEditorFactory<Gap, GapProperty> {

    public Widget create(PropertyAccessor<Gap, GapProperty> pa) {
        return new GapEditor(pa);
    }

    static class GapEditor extends DialogLayout {
        private static final Gap DEFAULT_GAP_VALUE = new Gap();

        private final PropertyAccessor<Gap, GapProperty> pa;
        private final GapIntegerModel modelMin;
        private final GapIntegerModel modelPref;
        private final GapIntegerModel modelMax;
        private Gap gap;

        public GapEditor(PropertyAccessor<Gap, GapProperty> pa) {
            this.pa = pa;
            this.gap = pa.getValue(DEFAULT_GAP_VALUE);

            this.modelMin = new GapIntegerModel() {
                public int getValue() {
                    return gap.min;
                }
                public void setValue(int value) {
                    setGap(value, Math.max(value, gap.preferred), Math.max(value, gap.max));
                }
            };
            this.modelPref = new GapIntegerModel() {
                public int getValue() {
                    return gap.preferred;
                }
                public void setValue(int value) {
                    setGap(Math.min(value, gap.min), value, Math.max(value, gap.max));
                }
            };
            this.modelMax = new GapIntegerModel() {
                public int getValue() {
                    return gap.max;
                }
                public void setValue(int value) {
                    setGap(Math.min(value, gap.min), Math.min(value, gap.preferred), value);
                }
            };

            ValueAdjusterInt adjusters[] = new ValueAdjusterInt[] {
                new ValueAdjusterInt(modelMin),
                new ValueAdjusterInt(modelPref),
                new ValueAdjusterInt(modelMax),
            };

            adjusters[0].setDisplayPrefix("Min: ");
            adjusters[1].setDisplayPrefix("Pref: ");
            adjusters[2].setDisplayPrefix("Max: ");

            setHorizontalGroup(createParallelGroup(adjusters));
            setVerticalGroup(createSequentialGroup().addWidgetsWithGap("adjuster", adjusters));
        }

        void setGap(int min, int pref, int max) {
            gap = new Gap(min, pref, max);
            pa.setValue(gap);
            modelMin.fireCallback();
            modelPref.fireCallback();
            modelMax.fireCallback();
        }

        abstract class GapIntegerModel extends AbstractIntegerModel {
            public int getMaxValue() {
                return Short.MAX_VALUE;
            }
            public int getMinValue() {
                return 0;
            }
            void fireCallback() {
                doCallback();
            }
        }
    }
}
