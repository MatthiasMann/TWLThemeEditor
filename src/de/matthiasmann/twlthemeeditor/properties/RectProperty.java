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

import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect.AbstractAction;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;

/**
 *
 * @author Matthias Mann
 */
public abstract class RectProperty extends DerivedProperty<ExtRect> implements ExtRect.ExtRectProperty {

    private static final ExtRect DEFAULT_RECT = new ExtRect(0, 0, 1, 1, false, false, false);
    
    private final String name;
    
    public RectProperty(Property<String> base, String name) {
        super(base, ExtRect.class, DEFAULT_RECT);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected ExtRect parse(String value) throws IllegalArgumentException {
        if("*".equals(value)) {
            Dimension dim = getLimit();
            return new ExtRect(0, 0, dim.getX(), dim.getY(), true, false, false);
        }
        int[] xywh = Utils.parseInts(value);
        return new ExtRect(xywh[0], xywh[1],
                Math.abs(xywh[2]), Math.abs(xywh[3]),
                false, xywh[2] < 0, xywh[3] < 0);
    }

    @Override
    protected String toString(ExtRect value) throws IllegalArgumentException {
        if(value.wholeArea) {
            return "*";
        }
        return value.x+","+value.y+","+
                (value.flipX ? -value.width : value.width)+","+
                (value.flipY ? -value.height : value.height);
    }

    public boolean supportsFlipping() {
        return true;
    }

    public boolean supportsWholeArea() {
        return true;
    }

    public AbstractAction[] getActions() {
        return null;
    }
}
