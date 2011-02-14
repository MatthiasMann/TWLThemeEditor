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

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twl.model.SimpleIntegerModel;
import de.matthiasmann.twlthemeeditor.datamodel.BorderFormula;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;

/**
 *
 * @author Matthias Mann
 */
public class BorderEditorFactory implements PropertyEditorFactory<Border, BorderProperty> {

    public Widget create(PropertyAccessor<Border, BorderProperty> pa) {
        return new BorderEditor(pa);
    }

    static final class BorderEditor extends DialogLayout implements Runnable, EditField.Callback {
        private final PropertyAccessor<Border, BorderProperty> pa;
        private final SimpleBooleanModel useFormula;
        private final SimpleIntegerModel modelTop;
        private final SimpleIntegerModel modelLeft;
        private final SimpleIntegerModel modelBottom;
        private final SimpleIntegerModel modelRight;
        private final EditField efFormula;
        private final ValueAdjusterInt adjusters[];

        boolean inSetProperty;

        private static final int MAX_BORDER_SIZE = 1000;

        @SuppressWarnings("LeakingThisInConstructor")
        public BorderEditor(PropertyAccessor<Border, BorderProperty> pa) {
            this.pa = pa;

            Border border = pa.getValue(Border.ZERO);

            int minValue = pa.getProperty().getMinValue();
            boolean allowFormula = pa.getProperty().isAllowFormula();

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
                    setProperty();
                }
            });

            Group horz = createParallelGroup();
            Group vert = createSequentialGroup();

            if(allowFormula) {
                ToggleButton btnUseFormula = new ToggleButton(useFormula);
                btnUseFormula.setTheme("btnUseFormula");
                efFormula = new EditField();
                efFormula.setText(Utils.toString(border, false));
                efFormula.addCallback(this);

                horz.addGroup(createSequentialGroup().addWidget(btnUseFormula).addWidget(efFormula));
                vert.addGroup(createParallelGroup().addWidget(btnUseFormula).addWidget(efFormula));
            } else {
                efFormula = null;
            }

            horz.addWidgets(adjusters);
            vert.addWidgetsWithGap("adjuster", adjusters);

            setHorizontalGroup(horz);
            setVerticalGroup(vert);

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
                pa.setValue(border);
            } finally {
                inSetProperty = false;
            }
        }

        void setEnabled() {
            if(efFormula != null) {
                efFormula.setEnabled(useFormula.getValue());
            }
            for(ValueAdjusterInt va : adjusters) {
                va.setEnabled(!useFormula.getValue());
            }
        }
    }
}
