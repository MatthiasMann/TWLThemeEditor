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

import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AbstractTreeTableModel;
import de.matthiasmann.twl.model.AbstractTreeTableNode;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;

/**
 *
 * @author Matthias Mann
 */
public class WidgetTreeModel extends AbstractTreeTableModel implements WidgetTreeNode {

    Context ctx;
    GUI gui;

    public WidgetTreeModel() {
    }

    private static final String COLUMN_NAMES[] = {"Theme", "Class"};

    public String getColumnHeaderText(int column) {
        return COLUMN_NAMES[column];
    }

    public int getNumColumns() {
        return COLUMN_NAMES.length;
    }

    public void createTreeFromWidget(Context ctx, GUI gui) {
        this.ctx = ctx;
        this.gui = gui;
        checkChildren(this);
    }

    public void refreshTree() {
        checkChildren(this);
    }

    public Widget getWidget() {
        return gui;
    }

    public Widget getWidget(TreeTableNode node) {
        if(node instanceof Node) {
            return ((Node)node).widget;
        } else {
            return null;
        }
    }

    public Node getNodeForWidget(Widget w) {
        if(w != null) {
            if(isTestWidgetContainer(w.getParent())) {
                w = w.getParent();
            }
            WidgetTreeNode parent;
            if(w == gui) {
                parent = this;
            } else {
                 parent = getNodeForWidget(w.getParent());
            }
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

    @Override
    public void removeAllChildren() {
        super.removeAllChildren();
    }

    public void add(TreeTableNode node) {
        insertChild(node, getNumChildren());
    }

    public void setLeaf() {
    }

    private void addChildren(WidgetTreeNode node) {
        node.removeAllChildren();
        Widget parentWidget = node.getWidget();
        for(int i=0,n=parentWidget.getNumChildren() ; i<n ; i++) {
            Widget widget = getChildWidget(parentWidget, i);
            Node child = new Node(node, ctx, widget);
            addChildren(child);
            node.add(child);
        }
        node.setLeaf();
    }

    private void checkChildren(WidgetTreeNode node) {
        Widget widget = node.getWidget();
        final int n = node.getNumChildren();
        if(n != widget.getNumChildren()) {
            addChildren(node);
        } else {
            for(int i=0 ; i<n ; i++) {
                Node child = (Node)node.getChild(i);
                if(getChildWidget(widget, i) != child.widget) {
                    addChildren(node);
                    return;
                }
            }
            for(int i=0 ; i<n ; i++) {
                Node child = (Node)node.getChild(i);
                checkChildren(child);
            }
        }
    }

    private static boolean isTestWidgetContainer(Widget widget) {
        return (widget instanceof TestWidgetContainer) && widget.getNumChildren() == 1;
    }

    private static Widget getChildWidget(Widget parent, int idx) {
        Widget widget = parent.getChild(idx);
        if(isTestWidgetContainer(widget)) {
            widget = widget.getChild(0);
        }
        return widget;
    }

    public static class Node extends AbstractTreeTableNode implements WidgetTreeNode {
        final Context ctx;
        final Widget widget;
        final String className;

        public Node(TreeTableNode parent, Context ctx, Widget w) {
            super(parent);
            this.ctx = ctx;
            this.widget = w;
            this.className = w.getClass().getSimpleName();
        }

        public Object getData(int column) {
            switch (column) {
                case 0: {
                    String theme = widget.getTheme();
                    if(theme.length() == 0) {
                        theme = "<EMPTY>";
                    }
                    if(ctx != null) {
                        return DecoratedText.apply(theme, ctx.getWidgetFlags(widget));
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

        public Widget getWidget() {
            return widget;
        }

        @Override
        public Object getTooltipContent(int column) {
            if(ctx != null) {
                return ctx.getTooltipForWidget(widget);
            }
            return null;
        }

        public void add(TreeTableNode n) {
            insertChild(n, getNumChildren());
        }

        @Override
        public void removeAllChildren() {
            super.removeAllChildren();
        }

        @Override
        public void setLeaf() {
            super.setLeaf(getNumChildren() == 0);
        }
    }
}
