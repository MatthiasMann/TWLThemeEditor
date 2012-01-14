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
package de.matthiasmann.twlthemeeditor.dom;

import de.matthiasmann.twl.utils.CallbackSupport;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author Matthias Mann
 */
public final class AttributeList implements Iterable<Attribute> {

    public interface AttributeListListener {
        
        public void attributeAdded(Element element, Attribute attribute, int index);
        
        public void attributeRemoved(Element element, Attribute attribute, int index);
        
        public void attributeChanged(Element element, Attribute attribute, String oldValue, String newValue);
        
    }
    
    static final Attribute[] EMPTY_ATTRIBUTE_ARRAY = new Attribute[0];
    
    final Element element;
    
    Attribute[] attributes;
    AttributeListListener[] listeners;

    AttributeList(Element element) {
        this.element = element;
        this.attributes = EMPTY_ATTRIBUTE_ARRAY;
    }

    public AttributeList(Element element, AttributeList src) {
        int size = src.attributes.length;
        
        this.element = element;
        this.attributes = new Attribute[size];
        
        for(int i=0 ; i<size ; i++) {
            attributes[i] = src.attributes[i].clone();
        }
    }
    
    public void addListener(AttributeListListener listener) {
        listeners = CallbackSupport.addCallbackToList(listeners, listener, AttributeListListener.class);
    }
    
    public void removeListener(AttributeListListener listener) {
        listeners = CallbackSupport.removeCallbackFromList(listeners, listener);
    }

    public int size() {
        return attributes.length;
    }
    
    public boolean isEmpty() {
        return attributes.length == 0;
    }
    
    public Attribute[] toArray() {
        if(attributes.length == 0) {
            return EMPTY_ATTRIBUTE_ARRAY;
        }
        return attributes.clone();
    }
    
    public Attribute get(int idx) {
        return attributes[idx];
    }
    
    public Attribute get(String name, Namespace namespace) {
        for(Attribute attribute : attributes) {
            if(attribute.equals(name, namespace)) {
                return attribute;
            }
        }
        return null;
    }

    public Attribute set(String name, Namespace namespace, String value) {
        for(Attribute attribute : attributes) {
            if(attribute.equals(name, namespace)) {
                attribute.setValue(value);
                return attribute;
            }
        }
        Attribute attribute = new Attribute(name, namespace, value);
        add(attribute);
        return attribute;
    }
    
    public Attribute findAttribute(long id) {
        for(Attribute attribute : attributes) {
            if(attribute.id == id) {
                return attribute;
            }
        }
        return null;
    }
    
    public void add(int idx, Attribute attribute) {
        if(attribute == null) {
            throw new NullPointerException("attribute");
        }
        if(attribute.element != null) {
            throw new IllegalStateException("attribute already has an element");
        }
        if(idx < 0 || idx > size()) {
            throw new IndexOutOfBoundsException();
        }
        
        final int oldLength = attributes.length;
        Attribute[] newAttributes = new Attribute[oldLength + 1];
        System.arraycopy(attributes, 0, newAttributes, 0, idx);
        System.arraycopy(attributes, idx, newAttributes, idx+1, oldLength-idx);
        for(int i=idx,n=oldLength ; i<n ; i++) {
            newAttributes[i+1].index++;
        }
        newAttributes[idx] = attribute;
        attributes = newAttributes;
        attribute.element = element;
        attribute.index = idx;
        attribute.firePropertyChange("parent", null, this);
        
        if(listeners != null) {
            for(AttributeListListener all : listeners) {
                all.attributeAdded(element, attribute, idx);
            }
        }
    }
    
    public void add(Attribute attribute) {
        add(size(), attribute);
    }
    
    public void remove(Attribute attribute) {
        if(attribute == null) {
            throw new NullPointerException("attribute");
        }
        if(attribute.element != element) {
            throw new IllegalStateException("attribute doesn't belong to this element");
        }
        remove(attribute.index);
    }
    
    public void remove(int idx) {
        if(attributes == null) {
            throw new IndexOutOfBoundsException();
        }
        
        Attribute attribute = attributes[idx];
        final int oldLength = attributes.length;
        if(oldLength == 1) {
            attributes = EMPTY_ATTRIBUTE_ARRAY;
        } else {
            final int newLength = oldLength - 1;
            Attribute[] newAttributes = new Attribute[newLength];
            System.arraycopy(attributes, 0, newAttributes, 0, idx);
            System.arraycopy(attributes, idx+1, newAttributes, idx, newLength-idx);
            for(int i=idx ; i<newLength ; i++) {
                newAttributes[i].index--;
            }
            attributes = newAttributes;
        }
        
        attribute.element = null;
        attribute.firePropertyChange("parent", this, null);
        
        if(listeners != null) {
            for(AttributeListListener all : listeners) {
                all.attributeRemoved(element, attribute, idx);
            }
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }
    
    public void toString(StringBuilder sb) {
        for(int i=0,n=attributes.length ; i<n ; i++) {
            if(i > 0) {
                sb.append(' ');
            }
            attributes[i].toString(sb);
        }
    }
    
    void attributeChanged(Attribute attribute, String oldValue, String newValue) {
        if(listeners != null) {
            for(AttributeListListener all : listeners) {
                all.attributeChanged(element, attribute, oldValue, newValue);
            }
        }
        Document.documentChanged(element);
    }

    public Iterator<Attribute> iterator() {
        return new I(attributes);
    }
    
    class I implements Iterator<Attribute> {
        private final Attribute[] attributes;
        private int idx;
        private Attribute a;

        I(Attribute[] attributes) {
            this.attributes = attributes;
        }
        
        public boolean hasNext() {
            return idx < attributes.length;
        }
        
        public Attribute next() {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }
            a = attributes[idx++];
            return a;
        }

        public void remove() {
            if(a == null) {
                throw new IllegalStateException("already removed");
            }
            AttributeList.this.remove(a);
            idx--;
            a = null;
        }
    }
}
