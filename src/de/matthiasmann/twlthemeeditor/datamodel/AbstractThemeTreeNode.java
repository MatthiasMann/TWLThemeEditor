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

import de.matthiasmann.twl.model.AbstractTreeTableNode;
import de.matthiasmann.twl.model.TreeTableNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Content;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public abstract class AbstractThemeTreeNode extends AbstractTreeTableNode implements ThemeTreeNode {

    protected boolean error;

    protected AbstractThemeTreeNode(TreeTableNode parent) {
        super(parent);
    }

    public ThemeTreeModel getThemeTreeModel() {
        return (ThemeTreeModel)getTreeTableModel();
    }
    
    public void appendChild(TreeTableNode ttn) {
        insertChild(ttn, getNumChildren());
    }

    @Override
    public void setLeaf(boolean leaf) {
        super.setLeaf(leaf);
    }

    public <E extends TreeTableNode> List<E> getChildren(Class<E> clazz) {
        return Utils.getChildren(this, clazz);
    }

    public void setError(boolean hasError) {
        this.error = hasError;
    }

    protected static List<ThemeTreeOperation> getDefaultOperations(Element element, ThemeTreeNode node) {
        List<ThemeTreeOperation> result = new ArrayList<ThemeTreeOperation>();
        result.add(new DeleteNodeOperations(element, node));
        int pos = node.getParent().getChildIndex(node);
        if(pos > 0) {
            result.add(new MoveNodeOperations("Move up", element, node, -1));
        }
        if(pos < node.getParent().getNumChildren()-1) {
            result.add(new MoveNodeOperations("Move down", element, node, +1));
        }
        return result;
    }

    protected static abstract class ElementOperation extends ThemeTreeOperation {
        protected final Element element;
        protected final ThemeTreeNode node;

        public ElementOperation(String groupName, String actionName, Element element, ThemeTreeNode node) {
            super(groupName, actionName);
            this.element = element;
            this.node = node;
        }

        protected ThemeTreeNode getNodeParent() {
            return (ThemeTreeNode)node.getParent();
        }
        protected int getElementPosition() {
            Element parent = element.getParentElement();
            for(int i=0,n=parent.getContentSize() ; i<n ; i++) {
                if(parent.getContent(i) == element) {
                    return i;
                }
            }
            return -1;
        }
        protected int getPrevSiblingPosition(int pos) {
            Element parent = element.getParentElement();
            do {
                pos--;
            } while(pos >= 0 && !(parent.getContent(pos) instanceof Element));
            return pos;
        }
        protected int getNextSiblingPosition(int pos) {
            Element parent = element.getParentElement();
            int count = parent.getContentSize();
            do {
                pos++;
            } while(pos < count && !(parent.getContent(pos) instanceof Element));
            return pos;
        }
    }
    
    protected static class DeleteNodeOperations extends ElementOperation {
        public DeleteNodeOperations(Element element, ThemeTreeNode node) {
            super(null, "Delete node", element, node);
        }

        @Override
        public void execute() throws IOException {
            element.detach();
            getNodeParent().addChildren();
        }
    }

    protected static class MoveNodeOperations extends ElementOperation {
        private final int direction;

        public MoveNodeOperations(String actionName, Element element, ThemeTreeNode node, int direction) {
            super(null, actionName, element, node);
            this.direction = direction;
        }

        @Override
        public void execute() throws IOException {
            int elementPos = getElementPosition();
            int elementTextPos = getPrevSiblingPosition(elementPos) + 1;

            if(direction < 0) {
                int insertPos = getPrevSiblingPosition(elementTextPos-1) + 1;
                if(insertPos < elementTextPos) {
                    moveContent(elementTextPos, insertPos, elementPos - elementTextPos + 1);
                    getNodeParent().addChildren();
                }
            } else {
                int insertPos = getNextSiblingPosition(elementPos);
                if(insertPos > elementPos) {
                    moveContent(elementTextPos, insertPos, elementPos - elementTextPos + 1);
                    getNodeParent().addChildren();
                }
            }
        }

        protected void moveContent(int from, int to, int count) {
            Element parent = element.getParentElement();
            if(from < to) {
                for(; count>0 ; count--) {
                    Content c = parent.removeContent(from);
                    parent.addContent(to, c);
                }
            } else if(from > to) {
                for(; count>0 ; count--) {
                    Content c = parent.removeContent(from++);
                    parent.addContent(to++, c);
                }
            }
        }
    }
}
