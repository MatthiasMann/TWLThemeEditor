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
package de.matthiasmann.twlthemeeditor.properties;

import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.HotSpot;

/**
 *
 * @author Matthias Mann
 */
public abstract class HotSpotProperty implements Property<HotSpot> {

    private final IntegerProperty baseX;
    private final IntegerProperty baseY;
    private final String name;

    public HotSpotProperty(Property<String> x, Property<String> y, String name) {
        this.baseX = new IntegerProperty(x, 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getX();
            }
        };
        this.baseY = new IntegerProperty(y, 0, 0) {
            @Override
            public int getMaxValue() {
                return getLimit().getY();
            }
        };
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Class<HotSpot> getType() {
        return HotSpot.class;
    }

    public boolean canBeNull() {
        return false;
    }

    public boolean isReadOnly() {
        return false;
    }

    public HotSpot getPropertyValue() {
        return new HotSpot(
                baseX.getValue(),
                baseY.getValue());
    }

    public void setPropertyValue(HotSpot value) throws IllegalArgumentException {
        baseX.setValue(value.getX());
        baseY.setValue(value.getY());
    }

    public void addValueChangedCallback(Runnable cb) {
        baseX.addValueChangedCallback(cb);
        baseY.addValueChangedCallback(cb);
    }

    public void removeValueChangedCallback(Runnable cb) {
        baseX.removeValueChangedCallback(cb);
        baseY.removeValueChangedCallback(cb);
    }

    public abstract Dimension getLimit();
}
