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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import java.util.Collection;
import java.util.HashMap;

/**
 *
 * @author Matthias Mann
 */
public final class PropertyPanel extends DialogLayout {

    private final PropertyFactories factories;
    private final HashMap<String, PropertyAccessor<?,?>> propertyAccessors;

    public PropertyPanel(PropertyFactories factories) {
        this.factories = factories;
        this.propertyAccessors = new HashMap<String, PropertyAccessor<?, ?>>();
        
        setHorizontalGroup(createParallelGroup());
        setVerticalGroup(createSequentialGroup());
    }

    public PropertyPanel(PropertyFactories factories, Property<?>[] properties) {
        this(factories);
        for(Property<?> p : properties) {
            addProperty(p);
        }
    }

    public PropertyPanel(PropertyFactories factories, Collection<Property<?>> properties) {
        this(factories);
        for(Property<?> p : properties) {
            addProperty(p);
        }
    }

    public PropertyAccessor<?, ?> getPropertyAccessor(String propertyName) {
        return propertyAccessors.get(propertyName);
    }

    @SuppressWarnings("unchecked")
    protected<T> void addProperty(Property<T> p) {
        boolean optional = p.canBeNull();
        
        PropertyEditorFactory<?, ?> factory = factories.getFactory(p);
        if(!optional && (factory instanceof SpecialPropertyEditorFactory<?>)) {
            if(((SpecialPropertyEditorFactory<T>)factory).createSpecial(
                    getHorizontalGroup(), getVerticalGroup(), p)) {
                return;
            }
        }

        if(factory != null) {
            BooleanModel activeModel = null;

            if(optional) {
                activeModel = new SimpleBooleanModel();
            }

            PropertyAccessor pa = new PropertyAccessor(p, activeModel);
            Widget content = factory.create(pa);

            propertyAccessors.put(p.getName(), pa);
            
            CollapsiblePanel panel = new CollapsiblePanel(
                    CollapsiblePanel.Direction.VERTICAL,
                    p.getName(), content, activeModel);
            
            getVerticalGroup().addWidget(panel);
            getHorizontalGroup().addWidget(panel);            
        } else {
            System.out.println("No factory for property " + p.getName() +
                    " type " + p.getClass() + "<" + p.getType() + ">");
        }
    }
}
