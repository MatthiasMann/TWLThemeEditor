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

import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.Kind;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;

/**
 *
 * @author Matthias Mann
 */
public abstract class NameProperty extends DerivedProperty<String> {

    private final ThemeTreeModel ttm;
    private final Kind kind;
    private final boolean isTopLevel;

    public NameProperty(Property<String> base, ThemeTreeModel ttm, Kind kind, boolean isTopLevel) {
        super(base, String.class);
        this.ttm = ttm;
        this.kind = kind;
        this.isTopLevel = isTopLevel;
    }

    public String getPropertyValue() {
        return base.getPropertyValue();
    }

    public void setPropertyValue(String value) throws IllegalArgumentException {
        validateName(value);
        String prevName = base.getPropertyValue();
        if(!prevName.equals(value)) {
            if(isTopLevel && ttm != null && kind != null) {
                ttm.handleNodeRenamed(prevName, value, kind);
            }
            base.setPropertyValue(value);
        }
    }

    public abstract void validateName(String name) throws IllegalArgumentException;
    
}
