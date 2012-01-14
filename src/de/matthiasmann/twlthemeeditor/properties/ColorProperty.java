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

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.model.ColorModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.datamodel.ConstantDef;
import de.matthiasmann.twlthemeeditor.datamodel.Kind;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;

/**
 *
 * @author Matthias Mann
 */
public class ColorProperty extends DerivedProperty<Color> implements ColorModel, OptionalProperty<Color> {

    private final ThemeTreeNode node;
    private Color lastColor;
    private String lastName;
    
    public ColorProperty(Property<String> base, ThemeTreeNode node) {
        super(base, Color.class, Color.WHITE);
        this.node = node;
    }

    @Override
    protected Color parse(String value) throws IllegalArgumentException {
        Color color = parseColor(value, node);
        if(color == null) {
            throw new IllegalArgumentException("Can't parse color: " + value);
        }
        lastColor = color;
        lastName = value;
        return color;
    }

    @Override
    protected String toString(Color value) throws IllegalArgumentException {
        if(value == lastColor) {
            return lastName;
        }
        return value.toString();
    }

    public String getColorName() {
        String value = base.getPropertyValue();
        if(value != null && isColorName(value)) {
            return value;
        }
        return null;
    }
    
    public static boolean isColorName(String value) {
        return value != null && value.length() > 0 && value.charAt(0) != '#';
    }
    
    public static Color parseColor(String value, ThemeTreeNode node) throws NumberFormatException {
        Color color = Color.parserColor(value);
        if(color == null) {
            ThemeTreeNode constantNode = node.getThemeFile().getTreeNode().findNode(value, Kind.CONSTANTDEF);
            if(constantNode instanceof ConstantDef) {
                ConstantDef constantDef = (ConstantDef)constantNode;
                Property<?> valueProperty = constantDef.getValueProperty();
                if(valueProperty.getType() == Color.class) {
                    color = (Color)valueProperty.getPropertyValue();
                }
            }
        }
        return color;
    }

    public Color getValue() {
        return getPropertyValue();
    }

    public void setValue(Color value) {
        setPropertyValue(value);
    }
}
