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

import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.StringModel;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.dom.Attribute;
import de.matthiasmann.twlthemeeditor.dom.AttributeList.AttributeListListener;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.dom.Namespace;

/**
 *
 * @author Matthias Mann
 */
public class AttributeProperty implements Property<String>, StringModel {

    private final Element element;
    private final String attribute;
    private final String name;
    private final boolean canBeNull;
    private final AttributeListListener all;
    private Runnable[] callbacks = null;

    public AttributeProperty(Element element, String attribute) {
        this(element, attribute, Utils.capitalize(attribute), false);
    }

    public AttributeProperty(Element element, String attribute, String name, boolean canBeNull) {
        this.element = element;
        this.attribute = attribute;
        this.name = name;
        this.canBeNull = canBeNull;
        this.all = new ALL();
    }

    public String getName() {
        return name;
    }

    public boolean isReadOnly() {
        return false;
    }

    public boolean canBeNull() {
        return canBeNull;
    }

    public Class<String> getType() {
        return String.class;
    }

    public String getValue() {
        return element.getAttributeValue(attribute);
    }

    public void setValue(String value) throws IllegalArgumentException {
        if(!canBeNull && value == null) {
            throw new NullPointerException("value");
        }
        String curValue = element.getAttributeValue(attribute);
        if(!Utils.equals(curValue, value)) {
            if(value == null) {
                element.removeAttribute(attribute);
            } else {
                element.setAttribute(attribute, value);
            }
        }
    }

    public String getPropertyValue() {
        return getValue();
    }

    public void setPropertyValue(String value) throws IllegalArgumentException {
        setValue(value);
    }

    public void addCallback(Runnable cb) {
        boolean wasEmpty = (callbacks == null);
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Runnable.class);
        if(wasEmpty) {
            element.getAttributes().addListener(all);
        }
    }

    public void removeCallback(Runnable cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
        if(callbacks == null) {
            element.getAttributes().removeListener(all);
        }
    }
    
    public void addValueChangedCallback(Runnable cb) {
        addCallback(cb);
    }

    public void removeValueChangedCallback(Runnable cb) {
        removeCallback(cb);
    }
    
    void check(Attribute attribute) {
        if(attribute.equals(this.attribute, Namespace.NO_NAMESPACE)) {
            CallbackSupport.fireCallbacks(callbacks);
        }
    }
    
    class ALL implements AttributeListListener {
        public void attributeAdded(Element element, Attribute attribute, int index) {
            check(attribute);
        }
        public void attributeChanged(Element element, Attribute attribute, String oldValue, String newValue) {
            check(attribute);
        }
        public void attributeRemoved(Element element, Attribute attribute, int index) {
            check(attribute);
        }
    }
}
