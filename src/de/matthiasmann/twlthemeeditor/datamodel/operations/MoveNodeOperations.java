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

import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import java.io.IOException;
import org.jdom.Content;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class MoveNodeOperations extends ElementOperation {

    private final int direction;

    public MoveNodeOperations(String actionID, Element element, ThemeTreeNode node, int direction) {
        super(null, actionID, element, node);
        this.direction = direction;
    }

    @Override
    public boolean isEnabled() {
        TreeTableNode parent = node.getParent();
        int pos = parent.getChildIndex(node);
        return (direction > 0 && pos < parent.getNumChildren()-1) ||
                (direction < 0 && pos > 0);
    }

    @Override
    public ThemeTreeNode execute() throws IOException {
        int elementPos = getElementPosition();
        int elementTextPos = getPrevSiblingPosition(elementPos) + 1;

        if(direction < 0) {
            int insertPos = getPrevSiblingPosition(elementTextPos - 1) + 1;
            if(insertPos >= 0 && insertPos < elementTextPos) {
                moveContent(elementTextPos, insertPos, elementPos - elementTextPos + 1);
            }
        } else {
            int insertPos = getNextSiblingPosition(elementPos);
            if(insertPos > elementPos && insertPos < element.getParent().getContentSize()) {
                moveContent(elementTextPos, insertPos, elementPos - elementTextPos + 1);
            }
        }

        return node;
    }

    protected void moveContent(int from, int to, int count) throws IOException {
        Element parent = element.getParentElement();
        if(from < to) {
            for(; count > 0; count--) {
                Content c = parent.removeContent(from);
                parent.addContent(to, c);
            }
        } else if(from > to) {
            for(; count > 0; count--) {
                Content c = parent.removeContent(from++);
                parent.addContent(to++, c);
            }
        }
        updateParent();
    }
}
