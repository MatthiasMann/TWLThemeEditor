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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author Matthias Mann
 */
class NameFilter implements Iterable<Element> {
    
    final Iterable<Content> src;
    final String name;
    final Namespace namespace;

    NameFilter(Iterable<Content> src, String name, Namespace namespace) {
        this.src = src;
        this.name = name;
        this.namespace = namespace;
    }

    public Iterator<Element> iterator() {
        final Iterator<Content> iter = src.iterator();
        return new Iterator<Element>() {
            private Element element;
            public boolean hasNext() {
                if(element != null) {
                    return true;
                }
                while(iter.hasNext()) {
                    Content c = iter.next();
                    if(c instanceof Element) {
                        Element e = (Element)c;
                        if(e.equals(name, namespace)) {
                            element = e;
                            return true;
                        }
                    }
                }
                return false;
            }
            public Element next() {
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }
                Element e = element;
                element = null;
                return e;
            }
            public void remove() {
                iter.remove();
            }
        };
    }    
}
