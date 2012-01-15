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
import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.StringModel;
import de.matthiasmann.twl.utils.WithRunnableCallback;
import de.matthiasmann.twlthemeeditor.datamodel.BorderFormula;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;

/**
 *
 * @author Matthias Mann
 */
public class BorderEditorFactory implements PropertyEditorFactory<Border> {

    static final int MAX_BORDER_SIZE = 1000;

    public Widget create(Property<Border> property, ExternalFetaures ef) {
        int minValue = 0;
        boolean allowFormula = false;

        if(property instanceof BorderProperty) {
            BorderProperty bp = (BorderProperty)property;
            minValue = bp.getMinValue();
            allowFormula = bp.isAllowFormula();
        }

        final ValueAdjusterInt[] adjusters = new ValueAdjusterInt[] {
            new ValueAdjusterInt(new IM(property, minValue) {
                public int getValue() {
                    return getBorder().getTop();
                }
                public void setValue(int value) {
                    Border border = getBorder();
                    property.setPropertyValue(new Border(value, border.getLeft(), border.getBottom(), border.getRight()));
                }
            }),
            new ValueAdjusterInt(new IM(property, minValue) {
                public int getValue() {
                    return getBorder().getLeft();
                }
                public void setValue(int value) {
                    Border border = getBorder();
                    property.setPropertyValue(new Border(border.getTop(), value, border.getBottom(), border.getRight()));
                }
            }),
            new ValueAdjusterInt(new IM(property, minValue) {
                public int getValue() {
                    return getBorder().getBottom();
                }
                public void setValue(int value) {
                    Border border = getBorder();
                    property.setPropertyValue(new Border(border.getTop(), border.getLeft(), value, border.getRight()));
                }
            }),
            new ValueAdjusterInt(new IM(property, minValue) {
                public int getValue() {
                    return getBorder().getRight();
                }
                public void setValue(int value) {
                    Border border = getBorder();
                    property.setPropertyValue(new Border(border.getTop(), border.getLeft(), border.getBottom(), value));
                }
            })
        };

        adjusters[0].setDisplayPrefix("T: ");
        adjusters[1].setDisplayPrefix("L: ");
        adjusters[2].setDisplayPrefix("B: ");
        adjusters[3].setDisplayPrefix("R: ");

        DialogLayout l = new DialogLayout();
        l.setTheme("bordereditor");
        Group horz = l.createParallelGroup();
        Group vert = l.createSequentialGroup();

        if(allowFormula) {
            EditField efFormula = new EditField();
            efFormula.setModel(new FormulaModel(property));

            ToggleButton btnUseFormula = new ToggleButton(new UseFormulaModel(property, adjusters, efFormula));
            btnUseFormula.setTheme("btnUseFormula");

            horz.addGroup(l.createSequentialGroup().addWidget(btnUseFormula).addWidget(efFormula));
            vert.addGroup(l.createParallelGroup().addWidget(btnUseFormula).addWidget(efFormula));
        }

        horz.addWidgets(adjusters);
        vert.addWidgetsWithGap("adjuster", adjusters);

        l.setHorizontalGroup(horz);
        l.setVerticalGroup(vert);
        return l;
    }
    
    static Border parse(String formula) {
        try {
            return Utils.parseBorder(formula);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    static abstract class M implements WithRunnableCallback {
        final Property<Border> property;
        M(Property<Border> property) {
            this.property = property;
        }
        final Border getBorder() {
            Border value = property.getPropertyValue();
            return (value == null) ? Border.ZERO : value;
        }
        public void addCallback(Runnable cb) {
            property.addValueChangedCallback(cb);
        }
        public void removeCallback(Runnable cb) {
            property.removeValueChangedCallback(cb);
        }
    }
    
    static abstract class IM extends M implements IntegerModel {
        final int minValue;
        IM(Property<Border> property, int minValue) {
            super(property);
            this.minValue = minValue;
        }
        public int getMinValue() {
            return minValue;
        }
        public int getMaxValue() {
            return MAX_BORDER_SIZE;
        }
    }
    
    static final class UseFormulaModel extends M implements BooleanModel {
        final ValueAdjusterInt[] adjuster;
        final EditField ef;

        UseFormulaModel(Property<Border> property, ValueAdjusterInt[] adjuster, EditField ef) {
            super(property);
            this.adjuster = adjuster;
            this.ef = ef;
        }
        public boolean getValue() {
            Border value = property.getPropertyValue();
            boolean result = (value instanceof BorderFormula);
            setEnabled(result);
            return result;
        }
        public void setValue(boolean value) {
            Border border = property.getPropertyValue();
            if(!value && (border instanceof BorderFormula)) {
                Border simple = parse(((BorderFormula)border).getFormula());
                if(simple != null) {
                    property.setPropertyValue(simple);
                }
            } else if(value) {
                property.setPropertyValue(new BorderFormula(border, Utils.toString(border, false)));
            }
        }
        void setEnabled(boolean useFormula) {
            for(ValueAdjusterInt va : adjuster) {
                va.setEnabled(!useFormula);
            }
            ef.setEnabled(useFormula);
        }
    }
    
    static final class FormulaModel extends M implements StringModel {
        FormulaModel(Property<Border> property) {
            super(property);
        }
        public String getValue() {
            Border value = property.getPropertyValue();
            if(value instanceof BorderFormula) {
                return ((BorderFormula)value).getFormula();
            }
            return Utils.toString(value, false);
        }
        public void setValue(String value) {
            Border simple = parse(value);
            if(simple != null) {
                property.setPropertyValue(new BorderFormula(simple, value));
            } else {
                property.setPropertyValue(new BorderFormula(value));
            }
        }
    }
}
