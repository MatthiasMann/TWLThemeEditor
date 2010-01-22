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

import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Timer;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.utils.HashEntry;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.input.Keyboard;

/**
 *
 * @author Matthias Mann
 */
public class AnimStateEditorFactory implements PropertyEditorFactory<AnimationState, Property<AnimationState>>{

    public Widget create(PropertyAccessor<AnimationState, Property<AnimationState>> pa) {
        return new AnimStateEditor(pa.getProperty());
    }

    static class AnimStateEditor extends DialogLayout implements Runnable, EditField.Callback {
        private final AnimationState animationState;
        private final Field stateTableField;
        private final TreeMap<String, ToggleButton> buttons;
        private final ArrayList<StateBooleanModel> models;
        private final EditField stateNameField;
        private final Button addStateNameButton;
        private Timer timer;

        public AnimStateEditor(Property<AnimationState> property) {
            this.animationState = property.getPropertyValue();

            Class<AnimationState> clazz = AnimationState.class;
            stateTableField = getField(clazz, "stateTable");

            buttons = new TreeMap<String, ToggleButton>();
            models = new ArrayList<StateBooleanModel>();

            stateNameField = new EditField();
            stateNameField.addCallback(this);
            addStateNameButton = new Button();
            addStateNameButton.setTheme("addbutton");
            addStateNameButton.addCallback(new Runnable() {
                public void run() {
                    addState();
                }
            });

            createLayout();
        }

        @Override
        protected void afterAddToGUI(GUI gui) {
            super.afterAddToGUI(gui);
            timer = gui.createTimer();
            timer.setCallback(this);
            timer.setContinuous(true);
            timer.setDelay(250);
            timer.start();
        }

        @Override
        protected void beforeRemoveFromGUI(GUI gui) {
            timer.stop();
            super.beforeRemoveFromGUI(gui);
            timer = null;
        }

        @Override
        protected void paint(GUI gui) {
            updateModels();
            super.paint(gui);
        }

        public void run() {
            if(createStateButtons()) {
                createLayout();
            }
        }

        public void callback(int key) {
            addStateNameButton.setEnabled(stateNameField.getTextLength() > 0);
            if(key == Keyboard.KEY_RETURN) {
                addState();
            }
        }

        void addState() {
            String stateName = stateNameField.getText();
            if(stateName.length() > 0) {
                if(createStateButton(stateName)) {
                    createLayout();
                }
            }
        }

        private void createLayout() {
            setHorizontalGroup(null);
            removeAllChildren();
            
            ToggleButton[] btns = buttons.values().toArray(new ToggleButton[buttons.size()]);

            setHorizontalGroup(createParallelGroup()
                    .addWidgets(btns)
                    .addGroup(createSequentialGroup(stateNameField, addStateNameButton)));
            setVerticalGroup(createSequentialGroup()
                    .addWidgetsWithGap("vbutton", btns)
                    .addGroup(createParallelGroup(stateNameField, addStateNameButton)));
        }

        private boolean createStateButtons() {
            boolean redoLayout = false;
            if(stateTableField != null) {
                try {
                    @SuppressWarnings("unchecked")
                    HashEntry<String, ?>[] stateTable = (HashEntry<String, ?>[])stateTableField.get(animationState);
                    for(HashEntry<String, ?> entry : stateTable) {
                         for(; entry != null ; entry=entry.next()) {
                             redoLayout |= createStateButton(entry.key);
                         }
                    }
                } catch(Throwable ex) {
                    Logger.getLogger(AnimStateEditorFactory.class.getName()).log(
                            Level.SEVERE, "Can't access state table", ex);
                }
            }
            return redoLayout;
        }

        private boolean createStateButton(final String state) {
            ToggleButton btn = buttons.get(state);
            if(btn == null) {
                StateBooleanModel model = new StateBooleanModel(animationState, state);
                btn = new ToggleButton(model);
                btn.setTheme("statebutton");
                btn.setText(state);
                buttons.put(state, btn);
                models.add(model);
                return true;
            }
            return false;
        }

        private void updateModels() {
            for(int i = 0, n = models.size(); i < n; i++) {
                models.get(i).update();
            }
        }

        private static Field getField(Class<?> clazz, String name) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch(Throwable ex) {
                Logger.getLogger(AnimStateEditorFactory.class.getName()).log(
                        Level.SEVERE, "Can't access field", ex);
                return null;
            }
        }

        static class StateBooleanModel extends HasCallback implements BooleanModel {
            private final AnimationState animationState;
            private final String stateName;
            private boolean lastState;

            public StateBooleanModel(AnimationState animationState, String stateName) {
                this.animationState = animationState;
                this.stateName = stateName;
            }

            public boolean getValue() {
                return lastState;
            }

            public void setValue(boolean value) {
                animationState.setAnimationState(stateName, value);
            }

            public void update() {
                boolean state = animationState.getAnimationState(stateName);
                if(lastState != state) {
                    lastState = state;
                    doCallback();
                }
            }
        }
    }
}
