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

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;

/**
 *
 * @author Matthias Mann
 */
public class RectEditorFactory implements PropertyEditorFactory<Rect, RectProperty> {

    public Widget create(final PropertyAccessor<Rect, RectProperty> pa) {
        return new RectEditor(pa);
    }

    static final class RectEditor extends DialogLayout {
        private final BooleanModel wholeAreaModel;
        private final ToggleButton toggleWholeArea;
        private final ValueAdjusterInt adjusterX;
        private final ValueAdjusterInt adjusterY;
        private final ValueAdjusterInt adjusterW;
        private final ValueAdjusterInt adjusterH;

        public RectEditor(PropertyAccessor<Rect, RectProperty> pa) {
            adjusterX = new ValueAdjusterInt(pa.getProperty().getXProperty());
            adjusterY = new ValueAdjusterInt(pa.getProperty().getYProperty());
            adjusterW = new ValueAdjusterInt(pa.getProperty().getWidthProperty());
            adjusterH = new ValueAdjusterInt(pa.getProperty().getHeightProperty());

            adjusterX.setTooltipContent("X position");
            adjusterY.setTooltipContent("Y position");
            adjusterW.setTooltipContent("Width");
            adjusterH.setTooltipContent("Height");

            adjusterX.setDisplayPrefix("X: ");
            adjusterY.setDisplayPrefix("Y: ");
            adjusterW.setDisplayPrefix("W: ");
            adjusterH.setDisplayPrefix("H: ");

            Group horz = createParallelGroup();
            Group vert = createSequentialGroup();

            wholeAreaModel = pa.getProperty().getWholeAreaModel();
            if(wholeAreaModel != null) {
                toggleWholeArea = new ToggleButton(wholeAreaModel);
                toggleWholeArea.setTheme("btnWholeArea");

                horz.addWidget(toggleWholeArea);
                vert.addWidget(toggleWholeArea);
            } else {
                toggleWholeArea = null;
            }

            horz.addWidgets(adjusterX, adjusterY, adjusterW, adjusterH);
            vert.addWidgetsWithGap("adjuster", adjusterX, adjusterY, adjusterW, adjusterH);
            
            RectProperty.AbstractAction[] actions = pa.getProperty().getActions();
            if(actions.length > 0) {
                Group hActions = createSequentialGroup();
                Group vActions = createParallelGroup();

                for(RectProperty.AbstractAction action : actions) {
                    Button btn = new Button(action.getName());
                    btn.setTheme("actionButton");
                    btn.setTooltipContent(action.getTooltip());
                    btn.addCallback(action);
                    hActions.addWidget(btn);
                    vActions.addWidget(btn);
                }

                hActions.addGap();

                horz.addGroup(hActions);
                vert.addGroup(vActions);
            }

            setHorizontalGroup(horz);
            setVerticalGroup(vert);
        }
    }
}
