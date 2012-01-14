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
package de.matthiasmann.twlthemeeditor.datamodel.operations;

import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.dom.Element;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Matthias Mann
 */
public class CreateNewParam extends CreateChildOperation {

    private final String tagName;
    private final String initialText;

    private final static HashMap<String, String[]> NAME_LIST;

    static {
        NAME_LIST = new HashMap<String, String[]>();
        NAME_LIST.put("image", new String[] { "background", "overlay" });
        NAME_LIST.put("string", new String[] { "text", "tooltip" });
        NAME_LIST.put("border", new String[] { "border" });
        NAME_LIST.put("font", new String[] { "font" });
        NAME_LIST.put("cursor", new String[] { "mouseCursor" });
        NAME_LIST.put("int", new String[] { "maxWidth", "maxHeight", "minWidth", "minHeight" });
        NAME_LIST.put("inputMap", new String[] { "inputMap" });
        NAME_LIST.put("inputMapDef", new String[] { "inputMap" });
    }

    public CreateNewParam(Element element, String tagName, ThemeTreeNode parent, String initialText) {
        this("opNewParam" + Utils.capitalize(tagName), parent, element, tagName, initialText);
        indentChildren = "map".equals(tagName);
    }

    protected CreateNewParam(String actionID, ThemeTreeNode parent, Element element, String tagName, String initialText) {
        super(actionID, parent, element);
        this.tagName = tagName;
        this.initialText = initialText;
    }

    @Override
    public ThemeTreeNode executeAt(Object[] parameter, int pos) throws IOException {
        Element e = new Element("param");
        e.setAttribute("name", makeName());

        Element v = new Element(tagName);
        initParamElement(v, parameter);
        e.addContent(v);

        return addChild(e, pos);
    }

    protected void initParamElement(Element v, Object[] parameter) {
        v.setText(initialText);
    }

    protected String makeName() {
        String[] list = NAME_LIST.get(tagName);
        return makeName(list);
    }

    protected String makeName(String ... list) {
        if(list != null) {
            HashSet<String> used = findUsedNames();
            for(String name : list) {
                if(!used.contains(name)) {
                    return name;
                }
            }
            for(int i=2 ; i<10 ; i++) {
                String name = list[0] + i;
                if(!used.contains(name)) {
                    return name;
                }
            }
        }
        return makeRandomName();
    }

    protected HashSet<String> findUsedNames() {
        HashSet<String> result = new HashSet<String>();
        for(Object child : element.getChildren("param")) {
            Element e = (Element)child;
            result.add(e.getAttributeValue("name"));
        }
        return result;
    }
}
