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
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect.AbstractAction;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect.BM;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect.ExtRectProperty;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect.IM;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;

/**
 *
 * @author Matthias Mann
 */
public class RectEditorFactory implements PropertyEditorFactory<ExtRect> {

    public Widget create(Property<ExtRect> property, ExternalFetaures ef) {
        ValueAdjusterInt adjusterX = new ValueAdjusterInt(new IM(property) {
            public int getMaxValue() {
                return getLimit().getX();
            }
            public int getValue() {
                return getRect().x;
            }
            public void setValue(int value) {
                ExtRect rect = getRect();
                setRect(value, rect.y, Math.min(getLimit().getX() - value, rect.width), rect.height, false, rect.flipX, rect.flipY);
            }
        });
        ValueAdjusterInt adjusterY = new ValueAdjusterInt(new IM(property) {
            public int getMaxValue() {
                return getLimit().getY();
            }
            public int getValue() {
                return getRect().y;
            }
            public void setValue(int value) {
                ExtRect rect = getRect();
                setRect(rect.x, value, rect.width, Math.min(getLimit().getY() - value, rect.height), false, rect.flipX, rect.flipY);
            }
        });
        ValueAdjusterInt adjusterW = new ValueAdjusterInt(new IM(property) {
            public int getMaxValue() {
                return getLimit().getX();
            }
            public int getValue() {
                return Math.abs(getRect().width);
            }
            public void setValue(int value) {
                ExtRect rect = getRect();
                setRect(Math.min(getLimit().getX() - value, rect.x), rect.y, value, rect.height, false, rect.flipX, rect.flipY);
            }
        });
        ValueAdjusterInt adjusterH = new ValueAdjusterInt(new IM(property) {
            public int getMaxValue() {
                return getLimit().getY();
            }
            public int getValue() {
                return getRect().height;
            }
            public void setValue(int value) {
                ExtRect rect = getRect();
                setRect(rect.x, Math.min(getLimit().getY() - value, rect.y), rect.width, value, false, rect.flipX, rect.flipY);
            }
        });

        adjusterX.setTooltipContent("X position");
        adjusterY.setTooltipContent("Y position");
        adjusterW.setTooltipContent("Width");
        adjusterH.setTooltipContent("Height");

        adjusterX.setDisplayPrefix("X: ");
        adjusterY.setDisplayPrefix("Y: ");
        adjusterW.setDisplayPrefix("W: ");
        adjusterH.setDisplayPrefix("H: ");

        DialogLayout l = new DialogLayout();
        l.setTheme("recteditor");
        Group horz = l.createParallelGroup();
        Group vert = l.createSequentialGroup();

        if(property instanceof ExtRectProperty) {
            ExtRectProperty extRectProperty = (ExtRectProperty)property;
            
            if(extRectProperty.supportsWholeArea()) {
                ToggleButton toggleWholeArea = new ToggleButton(new BM(property) {
                    public boolean getValue() {
                        return getRect().wholeArea;
                    }
                    public void setValue(boolean value) {
                        ExtRect rect = getRect();
                        setRect(rect.x, rect.y, rect.width, rect.height, value, rect.flipX, rect.flipY);
                    }
                });
                toggleWholeArea.setTheme("btnWholeArea");

                horz.addWidget(toggleWholeArea);
                vert.addWidget(toggleWholeArea);
            }
            
            horz.addWidgets(adjusterX, adjusterY, adjusterW, adjusterH);
            vert.addWidgetsWithGap("adjuster", adjusterX, adjusterY, adjusterW, adjusterH);
            
            if(extRectProperty.supportsFlipping()) {
                ToggleButton toggleFlipHorz = new ToggleButton(new BM(property) {
                    public boolean getValue() {
                        return getRect().flipX;
                    }
                    public void setValue(boolean value) {
                        ExtRect rect = getRect();
                        setRect(rect.x, rect.y, rect.width, rect.height, rect.wholeArea, value, rect.flipY);
                    }
                });
                toggleFlipHorz.setTheme("btnFlipHorz");

                horz.addWidget(toggleFlipHorz);
                vert.addWidget(toggleFlipHorz);

                ToggleButton toggleFlipVert = new ToggleButton(new BM(property) {
                    public boolean getValue() {
                        return getRect().flipY;
                    }
                    public void setValue(boolean value) {
                        ExtRect rect = getRect();
                        setRect(rect.x, rect.y, rect.width, rect.height, rect.wholeArea, rect.flipX, value);
                    }
                });
                toggleFlipVert.setTheme("btnFlipVert");

                horz.addWidget(toggleFlipVert);
                vert.addWidget(toggleFlipVert);
            }
            
            AbstractAction[] actions = extRectProperty.getActions();
            if(actions != null && actions.length > 0) {
                Group hActions = l.createSequentialGroup();
                Group vActions = l.createParallelGroup();

                for(AbstractAction action : actions) {
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
        } else {
            horz.addWidgets(adjusterX, adjusterY, adjusterW, adjusterH);
            vert.addWidgetsWithGap("adjuster", adjusterX, adjusterY, adjusterW, adjusterH);
        }

        l.setHorizontalGroup(horz);
        l.setVerticalGroup(vert);
        return l;
    }
    
}
