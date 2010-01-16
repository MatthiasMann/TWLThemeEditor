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

import de.matthiasmann.twl.SimpleDialog;
import de.matthiasmann.twl.BoxLayout;
import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.PopupMenu;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SubMenu;
import de.matthiasmann.twl.Table;
import de.matthiasmann.twl.TableBase.Callback;
import de.matthiasmann.twl.TableBase.StringCellRenderer;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.datamodel.FilteredModel;
import de.matthiasmann.twlthemeeditor.datamodel.NodeNameWithError;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeOperation;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final BoxLayout buttons;

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
        this.buttons = new BoxLayout(BoxLayout.Direction.HORIZONTAL);

        CollapsiblePanel collapsibleButtons = new CollapsiblePanel(
                CollapsiblePanel.Direction.HORIZONTAL, "", buttons, null);

        StringCellRenderer errorRenderer = new StringCellRenderer();
        errorRenderer.getAnimationState().setAnimationState("error", true);
        treeTable.registerCellRenderer(NodeNameWithError.class, errorRenderer);

        scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);
        treeTable.setSelectionManager(new TableRowSelectionManager(treeTableSelectionModel));
        table.setSelectionManager(new TableRowSelectionManager(tableSelectionModel));

        filterEditField.setTheme("filter");
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
        treeTable.addCallback(new Callback() {
            public void mouseDoubleClicked(int row, int column) {
                treeTable.setRowExpanded(row, !treeTable.isRowExpanded(row));
            }
            public void columnHeaderClicked(int column) {
            }
        });

        setHorizontalGroup(createParallelGroup()
                .addWidget(scrollPane)
                .addGroup(createSequentialGroup(filterEditField, collapsibleButtons)));
        setVerticalGroup(createSequentialGroup()
                .addWidget(scrollPane)
                .addGroup(createParallelGroup(filterEditField, collapsibleButtons)));
    }

    public void addCallback(Runnable cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Runnable.class);
    }

    public void removeCallback(Runnable cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }

    void updateFilter() {
        if(filter.setString(filterEditField.getText())) {
            TreeTableNode curSel = selected;
            if(filter.hasFilter()) {
                scrollPane.setContent(table);
                if(filteredModel != null) {
                    filteredModel.setFilter(filter);
                }
            } else {
                scrollPane.setContent(treeTable);
            }
            selectNode(curSel);
        }
    }

    void setSelected(TreeTableNode node) {
        if(selected != node) {
            selected = node;
            CallbackSupport.fireCallbacks(callbacks);
            updateOperationButtons();
        }
    }

    void updateOperationButtons() {
        buttons.removeAllChildren();
        if(selected instanceof ThemeTreeNode) {
            List<ThemeTreeOperation> operations = ((ThemeTreeNode)selected).getOperations();
            createButtons(operations, buttons);
        }
    }

    public TreeTableNode getSelected() {
        return selected;
    }

    public void setModel(ThemeTreeModel model) {
        filteredModel = new FilteredModel(model);
        treeTable.setModel(model);
        treeTable.setRowExpanded(0, true);
        table.setModel(filteredModel);
        updateFilter();
    }

    void showNodeOperations(int x, int y) {
        List<ThemeTreeOperation> operations = ((ThemeTreeNode)selected).getOperations();
        if(!operations.isEmpty()) {
            PopupMenu popupMenu = new PopupMenu(this);
            createButtons(operations, popupMenu);
            popupMenu.showPopup(x, y);
        }
    }

    private void createButtons(List<ThemeTreeOperation> operations, Widget container) {
        HashMap<String, PopupMenu> submenus = new HashMap<String, PopupMenu>();
        for(final ThemeTreeOperation operation : operations) {
            String groupID = operation.getGroupID();

            PopupMenu subPopupMenu = null;
            if(groupID != null) {
                subPopupMenu = submenus.get(groupID);
                if(subPopupMenu == null) {
                     SubMenu subMenuBtn = new SubMenu();
                     subMenuBtn.setTheme(groupID);
                     subPopupMenu = subMenuBtn.getPopupMenu();
                     subPopupMenu.setTheme(groupID + "-popupMenu");
                     submenus.put(groupID, subPopupMenu);
                     container.add(subMenuBtn);
                }
            }

            final PopupMenu menuToClose = (container instanceof PopupMenu) ?
                ((PopupMenu)container) : subPopupMenu;

            Button btn = new Button();
            btn.setTheme(operation.getActionID());
            btn.setEnabled(operation.isEnabled());
            btn.addCallback(new Runnable() {
                public void run() {
                    if(menuToClose != null) {
                        menuToClose.closePopup();
                    }
                    if(operation.needConfirm()) {
                        confirmOperation(operation);
                    } else {
                        executeOperation(operation);
                    }
                }
            });

            ((subPopupMenu != null) ? subPopupMenu : container).add(btn);
        }
    }

    void executeOperation(ThemeTreeOperation operation) {
        TreeTableNode curSel = selected;
        try {
            operation.execute();
        } catch(Throwable ex) {
            Logger.getLogger(ThemeTreePane.class.getName()).log(Level.SEVERE,
                    "Error while executing tree operation", ex);
        }
        selectNode(curSel);
    }

    void selectNode(TreeTableNode node) {
        int idx = (node != null) ? treeTable.getRowFromNodeExpand(node) : -1;
        treeTableSelectionModel.setSelection(idx, idx);
        if(idx >= 0) {
            treeTable.scrollToRow(idx);
        }

        idx = (node != null) ? filteredModel.getRowFromNode(node) : -1;
        tableSelectionModel.setSelection(idx, idx);
        if(idx >= 0) {
            table.scrollToRow(idx);
        }

        updateOperationButtons();
    }

    void confirmOperation(final ThemeTreeOperation operation) {
        SimpleDialog dialog = new SimpleDialog();
        dialog.setTheme("confirmationDlg-" + operation.getActionID());
        dialog.setTitle(operation.getActionID());
        dialog.setMessage(selected.toString());
        dialog.setOkCallback(new Runnable() {
            public void run() {
                executeOperation(operation);
            }
        });
        dialog.showDialog(this);
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
        boolean hasFilter() {
            return str.length() > 0;
        }
        public boolean isVisible(TreeTableNode node) {
            return String.valueOf(node.getData(0)).contains(str);
        }
    }
}
