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
package de.matthiasmann.twlthemeeditor.gui.editors;

import de.matthiasmann.twl.ComboBox;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.EnumProperty;

/**
 *
 * @author Matthias Mann
 */
public class EnumEditorFactory<E extends Enum<E>> implements PropertyEditorFactory<E, Property<E>> {

    public Widget create(final PropertyAccessor<E, Property<E>> pa) {
        final Property<E> property = pa.getProperty();
        final SimpleChangableListModel<E> model = new SimpleChangableListModel<E>(
                property.getType().getEnumConstants());
        final ComboBox<E> comboBox = new ComboBox<E>(model);
        final E defaultValue = (property instanceof EnumProperty) ?
                ((EnumProperty<E>)property).getDefaultValue() : null;
        comboBox.setSelected(Utils.find(model, pa.getValue(defaultValue)));
        comboBox.addCallback(new Runnable() {
            public void run() {
                int idx = comboBox.getSelected();
                if(idx >= 0) {
                    pa.setValue(model.getEntry(idx));
                }
            }
        });
        return comboBox;
    }

}
