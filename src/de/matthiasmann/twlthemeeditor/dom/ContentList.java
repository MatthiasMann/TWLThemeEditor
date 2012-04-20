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
import de.matthiasmann.twlthemeeditor.dom.Parent.ContentListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author Matthias Mann
 */
final class ContentList implements Iterable<Content> {
    
    private static final Content[] EMPTY = new Content[0];
    
    final Parent parent;
    Content[] data;
    int size;
    ContentListener[] contentListeners;

    ContentList(Parent parent) {
        this.parent = parent;
        this.data = EMPTY;
    }

    ContentList(Parent parent, ContentList src) {
        this.parent = parent;
        this.size = src.size;
        
        if(size > 0) {
            this.data = new Content[Math.max(8, size)];
            for(int i=0 ; i<size ; i++) {
                Content child = src.data[i].clone();
                this.data[i] = child;
                child.parent = parent;
                child.index = i;
            }
        } else {
            this.data = EMPTY;
        }
    }
    
    void addContentListener(ContentListener listener) {
        contentListeners = CallbackSupport.addCallbackToList(contentListeners, listener, ContentListener.class);
    }

    void removeContentListener(ContentListener listener) {
        contentListeners = CallbackSupport.removeCallbackFromList(contentListeners, listener);
    }

    Content get(int idx) {
        if(idx < 0 || idx >= size) {
            throw new IndexOutOfBoundsException();
        }
        return data[idx];
    }
    
    void fireAddEvent(Content child, int idx) {
        child.firePropertyChange("parent", null, this);
        if(contentListeners != null) {
            for(ContentListener cl : contentListeners) {
                cl.contentAdded(parent, child, idx);
            }
        }
        Document.documentChanged(parent);
    }
    
    void fireRemoveEvent(Content child, int idx) {
        child.firePropertyChange("parent", this, null);
        if(contentListeners != null) {
            for(ContentListener cl : contentListeners) {
                cl.contentRemoved(parent, child, idx);
            }
        }
        Document.documentChanged(parent);
    }
    
    void fireMoveEvent(Content child, int oldIndex, int newIndex) {
        if(contentListeners != null) {
            for(ContentListener cl : contentListeners) {
                cl.contentMoved(parent, child, oldIndex, newIndex);
            }
        }
        Document.documentChanged(parent);
    }
    
    void add(int idx, Content child) {
        if(size == data.length) {
            Content[] tmp = new Content[Math.max(8, size * 2)];
            System.arraycopy(data,   0, tmp,     0,      idx);
            System.arraycopy(data, idx, tmp, idx+1, size-idx);
            data = tmp;
        } else {
            System.arraycopy(data, idx, data, idx+1, size-idx);
        }
        
        child.parent = parent;
        data[idx] = child;
        size++;
        
        reindex(idx);
    }

    Content remove(int idx) {
        Content child = data[idx];
        --size;
        System.arraycopy(data, idx+1, data, idx, size-idx);
        data[size] = null;
        child.parent = null;
        reindex(idx);
        return child;
    }
    
    void move(int from, int to) {
        Content tmp = data[from];
        if(from < to) {
            System.arraycopy(data, from+1, data, from, to-from);
            data[to] = tmp;
            reindex(from);
        } else {
            System.arraycopy(data, to, data, to+1, from-to);
            data[to] = tmp;
            reindex(to);
        }
    }
    
    Element getChild(String name, Namespace namespace) {
        Content[] d = data;
        for(int i=0,n=size ; i<n ; i++) {
            Content c = d[i];
            if(c instanceof Element) {
                Element e = (Element)c;
                if(e.equals(name, namespace)) {
                    return e;
                }
            }
        }
        return null;
    }
    
    DomNode findNode(long id) {
        Content[] d = data;
        for(int i=0,n=size ; i<n ; i++) {
            DomNode node = d[i].findNode(id);
            if(node != null) {
                return node;
            }
        }
        return null;
    }
    
    Text findTextNode() {
        for(int i=0,n=size ; i<n ; i++) {
            Content c = data[i];
            if(c instanceof Text) {
                Text text = (Text)c;
                if(!text.isCData()) {
                    return text;
                }
            }
        }
        return null;
    }
    
    void serialize(XmlSerializer xs) throws IOException {
        for(int i=0,n=size ; i<n ; i++) {
            data[i].serialize(xs);
        }
    }
    
    public Iterator<Content> iterator() {
        return new Iterator<Content>() {
            int idx = 0;
            Content c;

            public boolean hasNext() {
                return idx < size;
            }
            public Content next() {
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }
                c = data[idx++];
                return c;
            }
            public void remove() {
                if(c == null) {
                    throw new IllegalStateException("already removed");
                }
                parent.removeContent(c);
                c = null;
                idx--;
            }
        };
    }
    
    private void reindex(int idx) {
        Content[] d = data;
        for(int n=size ; idx<n ; idx++) {
            d[idx].index = idx;
        }
    }
}
