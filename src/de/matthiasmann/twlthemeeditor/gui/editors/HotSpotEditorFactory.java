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
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.ValueAdjusterInt;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.HotSpot;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.HotSpotProperty;

/**
 *
 * @author Matthias Mann
 */
public class HotSpotEditorFactory implements PropertyEditorFactory<HotSpot> {

    public Widget create(Property<HotSpot> property, ExternalFetaures ef) {
        ValueAdjusterInt adjusterX = new ValueAdjusterInt(new IM(property) {
            public int getMaxValue() {
                return getLimit().getX();
            }
            public int getValue() {
                return getHotSpot().getX();
            }
            public void setValue(int value) {
                setHotSpot(value, getHotSpot().getY());
            }
        });
        adjusterX.setDisplayPrefix("X: ");

        ValueAdjusterInt adjusterY = new ValueAdjusterInt(new IM(property) {
            public int getMaxValue() {
                return getLimit().getY();
            }
            public int getValue() {
                return getHotSpot().getY();
            }
            public void setValue(int value) {
                setHotSpot(getHotSpot().getX(), value);
            }
        });
        adjusterY.setDisplayPrefix("Y: ");

        DialogLayout l = new DialogLayout();
        l.setTheme("hotspoteditor");
        l.setHorizontalGroup(l.createParallelGroup(adjusterX, adjusterY));
        l.setVerticalGroup(l.createSequentialGroup().addWidgetsWithGap("adjuster", adjusterX, adjusterY));
        return l;
    }

    static abstract class IM implements IntegerModel {
        static final HotSpot NULL_HOTSPOT = new HotSpot(0,0);
        static final Dimension NO_LIMIT = new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
        final Property<HotSpot> property;

        IM(Property<HotSpot> property) {
            this.property = property;
        }
        Dimension getLimit() {
            if(property instanceof HotSpotProperty) {
                return ((HotSpotProperty)property).getLimit();
            }
            return NO_LIMIT;
        }
        HotSpot getHotSpot() {
            HotSpot hs = property.getPropertyValue();
            return (hs != null) ? hs : NULL_HOTSPOT;
        }
        void setHotSpot(int x, int y) {
            property.setPropertyValue(new HotSpot(x, y));
        }
        public int getMinValue() {
            return 0;
        }
        public void addCallback(Runnable cb) {
            property.addValueChangedCallback(cb);
        }
        public void removeCallback(Runnable cb) {
            property.removeValueChangedCallback(cb);
        }
    }
}
