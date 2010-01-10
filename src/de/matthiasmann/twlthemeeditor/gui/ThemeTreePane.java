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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.Table;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.datamodel.FilteredModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;

/**
 *
 * @author Matthias Mann
 */
public class ThemeTreePane extends DialogLayout {

    private final TreeTable treeTable;
    private final TableSingleSelectionModel treeTableSelectionModel;
    private final Table table;
    private final TableSingleSelectionModel tableSelectionModel;
    private final ScrollPane scrollPane;
    private final EditField filterEditField;
    private final MyFilter filter;

    private FilteredModel filteredModel;
    private TreeTableNode selected;
    private Runnable[] callbacks;

    public ThemeTreePane() {
        this.treeTable = new TreeTable();
        this.treeTableSelectionModel = new TableSingleSelectionModel();
        this.table = new Table();
        this.tableSelectionModel = new TableSingleSelectionModel();
        this.scrollPane = new ScrollPane(treeTable);
        this.filterEditField = new EditField();
        this.filter = new MyFilter();

        scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);
        treeTable.setSelectionManager(new TableRowSelectionManager(treeTableSelectionModel));
        table.setSelectionManager(new TableRowSelectionManager(tableSelectionModel));

        filterEditField.addCallback(new EditField.Callback() {
            public void callback(int key) {
                updateFilter();
            }
        });
        treeTableSelectionModel.addSelectionChangeListener(new Runnable() {
            public void run() {
                TreeTableNode node = null;
                if(treeTableSelectionModel.hasSelection()) {
                    node = treeTable.getNodeFromRow(treeTableSelectionModel.getFirstSelected());
                }
                setSelected(node);
            }
        });
        tableSelectionModel.addSelectionChangeListener(new Runnable() {
            public void run() {
                TreeTableNode node = null;
                if(tableSelectionModel.hasSelection()) {
                    node = filteredModel.getRow(tableSelectionModel.getFirstSelected());
                }
                setSelected(node);
            }
        });

        setHorizontalGroup(createParallelGroup(scrollPane, filterEditField));
        setVerticalGroup(createSequentialGroup(scrollPane, filterEditField));
    }

    public void addCallback(Runnable cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Runnable.class);
    }

    public void removeCallback(Runnable cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    void updateFilter() {
        if(filter.setString(filterEditField.getText())) {
            if(filterEditField.hasSelection()) {
                scrollPane.setContent(treeTable);
            } else {
                scrollPane.setContent(table);
                if(filteredModel != null) {
                    filteredModel.setFilter(filter);
                }
            }
        }
    }

    void setSelected(TreeTableNode node) {
        if(selected != node) {
            selected = node;
            CallbackSupport.fireCallbacks(callbacks);
        }
    }

    public TreeTableNode getSelected() {
        return selected;
    }

    public void setModel(ThemeTreeModel model) {
        this.filteredModel = new FilteredModel(model);
        treeTable.setModel(model);
        table.setModel(filteredModel);
        updateFilter();
    }

    static class MyFilter implements FilteredModel.Filter {
        private String str = "";

        boolean setString(String str) {
            if(!this.str.equals(str)) {
                this.str = str;
                return true;
            }
            return false;
        }
        public boolean isVisible(TreeTableNode node) {
            return String.valueOf(node.getData(0)).contains(str);
        }
    }
}
