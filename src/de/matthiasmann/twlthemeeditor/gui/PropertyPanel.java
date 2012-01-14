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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory.ExternalFetaures;
import de.matthiasmann.twlthemeeditor.properties.OptionalProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

/**
 *
 * @author Matthias Mann
 */
public final class PropertyPanel extends DialogLayout {

    private final PropertyFactories factories;
    private final HashMap<String, Runnable> focusWidgets;

    public PropertyPanel(PropertyFactories factories) {
        this.factories = factories;
        this.focusWidgets = new HashMap<String, Runnable>();
        
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
    
    public void focusWidget(String propertyName) {
        Runnable cb = focusWidgets.get(propertyName);
        if(cb != null) {
            cb.run();
        }
    }

    @SuppressWarnings("unchecked")
    protected<T> void addProperty(Property<T> p) {
        PropertyEditorFactory factoryNew = factories.getFactory(p);
        if(factoryNew != null) {
            PresentModel activeModel = null;
            if(p instanceof OptionalProperty<?>) {
                OptionalProperty<?> op = (OptionalProperty<?>)p;
                if(op.isOptional()) {
                    activeModel = new PresentModel(op);
                }
            }
            
            PresentModel ef = (activeModel != null)
                    ? activeModel : new PresentModel(null);

            Widget content = factoryNew.create(p, ef);

            if(activeModel != null && ef.focusWidgetCB != null) {
                focusWidgets.put(p.getName(), ef.focusWidgetCB);
            }
            
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
    
    static class PresentModel implements BooleanModel, ExternalFetaures {
        private final OptionalProperty<?> property;
        private Runnable actionOnSetPresent;
        private ArrayList<Widget> widgetsToDisable;
        private Boolean wasPresent;
        Runnable focusWidgetCB;

        public PresentModel(OptionalProperty<?> property) {
            this.property = property;
        }
        public boolean getValue() {
            boolean isPresent = property.isPresent();
            if(wasPresent == null || isPresent != wasPresent.booleanValue()) {
                wasPresent = isPresent;
                // don't need to register a callback - the checkbox will call getValue()
                setWidgets();
            }
            return isPresent;
        }
        public void setValue(boolean value) {
            boolean call = (actionOnSetPresent != null) && value && !getValue();
            property.setPresent(value);
            if(call) {
                actionOnSetPresent.run();
            }
        }
        public void addCallback(Runnable cb) {
            property.addValueChangedCallback(cb);
        }
        public void removeCallback(Runnable cb) {
            property.removeValueChangedCallback(cb);
        }
        public void setPresentAction(Runnable cb) {
            actionOnSetPresent = cb;
        }
        public void disableOnNotPresent(Widget... widgets) {
            if(widgetsToDisable == null) {
                widgetsToDisable = new ArrayList<Widget>();
            }
            widgetsToDisable.addAll(Arrays.asList(widgets));
        }
        public void setFocusWidgetCB(Runnable cb) {
            this.focusWidgetCB = cb;
        }
        private void setWidgets() {
            if(widgetsToDisable != null) {
                for(int i=0,n=widgetsToDisable.size() ; i<n ; i++) {
                    widgetsToDisable.get(i).setEnabled(wasPresent);
                }
            }
        }
    }
}
