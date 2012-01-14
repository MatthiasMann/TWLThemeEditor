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

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Matthias Mann
 */
public final class Attribute extends PCS implements Value {
    
    static final AtomicLong ATTR_ID_GEN = new AtomicLong();
    
    final long id;
    final String name;
    final Namespace namespace;
    Element element;
    int index;
    String value;

    public Attribute(String name, Namespace namespace, String value) {
        this(ATTR_ID_GEN.incrementAndGet(), name, namespace, value);
        if(name == null) {
            throw new NullPointerException("name");
        }
        if(namespace == null) {
            throw new NullPointerException("namespace");
        }
        if(value == null) {
            throw new NullPointerException("value");
        }
    }

    public Attribute(String name, String value) {
        this(name, Namespace.NO_NAMESPACE, value);
    }

    Attribute(long id, String name, Namespace namespace, String value) {
        this.id = id;
        this.name = name;
        this.namespace = namespace;
        this.value = value;
    }

    public long getAttributeID() {
        return id;
    }

    public Element getElement() {
        return element;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public Namespace getNamespace() {
        return namespace;
    }
    
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if(value == null) {
            throw new NullPointerException("value");
        }
        String oldValue = this.value;
        if(!oldValue.equals(value)) {
            this.value = value;
            firePropertyChange("value", oldValue, value);
            if(element != null) {
                element.attributes.attributeChanged(this, oldValue, value);
            }
        }
    }

    public boolean isEmpty() {
        return value.length() == 0;
    }

    public boolean equals(String name, Namespace namespace) {
        if(this.name.equals(name)) {
            Namespace ns = this.namespace;
            if(ns == namespace || ns.uri.equals(namespace.uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
    
    public StringBuilder toString(StringBuilder sb) {
        return namespace.appendUri(sb).append(name).append("=\"").append(value).append('"');
    }
    
    @Override
    public Attribute clone() {
        return new Attribute(name, namespace, value);
    }
}
