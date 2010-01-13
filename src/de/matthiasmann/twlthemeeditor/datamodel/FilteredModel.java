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

import de.matthiasmann.twl.model.AbstractTableModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import java.util.ArrayList;

/**
 *
 * @author Matthias Mann
 */
public class FilteredModel extends AbstractTableModel {

    public interface Filter {
        public boolean isVisible(TreeTableNode node);
    }

    private final TreeTableModel model;
    private final ArrayList<TreeTableNode> rows;

    private Filter filter;

    public FilteredModel(TreeTableModel model) {
        this.model = model;
        this.rows = new ArrayList<TreeTableNode>();

        updateList();

        model.addChangeListener(new TreeTableModel.ChangeListener() {
            public void nodesAdded(TreeTableNode parent, int idx, int count) {
                updateList();
            }
            public void nodesRemoved(TreeTableNode parent, int idx, int count) {
                updateList();
            }
            public void nodesChanged(TreeTableNode parent, int idx, int count) {
                updateList();
            }
            public void columnInserted(int idx, int count) {
                fireColumnInserted(idx, count);
            }
            public void columnDeleted(int idx, int count) {
                fireColumnDeleted(idx, count);
            }
            public void columnHeaderChanged(int column) {
                fireColumnHeaderChanged(column);
            }
        });
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
        updateList();
    }

    public String getColumnHeaderText(int column) {
        return model.getColumnHeaderText(column);
    }

    public int getNumColumns() {
        return model.getNumColumns();
    }

    public TreeTableNode getRow(int row) {
        return rows.get(row);
    }

    public Object getCell(int row, int column) {
        return getRow(row).getData(column);
    }

    public int getNumRows() {
        return rows.size();
    }

    public int getRowFromNode(TreeTableNode node) {
        for(int i=0,n=rows.size() ; i<n ; i++) {
            if(rows.get(i) == node) {
                return i;
            }
        }
        return -1;
    }

    void addRow(int pos, TreeTableNode node) {
        rows.add(pos, node);
        fireRowsInserted(pos, 1);
    }

    void updateList() {
        rows.clear();
        updateList(model);
        fireAllChanged();
    }

    void updateList(TreeTableNode node) {
        if((filter == null) || filter.isVisible(node)) {
            rows.add(node);
        }
        for(int i=0,n=node.getNumChildren() ; i<n ; i++) {
            updateList(node.getChild(i));
        }
    }

}
