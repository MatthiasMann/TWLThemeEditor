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

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;

/**
 *
 * @author Matthias Mann
 */
public class NameEditorFactory implements PropertyEditorFactory<String> {

    public Widget create(final Property<String> property, ExternalFetaures ef) {
        return new NameEditor(property, ef);
    }

    static final class NameEditor extends DialogLayout implements EditField.Callback, Runnable {
        final Property<String> property;
        final Button applyBtn;
        final EditField editfield;
        final Runnable propertyCB;

        @SuppressWarnings("LeakingThisInConstructor")
        public NameEditor(Property<String> property, ExternalFetaures ef) {
            this.property = property;
            this.applyBtn = new Button();
            this.editfield = new EditField();
            this.propertyCB = new Runnable() {
                public void run() {
                    propertyChanged();
                }
            };
            
            applyBtn.setTheme("applybutton");
            applyBtn.addCallback(this);
            applyBtn.setEnabled(false);

            editfield.addCallback(this);
            
            ef.setFocusWidgetCB(new Runnable() {
                public void run() {
                    if(!editfield.hasKeyboardFocus()) {
                        if(editfield.requestKeyboardFocus()) {
                            editfield.selectAll();
                        }
                    }
                }
            });
            
            setHorizontalGroup(createSequentialGroup(editfield, applyBtn));
            setVerticalGroup(createParallelGroup(editfield, applyBtn));
            
            propertyChanged();
        }
        
        void propertyChanged() {
            String value = property.getPropertyValue();
            if(!Utils.equals(editfield.getText(), value)) {
                editfield.setText(value);
            }
        }
        
        public void run() {
            try {
                property.setPropertyValue(editfield.getText());
                editfield.setErrorMessage(null);
                applyBtn.setEnabled(false);
            } catch(IllegalArgumentException ex) {
                editfield.setErrorMessage(ex.getMessage());
            }
        }
        
        public void callback(int key) {
            if(key == Event.KEY_RETURN) {
                if(applyBtn.isEnabled()) {
                    run();
                }
            } else {
                String name = editfield.getText();
                try {
                    if(property instanceof NameProperty) {
                        ((NameProperty)property).validateName(name);
                    }
                    editfield.setErrorMessage(null);
                    applyBtn.setEnabled(!Utils.equals(property.getPropertyValue(), name));
                } catch(IllegalArgumentException ex) {
                    editfield.setErrorMessage(ex.getMessage());
                    applyBtn.setEnabled(false);
                }
            }
        }
    }
}
