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

import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.SimpleDialog;
import de.matthiasmann.twl.BoxLayout;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.MenuAction;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.Table;
import de.matthiasmann.twl.TableBase.Callback;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.datamodel.FilteredModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeOperation;
import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class ThemeTreePane extends DialogLayout {

    private final MessageLog messageLog;
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

    public ThemeTreePane(MessageLog messageLog) {
        this.messageLog = messageLog;
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

        DecoratedTextRenderer.install(treeTable);

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
                if(row >= 0 && row < treeTable.getNumRows()) {
                    treeTable.setRowExpanded(row, !treeTable.isRowExpanded(row));
                }
            }
            public void mouseRightClick(int row, int column, Event evt) {
                if(row >= 0 && row < treeTable.getNumRows()) {
                    showTreeTablePopupMenu(row, evt);
                }
            }
            public void columnHeaderClicked(int column) {
            }
        });
        table.addCallback(new Callback() {
            public void mouseDoubleClicked(int row, int column) {
                clearFilterAndJumpToRow(row);
            }
            public void mouseRightClick(int row, int column, Event evt) {
            }
            public void columnHeaderClicked(int column) {
            }
        });

        setHorizontalGroup(createParallelGroup()
                .addWidget(scrollPane)
                .addGroup(createSequentialGroup().addGap(DEFAULT_GAP).addWidget(filterEditField).addWidget(collapsibleButtons)));
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
            ThemeTreeNode node = (ThemeTreeNode)selected;
            List<ThemeTreeOperation> operations = node.getOperations();
            Menu m = createButtons(node, operations);
            m.createMenuBar(buttons);
        }
    }

    void showTreeTablePopupMenu(int row, Event evt) {
        TreeTableNode nodeFromRow = treeTable.getNodeFromRow(row);
        if(nodeFromRow instanceof ThemeTreeNode) {
            ThemeTreeNode node = (ThemeTreeNode)nodeFromRow;
            Menu menu = new Menu();

            if(node.getParent() instanceof ThemeTreeNode) {
                ThemeTreeNode parentNode = (ThemeTreeNode)node.getParent();
                List<ThemeTreeOperation> parentOperations = parentNode.getOperations();
                if(hasOperation(parentOperations, "opNewNode", null)) {
                    // TODO: need a way to specify the insert location
                }
            }
            
            List<ThemeTreeOperation> operations = node.getOperations();
            Menu createSubMenu = new Menu("New child");
            if(createSubMenu(createSubMenu, node, operations, "opNewNode", null) > 0) {
                menu.add(createSubMenu);
            }
            if(hasOperation(operations, null, "opDeleteNode") || hasOperation(operations, null, "opCloneNode")) {
                if(menu.getNumElements() > 0) {
                    menu.addSpacer();
                }
                createSubMenu(menu, node, operations, null, "opCloneNode");
                createSubMenu(menu, node, operations, null, "opDeleteNode");
            }

            if(menu.getNumElements() > 0) {
                menu.openPopupMenu(treeTable, evt.getMouseX(), evt.getMouseY());
            }
        }
    }

    public TreeTableNode getSelected() {
        return selected;
    }

    public void setModel(ThemeTreeModel model) {
        treeTable.setModel(model);
        if(model == null) {
            filteredModel = null;
        } else {
            treeTable.setRowExpanded(0, true);
            filteredModel = new FilteredModel(model);
        }
        table.setModel(filteredModel);
        updateFilter();
    }

    private boolean hasOperation(List<ThemeTreeOperation> operations, String groupID, String actionID) {
        for(final ThemeTreeOperation operation : operations) {
            if(groupID != null && groupID.equals(operation.getGroupID())) {
                return true;
            }
            if(actionID != null && actionID.equals(operation.getActionID())) {
                return true;
            }
        }
        return false;
    }

    private int createSubMenu(Menu menu, final ThemeTreeNode node, List<ThemeTreeOperation> operations, String groupID, String actionID) {
        int count = 0;
        for(final ThemeTreeOperation operation : operations) {
            if((groupID != null && groupID.equals(operation.getGroupID())) ||
                    (actionID != null && actionID.equals(operation.getActionID()))) {
                menu.add(createMenuAction(node, operation));
                count++;
            }
        }
        return count;
    }

    private Menu createButtons(final ThemeTreeNode node, List<ThemeTreeOperation> operations) {
        Menu menu = new Menu();
        HashMap<String, Menu> submenus = new HashMap<String, Menu>();
        for(final ThemeTreeOperation operation : operations) {
            String groupID = operation.getGroupID();

            Menu subPopupMenu = null;
            if(groupID != null) {
                subPopupMenu = submenus.get(groupID);
                if(subPopupMenu == null) {
                     subPopupMenu = new Menu();
                     subPopupMenu.setTheme(groupID);
                     subPopupMenu.setPopupTheme(groupID + "-popupMenu");
                     submenus.put(groupID, subPopupMenu);
                     menu.add(subPopupMenu);
                }
            }

            MenuAction action = createMenuAction(node, operation);

            ((subPopupMenu != null) ? subPopupMenu : menu).add(action);
        }
        return menu;
    }

    private MenuAction createMenuAction(final ThemeTreeNode node, final ThemeTreeOperation operation) {
        MenuAction action = new MenuAction();
        action.setTheme(operation.getActionID());
        action.setEnabled(operation.isEnabled());
        action.setCallback(new Runnable() {

            public void run() {
                queryOperationParameter(node, operation);
            }
        });
        return action;
    }

    private static final MessageLog.Category CAT_THEME_TREE_OPERATION =
            new MessageLog.Category("Tree operation", MessageLog.CombineMode.NONE, DecoratedText.ERROR);

    void executeOperation(ThemeTreeOperation operation, Object[] paramter) {
        TreeTableNode newSelection = null;
        try {
            newSelection = operation.execute(paramter);
        } catch(IllegalArgumentException ex) {
            messageLog.add(new MessageLog.Entry(CAT_THEME_TREE_OPERATION, "Invalid parameters for operation", ex.getMessage(), null));
        } catch(Throwable ex) {
            messageLog.add(new MessageLog.Entry(CAT_THEME_TREE_OPERATION, "Error while executing tree operation", null, ex));
        }
        selectNode(newSelection);
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

    void clearFilterAndJumpToRow(int row) {
        if(row >= 0 && row < filteredModel.getNumRows()) {
            selectNode(filteredModel.getRow(row));
            filterEditField.setText("");
        }
    }

    void queryOperationParameter(ThemeTreeNode node, final ThemeTreeOperation operation) {
        ThemeTreeOperation.Parameter[] parameter = operation.getParameter();
        if(parameter != null && parameter.length > 0) {
            File startDir = null;
            
            try {
                startDir = new File(node.getThemeFile().getURL().toURI()).getParentFile();
            } catch (URISyntaxException ex) {
                Logger.getLogger(ThemeTreePane.class.getName()).log(Level.SEVERE, "Can't determine start dir", ex);
            }

            final QueryOperationParameter qop = new QueryOperationParameter(startDir);
            qop.setParameter(parameter);

            SimpleDialog dialog = new SimpleDialog();
            dialog.setTheme("parameterDlg-" + operation.getActionID());
            dialog.setTitle(operation.getActionID());
            dialog.setMessage(qop);
            dialog.setOkCallback(new Runnable() {
                public void run() {
                    maybeConfirmOperation(operation, qop.getResults());
                }
            });
            dialog.showDialog(this);
        } else {
            maybeConfirmOperation(operation, null);
        }
    }

    void maybeConfirmOperation(ThemeTreeOperation operation, Object[] paramter) {
        if(operation.needConfirm()) {
            confirmOperation(operation, paramter);
        } else {
            executeOperation(operation, paramter);
        }
    }

    void confirmOperation(final ThemeTreeOperation operation, final Object[] paramter) {
        SimpleDialog dialog = new SimpleDialog();
        dialog.setTheme("confirmationDlg-" + operation.getActionID());
        dialog.setTitle(operation.getActionID());
        dialog.setMessage(selected.toString());
        dialog.setOkCallback(new Runnable() {
            public void run() {
                executeOperation(operation, paramter);
            }
        });
        dialog.showDialog(this);
    }

    static class MyFilter implements FilteredModel.Filter {
        private String str = "";

        boolean setString(String str) {
            str = str.toLowerCase();
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
            if(node instanceof ThemeTreeNode) {
                ThemeTreeNode ttn = (ThemeTreeNode)node;
                String name = ttn.getName();
                return (name != null) && name.toLowerCase().contains(str);
            }
            return false;
        }
    }
}
