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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.PropertyAccessor;
import org.lwjgl.input.Keyboard;

/**
 *
 * @author Matthias Mann
 */
public class NameEditorFactory implements PropertyEditorFactory<String, NameProperty> {

    public Widget create(final PropertyAccessor<String, NameProperty> pa) {
        final Button applyBtn = new Button();
        final EditField ef = new EditField();

        final Runnable applyCB = new Runnable() {
            public void run() {
                try {
                    pa.setValue(ef.getText());
                    ef.setErrorMessage(null);
                    applyBtn.setEnabled(false);
                } catch(IllegalArgumentException ex) {
                    ef.setErrorMessage(ex.getMessage());
                }
            }
        };

        applyBtn.setTheme("applybutton");
        applyBtn.addCallback(applyCB);
        applyBtn.setEnabled(false);

        ef.setText(pa.getValue(""));
        ef.addCallback(new EditField.Callback() {
            public void callback(int key) {
                if(key == Keyboard.KEY_RETURN) {
                    applyCB.run();
                } else {
                    String name = ef.getText();
                    try {
                        pa.getProperty().validateName(name);
                        ef.setErrorMessage(null);
                        applyBtn.setEnabled(!pa.getValue("").equals(name));
                    } catch(IllegalArgumentException ex) {
                        ef.setErrorMessage(ex.getMessage());
                        applyBtn.setEnabled(false);
                    }
                }
            }
        });

        DialogLayout l = new DialogLayout();
        l.setTheme("nameeditor");
        l.setHorizontalGroup(l.createSequentialGroup(ef, applyBtn));
        l.setVerticalGroup(l.createParallelGroup(ef, applyBtn));
        return l;
    }

}
