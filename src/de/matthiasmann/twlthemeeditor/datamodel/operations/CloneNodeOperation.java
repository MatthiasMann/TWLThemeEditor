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

import de.matthiasmann.twlthemeeditor.datamodel.Kind;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import java.io.IOException;
import org.jdom.Content;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class CloneNodeOperation extends ElementOperation {

    public CloneNodeOperation(Element element, ThemeTreeNode node) {
        super("opCloneNode", element, node);
    }

    @Override
    public boolean shouldFocusNameFieldAfterExecute() {
        return true;
    }

    @Override
    public ThemeTreeNode execute(Object[] parameter) throws IOException {
        int elementPos = getElementPosition();
        int elementTextPos = getPrevSiblingPosition(elementPos) + 1;

        Element cloneElement = null;

        for(int i=elementPos ; i>=elementTextPos ; i--) {
            Content content = parent.getContent(i);
            Content clone = (Content)content.clone();
            if(clone instanceof Element) {
                cloneElement = (Element)clone;
                adjustClonedElement(cloneElement);
            }
            parent.addContent(elementPos+1, clone);
        }

        updateParent();
        return CreateChildOperation.findChildInParent(getNodeParent(), cloneElement);
    }

    protected void adjustClonedElement(Element clonedElement) {
        String name = clonedElement.getAttributeValue("name");
        if(name != null) {
            String newName = null;
            Kind kind = node.getKind();
            if(kind != Kind.NONE) {
                int len = name.length();
                int digitStart = len;
                while(digitStart > 0 && Character.isDigit(name.charAt(digitStart-1))) {
                    digitStart--;
                }
                if(digitStart < len) {
                    String baseName = name.substring(0, digitStart);
                    try {
                        long number = Long.parseLong(name.substring(digitStart));
                        for(int i=1 ; i<100 ; i++) {
                            String testName = baseName + (number + i);
                            if(node.getThemeTreeModel().findNode(testName, kind) == null) {
                                newName = testName;
                                break;
                            }
                        }
                    } catch(IllegalArgumentException ex) {
                    }
                }
            }
            if(newName == null) {
                newName = name + System.nanoTime();
            }
            clonedElement.setAttribute("name", newName);
        }
    }

}
