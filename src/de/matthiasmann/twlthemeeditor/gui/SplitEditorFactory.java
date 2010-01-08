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

import de.matthiasmann.twl.Widget;
import de.matthiasmann.twlthemeeditor.datamodel.Split;
import de.matthiasmann.twlthemeeditor.datamodel.SplitWithLimit;

/**
 *
 * @author Matthias Mann
 */
public class SplitEditorFactory implements PropertyEditorFactory<Split>{

    public Widget create(PropertyAccessor<Split> pa) {
        return new SplitEditor(pa);
    }

    static class SplitEditor extends IntegerArrayEditor {
        private final PropertyAccessor<Split> pa;
        private int maxValue = Short.MAX_VALUE;

        public SplitEditor(PropertyAccessor<Split> pa) {
            this.pa = pa;
            init(pa.getValue(new Split(0)).getSplits());
        }

        protected void updateMaxValue() {
            Split limit = pa.getValue(null);
            if(limit instanceof SplitWithLimit) {
                maxValue = ((SplitWithLimit)limit).getLimit();
            }
        }

        @Override
        protected int getMaxValue(int idx) {
            return maxValue;
        }

        @Override
        protected boolean isValid(int[] array) {
            updateMaxValue();
            if(array.length < 1) {
                errorMessage = "Need atleast 1 split entry";
                return false;
            }
            int last = 0;
            for(int pos : array) {
                if(pos < last) {
                    errorMessage = "Values must be monotonically increasing";
                    return false;
                }
                last = pos;
            }
            if(last > maxValue) {
                errorMessage = "split values outside range";
                return false;
            }
            return true;
        }

        @Override
        protected void updateProperty() {
            pa.setValue(new Split(array));
        }

    }
}