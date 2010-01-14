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
import de.matthiasmann.twlthemeeditor.datamodel.Weights;
import de.matthiasmann.twlthemeeditor.properties.WeightsProperty;

/**
 *
 * @author Matthias Mann
 */
public class WeightsEditorFactory implements PropertyEditorFactory<Weights, WeightsProperty> {

    private static final Weights DEFAULT_WEIGHTS = new Weights(1);
    
    public Widget create(final PropertyAccessor<Weights, WeightsProperty> pa) {
        return new WeightsEditor(pa);
    }

    class WeightsEditor extends IntegerArrayEditor {
        private final PropertyAccessor<Weights, WeightsProperty> pa;

        public WeightsEditor(PropertyAccessor<Weights, WeightsProperty> pa) {
            this.pa = pa;
            init(pa.getValue(DEFAULT_WEIGHTS).getWeights());
        }

        @Override
        protected void updateProperty() {
            pa.setValue(new Weights(array));
        }

        @Override
        protected boolean isValid(int[] array) {
            int sum = 0;
            for (int i : array) {
                if (i < 0) {
                    errorMessage = "No negative weights allowed";
                    return false;
                }
                sum += i;
            }
            if (sum > 0) {
                errorMessage = null;
                return true;
            } else {
                errorMessage = "Sum of all weights must be >= 1";
                return false;
            }
        }

        @Override
        protected int getNewValueForAppend(int[] array) {
            return 1;
        }

        @Override
        public void setWidgetsToEnable(Widget... widgetsToEnable) {
            pa.setWidgetsToEnable(widgetsToEnable);
        }
    }

}
