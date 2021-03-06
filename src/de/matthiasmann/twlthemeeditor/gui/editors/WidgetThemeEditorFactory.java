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

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.ComboBox;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.Context;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.WidgetThemeProperty;

/**
 *
 * @author Matthias Mann
 */
public class WidgetThemeEditorFactory implements PropertyEditorFactory<String> {

    private final Context ctx;

    public WidgetThemeEditorFactory(Context ctx) {
        this.ctx = ctx;
    }

    public Widget create(Property<String> prop, ExternalFetaures ef) {
        final WidgetThemeProperty property = (WidgetThemeProperty)prop;
        final ListModel<String> refableNodes = ctx.getRefableThemes(
                property.getThemePathElements(1));
        final ComboBox<String> cb = new ComboBox<String>(refableNodes);
        final Button btnJump = new Button();
        final EditField efThemePath = new EditField();
        final Runnable updateThemePath = new Runnable() {
            public void run() {
                efThemePath.setText(property.getThemePath());
            }
        };
        cb.setSelected(Utils.find(refableNodes, property.getPropertyValue()));
        cb.addCallback(new Runnable() {
            public void run() {
                int selected = cb.getSelected();
                if(selected >= 0) {
                    property.setPropertyValue(refableNodes.getEntry(selected));
                    updateThemePath.run();
                }
            }
        });
        btnJump.setTheme("jumpbutton");
        btnJump.addCallback(new Runnable() {
            public void run() {
                ctx.selectTheme(property.getThemePathElements(0));
            }
        });
        efThemePath.setTheme("themepath");
        efThemePath.setReadOnly(true);
        updateThemePath.run();

        DialogLayout l = new DialogLayout();
        l.setTheme("widgetthemeeditor");
        l.setHorizontalGroup(l.createParallelGroup()
                .addGroup(l.createSequentialGroup(cb, btnJump))
                .addWidget(efThemePath));
        l.setVerticalGroup(l.createSequentialGroup()
                .addGroup(l.createParallelGroup(cb, btnJump))
                .addWidget(efThemePath));

        return l;
    }
}
