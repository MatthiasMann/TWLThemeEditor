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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twl.utils.TypeMapping;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;
import de.matthiasmann.twlthemeeditor.properties.ConditionProperty;
import de.matthiasmann.twlthemeeditor.properties.HotSpotProperty;
import de.matthiasmann.twlthemeeditor.properties.ImageReferenceProperty;
import de.matthiasmann.twlthemeeditor.properties.IntegerProperty;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;
import de.matthiasmann.twlthemeeditor.properties.SplitProperty;
import de.matthiasmann.twlthemeeditor.properties.WeightsProperty;

/**
 *
 * @author Matthias Mann
 */
public class Context {

    private final ThemeTreeModel model;
    private final TypeMapping<PropertyEditorFactory<?,?>> factories;

    private TextureViewerPane textureViewerPane;

    public Context(ThemeTreeModel model) {
        this.model = model;
        
        factories = new TypeMapping<PropertyEditorFactory<?,?>>();
        factories.put(IntegerProperty.class, new IntegerEditorFactory());
        factories.put(BooleanProperty.class, new BooleanEditorFactory());
        factories.put(ColorProperty.class, new ColorEditor(this));
        factories.put(RectProperty.class, new RectEditorFactory(this));
        factories.put(ConditionProperty.class, new ConditionEditor());
        factories.put(ImageReferenceProperty.class, new ImageRefEditor(this));
        factories.put(WeightsProperty.class, new WeightsEditorFactory());
        factories.put(SplitProperty.class, new SplitEditorFactory());
        factories.put(HotSpotProperty.class, new HotSpotEditorFactory());
        factories.put(BorderProperty.class, new BorderEditorFactory());
        factories.put(NameProperty.class, new NameEditorFactory());
    }

    public ListModel<String> getRefableImages(Image stopAt, Image.Kind kind) {
        SimpleChangableListModel<String> result = new SimpleChangableListModel<String>();
        if(kind == Image.Kind.IMAGE) {
            result.addElement("none");
        }
        for(Image img : model.getImages()) {
            if(img == stopAt) {
                break;
            }
            if(img.getKind() == kind) {
                result.addElement(img.getName());
            }
        }
        return result;
    }

    public PropertyEditorFactory<?,?> getFactory(Class<?> clazz) {
        return factories.get(clazz);
    }

    public TextureViewerPane getTextureViewerPane() {
        return textureViewerPane;
    }

    public void setTextureViewerPane(TextureViewerPane textureViewerPane) {
        this.textureViewerPane = textureViewerPane;
    }
    
}
