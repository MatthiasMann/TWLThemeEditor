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

import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeOperation;
import de.matthiasmann.twlthemeeditor.dom.Content;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.dom.Text;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Matthias Mann
 */
public abstract class CreateChildOperation extends ThemeTreeOperation {

    protected static final int INDENTATION_SIZE = 4;

    protected final ThemeTreeNode parent;
    protected final Element element;
    protected boolean indentChildren = true;

    public CreateChildOperation(String actionID, ThemeTreeNode parent, Element element) {
        super(actionID);
        this.parent = parent;
        this.element = element;
    }

    @Override
    public boolean shouldFocusNameFieldAfterExecute() {
        return true;
    }

    protected int getBaseIndentation() {
        int indentation = 0;
        for(Element e=element ; e!=null ; e=e.getParentElement()) {
            indentation += INDENTATION_SIZE;
        }
        return indentation;
    }
    
    protected static String createIndentation(int indentation) {
        char[] buf = new char[indentation + 1];
        Arrays.fill(buf, ' ');
        buf[0] = '\n';
        return new String(buf);
    }

    @Override
    public final ThemeTreeNode execute(Object[] parameter) throws IOException {
        return executeAt(parameter, element.getContentSize());
    }

    public abstract ThemeTreeNode executeAt(Object[] parameter, int pos) throws IOException;

    protected ThemeTreeNode addChild(Element child, int pos) throws IOException {
        int indentation = getBaseIndentation();
        if(pos>0 && element.getContent(pos-1) instanceof Text) {
            pos--;
        } else {
            element.addContent(pos, new Text(createIndentation(indentation - INDENTATION_SIZE)));
        }
        if(indentChildren) {
            addIndentation(child, indentation);
        }
        element.addContent(pos++, new Text(createIndentation(indentation)));
        element.addContent(pos, child);
        return findChildInParent(parent, child);
    }
    
    protected static ThemeTreeNode findChildInParent(TreeTableNode parent, Element child) {
        for(int i=0,n=parent.getNumChildren() ; i<n ; i++) {
            TreeTableNode node = parent.getChild(i);
            if(node instanceof ThemeTreeNode) {
                ThemeTreeNode ttn = (ThemeTreeNode)node;
                if(ttn.getDOMElement() == child) {
                    return ttn;
                }
            }
        }
        return null;
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
                if("\n".equals(text.getValue())) {
                    text.setValue(createIndentation(indentation));
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
        if(parent.childrenNeedName()) {
            e.setAttribute("name", makeRandomName());
        }
    }
}
