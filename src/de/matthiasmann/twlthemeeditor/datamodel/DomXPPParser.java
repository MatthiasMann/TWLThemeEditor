/*
 * Copyright (c) 2008-2010, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.datamodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.Text;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author Matthias Mann
 */
public class DomXPPParser implements XmlPullParser {

    private final ArrayList<Object> events;
    private Iterator<Object> iter;
    private Object currentEvent;
    private int depth;

    public DomXPPParser() {
        this.events = new ArrayList<Object>();
        events.add(Boolean.TRUE);   // this causes teh START_DOCUMENT event
    }

    @SuppressWarnings("unchecked")
    public void addElement(Element e) {
        events.add(new StartTag(e.getName(), e.getAttributes()));
        for(Object child : e.getContent()) {
            if(child instanceof Element) {
                addElement((Element)child);
            } else if(child instanceof Text) {
                addText(((Text)child).getText());
            } else if(child instanceof CDATA) {
                addText(((CDATA)child).getText());
            } else {
                //System.out.println("Ignoring: " + child.getClass());
            }
        }
        events.add(new EndTag(e.getName()));
    }

    public void addStartTag(String name, Collection<Attribute> attributes) {
        events.add(new StartTag(name, attributes));
    }
    
    public void addStartTag(String name, Attribute ... attributes) {
        events.add(new StartTag(name, attributes));
    }

    public void addEndTag(String name) {
        events.add(new EndTag(name));
    }
    
    public void addText(String text) {
        Object lastEvent = events.get(events.size() - 1);
        if(lastEvent instanceof String) {
            events.set(events.size()-1, ((String)lastEvent).concat(text));
        } else {
            events.add(text);
        }
    }

    public void defineEntityReplacementText(String entityName, String replacementText) throws XmlPullParserException {
    }

    public int getAttributeCount() {
        return ((StartTag)currentEvent).attributes.length;
    }

    public String getAttributeName(int index) {
        return ((StartTag)currentEvent).attributes[index].getName();
    }

    public String getAttributeNamespace(int index) {
        return null;
    }

    public String getAttributePrefix(int index) {
        return null;
    }

    public String getAttributeType(int index) {
        return "CDATA";
    }

    public String getAttributeValue(int index) {
        return ((StartTag)currentEvent).attributes[index].getValue();
    }

    public String getAttributeValue(String namespace, String name) {
        if(namespace == null) {
            for(Attribute a : ((StartTag)currentEvent).attributes) {
                if(a.getName().equals(name)) {
                    return a.getValue();
                }
            }
        }
        return null;
    }

    public int getColumnNumber() {
        return 0;
    }

    public int getDepth() {
        return depth;
    }

    public int getEventType() throws XmlPullParserException {
        if(iter == null) {
            return next();
        }
        if(currentEvent instanceof StartTag) {
            return START_TAG;
        }
        if(currentEvent instanceof String) {
            return TEXT;
        }
        if(currentEvent instanceof EndTag) {
            return END_TAG;
        }
        if(iter.hasNext()) {
            return START_DOCUMENT;
        }
        return END_DOCUMENT;
    }

    public boolean getFeature(String name) {
        return false;
    }

    public String getInputEncoding() {
        return "UTF8";
    }

    public int getLineNumber() {
        return 0;
    }

    public String getName() {
        if(currentEvent instanceof StartTag) {
            return ((StartTag)currentEvent).name;
        }
        if(currentEvent instanceof EndTag) {
            return ((EndTag)currentEvent).name;
        }
        return null;
    }

    public String getNamespace(String prefix) {
        return null;
    }

    public String getNamespace() {
        return null;
    }

    public int getNamespaceCount(int depth) throws XmlPullParserException {
        return 0;
    }

    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        return null;
    }

    public String getNamespaceUri(int pos) throws XmlPullParserException {
        return null;
    }

    public String getPositionDescription() {
        return "?";
    }

    public String getPrefix() {
        return null;
    }

    public Object getProperty(String name) {
        return null;
    }

    public String getText() {
        return (String)currentEvent;
    }

    public char[] getTextCharacters(int[] holderForStartAndLength) {
        String s = getText();
        holderForStartAndLength[0] = 0;
        holderForStartAndLength[1] = s.length();
        return s.toCharArray();
    }

    public boolean isAttributeDefault(int index) {
        return false;
    }

    public boolean isEmptyElementTag() throws XmlPullParserException {
        return false;
    }

    public boolean isWhitespace() throws XmlPullParserException {
        return getText().trim().isEmpty();
    }

    public int next() throws XmlPullParserException {
        if(iter == null) {
            iter = events.iterator();
        }
        currentEvent = iter.next();
        int eventType = getEventType();
        switch (eventType) {
            case START_TAG:
                depth++;
                break;
            case END_TAG:
                depth--;
                break;
        }
        return eventType;
    }

    public int nextTag() throws XmlPullParserException, IOException {
        int eventType = next();
        if(eventType == TEXT && isWhitespace()) {   // skip whitespace
            eventType = next();
        }
        if(eventType != START_TAG && eventType != END_TAG) {
            throw new XmlPullParserException("expected start or end tag", this, null);
        }
        return eventType;
    }

    public String nextText() throws XmlPullParserException, IOException {
        if(getEventType() != START_TAG) {
            throw new XmlPullParserException("parser must be on START_TAG to read next text", this, null);
        }
        int eventType = next();
        if(eventType == TEXT) {
            String result = getText();
            eventType = next();
            if(eventType != END_TAG) {
                throw new XmlPullParserException("event TEXT it must be immediately followed by END_TAG", this, null);
            }
            return result;
        } else if(eventType == END_TAG) {
            return "";
        } else {
            throw new XmlPullParserException("parser must be on START_TAG or TEXT to read text", this, null);
        }
    }

    public int nextToken() throws XmlPullParserException, IOException {
        return next();
    }

    public void require(int type, String namespace, String name) throws XmlPullParserException, IOException {
        int eventType = getEventType();
        if(type != eventType) {
            throw new XmlPullParserException("expected event " + getEventTypeStr(type) + " got " + getEventTypeStr(eventType), this, null);
        }
        if(name != null && !name.equals(getName())) {
            throw new XmlPullParserException("expected name '" + name + "' got '" + getName() + "'", this, null);
        }
    }

    private String getEventTypeStr(int type) {
        switch (type) {
            case START_DOCUMENT:
                return "START_DOCUMENT";
            case START_TAG:
                return "START_TAG";
            case TEXT:
                return "TEXT";
            case END_TAG:
                return "END_TAG";
            case END_DOCUMENT:
                return "END_DOCUMENT";
            default:
                return "?";
        }
    }
    
    public void setFeature(String name, boolean state) throws XmlPullParserException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setInput(Reader in) throws XmlPullParserException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setInput(InputStream inputStream, String inputEncoding) throws XmlPullParserException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setProperty(String name, Object value) throws XmlPullParserException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    static class StartTag {
        final String name;
        final Attribute[] attributes;

        StartTag(String name, Attribute[] attributes) {
            this.name = name;
            this.attributes = attributes;
        }
        StartTag(String name, Collection<Attribute> attributes) {
            this.name = name;
            this.attributes = attributes.toArray(new Attribute[attributes.size()]);
        }
    }

    static class EndTag {
        final String name;

        EndTag(String name) {
            this.name = name;
        }
    }

}
