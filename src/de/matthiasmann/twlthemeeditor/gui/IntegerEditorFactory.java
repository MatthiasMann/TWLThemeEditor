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

import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;

/**
 *
 * @author Matthias Mann
 */
public class IntegerEditorFactory implements PropertyEditorFactory<Integer, Property<Integer>> {

    public Widget create(final PropertyAccessor<Integer, Property<Integer>> pa) {
        Property<Integer> property = pa.getProperty();
        ValueAdjusterInt va = new ValueAdjusterInt((property instanceof IntegerModel)
                ? (IntegerModel)property
                : new PropertyIntegerModel(property));
        pa.setWidgetsToEnable(va);

        return va;
    }

    static class PropertyIntegerModel implements IntegerModel {
        final Property<Integer> property;
        public PropertyIntegerModel(Property<Integer> property) {
            this.property = property;
        }
        public void addCallback(Runnable callback) {
            property.addValueChangedCallback(callback);
        }
        public void removeCallback(Runnable callback) {
            property.removeValueChangedCallback(callback);
        }
        public int getValue() {
            return property.getPropertyValue();
        }
        public void setValue(int value) {
            property.setPropertyValue(value);
        }
        public int getMaxValue() {
            return Short.MAX_VALUE;
        }
        public int getMinValue() {
            return Short.MIN_VALUE;
        }
    }
}
