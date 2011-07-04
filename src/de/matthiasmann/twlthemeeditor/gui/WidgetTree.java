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

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.ScrollPane.Fixed;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.ThemeInfo;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.renderer.MouseCursor;

/**
 *
 * @author Matthias Mann
 */
public class WidgetTree extends DialogLayout {

    private final WidgetTreeModel treeModel;
    private final PreviewWidget previewWidget;
    private final TableSingleSelectionModel selectionModel;
    private final TreeTable treeTable;
    private final ScrollPane scrollPane;
    private final Button btnReloadWidget;
    private final Button btnFlashSelectedWidget;
    private final Widget btnSelectWidget;

    private GUI testGUI;

    public WidgetTree(WidgetTreeModel treeModel, PreviewWidget previewWidget) {
        this.treeModel = treeModel;
        this.previewWidget = previewWidget;

        selectionModel = new TableSingleSelectionModel();
        treeTable = new TreeTable(treeModel);
        treeTable.setTheme("themetree");
        treeTable.setSelectionManager(new TableRowSelectionManager(selectionModel));

        DecoratedTextRenderer.install(treeTable);

        scrollPane = new ScrollPane(treeTable);
        scrollPane.setFixed(Fixed.HORIZONTAL);

        btnReloadWidget = new Button();
        btnReloadWidget.setTheme("btnReloadWidget");
        btnReloadWidget.setEnabled(false);
        
        btnFlashSelectedWidget = new Button();
        btnFlashSelectedWidget.setTheme("btnFlash");
        btnFlashSelectedWidget.setEnabled(false);
        btnFlashSelectedWidget.addCallback(new Runnable() {
            public void run() {
                flashSelectedWidget();
            }
        });

        btnSelectWidget = new Button() {
            Widget selectedWidget;
            MouseCursor cursorNormal;
            MouseCursor cursorDragging;

            @Override
            protected boolean handleEvent(Event evt) {
                if(evt.isMouseEvent() && evt.isMouseDragEvent()) {
                    selectedWidget = selectWidgetFromMouse(evt.getMouseX(), evt.getMouseY());
                    flashWidget(selectedWidget, false);
                    if(evt.isMouseDragEnd()) {
                        setMouseCursor(cursorNormal);
                        selectWidget(selectedWidget);
                        selectedWidget = null;
                    } else {
                        setMouseCursor(cursorDragging);
                        return true;
                    }
                }
                return super.handleEvent(evt) || evt.isMouseEventNoWheel();
            }

            @Override
            protected void applyTheme(ThemeInfo themeInfo) {
                super.applyTheme(themeInfo);
                cursorNormal = themeInfo.getMouseCursor("mouseCursor");
                cursorDragging = themeInfo.getMouseCursor("mouseCursorDragging");
            }
        };
        btnSelectWidget.setTheme("btnSelect");
        btnSelectWidget.setCanAcceptKeyboardFocus(true);

        selectionModel.addSelectionChangeListener(new Runnable() {
            public void run() {
                btnFlashSelectedWidget.setEnabled(selectionModel.hasSelection());
            }
        });

        setHorizontalGroup(createParallelGroup()
                .addWidget(scrollPane)
                .addGroup(createSequentialGroup()
                    .addGap("left-reload")
                    .addWidget(btnReloadWidget)
                    .addGap("reload-select")
                    .addWidget(btnSelectWidget)
                    .addGap("select-flash")
                    .addWidget(btnFlashSelectedWidget)
                    .addGap("flash-right")));
        setVerticalGroup(createSequentialGroup()
                .addWidget(scrollPane)
                .addGroup(createParallelGroup(btnReloadWidget, btnSelectWidget, btnFlashSelectedWidget)));
    }

    public void setTestGUI(Context ctx, GUI testGUI) {
        this.testGUI = testGUI;
        treeModel.createTreeFromWidget(ctx, testGUI);
        if(treeModel.getNumChildren() > 0) {
            selectionModel.setSelection(0, 0);
            treeTable.setRowExpanded(0, true);
        }
        btnReloadWidget.setEnabled(testGUI != null);
    }

    public void setContext(Context ctx) {
        setTestGUI(ctx, testGUI);
    }

    public void addReloadButtenCallback(Runnable cb) {
        btnReloadWidget.addCallback(cb);
    }
    
    public void addSelectionChangeListener(Runnable cb) {
        selectionModel.addSelectionChangeListener(cb);
    }

    public Widget getSelectedWidget() {
        int idx = selectionModel.getFirstSelected();
        Widget widget = (idx >= 0) ? treeModel.getWidget(treeTable.getNodeFromRow(idx)) : null;
        // make sure that the widget has not been removed (eg tree model is outdated)
        for(Widget w=widget ; w!=null ; w=w.getParent()) {
            if(w == testGUI) {
                return widget;
            }
        }
        return null;
    }

    void selectWidget(Widget widget) {
        WidgetTreeModel.Node node = treeModel.getNodeForWidget(widget);
        if(node != null) {
            int row = treeTable.getRowFromNodeExpand(node);
            selectionModel.setSelection(row, row);
            treeTable.scrollToRow(row);
        }
        flashWidget(widget, true);
    }

    Widget selectWidgetFromMouse(int x, int y) {
        return previewWidget.selectWidgetFromMouse(x, y);
    }
    
    void flashSelectedWidget() {
        flashWidget(getSelectedWidget(), true);
    }

    void flashWidget(Widget w, boolean flashing) {
        if(w != null) {
            previewWidget.flashRectangle(w.getX(), w.getY(), w.getWidth(), w.getHeight(), flashing);
        } else {
            previewWidget.flashRectangle(0, 0, 0, 0, false);
        }
    }
}
