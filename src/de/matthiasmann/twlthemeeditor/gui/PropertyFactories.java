/*
 * Copyright (c) 2008-2011, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Alignment;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.KeyStroke;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.utils.TypeMapping;
import de.matthiasmann.twlthemeeditor.datamodel.IntegerFormula;
import de.matthiasmann.twlthemeeditor.gui.editors.BooleanEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.BorderEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.ColorEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.DimensionEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.EnumEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.FloatEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.GapEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.HotSpotEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.IntegerEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.IntegerFormulaEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.KeyStrokeEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.NameEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.RectEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.SplitEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.StringEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.WeightsEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;
import de.matthiasmann.twlthemeeditor.properties.DimensionProperty;
import de.matthiasmann.twlthemeeditor.properties.GapProperty;
import de.matthiasmann.twlthemeeditor.properties.HotSpotProperty;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;
import de.matthiasmann.twlthemeeditor.properties.WeightsProperty;

/**
 *
 * @author Matthias Mann
 */
public class PropertyFactories {

    protected final TypeMapping<PropertyEditorFactory<?,?>> factories1;
    protected final TypeMapping<PropertyEditorFactory<?,?>> factories2;

    public PropertyFactories() {
        factories1 = new TypeMapping<PropertyEditorFactory<?,?>>();
        factories1.put(RectProperty.class, new RectEditorFactory());
        factories1.put(WeightsProperty.class, new WeightsEditorFactory());
        factories1.put(SplitProperty.class, new SplitEditorFactory());
        factories1.put(GapProperty.class, new GapEditorFactory());
        factories1.put(DimensionProperty.class, new DimensionEditorFactory());
        factories1.put(HotSpotProperty.class, new HotSpotEditorFactory());
        factories1.put(BorderProperty.class, new BorderEditorFactory());
        factories1.put(NameProperty.class, new NameEditorFactory());

        factories2 = new TypeMapping<PropertyEditorFactory<?,?>>();
        factories2.put(Color.class, new ColorEditorFactory());
        factories2.put(String.class, new StringEditorFactory());
        factories2.put(Integer.class, new IntegerEditorFactory());
        factories2.put(IntegerFormula.class, new IntegerFormulaEditorFactory());
        factories2.put(Float.class, new FloatEditorFactory());
        factories2.put(Boolean.class, new BooleanEditorFactory());
        factories2.put(Enum.class, new EnumEditorFactory<Alignment>()); // use dummy enum to get rid of raw type warning
        factories2.put(KeyStroke.class, new KeyStrokeEditorFactory());
    }

    public PropertyEditorFactory<?,?> getFactory(Property<?> property) {
        PropertyEditorFactory<?,?> factory = factories1.get(property.getClass());
        if(factory == null) {
            factory = factories2.get(property.getType());
        }
        return factory;
    }
}
