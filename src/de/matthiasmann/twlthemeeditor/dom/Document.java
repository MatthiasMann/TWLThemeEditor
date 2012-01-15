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
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author Matthias Mann
 */
public final class Document extends HasCallback implements Parent {
    
    static final AtomicLong ID_GEN = new AtomicLong();
    
    final long id;
    final ContentList content;
    final HashMap<String, Object> properties;
    
    Namespace[] namespaces;
    Element undoElement;

    public Document() {
        this.id = ID_GEN.incrementAndGet();
        this.content = new ContentList(this);
        this.properties = new HashMap<String, Object>();
    }

    public Document(Element rootElement) {
        this();
        addContent(rootElement);
    }

    public Document getDocument() {
        return this;
    }

    public long getID() {
        return id;
    }

    public DomNode findNode(long id) {
        if(this.id == id) {
            return this;
        }
        if(undoElement != null) {
            DomNode node = undoElement.findNode(id);
            if(node != null) {
                return node;
            }
        }
        return content.findNode(id);
    }
    
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public void addContentListener(ContentListener listener) {
        content.addContentListener(listener);
    }

    public void removeContentListener(ContentListener listener) {
        content.removeContentListener(listener);
    }

    public int getContentSize() {
        return content.size;
    }

    public Content getContent(int idx) {
        return content.get(idx);
    }

    public Element getRootElement() {
        for(int i=0,n=content.size ; i<n ; i++) {
            Content c = content.data[i];
            if(c instanceof Element) {
                return (Element)c;
            }
        }
        return null;
    }
    
    public Element detachRootElement() {
        Element rootElement = getRootElement();
        if(rootElement != null) {
            removeContent(rootElement);
        }
        return rootElement;
    }
    
    public void addContent(int idx, Content child) {
        if(child == null) {
            throw new NullPointerException("child");
        }
        if(child.parent != null) {
            throw new IllegalStateException("child already has a parent");
        }
        if(idx < 0 || idx > content.size) {
            throw new IndexOutOfBoundsException();
        }
        
        content.add(idx, child);
        content.fireAddEvent(child, idx);
    }

    public void addContent(Content child) {
        addContent(content.size, child);
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
        if(idx < 0 || idx >= content.size) {
            throw new IndexOutOfBoundsException();
        }
        
        Content child = content.remove(idx);
        content.fireRemoveEvent(child, idx);
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
    }

    public Iterator<Content> iterator() {
        return content.iterator();
    }
    
    public void serialize(OutputStream out, String encoding) throws IOException {
        TWLXmlSerializer xs = new TWLXmlSerializer();
        xs.setOutput(out, encoding);
        try {
            serialize(xs, encoding, null);
        } finally {
            xs.flush();
        }
    }
    
    public void serialize(Writer out) throws IOException {
        TWLXmlSerializer xs = new TWLXmlSerializer();
        xs.setOutput(out);
        try {
            serialize(xs);
        } finally {
            xs.flush();
        }
    }
    
    public void serialize(XmlSerializer xs) throws IOException {
        serialize(xs, "UTF-8", null);
    }
    
    public void serialize(XmlSerializer xs, String encoding, Boolean standAlone) throws IOException {
        xs.startDocument(encoding, standAlone);
        content.serialize(xs);
        xs.endDocument();
    }
    
    private static final int NAMESPACE_HASH_SIZE = 16;
    
    public Namespace getNamespace(String prefix, String uri) {
        if(prefix == null) {
            throw new NullPointerException("prefix");
        }
        if(uri == null) {
            throw new NullPointerException("uri");
        }
        if(namespaces == null) {
            namespaces = new Namespace[NAMESPACE_HASH_SIZE];
        }
        int hash = Namespace.hashCode(prefix, uri);
        int idx = hash & (NAMESPACE_HASH_SIZE-1);
        for(Namespace ns = namespaces[idx] ; ns!=null ; ns=ns.next) {
            if(ns.hash == hash && ns.prefix.equals(prefix) && ns.uri.equals(uri)) {
                return ns;
            }
        }
        Namespace ns = new Namespace(prefix, uri);
        ns.next = namespaces[idx];
        namespaces[idx] = ns;
        return ns;
    }
    
    public static Document load(XmlPullParser xpp) throws IOException, XmlPullParserException {
        try {
            xpp.setFeature("http://xmlpull.org/v1/doc/features.html#xml-roundtrip", true);
        } catch(XmlPullParserException ignore) {
        }
        
        xpp.require(XmlPullParser.START_DOCUMENT, null, null);
        Document document = new Document();
        ArrayList<Parent> open = new ArrayList<Parent>();
        Parent tos = document;
        StringBuilder sb = new StringBuilder();
        int[] textStartAndLen = new int[2];
        int token = xpp.nextToken();
        outer: for(;;) {
            switch(token) {
                case XmlPullParser.END_DOCUMENT:
                    if(!open.isEmpty()) {
                        xpp.require(XmlPullParser.END_TAG, null, null);
                    }
                    return document;
                case XmlPullParser.COMMENT:
                    tos.addContent(new Comment(xpp.getText()));
                    break;
                case XmlPullParser.DOCDECL:
                    tos.addContent(new DocType(xpp.getText()));
                    break;
                case XmlPullParser.IGNORABLE_WHITESPACE:
                case XmlPullParser.ENTITY_REF:
                case XmlPullParser.TEXT:
                    sb.setLength(0);
                    for(;;) {
                        switch(token) {
                            case XmlPullParser.TEXT:
                                char[] ca = xpp.getTextCharacters(textStartAndLen);
                                sb.append(ca, textStartAndLen[0], textStartAndLen[1]);
                                break;
                            case XmlPullParser.IGNORABLE_WHITESPACE:
                            case XmlPullParser.ENTITY_REF:
                                sb.append(xpp.getText());
                                break;
                            default:
                                tos.addContent(new Text(sb.toString()));
                                continue outer;
                        }
                        token = xpp.nextToken();
                    }
                case XmlPullParser.PROCESSING_INSTRUCTION:
                    tos.addContent(new ProcessingInstruction(xpp.getText()));
                    break;
                case XmlPullParser.CDSECT:
                    tos.addContent(new Text(xpp.getText()));
                    break;
                case XmlPullParser.START_TAG: {
                    String prefix = xpp.getPrefix();
                    Namespace namespace = Namespace.NO_NAMESPACE;
                    if(prefix != null) {
                        namespace = document.getNamespace(prefix, xpp.getNamespace());
                    }
                    Element e = new Element(xpp.getName(), namespace);
                    for(int i=0,n=xpp.getAttributeCount() ; i<n ; i++) {
                        prefix = xpp.getAttributePrefix(i);
                        namespace = Namespace.NO_NAMESPACE;
                        if(prefix != null) {
                            namespace = document.getNamespace(prefix, xpp.getNamespace());
                        }
                        e.setAttribute(xpp.getAttributeName(i), namespace, xpp.getAttributeValue(i));
                    }
                    tos.addContent(e);
                    open.add(tos);
                    tos = e;
                    break;
                }
                case XmlPullParser.END_TAG:
                    if(tos instanceof Element) {
                        Element e = (Element)tos;
                        xpp.require(XmlPullParser.END_TAG, e.namespace.xmlNS(), e.name);
                    } else {
                        xpp.require(XmlPullParser.END_DOCUMENT, null, null);
                    }
                    tos = open.remove(open.size() - 1);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown token " + token);
            }
            token = xpp.nextToken();
        }
    }

    static void documentChanged(DomNode content) {
        Document document = content.getDocument();
        if(document != null) {
            document.doCallback();
        }
    }
}
