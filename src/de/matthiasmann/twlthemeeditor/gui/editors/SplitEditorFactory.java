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
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;
import de.matthiasmann.twlthemeeditor.datamodel.Split;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;
import java.util.ArrayList;

/**
 *
 * @author Matthias Mann
 */
public class SplitEditorFactory implements PropertyEditorFactory<Split, SplitProperty> {

    public static final StateKey STATE_ADJUSTER_NOT_FIRST    = StateKey.get("adjusterNotFirst");
    public static final StateKey STATE_ADJUSTER_NOT_LAST     = StateKey.get("adjusterNotLast");
    public static final StateKey STATE_RADIOBUTTON_NOT_FIRST = StateKey.get("radiobuttonNotFirst");
    public static final StateKey STATE_RADIOBUTTON_NOT_LAST  = StateKey.get("radiobuttonNotLast");

    public Widget create(PropertyAccessor<Split, SplitProperty> pa) {
        return new SplitEditor(pa);
    }

    static class SplitEditor extends DialogLayout {
        private static final Split DEFAULT_SPLIT = new Split(
                new Split.Point(0, false), new Split.Point(0, true));

        private final PropertyAccessor<Split, SplitProperty> pa;

        public SplitEditor(PropertyAccessor<Split, SplitProperty> pa) {
            this.pa = pa;

            final Group adjusterColumn = createParallelGroup();
            final Group btnColumn1 = createParallelGroup();
            final Group btnColumn2 = createParallelGroup();
            final Group vert = createSequentialGroup();

            final String theme1;
            final String theme2;

            if(pa.getProperty().getAxis() == Split.Axis.HORIZONTAL) {
                theme1 = "btnRelativeLeft";
                theme2 = "btnRelativeRight";
            } else {
                theme1 = "btnRelativeTop";
                theme2 = "btnRelativeBottom";
            }

            final ArrayList<Widget> widgetsToEnable = new ArrayList<Widget>(6);
            
            for(int i=0 ; i<2 ; i++) {
                ValueAdjusterInt adjuster = new ValueAdjusterInt(new SplitIntegerModel(i));
                adjuster.getAnimationState().setAnimationState(STATE_ADJUSTER_NOT_FIRST, i == 1);
                adjuster.getAnimationState().setAnimationState(STATE_ADJUSTER_NOT_LAST, i == 0);

                ToggleButton btn1 = new ToggleButton(new EdgeBooleanModel(i, false));
                ToggleButton btn2 = new ToggleButton(new EdgeBooleanModel(i, true));

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
                vert.addGroup(createParallelGroup().addWidget(adjuster).addWidget(btn1).addWidget(btn2));

                widgetsToEnable.add(adjuster);
                widgetsToEnable.add(btn1);
                widgetsToEnable.add(btn2);
            }

            pa.setWidgetsToEnable(widgetsToEnable.toArray(new Widget[widgetsToEnable.size()]));
            pa.addActiveCallback(new ActiveCB());

            setHorizontalGroup(createSequentialGroup()
                    .addGroup(adjusterColumn)
                    .addGroup(btnColumn1)
                    .addGap("radiobutton")
                    .addGroup(btnColumn2));
            setVerticalGroup(vert);
        }

        Split getSplit() {
            return pa.getValue(DEFAULT_SPLIT);
        }

        Split getSplitDirect() {
            Split split = pa.getProperty().getPropertyValue();
            return (split != null) ? split : DEFAULT_SPLIT;
        }

        class SplitIntegerModel implements IntegerModel {
            final int idx;
            
            public SplitIntegerModel(int idx) {
                this.idx = idx;
            }
            public int getMaxValue() {
                return pa.getProperty().getLimit();
            }
            public int getMinValue() {
                return 0;
            }
            public int getValue() {
                return getSplit().getPoint(idx).getPos();
            }
            public void setValue(int value) {
                Split split = getSplitDirect();
                pa.setValue(split.setPoint(idx, split.getPoint(idx).setPos(value)));
            }
            public void addCallback(Runnable callback) {
                pa.getProperty().addCallback(callback);
            }
            public void removeCallback(Runnable callback) {
                pa.getProperty().removeCallback(callback);
            }
        }

        class EdgeBooleanModel implements BooleanModel {
            final int idx;
            final boolean thisEdge;

            public EdgeBooleanModel(int idx, boolean thisEdge) {
                this.idx = idx;
                this.thisEdge = thisEdge;
            }
            public boolean getValue() {
                return getSplit().getPoint(idx).isOtherEdge() == thisEdge;
            }
            public void setValue(boolean value) {
                if(value) {
                    final int limit = pa.getProperty().getLimit();
                    final Split split = getSplitDirect();
                    final Split.Point point = split.getPoint(idx);
                    final Split.Point newPoint = point.setOtherEdge(thisEdge, limit);
                    pa.setValue(split.setPoint(idx, newPoint));
                }
            }
            public void addCallback(Runnable callback) {
                pa.getProperty().addCallback(callback);
            }
            public void removeCallback(Runnable callback) {
                pa.getProperty().removeCallback(callback);
            }
        }
        
        class ActiveCB implements Runnable {
            public void run() {
                if(pa.isActive() && pa.getProperty().getPropertyValue() == null) {
                    pa.setValue(DEFAULT_SPLIT);
                }
            }
        }
    }
}
