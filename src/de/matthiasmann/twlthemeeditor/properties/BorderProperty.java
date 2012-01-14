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
package de.matthiasmann.twlthemeeditor.properties;

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.BorderFormula;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;

/**
 *
 * @author Matthias Mann
 */
public class BorderProperty extends DerivedProperty<Border> {

    private final int minValue;
    private final boolean allowFormula;

    public BorderProperty(Property<String> base, int minValue, boolean allowFormula) {
        super(base, Border.class, Border.ZERO);
        this.minValue = minValue;
        this.allowFormula = allowFormula;
    }

    @Override
    protected Border parse(String value) throws IllegalArgumentException {
        try {
            return Utils.parseBorder(value);
        } catch (NumberFormatException ex) {
            if(allowFormula) {
                return new BorderFormula(value);
            } else {
                throw ex;
            }
        }
    }

    @Override
    protected String toString(Border value) throws IllegalArgumentException {
        return Utils.toString(value, isOptional());
    }

    public int getMinValue() {
        return minValue;
    }

    public boolean isAllowFormula() {
        return allowFormula;
    }
}
