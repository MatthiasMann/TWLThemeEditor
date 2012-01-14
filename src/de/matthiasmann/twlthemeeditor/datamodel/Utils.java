/*
 * Copyright (c) 2008-2011, Matthias Mann
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

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.DialogLayout.Gap;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.XMLParser;
import de.matthiasmann.twlthemeeditor.dom.Attribute;
import de.matthiasmann.twlthemeeditor.dom.AttributeList;
import de.matthiasmann.twlthemeeditor.dom.Document;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import org.xmlpull.v1.XmlPullParser;

/**
 *
 * @author Matthias Mann
 */
public final class Utils {

    public static int parseInt(String value) throws NumberFormatException {
        if(value.startsWith("0x")) {
            return Integer.parseInt(value.substring(2), 16);
        } else {
            return Integer.parseInt(value, 10);
        }
    }
    
    public static int[] parseInts(String value) {
        int count = 1;
        for(int pos=-1 ; (pos=value.indexOf(',', pos+1)) >= 0 ;) {
            count++;
        }
        int[] result = new int[count];
        for(int i=0,pos=0 ; i<count ; i++) {
            int end = value.indexOf(',', pos);
            if(end < 0) {
                end = value.length();
            }
            String part = value.substring(pos, end);
            result[i] = Integer.parseInt(part.trim());
            pos = end+1;
        }
        return result;
    }

    public static String toString(int[] ints) {
        if(ints.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for(int i : ints) {
            sb.append(',').append(i);
        }
        return sb.substring(1);
    }
    
    public static Border parseBorder(String value) {
        if(value == null) {
            return null;
        }
        int[] values = parseInts(value);
        if(values == null) {
            return null;
        }
        switch (values.length) {
            case 1:
                return new Border(values[0]);
            case 2:
                return new Border(values[0], values[1]);
            case 4:
                return new Border(values[0], values[1], values[2], values[3]);
            default:
                return null;
        }
    }

    public static Gap parseGap(String value) {
        if(value == null) {
            return null;
        }
        if(value.length() == 0) {
            return new Gap();
        }
        int[] values = parseInts(value);
        if(values == null) {
            return null;
        }
        switch (values.length) {
            case 1:
                return new Gap(values[0]);
            case 2:
                return new Gap(values[0], values[1]);
            case 3:
                return new Gap(values[0], values[1], values[2]);
            default:
                return null;
        }
    }

    public static Dimension parseDimension(String value) {
        if(value == null) {
            return null;
        }
        int[] values = parseInts(value);
        if(values == null) {
            return null;
        }
        switch (values.length) {
            case 1:
                return new Dimension(values[0], values[0]);
            case 2:
                return new Dimension(values[0], values[1]);
            default:
                return null;
        }
    }

    public static String toString(Border border, boolean canBeNull) {
        if(border == null) {
            return null;
        }
        if(border instanceof BorderFormula) {
            return border.toString();
        }
        if(border.getBorderTop() == border.getBorderBottom() && border.getBorderLeft() == border.getBorderRight()) {
            if(border.getBorderTop() == border.getBorderLeft()) {
                if(canBeNull && border.getBorderTop() == 0) {
                    return null;
                }
                return Integer.toString(border.getBorderTop());
            }
            return border.getBorderLeft()+","+border.getBorderTop();
        }
        return border.getBorderTop()+","+border.getBorderLeft()+","+border.getBorderBottom()+","+border.getBorderRight();
    }

    public static String capitalize(String str) {
        if(str.length() > 0) {
            return Character.toUpperCase(str.charAt(0)) + str.substring(1);
        }
        return str;
    }
    
    public static void addToXPP(DomXPPParser xpp, ThemeTreeNode node) {
        for(int i=0,n=node.getNumChildren() ; i<n ; i++) {
            TreeTableNode child = node.getChild(i);
            if(child instanceof ThemeTreeNode) {
                ((ThemeTreeNode)child).addToXPP(xpp);
            }
        }
    }

    public static void addToXPP(DomXPPParser xpp, String tagName, ThemeTreeNode node, AttributeList attributes) {
        addToXPP(xpp, tagName, node, attributes.toArray());
    }
    
    public static void addToXPP(DomXPPParser xpp, String tagName, ThemeTreeNode node, Attribute ... attributes) {
        xpp.addStartTag(node, tagName, attributes);
        addToXPP(xpp, node);
        xpp.addEndTag(tagName);
    }

    public static Document loadDocument(URL url) throws IOException {
        try {
            InputStream is = url.openStream();
            try {
                return loadDocument(is);
            } finally {
                is.close();
            }
        } catch(IOException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new IOException(ex);
        }
    }

    public static Document loadDocument(InputStream is) throws IOException {
        try {
            XmlPullParser xpp = XMLParser.createParser();
            xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            xpp.setInput(is, "UTF-8");
            return Document.load(xpp);
        } catch(IOException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new IOException(ex);
        }
    }

    public static Document loadDocument(String str) throws IOException {
        try {
            XmlPullParser xpp = XMLParser.createParser();
            xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            xpp.setInput(new StringReader(str));
            return Document.load(xpp);
        } catch(IOException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new IOException(ex);
        }
    }

    public static<T> int find(ListModel<T> list, T entry) {
        for(int i=0,n=list.getNumEntries() ; i<n ; i++) {
            if(equals(list.getEntry(i), entry)) {
                return  i;
            }
        }
        return -1;
    }
    
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
