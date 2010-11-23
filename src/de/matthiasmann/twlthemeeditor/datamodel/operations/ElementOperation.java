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

import de.matthiasmann.twlthemeeditor.datamodel.ThemeFile;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeOperation;
import java.io.IOException;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
abstract class ElementOperation extends ThemeTreeOperation {

    protected final Element element;
    protected final Element parent;
    protected final ThemeTreeNode node;

    protected ElementOperation(String actionName, Element element, ThemeTreeNode node) {
        super(actionName);
        this.element = element;
        this.parent = element.getParentElement();
        this.node = node;
    }

    protected ThemeTreeNode getNodeParent() {
        return (ThemeTreeNode)node.getParent();
    }

    protected int getElementPosition() {
        for(int i = 0, n = parent.getContentSize(); i < n; i++) {
            if(parent.getContent(i) == element) {
                return i;
            }
        }
        return -1;
    }

    protected int getPrevSiblingPosition(int pos) {
        do {
            pos--;
        } while(pos >= 0 && !(parent.getContent(pos) instanceof Element));
        return pos;
    }

    protected int getNextSiblingPosition(int pos) {
        int count = parent.getContentSize();
        do {
            pos++;
        } while(pos < count && !(parent.getContent(pos) instanceof Element));
        return pos;
    }

    protected void updateParent() throws IOException {
        getNodeParent().addChildren();
        setModified(parent);
    }

    protected static void setModified(Element parent) {
        ThemeFile themeFile = ThemeFile.getThemeFile(parent);
        if(themeFile != null) {
            themeFile.setModified(true);
        }
    }
}
