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
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author Matthias Mann
 */
public abstract class Content extends PCS implements DomNode {
    
    final long id;
    Parent parent;
    int index;

    static long genID() {
        return Document.ID_GEN.incrementAndGet();
    }
    
    Content() {
        this(genID());
    }

    Content(long id) {
        this.id = id;
    }

    public Parent getParent() {
        return parent;
    }
    
    public Element getParentElement() {
        return (parent instanceof Element) ? (Element)parent : null;
    }

    public void detach() {
        if(parent != null) {
            parent.removeContent(this);
        }
    }
    
    public long getID() {
        return id;
    }

    public DomNode findNode(long id) {
        if(this.id == id) {
            return this;
        }
        return null;
    }
    
    public Document getDocument() {
        return (parent != null) ? parent.getDocument() : null;
    }
    
    public int getIndex() {
        return (parent != null) ? index : -1;
    }
    
    public Content getPrevSibling() {
        if(parent != null && index > 0) {
            return parent.getContent(index - 1);
        }
        return null;
    }
    
    public Content getNextSibling() {
        if(parent != null && index+1 < parent.getContentSize()) {
            return parent.getContent(index + 1);
        }
        return null;
    }
    
    @Override
    public abstract Content clone();
    
    public abstract void serialize(XmlSerializer xs) throws IOException;
}
