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

import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.SpecialPropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.HasTooltip;

/**
 *
 * @author Matthias Mann
 */
public class BooleanEditorFactory implements
        PropertyEditorFactory<Boolean>,
        SpecialPropertyEditorFactory<Boolean> {

    public Widget create(Property<Boolean> property, ExternalFetaures ef) {
        return createToggleButton(property, "boolean");
    }

    public boolean createSpecial(Group horz, Group vert, Property<Boolean> property) {
        ToggleButton btn = createToggleButton(property, "checkbox");
        horz.addWidget(btn);
        vert.addWidget(btn);
        return true;
    }

    private ToggleButton createToggleButton(Property<Boolean> property, String theme) {
        ToggleButton btn = new ToggleButton((property instanceof BooleanModel)
                ? (BooleanModel)property
                : new PropertyBooleanModel(property));
        btn.setText(property.getName());
        btn.setTheme(theme);
        if(property instanceof HasTooltip) {
            btn.setTooltipContent(((HasTooltip)property).getTooltip());
        }
        return btn;
    }

    static class PropertyBooleanModel implements BooleanModel {
        final Property<Boolean> property;
        public PropertyBooleanModel(Property<Boolean> property) {
            this.property = property;
        }
        public void addCallback(Runnable callback) {
            property.addValueChangedCallback(callback);
        }
        public void removeCallback(Runnable callback) {
            property.removeValueChangedCallback(callback);
        }
        public boolean getValue() {
            return property.getPropertyValue();
        }
        public void setValue(boolean value) {
            property.setPropertyValue(value);
        }
    }
}
