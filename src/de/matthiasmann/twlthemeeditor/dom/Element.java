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

import java.io.IOException;
import java.util.Iterator;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author Matthias Mann
 */
public final class Element extends Content implements Parent {
    
    final AttributeList attributes;
    final ContentList content;
    
    String name;
    Namespace namespace;
    ElementText elementText;

    public Element(String name, Namespace namespace) {
        this(genID(), name, namespace);
        if(name == null) {
            throw new NullPointerException("name");
        }
        if(namespace == null) {
            throw new NullPointerException("namespace");
        }
    }

    Element(long id, String name, Namespace namespace) {
        super(id);
        this.attributes = new AttributeList(this);
        this.content = new ContentList(this);
        this.name = name;
        this.namespace = namespace;
    }

    public Element(Element src) {
        super(genID());
        this.attributes = new AttributeList(this, src.attributes);
        this.content = new ContentList(this, src.content);
        this.name = src.name;
        this.namespace = src.namespace;
    }
    
    public Element(String name) {
        this(name, Namespace.NO_NAMESPACE);
    }

    public String getName() {
        return name;
    }
    
    public Namespace getNamespace() {
        return namespace;
    }

    public void setName(String name) {
        if(name == null) {
            throw new NullPointerException("name");
        }
        String oldName = this.name;
        if(!oldName.equals(name)) {
            this.name = name;
            firePropertyChange("name", oldName, name);
        }
    }

    public void setNamespace(Namespace namespace) {
        if(namespace == null) {
            throw new NullPointerException("namespace");
        }
        Namespace oldNamespace = this.namespace;
        if(!oldNamespace.equals(namespace)) {
            this.namespace = namespace;
            firePropertyChange("namespace", oldNamespace, namespace);
        }
    }
    
    public void addContentListener(ContentListener listener) {
        content.addContentListener(listener);
    }

    public void removeContentListener(ContentListener listener) {
        content.removeContentListener(listener);
    }

    public boolean isRootElement() {
        return parent instanceof Document;
    }

    public int getContentSize() {
        return content.size;
    }

    public Content getContent(int idx) {
        return content.get(idx);
    }

    public Iterator<Content> iterator() {
        return content.iterator();
    }

    public Element getChild(String name, Namespace namespace) {
        return content.getChild(name, namespace);
    }
    
    public Element getChild(String name) {
        return content.getChild(name, Namespace.NO_NAMESPACE);
    }
    
    public Iterable<Element> getChildren(String name, Namespace namespace) {
        return new NameFilter(content, name, namespace);
    }
    
    public Iterable<Element> getChildren(String name) {
        return getChildren(name, Namespace.NO_NAMESPACE);
    }
    
    public void addContent(int idx, Content child) {
        if(child == null) {
            throw new NullPointerException("child");
        }
        if(child.parent != null) {
            throw new IllegalStateException("child already has a parent");
        }
        if(idx < 0 || idx > getContentSize()) {
            throw new IndexOutOfBoundsException();
        }
        
        content.add(idx, child);
        content.fireAddEvent(child, idx);
        textChanged(child);
    }

    public void addContent(Content content) {
        addContent(getContentSize(), content);
    }
    
    public void addContent(String text) {
        addContent(new Text(text));
    }
    
    public void removeContent(Content child) {
        if(child == null) {
            throw new NullPointerException("child");
        }
        if(child.parent != this) {
            throw new IllegalStateException("not a direct child");
        }
        removeContent(child.index);
    }

    public Content removeContent(int idx) {
        if(content == null || idx < 0 || idx >= content.size) {
            throw new IndexOutOfBoundsException();
        }
        
        Content child = content.remove(idx);
        content.fireRemoveEvent(child, idx);
        textChanged(child);
        return child;
    }

    public void moveContent(int fromIndex, int toIndex) {
        Content child = content.get(fromIndex);
        if(toIndex < 0 || toIndex >= content.size) {
            throw new IndexOutOfBoundsException();
        }
        
        if(toIndex == fromIndex) {
            return;
        }
        
        content.move(fromIndex, toIndex);
        content.fireMoveEvent(child, fromIndex, toIndex);
        textChanged(child);
    }

    public int indexOf(Content child) {
        if(child.parent != this) {
            return -1;
        }
        return child.index;
    }
    
    boolean disableTextChanged() {
        if(elementText != null) {
            return elementText.disableEvent();
        }
        return false;
    }
    
    void enableTextChanged(boolean oldState) {
        if(elementText != null) {
            elementText.disableEvent = oldState;
            elementText.textChanged();
        }
    }
    
    void textChanged(Content child) {
        if(elementText != null && (child instanceof Text)) {
            elementText.textChanged();
        }
    }
    
    void textChanged() {
        if(elementText != null) {
            elementText.textChanged();
        }
    }
    
    public ElementText getElementText() {
        if(elementText == null) {
            elementText = new ElementText(this);
        }
        return elementText;
    }
    
    public void setText(String value) {
        if(elementText != null) {
            elementText.setText(value);
        } else {
            doSetText(value);
            Document.documentChanged(this);
        }
    }
    
    void doSetText(String value) {
        Undo.startComplexOperation();
        try {
            // step 1: find Text note to reuse (no CDATA text)
            Text textNode = content.findTextNode();
            
            // step 2: remove all other nodes
            for(int i=content.size; i-->0 ;) {
                Content c = content.data[i];
                if(c != textNode) {
                    removeContent(i);
                }
            }
            
            if(textNode != null) {
                textNode.setValue(value);
            } else if(value.length() > 0) {
                textNode = new Text(value);
                addContent(textNode);
            }
        } finally {
            Undo.endComplexOperation();
        }
    }
    
    public AttributeList getAttributes() {
        return attributes;
    }
    
    public Attribute getAttribute(String name, Namespace namespace) {
        return attributes.get(name, namespace);
    }
    
    public Attribute getAttribute(String name) {
        return attributes.get(name, Namespace.NO_NAMESPACE);
    }
    
    public String getAttributeValue(String name, Namespace namespace) {
        Attribute attribute = attributes.get(name, namespace);
        return (attribute != null) ? attribute.value : null;
    }
    
    public String getAttributeValue(String name) {
        return getAttributeValue(name, Namespace.NO_NAMESPACE);
    }
    
    public Element setAttribute(String name, Namespace namespace, String value) {
        attributes.set(name, namespace, value);
        return this;
    }
    
    public Element setAttribute(String name, String value) {
        attributes.set(name, Namespace.NO_NAMESPACE, value);
        return this;
    }
    
    public void removeAttribute(String name, Namespace namespace) {
        Attribute attribute = getAttribute(name, namespace);
        if(attribute != null) {
            attributes.remove(attribute);
        }
    }
    
    public void removeAttribute(String name) {
        removeAttribute(name, Namespace.NO_NAMESPACE);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        namespace.appendUri(sb.append('<')).append(name);
        if(!attributes.isEmpty()) {
            attributes.toString(sb.append(' '));
        }
        if(content.size == 0) {
            sb.append("/>");
        } else {
            sb.append("> ... ").append(content.size).append(" children ... </");
            namespace.appendUri(sb).append(name).append('>');
        }
        return sb.toString();
    }

    @Override
    public Element clone() {
        return new Element(this);
    }
    
    public void serialize(XmlSerializer xs) throws IOException {
        String ns = null;
        if(namespace.uri.length() > 0) {
            if(namespace.prefix.length() > 0) {
                xs.setPrefix(namespace.prefix, namespace.uri);
            }
            ns = namespace.uri;
        }
        
        xs.startTag(ns, name);
        for(int i=0,n=attributes.size() ; i<n ; i++) {
            Attribute attribute = attributes.get(i);
            xs.attribute(attribute.namespace.xmlNS(), attribute.name, attribute.value);
        }
        if(content != null) {
            content.serialize(xs);
        }
        xs.endTag(ns, name);
    }

    @Override
    public DomNode findNode(long id) {
        if(this.id == id) {
            return this;
        }
        if(content != null) {
            return content.findNode(id);
        }
        return null;
    }

    public boolean equals(String name, Namespace namespace) {
        return this.name.equals(name) && this.namespace.equals(namespace);
    }
}
