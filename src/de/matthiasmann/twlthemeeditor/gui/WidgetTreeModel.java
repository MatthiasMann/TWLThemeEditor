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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AbstractTreeTableModel;
import de.matthiasmann.twl.model.AbstractTreeTableNode;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;

/**
 *
 * @author Matthias Mann
 */
public class WidgetTreeModel extends AbstractTreeTableModel {

    Context ctx;
    Node rootNode;

    public WidgetTreeModel() {
    }

    private static final String COLUMN_NAMES[] = {"Theme", "Class"};

    public String getColumnHeaderText(int column) {
        return COLUMN_NAMES[column];
    }

    public int getNumColumns() {
        return COLUMN_NAMES.length;
    }

    public void createTreeFromWidget(Context ctx, Widget w) {
        this.ctx = ctx;
        removeAllChildren();
        if(w != null) {
            rootNode = createNode(this, w);
            insertChild(rootNode, 0);
        } else {
            rootNode = null;
        }
    }

    public void refreshTree() {
        if(rootNode != null) {
            rootNode.checkChildren();
        }
    }

    public Widget getWidget(TreeTableNode node) {
        if(node instanceof Node) {
            return ((Node)node).widget;
        } else {
            return null;
        }
    }

    public Node getNodeForWidget(Widget w) {
        if(w != null && rootNode != null) {
            if(w == rootNode.widget) {
                return rootNode;
            }
            Node parent = getNodeForWidget(w.getParent());
            if(parent != null) {
                for(int i=0,n=parent.getNumChildren() ; i<n ; ++i) {
                    Node child = (Node)parent.getChild(i);
                    if(child.widget == w) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    private Node createNode(TreeTableNode parent, Widget w) {
        Node node = new Node(parent, w);
        node.addChildren();
        return node;
    }

    public class Node extends AbstractTreeTableNode {
        final Widget widget;
        final String className;

        public Node(TreeTableNode parent, Widget w) {
            super(parent);
            this.widget = w;
            this.className = w.getClass().getSimpleName();
        }

        public Object getData(int column) {
            switch (column) {
                case 0: {
                    String theme = widget.getTheme();
                    if(ctx != null) {
                        return DecoratedText.apply(theme,ctx.getWidgetFlags(widget));
                    } else {
                        return theme;
                    }
                }
                case 1:
                    return className;
                default:
                    return "";
            }
        }

        @Override
        public Object getTooltipContent(int column) {
            if(ctx != null) {
                return ctx.getTooltipForWidget(widget);
            }
            return null;
        }

        void add(Node n) {
            insertChild(n, getNumChildren());
        }

        void addChildren() {
            removeAllChildren();
            for(int i=0,n=widget.getNumChildren() ; i<n ; i++) {
                add(createNode(this, widget.getChild(i)));
            }
            setLeaf(getNumChildren() == 0);
        }

        void checkChildren() {
            if(getNumChildren() != widget.getNumChildren()) {
                addChildren();
            } else {
                for(int i=0,n=getNumChildren() ; i<n ; i++) {
                    if(widget.getChild(i) != ((Node)getChild(i)).widget) {
                        addChildren();
                        return;
                    }
                }
                for(int i=0,n=getNumChildren() ; i<n ; i++) {
                    ((Node)getChild(i)).checkChildren();
                }
            }
        }
    }
}
