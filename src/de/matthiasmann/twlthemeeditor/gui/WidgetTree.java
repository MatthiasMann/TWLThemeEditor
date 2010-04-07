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

import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.TableBase.StringCellRenderer;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.TableSingleSelectionModel;

/**
 *
 * @author Matthias Mann
 */
public class WidgetTree extends ScrollPane {

    private final WidgetTreeModel treeModel;
    private final TableSingleSelectionModel selectionModel;
    private final TreeTable treeTable;

    private Widget rootWidget;

    public WidgetTree(WidgetTreeModel treeModel) {
        this.treeModel = treeModel;

        selectionModel = new TableSingleSelectionModel();
        treeTable = new TreeTable(treeModel);
        treeTable.setTheme("themetree");
        treeTable.setSelectionManager(new TableRowSelectionManager(selectionModel));

        DecoratedTextRenderer.install(treeTable);

        setFixed(Fixed.HORIZONTAL);
        setContent(treeTable);
    }

    public void setRootWidget(Context ctx, Widget rootWidget) {
        this.rootWidget = rootWidget;
        treeModel.createTreeFromWidget(ctx, rootWidget);
        if(treeModel.getNumChildren() > 0) {
            selectionModel.setSelection(0, 0);
            treeTable.setRowExpanded(0, true);
        }
    }

    public void addSelectionChangeListener(Runnable cb) {
        selectionModel.addSelectionChangeListener(cb);
    }

    public Widget getSelectedWidget() {
        int idx = selectionModel.getFirstSelected();
        Widget widget = (idx >= 0) ? treeModel.getWidget(treeTable.getNodeFromRow(idx)) : null;
        // make sure that the widget has not been removed (eg tree model is outdated)
        for(Widget w=widget ; w!=null ; w=w.getParent()) {
            if(w == rootWidget) {
                return widget;
            }
        }
        return null;
    }
}
