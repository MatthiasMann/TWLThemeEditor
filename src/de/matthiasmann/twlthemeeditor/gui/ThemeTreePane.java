/*
 * Copyright (c) 2008-2012, Matthias Mann
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
import de.matthiasmann.twl.Menu.Listener;
import de.matthiasmann.twl.MenuAction;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.Table;
import de.matthiasmann.twl.TableBase.Callback;
import de.matthiasmann.twl.TableBase.DragListener;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.renderer.MouseCursor;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.datamodel.FilteredModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateAtWrapper;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.MoveNodeOperations;
import de.matthiasmann.twlthemeeditor.dom.Undo;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 *
 * @author Matthias Mann
 */
public class ThemeTreePane extends DialogLayout {

    private final MessageLog messageLog;
    private final TestWidgetManager testWidgetManager;
    private final TreeTable treeTable;
    private final TableSingleSelectionModel treeTableSelectionModel;
    private final Table table;
    private final TableSingleSelectionModel tableSelectionModel;
    private final ScrollPane scrollPane;
    private final EditField filterEditField;
    private final MyFilter filter;
    private final BoxLayout buttons;

    private ThemeTreeModel model;
    private FilteredModel filteredModel;
    private TreeTableNode selected;
    private Runnable[] callbacks;
    private Runnable focusNameFieldCB;

    public ThemeTreePane(MessageLog messageLog, TestWidgetManager testWidgetManager) {
        this.messageLog = messageLog;
        this.testWidgetManager = testWidgetManager;
        this.treeTable = new TreeTable() {
            @Override
            protected boolean handleKeyStrokeAction(String action, Event event) {
                if(handleOperationKeyStrokeAction(action, event)) {
                    return true;
                }
                return super.handleKeyStrokeAction(action, event);
            }
        };
        this.treeTableSelectionModel = new TableSingleSelectionModel();
        this.table = new Table();
        this.tableSelectionModel = new TableSingleSelectionModel();
        this.scrollPane = new ScrollPane(treeTable);
        this.filterEditField = new EditField();
        this.filter = new MyFilter();
        this.buttons = new BoxLayout(BoxLayout.Direction.HORIZONTAL);

        CollapsiblePanel collapsibleButtons = new CollapsiblePanel(
                CollapsiblePanel.Direction.HORIZONTAL, "", buttons, null);

        IconCellRenderer.install(treeTable);

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
        treeTable.setDragListener(new DragListener() {
            TreeTableNode dragParent;
            MoveNodeOperations moveUp;
            MoveNodeOperations moveDown;
            public boolean dragStarted(int row, int col, Event evt) {
                if((evt.getModifiers() & Event.MODIFIER_BUTTON) != Event.MODIFIER_LBUTTON) {
                    return false;
                }
                if(!(selected instanceof ThemeTreeNode)) {
                    return false;
                }
                dragParent = selected.getParent();
                if(dragParent == null) {
                    return false;
                }
                ThemeTreeNode selectedThemeNode = (ThemeTreeNode)selected;
                moveUp = selectedThemeNode.getMoveOperation(-1);
                moveDown = selectedThemeNode.getMoveOperation(+1);
                if(moveUp == null || moveDown == null) {
                    return false;
                }
                return true;
            }
            public MouseCursor dragged(Event evt) {
                if(!treeTable.setDropMarker(evt) || getNewNodeIndex() < 0) {
                    treeTable.clearDropMarker();
                }
                scrollPane.checkAutoScroll(evt);
                return null;
            }
            public void dragStopped(Event evt) {
                int newIndex = getNewNodeIndex();
                if(newIndex >= 0) {
                    Undo.startComplexOperation();
                    try {
                        int oldIndex = dragParent.getChildIndex(selected);
                        if(newIndex < oldIndex) {
                            while(moveUp.isEnabled(false) && newIndex < oldIndex) {
                                executeOperation(moveUp, null);
                                --oldIndex;
                            }
                        } else {
                            --newIndex;
                            while(moveDown.isEnabled(false) && newIndex > oldIndex) {
                                executeOperation(moveDown, null);
                                ++oldIndex;
                            }
                        }
                    } finally {
                        Undo.endComplexOperation();
                    }
                }
                dragCanceled();
            }
            public void dragCanceled() {
                treeTable.clearDropMarker();
                scrollPane.stopAutoScroll();
                moveUp = null;
                moveDown = null;
                dragParent = null;
            }
            private int getNewNodeIndex() {
                if(!treeTable.isDropMarkerBeforeRow()) {
                    return -1;
                }
                int row = treeTable.getDropMarkerRow();
                if(row < treeTable.getNumRows()) {
                    TreeTableNode node = treeTable.getNodeFromRow(row);
                    if(node.getParent() == dragParent) {
                        return dragParent.getChildIndex(node);
                    }
                }
                if(row > 0) {
                    // special case: allow to drop at the end of the parent
                    TreeTableNode node = treeTable.getNodeFromRow(row - 1);
                    if(node.getParent() == dragParent) {
                        int idx = dragParent.getChildIndex(node);
                        if(idx == dragParent.getNumChildren()-1) {
                            return idx + 1;
                        }
                    }
                }
                return -1;
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

    public void setFocusNameFieldCB(Runnable focusNameFieldCB) {
        this.focusNameFieldCB = focusNameFieldCB;
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
            if(model != null) {
                Long state = null;
                if(node instanceof ThemeTreeNode) {
                    state = ((ThemeTreeNode)node).getDOMElement().getID();
                }
                model.getUndo().setUserState(state);
            }
        }
    }
    
    void undoGotoLastSelected() {
        if(model != null) {
            Object state = model.getUndo().getUserState();
            if(state instanceof Long) {
                ThemeTreeNode node = model.findNode((Long)state);
                if(node != null) {
                    setSelected(node);
                }
            }
        }
    }

    void updateOperationButtons() {
        buttons.removeAllChildren();
        if(selected instanceof ThemeTreeNode) {
            ThemeTreeNode node = (ThemeTreeNode)selected;
            Menu m = new Menu();
            addButtons(m, node, node.getOperations());
            addCreateSubMenu(m, null, node, node.getCreateChildOperations());
            m.createMenuBar(buttons);
        }
    }

    void showTreeTablePopupMenu(int row, Event evt) {
        TreeTableNode nodeFromRow = treeTable.getNodeFromRow(row);
        if(nodeFromRow instanceof ThemeTreeNode) {
            ThemeTreeNode node = (ThemeTreeNode)nodeFromRow;
            Menu menu = new Menu();

            boolean[] enableUpdated = new boolean[1];
            
            if(node.getParent() instanceof ThemeTreeNode) {
                ThemeTreeNode parentNode = (ThemeTreeNode)node.getParent();
                List<CreateChildOperation> parentOperations = parentNode.getCreateChildOperations();
                addCreateSubMenu(menu, "opNewNodeBefore", enableUpdated, parentNode, parentOperations, CreateAtWrapper.Location.BEFORE, node);
                addCreateSubMenu(menu, "opNewNodeAfter", enableUpdated, parentNode, parentOperations, CreateAtWrapper.Location.AFTER, node);
            }
            
            addCreateSubMenu(menu, enableUpdated, node, node.getCreateChildOperations());
            List<ThemeTreeOperation> operations = node.getOperations();
            if(!operations.isEmpty()) {
                if(menu.getNumElements() > 0) {
                    menu.addSpacer();
                }
                addButtons(menu, node, operations);
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
        this.model = model;
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

    private void addButtons(Menu menu, ThemeTreeNode node, List<ThemeTreeOperation> operations) {
        for(final ThemeTreeOperation operation : operations) {
            MenuAction action = createMenuAction(node, operation, false);
            menu.add(action);
        }
    }

    private void addCreateSubMenu(Menu menu, boolean[] enableUpdated, final ThemeTreeNode node, List<CreateChildOperation> operations) {
        addCreateSubMenu(menu, "opNewNode", enableUpdated, node, operations, null, null);
    }

    private void addCreateSubMenu(Menu menu, String id,
            final boolean[] enableUpdated,
            final ThemeTreeNode node,
            final List<CreateChildOperation> operations,
            final CreateAtWrapper.Location location,
            final ThemeTreeNode pos) {
        if(!operations.isEmpty()) {
            final Menu subPopupMenu = new Menu();
            subPopupMenu.setTheme(id);
            subPopupMenu.setPopupTheme("opNewNode-popupMenu");
            menu.add(subPopupMenu);

            subPopupMenu.addListener(new Listener() {
                boolean hasEntries;
                public void menuOpening(Menu menu) {
                    if(!hasEntries) {
                        hasEntries = true;
                        
                        boolean isFirst = true;
                        if(enableUpdated != null) {
                            isFirst = !enableUpdated[0];
                            enableUpdated[0] = true;
                        }
                        
                        for(CreateChildOperation operation : operations) {
                            if(isFirst) {
                                operation.updateEnabledStateForPopup();
                            }
                            ThemeTreeOperation op = (pos == null) ? operation
                                    : new CreateAtWrapper(operation, location, pos);
                            MenuAction action = createMenuAction(node, op, true);
                            subPopupMenu.add(action);
                        }
                    }
                }
                public void menuOpened(Menu menu) {
                }
                public void menuClosed(Menu menu) {
                }
            });
        }
    }

    private MenuAction createMenuAction(final ThemeTreeNode node, final ThemeTreeOperation operation, boolean subMenu) {
        MenuAction action = new MenuAction();
        action.setTheme(operation.getActionID());
        action.setEnabled(operation.isEnabled(subMenu));
        action.setCallback(new Runnable() {
            public void run() {
                queryOperationParameter(node, operation, false);
            }
        });
        return action;
    }

    private static final MessageLog.Category CAT_THEME_TREE_OPERATION =
            new MessageLog.Category("Tree operation", MessageLog.CombineMode.NONE, DecoratedText.ERROR);

    void executeOperation(ThemeTreeOperation operation, Object[] paramter) {
        TreeTableNode newSelection = null;
        Undo.startComplexOperation();
        try {
            newSelection = operation.execute(paramter);
        } catch(IllegalArgumentException ex) {
            messageLog.add(new MessageLog.Entry(CAT_THEME_TREE_OPERATION, "Invalid parameters for operation", ex.getMessage(), null));
        } catch(Throwable ex) {
            messageLog.add(new MessageLog.Entry(CAT_THEME_TREE_OPERATION, "Error while executing tree operation", null, ex));
        } finally {
            Undo.endComplexOperation();
        }
        selectNode(newSelection);
        if(newSelection != null && focusNameFieldCB != null &&
                operation.shouldFocusNameFieldAfterExecute()) {
            getGUI().invokeLater(focusNameFieldCB);
        } else {
            treeTable.requestKeyboardFocus();
        }
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

    private static final MessageLog.Category CAT_URL_ERROR =
            new MessageLog.Category("URL", MessageLog.CombineMode.NONE, DecoratedText.ERROR);
    private static final MessageLog.Category CAT_URL_WARNING =
            new MessageLog.Category("URL", MessageLog.CombineMode.REPLACE, DecoratedText.WARNING);

    void queryOperationParameter(final ThemeTreeNode node, final ThemeTreeOperation operation, final boolean skipConfirm) {
        ThemeTreeOperation.Parameter[] parameter = operation.getParameter();
        if(parameter != null && parameter.length > 0) {
            File startDir = null;
            URL url = node.getThemeFile().getURL();
            
            try {
                URI uri = url.toURI();
                if("file".equals(uri.getScheme())) {
                    startDir = new File(uri).getParentFile();
                } else {
                    messageLog.add(new MessageLog.Entry(CAT_URL_WARNING, "Can't resolve URI in file system", uri.toString(), null));
                }
            } catch (Exception ex) {
                messageLog.add(new MessageLog.Entry(CAT_URL_ERROR, "Error resolving URL to file system", url.toString(), ex));
            }

            final QueryOperationParameter qop = new QueryOperationParameter(startDir, testWidgetManager);
            qop.setParameter(parameter);

            SimpleDialog dialog = new SimpleDialog();
            dialog.setTheme("parameterDlg-" + operation.getActionID());
            dialog.setTitle(operation.getActionID());
            dialog.setMessage(qop);
            dialog.setOkCallback(new Runnable() {
                public void run() {
                    maybeConfirmOperation(node, operation, qop.getResults(), skipConfirm);
                }
            });
            dialog.showDialog(this);
        } else {
            maybeConfirmOperation(node, operation, null, skipConfirm);
        }
    }

    void maybeConfirmOperation(ThemeTreeNode node, ThemeTreeOperation operation, Object[] paramter, boolean skipConfirm) {
        if(!skipConfirm && operation.needConfirm()) {
            confirmOperation(node, operation, paramter);
        } else {
            executeOperation(operation, paramter);
        }
    }

    void confirmOperation(ThemeTreeNode node, final ThemeTreeOperation operation, final Object[] paramter) {
        SimpleDialog dialog = new SimpleDialog();
        dialog.setTheme("confirmationDlg-" + operation.getActionID());
        dialog.setTitle(operation.getActionID());
        dialog.setMessage(node.toString());
        dialog.setOkCallback(new Runnable() {
            public void run() {
                executeOperation(operation, paramter);
            }
        });
        dialog.showDialog(treeTable);
    }

    boolean handleOperationKeyStrokeAction(String action, Event event) {
        if(event.isKeyPressedEvent() && !event.isKeyRepeated() && selected instanceof ThemeTreeNode) {
            ThemeTreeNode node = (ThemeTreeNode)selected;
            ThemeTreeOperation o = findOperation(node.getOperations(), action);
            if(o == null) {
                o = findOperation(node.getCreateChildOperations(), action);
            }
            if(o != null) {
                queryOperationParameter(node, o, (event.getModifiers() & Event.MODIFIER_SHIFT) != 0);
                return true;
            }
        }
        return false;
    }

    private ThemeTreeOperation findOperation(List<? extends ThemeTreeOperation> operations, String action) {
        for(ThemeTreeOperation o : operations) {
            if(o.getActionID().equals(action)) {
                return o;
            }
        }
        return null;
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
