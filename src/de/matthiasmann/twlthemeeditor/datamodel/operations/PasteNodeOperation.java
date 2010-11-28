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

import de.matthiasmann.twl.Clipboard;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import java.io.IOException;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;

/**
 *
 * @author Matthias Mann
 */
public class PasteNodeOperation extends CreateChildOperation {

    private final Element pasteElement;

    public PasteNodeOperation(ThemeTreeNode parent, Element element) {
        super("opPasteNode", parent, element);

        indentChildren = false;
        pasteElement = getFromClipboard();
    }

    private Element getFromClipboard() {
        String str = Clipboard.getClipboard();
        if(str == null || str.isEmpty() || !str.startsWith("<")) {
            return null;
        }
        try {
            Document document = Utils.loadDocument(str);
            return document.detachRootElement();
        } catch(IOException ex) {
            return null;
        }
    }

    @Override
    public boolean isEnabled() {
        return pasteElement != null && parent.canPasteElement(pasteElement);
    }

    @Override
    public ThemeTreeNode executeAt(Object[] parameter, int pos) throws IOException {
        if(isEnabled()) {
            Element e = (Element)pasteElement.clone();
            boolean hasName = e.getAttribute("name") != null;
            if(!parent.childrenNeedName()) {
                if(hasName) {
                    e.removeAttribute("name");
                }
            } else if(!hasName) {
                e.setAttribute("name", makeRandomName());
            }

            adjustIndentation(e, getBaseIndentation());
            return addChild(e, pos);
        }

        return null;
    }

    private static void adjustIndentation(Element e, int indentation) {
        boolean first = true;
        for(int i=e.getContentSize() ; i-->0 ;) {
            Content c = e.getContent(i);
            if(c instanceof Text) {
                Text te = (Text)c;
                String text = te.getText();
                if(text.trim().isEmpty()) {
                    int count = 0;
                    for(int idx=-1 ; (idx=text.indexOf('\n', idx+1))>=0 ;) {
                        count++;
                    }
                    String indentStr = createIndentation(indentation);
                    if(count > 1) {
                        StringBuilder sb = new StringBuilder();
                        for(int j=0 ; j<count ; j++) {
                            sb.append(indentStr);
                        }
                        indentStr = sb.toString();
                    }
                    te.setText(indentStr);
                }
            }
            if(first) {
                indentation += INDENTATION_SIZE;
                first = false;
            }
            if(c instanceof Element) {
                adjustIndentation((Element)c, indentation);
            }
        }
    }
}
