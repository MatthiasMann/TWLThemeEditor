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

import de.matthiasmann.twlthemeeditor.properties.PropertyAccessor;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twlthemeeditor.properties.Optional;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.TreeMap;

/**
 *
 * @author Matthias Mann
 */
public class PropertyPanel extends DialogLayout {

    protected static final int NUM_COLUMNS = 2;

    protected final Context ctx;

    public PropertyPanel(Context ctx, Object obj) throws IntrospectionException {
        this.ctx = ctx;
        
        setHorizontalGroup(createParallelGroup());
        setVerticalGroup(createSequentialGroup());

        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        TreeMap<String, PropertyDescriptor> properties = new TreeMap<String, PropertyDescriptor>();
        for(PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            if(pd.getWriteMethod() != null && pd.getReadMethod() != null) {
                properties.put(pd.getName(), pd);
            }
        }

        for(String property : ctx.getPropertyOrder()) {
            PropertyDescriptor pd = properties.remove(property);
            if(pd != null) {
                addProperty(obj, pd);
            }
        }

        for(PropertyDescriptor pd : properties.values()) {
            addProperty(obj, pd);
        }
    }

    @SuppressWarnings("unchecked")
    protected void addProperty(Object obj, PropertyDescriptor pd) {
        boolean optional = pd.getReadMethod().isAnnotationPresent(Optional.class);
        Class<?> type = pd.getPropertyType();

        PropertyEditorFactory factory = ctx.getFactory(type);
        if(factory != null) {
            BooleanModel activeModel = null;

            if(optional) {
                activeModel = new SimpleBooleanModel();
            }

            Widget content = factory.create(new PropertyAccessor(obj, pd, activeModel));

            CollapsiblePanel panel = new CollapsiblePanel(
                    CollapsiblePanel.Direction.VERTICAL,
                    pd.getDisplayName(), content, activeModel);
            
            getVerticalGroup().addWidget(panel);
            getHorizontalGroup().addWidget(panel);            
        } else {
            System.out.println("No factory for property " + pd.getName() + " type " + type);
        }
    }
}
