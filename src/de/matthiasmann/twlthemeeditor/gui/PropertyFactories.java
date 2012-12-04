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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Alignment;
import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.DialogLayout.Gap;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.KeyStroke;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.utils.TypeMapping;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect;
import de.matthiasmann.twlthemeeditor.datamodel.HotSpot;
import de.matthiasmann.twlthemeeditor.datamodel.IntegerFormula;
import de.matthiasmann.twlthemeeditor.datamodel.Weights;
import de.matthiasmann.twlthemeeditor.datamodel.WidgetLayoutInfo;
import de.matthiasmann.twlthemeeditor.gui.editors.BooleanEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.BorderEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.ColorEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.DimensionEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.EnumEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.FloatEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.GapEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.GradientStopEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.HotSpotEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.IntegerEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.IntegerFormulaEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.KeyStrokeEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.NameEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.RectEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.SplitEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.StringEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.WeightsEditorFactory;
import de.matthiasmann.twlthemeeditor.gui.editors.WidgetLayoutInfoEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.GradientStopModel;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;

/**
 *
 * @author Matthias Mann
 */
public class PropertyFactories {

    protected final TypeMapping<PropertyEditorFactory<?>> factoriesP;
    protected final TypeMapping<PropertyEditorFactory<?>> factoriesT;

    public PropertyFactories() {
        factoriesP = new TypeMapping<PropertyEditorFactory<?>>();
        factoriesP.put(NameProperty.class, new NameEditorFactory());
        factoriesP.put(SplitProperty.class, new SplitEditorFactory());
        
        factoriesT = new TypeMapping<PropertyEditorFactory<?>>();
        factoriesT.put(Enum.class, new EnumEditorFactory<Alignment>()); // use dummy enum to get rid of raw type warning
        factoriesT.put(String.class, new StringEditorFactory());
        factoriesT.put(Color.class, new ColorEditorFactory());
        factoriesT.put(Float.class, new FloatEditorFactory());
        factoriesT.put(Integer.class, new IntegerEditorFactory());
        factoriesT.put(IntegerFormula.class, new IntegerFormulaEditorFactory());
        factoriesT.put(Border.class, new BorderEditorFactory());
        factoriesT.put(Boolean.class, new BooleanEditorFactory());
        factoriesT.put(GradientStopModel.class, new GradientStopEditorFactory());
        factoriesT.put(KeyStroke.class, new KeyStrokeEditorFactory());
        factoriesT.put(Gap.class, new GapEditorFactory());
        factoriesT.put(Dimension.class, new DimensionEditorFactory());
        factoriesT.put(HotSpot.class, new HotSpotEditorFactory());
        factoriesT.put(ExtRect.class, new RectEditorFactory());
        factoriesT.put(Weights.class, new WeightsEditorFactory());
        factoriesT.put(WidgetLayoutInfo.class, new WidgetLayoutInfoEditorFactory());
    }

    public PropertyEditorFactory<?> getFactory(Property<?> property) {
        PropertyEditorFactory<?> factory = factoriesP.get(property.getClass());
        if(factory == null) {
            factory = factoriesT.get(property.getType());
        }
        return factory;
    }
}
