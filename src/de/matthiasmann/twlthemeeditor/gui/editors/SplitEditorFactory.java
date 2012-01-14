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
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;
import de.matthiasmann.twlthemeeditor.datamodel.Split;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty.EdgeBooleanModel;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty.SplitIntegerModel;

/**
 *
 * @author Matthias Mann
 */
public class SplitEditorFactory implements PropertyEditorFactory<Split> {

    public static final StateKey STATE_ADJUSTER_NOT_FIRST    = StateKey.get("adjusterNotFirst");
    public static final StateKey STATE_ADJUSTER_NOT_LAST     = StateKey.get("adjusterNotLast");
    public static final StateKey STATE_RADIOBUTTON_NOT_FIRST = StateKey.get("radiobuttonNotFirst");
    public static final StateKey STATE_RADIOBUTTON_NOT_LAST  = StateKey.get("radiobuttonNotLast");

    public Widget create(Property<Split> prop, ExternalFetaures ef) {
        SplitProperty property = (SplitProperty)prop;
        
        DialogLayout l = new DialogLayout();
        l.setTheme("spliteditor");
        Group adjusterColumn = l.createParallelGroup();
        Group btnColumn1 = l.createParallelGroup();
        Group btnColumn2 = l.createParallelGroup();
        Group vert = l.createSequentialGroup();

        final String theme1;
        final String theme2;

        if(property.getAxis() == Split.Axis.HORIZONTAL) {
            theme1 = "btnRelativeLeft";
            theme2 = "btnRelativeRight";
        } else {
            theme1 = "btnRelativeTop";
            theme2 = "btnRelativeBottom";
        }

        final Widget[] widgetsToEnable = new Widget[6];

        for(int i=0 ; i<2 ; i++) {
            ValueAdjusterInt adjuster = new ValueAdjusterInt(new SplitIntegerModel(property, i, false));
            adjuster.getAnimationState().setAnimationState(STATE_ADJUSTER_NOT_FIRST, i == 1);
            adjuster.getAnimationState().setAnimationState(STATE_ADJUSTER_NOT_LAST, i == 0);

            ToggleButton btn1 = new ToggleButton(new EdgeBooleanModel(property, i, false));
            ToggleButton btn2 = new ToggleButton(new EdgeBooleanModel(property, i, true));

            btn1.setTheme(theme1);
            btn2.setTheme(theme2);

            btn1.getAnimationState().setAnimationState(STATE_RADIOBUTTON_NOT_LAST,  true);
            btn2.getAnimationState().setAnimationState(STATE_RADIOBUTTON_NOT_FIRST, true);

            if(i > 0) {
                vert.addGap("adjuster");
            }

            adjusterColumn.addWidget(adjuster);
            btnColumn1.addWidget(btn1);
            btnColumn2.addWidget(btn2);
            vert.addGroup(l.createParallelGroup().addWidget(adjuster).addWidget(btn1).addWidget(btn2));

            widgetsToEnable[i*3+0] = adjuster;
            widgetsToEnable[i*3+1] = btn1;
            widgetsToEnable[i*3+2] = btn2;
        }

        ef.disableOnNotPresent(widgetsToEnable);

        l.setHorizontalGroup(l.createSequentialGroup()
                .addGroup(adjusterColumn)
                .addGroup(btnColumn1)
                .addGap("radiobutton")
                .addGroup(btnColumn2));
        l.setVerticalGroup(vert);
        return l;
    }
}
