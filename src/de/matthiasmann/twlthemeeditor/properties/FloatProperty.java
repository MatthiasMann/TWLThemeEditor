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

import de.matthiasmann.twl.model.FloatModel;
import de.matthiasmann.twl.model.Property;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class FloatProperty extends DerivedProperty<Float> implements FloatModel {

    private final float minValue;
    private final float maxValue;
    private final float defaultValue;

    private float prevValue;

    public FloatProperty(Property<String> base, float minValue, float maxValue, float defaultValue) {
        super(base, Float.class);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        this.prevValue = defaultValue;
    }

    public Float getPropertyValue() {
        String value = base.getPropertyValue();
        if(value == null && canBeNull()) {
            return null;
        }
        try {
            return Float.valueOf(value);
        } catch (Throwable ex) {
            Logger.getLogger(FloatProperty.class.getName()).log(Level.SEVERE,
                    "Can't parse value of propterty '" + getName() + "': " + value, ex);
            return prevValue;
        }
    }

    public void setPropertyValue(Float value) throws IllegalArgumentException {
        if(canBeNull() && (value == null || value == defaultValue)) {
            if(value != null) {
                prevValue = defaultValue;
            }
            base.setPropertyValue(null);
        } else {
            prevValue = value;
            base.setPropertyValue(value.toString());
        }
    }

    public float getMaxValue() {
        return maxValue;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public void setValue(float value) {
       setPropertyValue(value);
    }

    public float getValue() {
        Float value = getPropertyValue();
        return (value != null) ? value : prevValue;
    }

}
