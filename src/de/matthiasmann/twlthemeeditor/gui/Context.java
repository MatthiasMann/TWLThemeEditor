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

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.utils.TypeMapping;
import de.matthiasmann.twlthemeeditor.datamodel.Condition;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.ImageReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Matthias Mann
 */
public class Context {

    private final ListModel<Image> images;
    private final TypeMapping<PropertyEditorFactory> factories;
    private final ArrayList<String> propertyOrder;

    public Context(ListModel<Image> images) {
        this.images = images;
        
        factories = new TypeMapping<PropertyEditorFactory>();
        factories.put(Integer.class, new IntegerEditor());
        factories.put(int.class, new IntegerEditor());
        factories.put(boolean.class, new BooleanEditor());
        factories.put(Color.class, new ColorEditor());
        factories.put(Rect.class, new RectEditor());
        factories.put(Condition.class, new ConditionEditor());
        factories.put(ImageReference.class, new ImageRefEditor(this));

        this.propertyOrder = new ArrayList<String>();
    }

    public ListModel<Image> getImages() {
        return images;
    }

    public PropertyEditorFactory getFactory(Class<?> clazz) {
        return factories.get(clazz);
    }

    public int findImage(ImageReference ref) {
        for(int i=0,n=images.getNumEntries() ; i<n ; i++) {
            if(ref.getName().equals(images.getEntry(i).getName())) {
                return i;
            }
        }
        return -1;
    }

    public List<String> getPropertyOrder() {
        return Collections.unmodifiableList(propertyOrder);
    }

    public void setPropertyOrder(String ... order) {
        propertyOrder.clear();
        propertyOrder.addAll(Arrays.asList(order));
    }
}
