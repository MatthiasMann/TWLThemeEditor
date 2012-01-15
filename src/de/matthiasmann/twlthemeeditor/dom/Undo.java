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

import de.matthiasmann.twl.model.HasCallback;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.IdentityHashMap;

/**
 *
 * @author Matthias Mann
 */
public final class Undo extends HasCallback {
    
    static final ThreadLocal<ComplexOP> COMPLEX_OPERATION = new ThreadLocal<ComplexOP>() {
        @Override
        protected ComplexOP initialValue() {
            return new ComplexOP();
        }
    };
    
    final Step undoListHead;
    final ArrayList<Entry> undoStepBuilder;
    int maxUndoSteps;
    int numUndoSteps;
    boolean undoActive;
    Entry[] lastUndoEntry;
    long lastUndoTime;
    Object userState;
    
    public Undo() {
        this.undoListHead = new Step(null, null);
        this.undoStepBuilder = new ArrayList<Entry>();
        this.maxUndoSteps = 1000;
        this.lastUndoTime = System.currentTimeMillis();
        
        undoListHead.next = undoListHead;
        undoListHead.prev = undoListHead;
    }
    
    public void registerDocument(Document document) {
        addListeners(document, new L(document));
    }
    
    public boolean hasUndo() {
        return numUndoSteps > 0;
    }
    
    public boolean undo() {
        if(isComplexOperationActive()) {
            throw new IllegalStateException("Complex operation active");
        }
        Step step = undoListHead.next;
        if(step == undoListHead) {
            return false;
        }
        step.unlink();
        numUndoSteps--;
        lastUndoEntry = null;
        undoActive = true;
        try {
            undo(step.entries);
        } finally {
            undoActive = false;
            userState = step.userState;
        }
        doCallback();
        return true;
    }

    public Object getUserState() {
        return userState;
    }

    public void setUserState(Object userState) {
        this.userState = userState;
    }
    
    public static void startComplexOperation() {
        //System.out.println("Start");
        COMPLEX_OPERATION.get().inc();
    }
    
    public static void endComplexOperation() {
        //System.out.println("End");
        COMPLEX_OPERATION.get().dec();
    }
    
    public boolean isComplexOperationActive() {
        return COMPLEX_OPERATION.get().count > 0;
    }
    
    void registerUndo(Entry entry) {
        undoStepBuilder.add(entry);
        if(COMPLEX_OPERATION.get().canComplete(this)) {
            completeStep();
        }
    }
    
    void completeStep() {
        if(!undoStepBuilder.isEmpty()) {
            Step step = new Step(undoStepBuilder.toArray(new Entry[undoStepBuilder.size()]), userState);
            undoStepBuilder.clear();
            
            while(numUndoSteps >= maxUndoSteps) {
                undoListHead.prev.unlink();
                numUndoSteps--;
            }
            
            step.linkAfter(undoListHead);
            numUndoSteps++;
            lastUndoEntry = step.entries;
            lastUndoTime = System.currentTimeMillis();
            doCallback();
            //System.out.println("Step added: " + step.entries.length);
        }
    }
    
    static void undo(Entry[] step) {
        for(int i=step.length ; i-->0 ;) {
            step[i].undo();
        }
    }
    
    <T extends Entry> T checkLastUndoTime(Class<T> type) {
        if(lastUndoEntry != null && lastUndoEntry.length == 1 &&
                type.isInstance(lastUndoEntry[0]) &&
                (System.currentTimeMillis() - lastUndoTime) < 1000) {
            return type.cast(lastUndoEntry[0]);
        }
        return null;
    }
    
    static void addListeners(DomNode node, L listener) {
        if(node instanceof Element) {
            Element element = (Element)node;
            element.attributes.addListener(listener);
            element.addPropertyChangeListener(listener);
        } else if(node instanceof Text) {
            Text text = (Text)node;
            text.addPropertyChangeListener(listener);
        }
        
        if(node instanceof Parent) {
            Parent parent = (Parent)node;
            parent.addContentListener(listener);
            for(int i=0,n=parent.getContentSize() ; i<n ; i++) {
                addListeners(parent.getContent(i), listener);
            }
        }
    }
    
    static void removeListeners(DomNode node, L listener) {
        if(node instanceof Element) {
            Element element = (Element)node;
            element.attributes.removeListener(listener);
            element.removePropertyChangeListener(listener);
        } else if(node instanceof Text) {
            Text text = (Text)node;
            text.removePropertyChangeListener(listener);
        }
        
        if(node instanceof Parent) {
            Parent parent = (Parent)node;
            parent.removeContentListener(listener);
            for(int i=0,n=parent.getContentSize() ; i<n ; i++) {
                removeListeners(parent.getContent(i), listener);
            }
        }
    }
    
    static Entry makeContentRemoved(Document doc, Parent parent, Content content, int index) {
        if(content instanceof Text) {
            return new TextRemoved(doc, parent, (Text)content, index);
        }
        if(content instanceof Element) {
            return new ElementRemoved(doc, parent, (Element)content, index);
        }
        if(content instanceof Comment) {
            return new CommentRemoved(doc, parent, (Comment)content, index);
        }
        throw new UnsupportedOperationException("Can't recored data type: " + content.getClass());
    }
    
    static final class Step {
        final Entry[] entries;
        final Object userState;
        
        Step prev;
        Step next;

        Step(Entry[] entries, Object userState) {
            this.entries = entries;
            this.userState = userState;
        }
        
        void unlink() {
            if(next == this || prev == this) {
                throw new IllegalStateException("Can't unlink list head");
            }
            prev.next = next;
            next.prev = prev;
            prev = null;
            next = null;
        }
        
        void linkAfter(Step step) {
            if(prev != null || next != null) {
                throw new IllegalStateException("Already linked");
            }
            next = step.next;
            prev = step;
            next.prev = this;
            prev.next = this;
        }
    }
    
    static final class ComplexOP {
        final IdentityHashMap<Undo, Boolean> undos;
        int count;

        ComplexOP() {
            undos = new IdentityHashMap<Undo, Boolean>();
        }
        void inc() {
            ++count;
        }
        void dec() {
            if(count == 0) {
                throw new IllegalStateException("No complex operation active");
            }
            if(--count == 0) {
                for(Undo undo : undos.keySet()) {
                    undo.completeStep();
                }
                undos.clear();
            }
        }
        boolean canComplete(Undo undo) {
            if(count > 0) {
                undos.put(undo, Boolean.TRUE);
                return false;
            }
            return true;
        }
    }
    
    class L implements Parent.ContentListener, AttributeList.AttributeListListener, PropertyChangeListener {
        private final Document doc;

        L(Document doc) {
            this.doc = doc;
        }

        public void contentAdded(Parent parent, Content child, int index) {
            if(!undoActive) {
                registerUndo(new ContentAdded(doc, child));
            }
            addListeners(child, this);
        }

        public void contentRemoved(Parent parent, Content child, int index) {
            if(!undoActive) {
                registerUndo(makeContentRemoved(doc, parent, child, index));
            }
            removeListeners(child, this);
        }

        public void contentMoved(Parent parent, Content child, int oldIndex, int newIndex) {
            if(!undoActive) {
                registerUndo(new ContentMoved(doc, child, oldIndex));
            }
        }

        public void attributeAdded(Element element, Attribute attribute, int index) {
            if(!undoActive) {
                registerUndo(new AttrAdded(doc, element, attribute));
            }
            attribute.addPropertyChangeListener(this);
        }

        public void attributeRemoved(Element element, Attribute attribute, int index) {
            if(!undoActive) {
                registerUndo(new AttrRemoved(doc, element, attribute, index));
            }
            attribute.removePropertyChangeListener(this);
        }

        public void attributeChanged(Element element, Attribute attribute, String oldValue, String newValue) {
            if(!undoActive) {
                AttrChange attrChange = checkLastUndoTime(AttrChange.class);
                if(attrChange != null && attrChange.same(attribute)) {
                    return;
                }
                registerUndo(new AttrChange(doc, attribute, oldValue));
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if(!undoActive) {
                Object source = evt.getSource();
                String propertyName = evt.getPropertyName();
                if("value".equals(propertyName) && (source instanceof Text)) {
                    Text text = (Text)source;
                    TextChanged textChanged = checkLastUndoTime(TextChanged.class);
                    if(textChanged != null && textChanged.same(text)) {
                        return;
                    }
                    registerUndo(new TextChanged(doc, text, (String)evt.getOldValue()));
                    return;
                }
                if(source instanceof Element) {
                    Element element = (Element)source;
                    int propertyIdx = ElementNameChanged.getPropertyIndex(propertyName);
                    if(propertyIdx >= 0) {
                        registerUndo(new ElementNameChanged(doc, element, propertyIdx, evt.getOldValue()));
                    }
                }
            }
        }
    }
    
    static abstract class Entry {
        final Document doc;

        Entry(Document doc) {
            this.doc = doc;
        }
        
        abstract void undo();
    }
    
    static class TextChanged extends Entry {
        final long textID;
        final String oldValue;

        TextChanged(Document doc, Text text, String oldValue) {
            super(doc);
            this.textID = text.id;
            this.oldValue = oldValue;
        }

        @Override
        void undo() {
            Text text = (Text)doc.findNode(textID);
            text.setValue(oldValue);
        }

        boolean same(Text text) {
            return textID == text.id;
        }
    }
    
    static class AttrChange extends Entry {
        final long elementID;
        final long attrID;
        final String oldValue;

        AttrChange(Document doc, Attribute attribute, String oldValue) {
            super(doc);
            this.elementID = attribute.element.id;
            this.attrID = attribute.id;
            this.oldValue = oldValue;
        }

        @Override
        void undo() {
            Element e = (Element)doc.findNode(elementID);
            Attribute a = e.getAttributes().findAttribute(attrID);
            a.setValue(oldValue);
        }

        boolean same(Attribute attribute) {
            return attrID == attribute.id && elementID == attribute.element.id;
        }
    }
    
    static class AttrRemoved extends Entry {
        final long elementID;
        final long attrID;
        final String name;
        final Namespace ns;
        final String value;
        final int index;

        AttrRemoved(Document doc, Element element, Attribute attribute, int index) {
            super(doc);
            this.elementID = element.id;
            this.attrID = attribute.id;
            this.name = attribute.name;
            this.ns = attribute.namespace;
            this.value = attribute.value;
            this.index = index;
        }

        @Override
        void undo() {
            Element e = (Element)doc.findNode(elementID);
            Attribute a = new Attribute(attrID, name, ns, value);
            e.getAttributes().add(index, a);
        }
    }
    
    static class AttrAdded extends Entry {
        final long elementID;
        final long attrID;

        AttrAdded(Document doc, Element element, Attribute attribute) {
            super(doc);
            this.elementID = element.id;
            this.attrID = attribute.id;
        }

        @Override
        void undo() {
            Element e = (Element)doc.findNode(elementID);
            Attribute a = e.getAttributes().findAttribute(attrID);
            e.getAttributes().remove(a);
        }
    }
    
    static class ContentAdded extends Entry {
        final long contentID;

        ContentAdded(Document doc, Content content) {
            super(doc);
            this.contentID = content.id;
        }

        @Override
        void undo() {
            Content content = (Content)doc.findNode(contentID);
            content.getParent().removeContent(content);
        }
    }
    
    static class ContentMoved extends Entry {
        final long contentID;
        final int oldIndex;

        ContentMoved(Document doc, Content content, int oldIndex) {
            super(doc);
            this.contentID = content.id;
            this.oldIndex = oldIndex;
        }

        @Override
        void undo() {
            Content content = (Content)doc.findNode(contentID);
            content.getParent().moveContent(content.index, oldIndex);
        }
    }
    
    static class TextRemoved extends Entry {
        final long parentID;
        final long textID;
        final boolean cdata;
        final String value;
        final int index;

        TextRemoved(Document doc, Parent parent, Text text, int index) {
            super(doc);
            this.parentID = parent.getID();
            this.textID = text.id;
            this.cdata = text.cdata;
            this.value = text.value;
            this.index = index;
        }

        @Override
        void undo() {
            Parent parent = (Parent)doc.findNode(parentID);
            Text text = new Text(textID, value, cdata);
            parent.addContent(index, text);
        }
    }
    
    static class ElementRemoved extends Entry {
        final long parentID;
        final long elementID;
        final String name;
        final Namespace ns;
        final int index;
        final long[] attrIDs;
        final String[] attrNameValues;
        final Namespace[] attrNamespaces;
        final Entry[] children;

        ElementRemoved(Document doc, Parent parent, Element element, int index) {
            super(doc);
            this.parentID = parent.getID();
            this.elementID = element.id;
            this.name = element.name;
            this.ns = element.namespace;
            this.index = index;
            
            int attributeSize = element.attributes.size();
            if(attributeSize > 0) {
                this.attrIDs = new long[attributeSize];
                this.attrNameValues = new String[attributeSize*2];
                this.attrNamespaces = new Namespace[attributeSize];
                
                for(int i=0 ; i<attributeSize ; i++) {
                    Attribute a = element.attributes.data[i];
                    attrIDs[i] = a.id;
                    attrNameValues[i*2+0] = a.name;
                    attrNameValues[i*2+1] = a.value;
                    attrNamespaces[i] = a.namespace;
                }
            } else {
                this.attrIDs = null;
                this.attrNameValues = null;
                this.attrNamespaces = null;
            }
            
            int contentSize = element.getContentSize();
            if(contentSize > 0) {
                this.children = new Entry[contentSize];
                for(int i=0,j=contentSize-1 ; i<contentSize ; i++) {
                    children[j--] = makeContentRemoved(doc, element, element.content.data[i], i);
                }
            } else {
                this.children = null;
            }
        }

        @Override
        void undo() {
            Parent parent = (Parent)doc.findNode(parentID);
            Element element = new Element(elementID, name, ns);
            if(attrIDs != null) {
                int n = attrIDs.length;
                element.attributes.data = new Attribute[n];
                for(int i=0 ; i<n ; i++) {
                    element.attributes.data[i] = new Attribute(
                            attrIDs[i], attrNameValues[i*2], attrNamespaces[i], attrNameValues[i*2+1]);
                }
            }
            if(children != null) {
                Element oldUndoElement = doc.undoElement;
                doc.undoElement = element;
                try {
                    Undo.undo(children);
                } finally {
                    doc.undoElement = oldUndoElement;
                }
            }
            parent.addContent(index, element);
        }
    }
    
    static class CommentRemoved extends Entry {
        final long parentID;
        final long commentID;
        final String value;
        final int index;

        CommentRemoved(Document doc, Parent parent, Comment comment, int index) {
            super(doc);
            this.parentID = parent.getID();
            this.commentID = comment.id;
            this.value = comment.value;
            this.index = index;
        }

        @Override
        void undo() {
            Parent parent = (Parent)doc.findNode(parentID);
            Comment comment = new Comment(commentID, value);
            parent.addContent(index, comment);
        }
    }
    
    static class ElementNameChanged extends Entry {
        final long elementID;
        final int propertyIdx;
        final Object oldValue;

        static int getPropertyIndex(String propertyName) {
            if("name".equals(propertyName)) {
                return 0;
            }
            if("namespace".equals(propertyName)) {
                return 1;
            }
            return -1;
        }
        
        ElementNameChanged(Document doc, Element element, int propertyIdx, Object oldValue) {
            super(doc);
            this.elementID = element.id;
            this.propertyIdx = propertyIdx;
            this.oldValue = oldValue;
        }

        @Override
        void undo() {
            Element e = (Element)doc.findNode(elementID);
            switch(propertyIdx) {
                case 0:
                    e.setName((String)oldValue);
                    break;
                case 1:
                    e.setNamespace((Namespace)oldValue);
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }
}
