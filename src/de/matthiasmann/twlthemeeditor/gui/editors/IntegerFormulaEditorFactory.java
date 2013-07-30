/*
 * Copyright (c) 2008-2013, Matthias Mann
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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.IntegerFormula;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @author Matthias Mann
 */
public class IntegerFormulaEditorFactory implements PropertyEditorFactory<IntegerFormula> {

    public Widget create(Property<IntegerFormula> property, ExternalFetaures ef) {
        return new IntegerEditor(property, ef);
    }

    static final class IntegerEditor extends DialogLayout implements EditField.Callback {
        private final Property<IntegerFormula> property;
        private final BooleanModel useFormula;
        private final EditField efFormula;
        private final ValueAdjusterInt adjuster;
        private final Runnable propertyCB;
        
        @SuppressWarnings("LeakingThisInConstructor")
        public IntegerEditor(Property<IntegerFormula> property, ExternalFetaures ef) {
            this.property = property;
            this.propertyCB = new Runnable() {
                public void run() {
                    setEnabled();
                    propertyChanged();
                }
            };

            ef.disableOnNotPresent(this);

            adjuster = new ValueAdjusterInt(new IM());
            useFormula = new UFM();
            
            ToggleButton btnUseFormula = new ToggleButton(useFormula);
            btnUseFormula.setTheme("btnUseFormula");
            
            efFormula = new EditField();
            efFormula.addCallback(this);

            setHorizontalGroup(createParallelGroup()
                .addGroup(createSequentialGroup().addWidget(btnUseFormula).addWidget(efFormula))
                .addWidget(adjuster));
            setVerticalGroup(createSequentialGroup()
                .addGroup(createParallelGroup().addWidget(btnUseFormula).addWidget(efFormula))
                .addWidget(adjuster));
            
            setEnabled();
        }

        @Override
        protected void afterAddToGUI(GUI gui) {
            super.afterAddToGUI(gui);
            property.addValueChangedCallback(propertyCB);
            propertyChanged();
        }

        @Override
        protected void beforeRemoveFromGUI(GUI gui) {
            property.removeValueChangedCallback(propertyCB);
            super.beforeRemoveFromGUI(gui);
        }

        public void callback(int key) {
            IntegerFormula value = property.getPropertyValue();
            if(value.hasFormula()) {
                String formula = efFormula.getText();
                if(!formula.equals(value.getFormula())) {
                    int intValue = adjuster.getValue();
                    try {
                        intValue = Utils.parseInt(formula);
                    } catch (NumberFormatException ex) {
                    }
                    property.setPropertyValue(new IntegerFormula(intValue, formula));
                }
            }
        }

        void propertyChanged() {
            IntegerFormula value = property.getPropertyValue();
            String text = value.toString();
            if(!efFormula.getText().equals(text)) {
                efFormula.setText(text);
            }
        }

        void setEnabled() {
            efFormula.setEnabled(useFormula.getValue());
            adjuster.setEnabled(!useFormula.getValue());
        }
        
        class IM implements IntegerModel {
            public int getMinValue() {
                return (property instanceof IntegerModel)
                        ? ((IntegerModel)property).getMinValue()
                        : Integer.MIN_VALUE;
            }
            public int getMaxValue() {
                return (property instanceof IntegerModel)
                        ? ((IntegerModel)property).getMaxValue()
                        : Integer.MAX_VALUE;
            }
            public int getValue() {
                IntegerFormula value = property.getPropertyValue();
                return (value != null) ? value.getValue() : 0;
            }
            public void setValue(int value) {
                if(!useFormula.getValue()) {
                    property.setPropertyValue(new IntegerFormula(value));
                }
            }
            public void addCallback(Runnable cb) {
                property.addValueChangedCallback(cb);
            }
            public void removeCallback(Runnable cb) {
                property.removeValueChangedCallback(cb);
            }
        }
        class UFM implements BooleanModel {
            public boolean getValue() {
                IntegerFormula value = property.getPropertyValue();
                return (value != null) ? value.hasFormula() : false;
            }
            public void setValue(boolean value) {
                if(value) {
                    property.setPropertyValue(new IntegerFormula(adjuster.getValue(), efFormula.getText()));
                } else {
                    property.setPropertyValue(new IntegerFormula(adjuster.getValue()));
                }
            }
            public void addCallback(Runnable cb) {
                property.addValueChangedCallback(cb);
            }
            public void removeCallback(Runnable cb) {
                property.removeValueChangedCallback(cb);
            }
        }
    }
}
