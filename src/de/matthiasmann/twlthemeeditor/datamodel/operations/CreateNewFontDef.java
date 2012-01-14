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
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.gui.MainUI.ExtFilter;
import java.io.IOException;

/**
 *
 * @author Matthias Mann
 */
public class CreateNewFontDef extends CreateChildOperation {

    public CreateNewFontDef(ThemeTreeNode parent, Element element) {
        super("opNewNodeFontDef", parent, element);
    }

    @Override
    public Parameter[] getParameter() {
        return getFontDefParameter();
    }

    @Override
    public ThemeTreeNode executeAt(Object[] parameter, int pos) throws IOException {
        Element e = new Element("fontDef");
        addNameAttributeIfNeeded(e);
        initFontDefElement(e, parameter);
        return addChild(e, pos);
    }

    static Parameter[] getFontDefParameter() {
        return new Parameter[] {
            new FileParameter("BMFont file", new ExtFilter(".fnt"))
        };
    }

    static void initFontDefElement(Element e, Object[] parameter) {
        if (parameter.length != 1) {
            throw new IllegalArgumentException("Wrong number of arguments");
        }
        if (!(parameter[0] instanceof String)) {
            throw new IllegalArgumentException("Font file not specified");
        }
        String fontFile = (String) parameter[0];

        e.setAttribute("filename", fontFile);
        e.setAttribute("color", "white");
    }
}
