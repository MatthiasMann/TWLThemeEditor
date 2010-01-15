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
package de.matthiasmann.twlthemeeditor.datamodel.operations;

import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeOperation;
import java.io.IOException;
import java.util.Arrays;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;

/**
 *
 * @author Matthias Mann
 */
abstract class CreateChildOperation extends ThemeTreeOperation {

    private static final int INDENTATION_SIZE = 4;

    protected final ThemeTreeNode parent;
    protected final Element element;

    public CreateChildOperation(String actionID, ThemeTreeNode parent, Element element) {
        super("opNewNode", actionID);
        this.parent = parent;
        this.element = element;
    }

    protected int getBaseIndentation() {
        int indentation = 0;
        for(Element e=element ; e!=null ; e=e.getParentElement()) {
            indentation += INDENTATION_SIZE;
        }
        return indentation;
    }
    
    protected String createIndentation(int indentation) {
        char[] buf = new char[indentation + 1];
        Arrays.fill(buf, ' ');
        buf[0] = '\n';
        return new String(buf);
    }

    protected void addChild(Element child) throws IOException {
        addChild(child, false);
    }

    protected void addChild(Element child, boolean dontIndentChilds) throws IOException {
        int indentation = getBaseIndentation();
        int pos = element.getContentSize();
        if(pos>0 && element.getContent(pos-1) instanceof Text) {
            pos--;
        }
        element.addContent(pos++, new Text(createIndentation(indentation)));
        element.addContent(pos, child);
        if(!dontIndentChilds) {
            addIndentation(child, indentation);
        }
        parent.addChildren();
    }

    protected void addIndentation(Element element, int indentation) {
        boolean hasElements = false;
        for(int i=element.getContentSize() ; i-->0 ;) {
            Content content = element.getContent(i);
            if(content instanceof Element) {
                addIndentation((Element)content, indentation + INDENTATION_SIZE);
                element.addContent(i, new Text(createIndentation(indentation + INDENTATION_SIZE)));
                hasElements = true;
            } else if(content instanceof Text) {
                Text text = (Text)content;
                if("\n".equals(text.getText())) {
                    text.setText(createIndentation(indentation));
                }
            }
        }
        if(hasElements) {
            element.addContent(createIndentation(indentation));
        }
    }

    protected String makeRandomName() {
        return "new" + System.nanoTime();
    }

    protected void addNameAttributeIfNeeded(Element e) {
        if(element.isRootElement() || "textures".equals(element.getName())) {
            e.setAttribute("name", makeRandomName());
        }
    }
}
