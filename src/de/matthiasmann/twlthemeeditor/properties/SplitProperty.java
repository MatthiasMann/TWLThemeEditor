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
package de.matthiasmann.twlthemeeditor.properties;

import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.utils.WithRunnableCallback;
import de.matthiasmann.twlthemeeditor.datamodel.Split;
import de.matthiasmann.twlthemeeditor.datamodel.Split.Point;

/**
 *
 * @author Matthias Mann
 */
public abstract class SplitProperty extends DerivedProperty<Split> {

    private final Split.Axis axis;

    public SplitProperty(Property<String> base, Split.Axis axis) {
        super(base, Split.class, null);
        this.axis = axis;
    }

    public Split.Axis getAxis() {
        return axis;
    }

    @Override
    protected Split parse(String value) throws IllegalArgumentException {
        return new Split(value);
    }

    @Override
    protected String toString(Split value) throws IllegalArgumentException {
        return (value != null) ? value.toString(axis) : null;
    }

    public abstract int getLimit();
    
    static final Split DEFAULT_SPLIT = new Split(new Split.Point(0, false), new Split.Point(0, true));

    public static abstract class PM implements WithRunnableCallback {
        final SplitProperty property;
        protected PM(SplitProperty property) {
            this.property = property;
        }
        protected final Split getSplit() {
            Split split = property.getPropertyValue();
            if(split == null) {
                return DEFAULT_SPLIT;
            }
            return split;
        }
        public void addCallback(Runnable callback) {
            property.addValueChangedCallback(callback);
        }
        public void removeCallback(Runnable callback) {
            property.removeValueChangedCallback(callback);
        }
    }
    
    public static final class SplitIntegerModel extends PM implements IntegerModel {
        final int idx;
        final boolean handleEdges;
        public SplitIntegerModel(SplitProperty property, int idx, boolean handleEdges) {
            super(property);
            this.idx = idx;
            this.handleEdges = handleEdges;
        }
        public int getMaxValue() {
            return property.getLimit();
        }
        public int getMinValue() {
            return 0;
        }
        public int getValue() {
            Point point = getSplit().getPoint(idx);
            if(point.isOtherEdge() && handleEdges) {
                return getMaxValue() - point.getPos();
            } else {
                return point.getPos();
            }
        }
        public void setValue(int value) {
            Split split = getSplit();
            Point point = split.getPoint(idx);
            if(point.isOtherEdge() && handleEdges) {
                value = getMaxValue() - value;
            }
            property.setPropertyValue(split.setPoint(idx, point.setPos(value)));
        }
    }

    public static final class EdgeBooleanModel extends PM implements BooleanModel {
        final int idx;
        final boolean thisEdge;

        public EdgeBooleanModel(SplitProperty property, int idx, boolean thisEdge) {
            super(property);
            this.idx = idx;
            this.thisEdge = thisEdge;
        }
        public boolean getValue() {
            return getSplit().getPoint(idx).isOtherEdge() == thisEdge;
        }
        public void setValue(boolean value) {
            if(value) {
                final int limit = property.getLimit();
                final Split split = getSplit();
                final Split.Point point = split.getPoint(idx);
                final Split.Point newPoint = point.setOtherEdge(thisEdge, limit);
                property.setPropertyValue(split.setPoint(idx, newPoint));
            }
        }
    }
}
