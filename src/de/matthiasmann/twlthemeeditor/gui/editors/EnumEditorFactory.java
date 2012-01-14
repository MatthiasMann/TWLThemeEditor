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

import de.matthiasmann.twl.ComboBox;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.ListSelectionModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twl.model.SimpleListSelectionModel;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @param <E> the enum type
 * @author Matthias Mann
 */
public class EnumEditorFactory<E extends Enum<E>> implements PropertyEditorFactory<E> {

    public Widget create(Property<E> property, ExternalFetaures ef) {
        ListSelectionModel<E> model = new LSM<E>(property);
        ComboBox<E> comboBox = new ComboBox<E>(model);
        ef.disableOnNotPresent(comboBox);
        return comboBox;
    }
    
    static final class LSM<E extends Enum<E>> extends SimpleListSelectionModel<E> {
        final Property<E> property;
        final Runnable propertyCB;
        
        LSM(Property<E> property) {
            super(new SimpleChangableListModel<E>(property.getType().getEnumConstants()));
            this.property = property;
            this.propertyCB = new Runnable() {
                public void run() {
                    syncFromProperty();
                }
            };
            syncFromProperty();
        }

        @Override
        public void setValue(int value) {
            super.setValue(value);
            property.setPropertyValue(getSelectedEntry());
        }

        @Override
        public void addCallback(Runnable cb) {
            boolean hadCallbacks = hasCallbacks();
            super.addCallback(cb);
            if(!hadCallbacks) {
                property.addValueChangedCallback(propertyCB);
                syncFromProperty();
            }
        }

        @Override
        public void removeCallback(Runnable cb) {
            super.removeCallback(cb);
            if(!hasCallbacks()) {
                property.removeValueChangedCallback(propertyCB);
            }
        }

        void syncFromProperty() {
            super.setValue(Utils.find(getListModel(), property.getPropertyValue()));
        }
    }

}
