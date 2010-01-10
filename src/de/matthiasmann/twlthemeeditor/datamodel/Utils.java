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

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twl.model.TreeTableNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public final class Utils {

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
            result[i] = Integer.parseInt(part);
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

    public static String toString(Border border) {
        if(border == null) {
            return null;
        }
        if(border.getBorderTop() == border.getBorderBottom() && border.getBorderLeft() == border.getBorderRight()) {
            if(border.getBorderTop() == border.getBorderLeft()) {
                if(border.getBorderTop() == 0) {
                    return null;
                }
                return Integer.toString(border.getBorderTop());
            }
            return border.getBorderLeft()+","+border.getBorderTop();
        }
        return border.getBorderTop()+","+border.getBorderLeft()+","+border.getBorderBottom()+","+border.getBorderRight();
    }

    public static void addChildren(ThemeFile themeFile, ThemeTreeNode parent, Element node, DomWrapper wrapper) throws IOException {
        for(Object child : node.getChildren()) {
            if(child instanceof Element) {
                Element element = (Element)child;
                TreeTableNode ttn = wrapper.wrap(themeFile, parent, element);
                if(ttn == null) {
                    ttn = new Unknown(parent, element);
                }
                if(ttn instanceof ThemeTreeNode) {
                    ThemeTreeNode mttn = (ThemeTreeNode)ttn;
                    mttn.addChildren();
                    mttn.setLeaf(ttn.getNumChildren() == 0);
                }
                parent.appendChild(ttn);
            }
        }
    }

    public static <T extends TreeTableNode> void getChildren(TreeTableNode node, Class<T> clazz, List<T> result) {
        for(int i=0,n=node.getNumChildren() ; i<n ; i++) {
            TreeTableNode child = node.getChild(i);
            if(clazz.isInstance(child)) {
                result.add(clazz.cast(child));
            }
        }
    }

    public static <T extends TreeTableNode> List<T> getChildren(TreeTableNode node, Class<T> clazz) {
        ArrayList<T> result = new ArrayList<T>();
        getChildren(node, clazz, result);
        return result;
    }

    public static void addToXPP(DomXPPParser xpp, ThemeTreeNode node) {
        for(int i=0,n=node.getNumChildren() ; i<n ; i++) {
            TreeTableNode child = node.getChild(i);
            if(child instanceof ThemeTreeNode) {
                ((ThemeTreeNode)child).addToXPP(xpp);
            }
        }
    }

    public static void addToXPP(DomXPPParser xpp, String tagName, ThemeTreeNode node, Collection<Attribute> attributes) {
        xpp.addStartTag(tagName, attributes);
        addToXPP(xpp, node);
        xpp.addEndTag(tagName);
    }

    public static void addToXPP(DomXPPParser xpp, NodeWrapper wrapper, ThemeTreeNode node) {
        addToXPP(xpp, wrapper.node.getName(), node, wrapper.getAttributes());
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
    
    public static String toStringOrNull(Object o) {
        return (o == null) ? null : o.toString();
    }
}
