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

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twlthemeeditor.datamodel.BorderFormula;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;
import de.matthiasmann.twlthemeeditor.properties.OptionalProperty;

/**
 *
 * @author Matthias Mann
 */
public class BorderEditorFactory implements PropertyEditorFactory<Border> {

    public Widget create(Property<Border> property, ExternalFetaures ef) {
        return new BorderEditor(property);
    }

    static final class BorderEditor extends DialogLayout implements Runnable, EditField.Callback {
        private final Property<Border> property;
        private final SimpleBooleanModel useFormula;
        private final SimpleIntegerModel modelTop;
        private final SimpleIntegerModel modelLeft;
        private final SimpleIntegerModel modelBottom;
        private final SimpleIntegerModel modelRight;
        private final ToggleButton btnUseFormula;
        private final EditField efFormula;
        private final ValueAdjusterInt adjusters[];
        private final Runnable propertyCB;

        boolean inSetProperty;

        private static final int MAX_BORDER_SIZE = 1000;

        @SuppressWarnings("LeakingThisInConstructor")
        public BorderEditor(Property<Border> property) {
            this.property = property;
            
            int minValue = 0;
            boolean allowFormula = false;

            if(property instanceof BorderProperty) {
                BorderProperty bp = (BorderProperty)property;
                minValue = bp.getMinValue();
                allowFormula = bp.isAllowFormula();
            }
            
            Border border = property.getPropertyValue();
            useFormula = new SimpleBooleanModel(border instanceof BorderFormula);
            
            modelTop = new SimpleIntegerModel(minValue, MAX_BORDER_SIZE, border.getBorderTop());
            modelLeft = new SimpleIntegerModel(minValue, MAX_BORDER_SIZE, border.getBorderLeft());
            modelBottom = new SimpleIntegerModel(minValue, MAX_BORDER_SIZE, border.getBorderBottom());
            modelRight = new SimpleIntegerModel(minValue, MAX_BORDER_SIZE, border.getBorderRight());

            adjusters = new ValueAdjusterInt[] {
                new ValueAdjusterInt(modelTop),
                new ValueAdjusterInt(modelLeft),
                new ValueAdjusterInt(modelBottom),
                new ValueAdjusterInt(modelRight)
            };

            adjusters[0].setDisplayPrefix("T: ");
            adjusters[1].setDisplayPrefix("L: ");
            adjusters[2].setDisplayPrefix("B: ");
            adjusters[3].setDisplayPrefix("R: ");

            modelTop.addCallback(this);
            modelLeft.addCallback(this);
            modelBottom.addCallback(this);
            modelRight.addCallback(this);

            useFormula.addCallback(new Runnable() {
                public void run() {
                    setEnabled();
                    setProperty(false);
                }
            });

            Group horz = createParallelGroup();
            Group vert = createSequentialGroup();

            if(allowFormula) {
                btnUseFormula = new ToggleButton(useFormula);
                btnUseFormula.setTheme("btnUseFormula");
                efFormula = new EditField();
                efFormula.setText(Utils.toString(border, false));
                efFormula.addCallback(this);

                horz.addGroup(createSequentialGroup().addWidget(btnUseFormula).addWidget(efFormula));
                vert.addGroup(createParallelGroup().addWidget(btnUseFormula).addWidget(efFormula));
            } else {
                btnUseFormula = null;
                efFormula = null;
            }

            horz.addWidgets(adjusters);
            vert.addWidgetsWithGap("adjuster", adjusters);

            setHorizontalGroup(horz);
            setVerticalGroup(vert);

            setEnabled();
            propertyCB = new Runnable() {
                public void run() {
                    setEnabled();
                    setProperty(true);
                }
            };
        }

        public void run() {
            setEnabled();
            setProperty(false);
        }

        public void callback(int key) {
            if(!inSetProperty) {
                setProperty(false);
            }
        }

        void setProperty(boolean fromProperty) {
            try {
                inSetProperty = true;
                Border border;
                if(useFormula.getValue()) {
                    String formula = efFormula.getText();
                    border = new BorderFormula(formula);
                    try {
                        Border values = Utils.parseBorder(formula);
                        if(values != null) {
                            modelTop.setValue(values.getBorderTop());
                            modelLeft.setValue(values.getBorderLeft());
                            modelBottom.setValue(values.getBorderBottom());
                            modelRight.setValue(values.getBorderRight());
                        }
                    } catch (NumberFormatException ex) {
                    }
                } else {
                    border = new Border(
                        modelTop.getValue(),
                        modelLeft.getValue(),
                        modelBottom.getValue(),
                        modelRight.getValue());

                    if(efFormula != null) {
                        try {
                            // if the current formula is a parseable border then update it
                            if(Utils.parseBorder(efFormula.getText()) != null) {
                                efFormula.setText(Utils.toString(border, false));
                            }
                        } catch (NumberFormatException ex) {
                        }
                    }
                }
                if(!fromProperty) {
                    property.setPropertyValue(border);
                }
            } finally {
                inSetProperty = false;
            }
        }

        void setEnabled() {
            boolean isPresent = true;
            if(property instanceof OptionalProperty<?>) {
                isPresent = ((OptionalProperty<?>)property).isPresent();
            }
            if(efFormula != null) {
                btnUseFormula.setEnabled(isPresent);
                efFormula.setEnabled(isPresent && useFormula.getValue());
            }
            for(ValueAdjusterInt va : adjusters) {
                va.setEnabled(isPresent && !useFormula.getValue());
            }
        }

        @Override
        protected void afterAddToGUI(GUI gui) {
            super.afterAddToGUI(gui);
            property.addValueChangedCallback(propertyCB);
            setProperty(true);
        }

        @Override
        protected void beforeRemoveFromGUI(GUI gui) {
            property.removeValueChangedCallback(propertyCB);
            super.beforeRemoveFromGUI(gui);
        }
    }
}
