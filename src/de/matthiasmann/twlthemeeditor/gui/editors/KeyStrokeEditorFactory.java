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

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.KeyStroke;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @author Matthias Mann
 */
public class KeyStrokeEditorFactory implements PropertyEditorFactory<KeyStroke, Property<KeyStroke>> {

    public static final StateKey STATE_ERROR = StateKey.get("error");

    public Widget create(PropertyAccessor<KeyStroke, Property<KeyStroke>> pa) {
        KeyStrokeEditor kse = new KeyStrokeEditor(pa);
        return kse.button;
    }

    static final class KeyStrokeEditor {
        final PropertyAccessor<KeyStroke, Property<KeyStroke>> pa;
        final Button button;

        public KeyStrokeEditor(PropertyAccessor<KeyStroke, Property<KeyStroke>> pa) {
            this.pa = pa;

            button = new Button();
            button.setTheme("keyStrokeEditor");
            button.addCallback(new Runnable() {
                public void run() {
                    editKeyStroke();
                }
            });

            pa.getProperty().addValueChangedCallback(new Runnable() {
                public void run() {
                    valueChanged();
                }
            });

            valueChanged();
        }

        void valueChanged() {
            KeyStroke keyStroke = pa.getProperty().getPropertyValue();
            if(keyStroke == null) {
                button.setText("INVALID");
                button.getAnimationState().setAnimationState(STATE_ERROR, true);
            } else {
                button.setText(keyStroke.getStroke());
                button.getAnimationState().setAnimationState(STATE_ERROR, false);
            }
        }

        void editKeyStroke() {
            KeyStroke keyStroke = pa.getProperty().getPropertyValue();
            final KeyStrokePopup popup = new KeyStrokePopup(button,
                    (keyStroke != null) ? keyStroke.getAction() : "dummy");
            popup.setKeyStroke(keyStroke);

            popup.btnOk.addCallback(new Runnable() {
                public void run() {
                    popup.closePopup();
                    pa.setValue(popup.keyStroke);
                }
            });
            popup.btnCancel.addCallback(new Runnable() {
                public void run() {
                    popup.closePopup();
                }
            });

            popup.openPopupCentered();
            popup.currentStrokeLabel.requestKeyboardFocus();
        }
    }

    static final class KeyStrokePopup extends PopupWindow {
        final Button btnOk;
        final Button btnCancel;
        final Label currentStrokeLabel;
        final String action;
        KeyStroke keyStroke;

        public KeyStrokePopup(Widget owner, String action) {
            super(owner);
            this.action = action;

            btnOk = new Button("Ok");
            btnCancel = new Button("Cancel");
            currentStrokeLabel = new Label();

            Label labelTitle = new Label("Press the desired keystroke");
            labelTitle.setTheme("title");
            labelTitle.setLabelFor(currentStrokeLabel);

            DialogLayout layout = new DialogLayout();
            layout.setTheme("content");
            layout.setHorizontalGroup(layout.createParallelGroup()
                    .addWidget(labelTitle)
                    .addGroup(layout.createSequentialGroup()
                        .addGap("left-msg")
                        .addWidget(currentStrokeLabel)
                        .addGap("msg-right"))
                    .addGroup(layout.createSequentialGroup()
                        .addGap("left-btnOk")
                        .addWidget(btnOk)
                        .addGap("btnOk-btnCancel")
                        .addWidget(btnCancel)
                        .addGap("btnCancel-right")));
            layout.setVerticalGroup(layout.createSequentialGroup()
                    .addWidget(labelTitle)
                    .addGap("title-msg")
                    .addWidget(currentStrokeLabel)
                    .addGap("msg-buttons")
                    .addGroup(layout.createParallelGroup(btnOk, btnCancel)));

            add(layout);
            setCanAcceptKeyboardFocus(true);
            setCloseOnEscape(false);
        }

        @Override
        protected boolean handleEventPopup(Event evt) {
            if(evt.isKeyPressedEvent() && !isModifierKey(evt.getKeyCode())) {
                setKeyStroke(KeyStroke.fromEvent(evt, action));
            }
            return super.handleEventPopup(evt);
        }

        void setKeyStroke(KeyStroke keyStroke) {
            this.keyStroke = keyStroke;
            currentStrokeLabel.setText((keyStroke != null) ? keyStroke.getStroke() : "INVALID");
        }

        private boolean isModifierKey(int keyCode) {
            switch(keyCode) {
                case Event.KEY_LSHIFT:
                case Event.KEY_LMETA:
                case Event.KEY_LCONTROL:
                case Event.KEY_LMENU:
                case Event.KEY_RSHIFT:
                case Event.KEY_RMETA:
                case Event.KEY_RCONTROL:
                case Event.KEY_RMENU:
                    return true;
                default:
                    return false;
            }
        }
    }
}
