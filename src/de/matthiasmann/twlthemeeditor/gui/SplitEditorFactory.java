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
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AbstractIntegerModel;
import de.matthiasmann.twlthemeeditor.datamodel.Split;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;

/**
 *
 * @author Matthias Mann
 */
public class SplitEditorFactory implements PropertyEditorFactory<Split, SplitProperty> {

    public Widget create(PropertyAccessor<Split, SplitProperty> pa) {
        return new SplitEditor(pa);
    }

    static class SplitEditor extends DialogLayout {
        private static final Split DEFAULT_SPLIT = new Split(0, 0);

        private final PropertyAccessor<Split, SplitProperty> pa;
        private final SplitIntegerModel model1;
        private final SplitIntegerModel model2;
        private Split split;

        public SplitEditor(PropertyAccessor<Split, SplitProperty> pa) {
            this.pa = pa;
            this.split = pa.getValue(DEFAULT_SPLIT);

            this.model1 = new SplitIntegerModel() {
                public int getValue() {
                    return split.getSplit1();
                }
                public void setValue(int value) {
                    setSplit(value, Math.max(value, split.getSplit2()));
                }
            };
            this.model2 = new SplitIntegerModel() {
                public int getValue() {
                    return split.getSplit2();
                }
                public void setValue(int value) {
                    setSplit(Math.min(value, split.getSplit1()), value);
                }
            };

            ValueAdjusterInt adjuster1 = new ValueAdjusterInt(model1);
            ValueAdjusterInt adjuster2 = new ValueAdjusterInt(model2);

            pa.setWidgetsToEnable(adjuster1, adjuster2);

            setHorizontalGroup(createParallelGroup(adjuster1, adjuster2));
            setVerticalGroup(createSequentialGroup().addWidgetsWithGap("adjuster", adjuster1, adjuster2));
        }

        void setSplit(int split1, int split2) {
            split = new Split(split1, split2);
            pa.setValue(split);
            model1.fireCallback();
            model2.fireCallback();
        }

        abstract class SplitIntegerModel extends AbstractIntegerModel {
            public int getMaxValue() {
                return pa.getProperty().getLimit();
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
