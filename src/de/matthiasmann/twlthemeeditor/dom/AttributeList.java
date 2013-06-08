/*
 * Copyright (c) 2008-2013, Matthias Mann
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
    
    Attribute[] data;
    AttributeListListener[] listeners;

    AttributeList(Element element) {
        this.element = element;
        this.data = EMPTY_ATTRIBUTE_ARRAY;
    }

    public AttributeList(Element element, AttributeList src) {
        int size = src.data.length;
        
        this.element = element;
        this.data = new Attribute[size];
        
        for(int i=0 ; i<size ; i++) {
            data[i] = src.data[i].clone();
            data[i].element = element;
            data[i].index = i;
        }
    }
    
    public void addListener(AttributeListListener listener) {
        listeners = CallbackSupport.addCallbackToList(listeners, listener, AttributeListListener.class);
    }
    
    public void removeListener(AttributeListListener listener) {
        listeners = CallbackSupport.removeCallbackFromList(listeners, listener);
    }

    public int size() {
        return data.length;
    }
    
    public boolean isEmpty() {
        return data.length == 0;
    }
    
    public Attribute[] toArray() {
        if(data.length == 0) {
            return EMPTY_ATTRIBUTE_ARRAY;
        }
        return data.clone();
    }
    
    public Attribute get(int idx) {
        return data[idx];
    }
    
    public Attribute get(String name, Namespace namespace) {
        for(Attribute attribute : data) {
            if(attribute.equals(name, namespace)) {
                return attribute;
            }
        }
        return null;
    }

    public Attribute set(String name, Namespace namespace, String value) {
        for(Attribute attribute : data) {
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
        for(Attribute attribute : data) {
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
        
        final int oldLength = data.length;
        Attribute[] newData = new Attribute[oldLength + 1];
        System.arraycopy(data, 0, newData, 0, idx);
        System.arraycopy(data, idx, newData, idx+1, oldLength-idx);
        for(int i=idx,n=oldLength ; i<n ; i++) {
            newData[i+1].index++;
        }
        newData[idx] = attribute;
        data = newData;
        attribute.element = element;
        attribute.index = idx;
        attribute.firePropertyChange("parent", null, this);
        
        if(listeners != null) {
            for(AttributeListListener all : listeners) {
                all.attributeAdded(element, attribute, idx);
            }
        }
        Document.documentChanged(element);
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
        if(data == null) {
            throw new IndexOutOfBoundsException();
        }
        
        Attribute attribute = data[idx];
        final int oldLength = data.length;
        if(oldLength == 1) {
            data = EMPTY_ATTRIBUTE_ARRAY;
        } else {
            final int newLength = oldLength - 1;
            Attribute[] newData = new Attribute[newLength];
            System.arraycopy(data, 0, newData, 0, idx);
            System.arraycopy(data, idx+1, newData, idx, newLength-idx);
            for(int i=idx ; i<newLength ; i++) {
                newData[i].index--;
            }
            data = newData;
        }
        
        attribute.element = null;
        attribute.firePropertyChange("parent", this, null);
        
        if(listeners != null) {
            for(AttributeListListener all : listeners) {
                all.attributeRemoved(element, attribute, idx);
            }
        }
        Document.documentChanged(element);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }
    
    public void toString(StringBuilder sb) {
        for(int i=0,n=data.length ; i<n ; i++) {
            if(i > 0) {
                sb.append(' ');
            }
            data[i].toString(sb);
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
        return new I(data);
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
