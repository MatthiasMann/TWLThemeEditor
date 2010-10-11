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
package de.matthiasmann.twlthemeeditor.gui.testwidgets;

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Widget;

/**
 * An example formular to show how to use DialogLayout.
 * 
 * @author Matthias Mann
 */
public class TestFormular extends DialogLayout {

    private final EditField nameField;
    private final EditField addressField;
    private final EditField zipField;
    private final EditField cityField;

    public TestFormular() {
        nameField    = new EditField();
        addressField = new EditField();
        zipField     = new EditField();
        cityField    = new EditField();

        // put all widgets in an array for easier handling
        Widget[] widgets = new Widget[] {
            nameField, addressField, zipField, cityField
        };

        // text for the labels - has to be the same order as widgets above
        String[] labels = new String[] {
            "Name", "Address", "ZIP", "City"
        };

        // we need 2 columns, one for the labels and one for the widgets
        Group hLabels = createParallelGroup();
        Group hWidgets = createParallelGroup();

        // we need several rows
        Group vRows = createSequentialGroup();

        // add all widgets
        for(int i=0 ; i<widgets.length ; i++) {
            Label label = new Label(labels[i]);
            label.setLabelFor(widgets[i]);

            hLabels.addWidget(label);
            hWidgets.addWidget(widgets[i]);
            vRows.addGroup(createParallelGroup(label, widgets[i]));
        }

        // the 2 columns should appear besides each other
        // so put them in a sequential group
        setHorizontalGroup(createSequentialGroup(hLabels, hWidgets));
        setVerticalGroup(vRows);
    }
}
