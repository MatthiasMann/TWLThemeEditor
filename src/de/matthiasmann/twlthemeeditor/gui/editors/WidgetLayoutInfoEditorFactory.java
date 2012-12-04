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
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.WidgetLayoutInfo;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @author Matthias Mann
 */
public class WidgetLayoutInfoEditorFactory implements PropertyEditorFactory<WidgetLayoutInfo> {

    public Widget create(final Property<WidgetLayoutInfo> property, ExternalFetaures ef) {
        DialogLayout l = new DialogLayout();
        l.setTheme("widgetlayoutinfoeditor");
        Group vert = l.createSequentialGroup();
        
        final Label valueLabel[] = new Label[6];
        final Label nameLabel[] = new Label[6];
        for(int i=0 ; i<6 ; i++) {
            valueLabel[i] = new Label();
            valueLabel[i].setTheme("value");
            nameLabel[i] = new Label();
            nameLabel[i].setTheme("name");
            nameLabel[i].setLabelFor(valueLabel[i]);
            vert.addGroup(l.createParallelGroup()
                    .addWidget(nameLabel[i])
                    .addWidget(valueLabel[i]));
        }
        
        nameLabel[0].setText("min Width");
        nameLabel[1].setText("min Height");
        nameLabel[2].setText("preferred Width");
        nameLabel[3].setText("preferred Height");
        nameLabel[4].setText("max Width");
        nameLabel[5].setText("max Height");
        
        final Runnable callback = new Runnable() {
            public void run() {
                WidgetLayoutInfo info = property.getPropertyValue();
                if(info == null) {
                    for(int i=0 ; i<6 ; i++) {
                        valueLabel[i].setText("");
                    }
                } else {
                    valueLabel[0].setText(Integer.toString(info.getMinWidth()));
                    valueLabel[1].setText(Integer.toString(info.getMinHeight()));
                    valueLabel[2].setText(Integer.toString(info.getPrefWidth()));
                    valueLabel[3].setText(Integer.toString(info.getPrefHeight()));
                    valueLabel[4].setText(Integer.toString(info.getMaxWidth()));
                    valueLabel[5].setText(Integer.toString(info.getMaxHeight()));
                }
            }
        };
        
        callback.run();
        property.addValueChangedCallback(callback);
        
        l.setHorizontalGroup(l.createSequentialGroup()
                .addGroup(l.createParallelGroup(nameLabel))
                .addGroup(l.createParallelGroup(valueLabel)));
        l.setVerticalGroup(vert);
        return l;
    }

}
