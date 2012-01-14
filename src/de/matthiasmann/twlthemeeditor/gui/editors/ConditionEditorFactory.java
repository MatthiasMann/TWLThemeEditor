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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twl.utils.StateExpression;
import de.matthiasmann.twlthemeeditor.datamodel.Condition;
import de.matthiasmann.twlthemeeditor.gui.Context;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.StateEditField;
import java.text.ParseException;

/**
 *
 * @author Matthias Mann
 */
public class ConditionEditorFactory implements PropertyEditorFactory<Condition> {

    final Context ctx;

    public ConditionEditorFactory(Context ctx) {
        this.ctx = ctx;
    }

    public Widget create(Property<Condition> property, ExternalFetaures ef) {
        final ConditionModifier cm = new ConditionModifier(ctx, property);

        ToggleButton btnNone = new ToggleButton(cm.new TypeBooleanModel(Condition.Type.NONE));
        btnNone.setTheme("condition-none");

        ToggleButton btnIf = new ToggleButton(cm.new TypeBooleanModel(Condition.Type.IF));
        btnIf.setTheme("condition-if");

        ToggleButton btnUnless = new ToggleButton(cm.new TypeBooleanModel(Condition.Type.UNLESS));
        btnUnless.setTheme("condition-unless");

        DialogLayout panel = new DialogLayout();
        panel.setTheme("conditioneditor");
        Group horzType = panel.createSequentialGroup().addWidgetsWithGap("radiobutton", btnNone, btnIf, btnUnless).addGap();

        panel.setHorizontalGroup(panel.createParallelGroup()
                .addGroup(horzType)
                .addWidget(cm.ef));
        panel.setVerticalGroup(panel.createSequentialGroup()
                .addGroup(panel.createParallelGroup(btnNone, btnIf, btnUnless))
                .addWidget(cm.ef));
        return panel;
    }

    static class ConditionModifier extends HasCallback implements EditField.Callback {
        final Property<Condition> property;
        final EditField ef;
        Condition.Type conditionType;

        @SuppressWarnings("LeakingThisInConstructor")
        protected ConditionModifier(Context ctx, Property<Condition> property) {
            this.property = property;

            Condition condition = property.getPropertyValue();

            conditionType = condition.getType();

            ef = new StateEditField();
            ef.setText(condition.getCondition());
            ef.addCallback(this);
            ef.setAutoCompletion(ctx.collectAllStates());
            
            setEnable();
        }

        boolean setCondition() {
            doCallback();
            setEnable();
            
            String condition = ef.getText();

            if(ef.isLocallyEnabled()) {
                try {
                    StateExpression.parse(condition, false);
                } catch (ParseException ex) {
                    ef.setErrorMessage(ex.getMessage());
                    return false;
                }
            }

            ef.setErrorMessage(null);
            property.setPropertyValue(new Condition(conditionType, condition));
            return true;
        }

        private void setEnable() {
            ef.setEnabled(conditionType != Condition.Type.NONE);
        }

        public void callback(int key) {
            if(setCondition() && key == Event.KEY_RETURN) {
                ef.getAutoCompletionWindow().closeInfo();
            }
        }

        class TypeBooleanModel extends SimpleBooleanModel implements Runnable {
            private final Condition.Type type;

            @SuppressWarnings("LeakingThisInConstructor")
            public TypeBooleanModel(Condition.Type type) {
                this.type = type;
                run();
                ConditionModifier.this.addCallback(this);
            }

            @Override
            public void setValue(boolean value) {
                if(value) {
                    conditionType = type;
                    setCondition();
                }
            }

            public final void run() {
                super.setValue(conditionType == type);
            }
        }
    }
}
