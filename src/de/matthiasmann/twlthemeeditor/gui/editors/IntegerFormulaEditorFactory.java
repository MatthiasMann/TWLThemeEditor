/*
 * Copyright (c) 2008-2011, Matthias Mann
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
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twlthemeeditor.datamodel.IntegerFormula;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @author Matthias Mann
 */
public class IntegerFormulaEditorFactory implements PropertyEditorFactory<IntegerFormula, Property<IntegerFormula>> {

    public Widget create(PropertyAccessor<IntegerFormula, Property<IntegerFormula>> pa) {
        return new IntegerEditor(pa);
    }

    static final class IntegerEditor extends DialogLayout implements Runnable, EditField.Callback {
        private final PropertyAccessor<IntegerFormula, Property<IntegerFormula>> pa;
        private final SimpleBooleanModel useFormula;
        private final EditField efFormula;
        private final SimpleIntegerModel model;
        private final ValueAdjusterInt adjuster;

        boolean inSetProperty;
        
        @SuppressWarnings("LeakingThisInConstructor")
        public IntegerEditor(PropertyAccessor<IntegerFormula, Property<IntegerFormula>> pa) {
            this.pa = pa;

            Property<IntegerFormula> property = pa.getProperty();

            int minValue = Integer.MIN_VALUE;
            int maxValue = Integer.MAX_VALUE;

            if(property instanceof IntegerModel) {
                IntegerModel im = (IntegerModel)property;
                minValue = im.getMinValue();
                maxValue = im.getMaxValue();
            }

            pa.setWidgetsToEnable(this);

            IntegerFormula value = pa.getValue(new IntegerFormula(
                    (minValue < 0 && maxValue >= 0) ? 0 : minValue));

            model = new SimpleIntegerModel(
                    minValue, maxValue, value.getValue());
            model.addCallback(this);
            
            adjuster = new ValueAdjusterInt(model);

            useFormula = new SimpleBooleanModel(value.hasFormula());
            useFormula.addCallback(new Runnable() {
                public void run() {
                    setEnabled();
                    setProperty();
                }
            });

            ToggleButton btnUseFormula = new ToggleButton(useFormula);
            btnUseFormula.setTheme("btnUseFormula");
            efFormula = new EditField();
            efFormula.setText(value.toString());
            efFormula.addCallback(this);

            setHorizontalGroup(createParallelGroup()
                .addGroup(createSequentialGroup().addWidget(btnUseFormula).addWidget(efFormula))
                .addWidget(adjuster));
            setVerticalGroup(createSequentialGroup()
                .addGroup(createParallelGroup().addWidget(btnUseFormula).addWidget(efFormula))
                .addWidget(adjuster));
            
            setEnabled();
        }

        public void run() {
            setProperty();
        }

        public void callback(int key) {
            if(!inSetProperty) {
                setProperty();
            }
        }

        void setProperty() {
            try {
                inSetProperty = true;
                IntegerFormula value;

                if(useFormula.getValue()) {
                    String formula = efFormula.getText();
                    try {
                        int intValue = Utils.parseInt(formula);
                        model.setValue(intValue);
                    } catch (NumberFormatException ex) {
                    }
                    value = new IntegerFormula(model.getValue(), formula);
                } else {
                    value = new IntegerFormula(model.getValue());

                    try {
                        // if the current formula is a parseable border then update it
                        Utils.parseInt(efFormula.getText());
                        efFormula.setText(value.toString());
                    } catch (NumberFormatException ex) {
                    }
                }
                pa.setValue(value);
            } finally {
                inSetProperty = false;
            }
        }

        void setEnabled() {
            efFormula.setEnabled(useFormula.getValue());
            adjuster.setEnabled(!useFormula.getValue());
        }
    }
}
