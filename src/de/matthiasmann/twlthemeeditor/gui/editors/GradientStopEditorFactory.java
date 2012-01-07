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
package de.matthiasmann.twlthemeeditor.gui.editors;

import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.ColorSelector;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Table;
import de.matthiasmann.twl.ThemeInfo;
import de.matthiasmann.twl.ValueAdjusterFloat;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.AbstractTableModel;
import de.matthiasmann.twl.model.ColorSpaceHSL;
import de.matthiasmann.twl.model.ListModel;
import de.matthiasmann.twlthemeeditor.gui.ColorButton;
import de.matthiasmann.twlthemeeditor.gui.PropertyAccessor;
import de.matthiasmann.twlthemeeditor.gui.PropertyEditorFactory;
import de.matthiasmann.twlthemeeditor.properties.GradientStopModel;
import de.matthiasmann.twlthemeeditor.properties.GradientStopModel.Stop;
import de.matthiasmann.twlthemeeditor.properties.GradientStopProperty;

/**
 *
 * @author Matthias Mann
 */
public class GradientStopEditorFactory implements PropertyEditorFactory<GradientStopModel, GradientStopProperty> {

    public Widget create(PropertyAccessor<GradientStopModel, GradientStopProperty> pa) {
        final GSModel model = new GSModel(pa.getProperty().getPropertyValue());
        final Table table = new Table(model);
        table.setTheme("gradientstopeditor");
        table.registerCellRenderer(Stop.class, model.new CellEditor());
        return table;
    }
    
    static class GSModel extends AbstractTableModel implements ListModel.ChangeListener {
        private final GradientStopModel model;

        public GSModel(GradientStopModel model) {
            this.model = model;
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            boolean hadCallbacks = hasCallbacks();
            super.addChangeListener(listener);
            if(!hadCallbacks && hasCallbacks()) {
                model.addChangeListener(this);
            }
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            super.removeChangeListener(listener);
            if(!hasCallbacks()) {
                model.removeChangeListener(this);
            }
        }
        
        private static final String[] COLUMN_HEADER = { "Pos", "Color", "", "" };
        
        public String getColumnHeaderText(int column) {
            return COLUMN_HEADER[column];
        }

        public int getNumColumns() {
            return COLUMN_HEADER.length;
        }

        public Object getCell(int rowNr, int column) {
            Stop stop = model.getEntry(rowNr);
            if(stop.isSpecial() && column != 2) {
                return null;
            }
            return stop;
        }

        public int getNumRows() {
            return model.getNumEntries();
        }

        public void allChanged() {
            fireAllChanged();
        }

        public void entriesChanged(int first, int last) {
            fireRowsChanged(first, last - first + 1);
        }

        public void entriesDeleted(int first, int last) {
            fireRowsDeleted(first, last - first + 1);
        }

        public void entriesInserted(int first, int last) {
            fireRowsInserted(first, last - first + 1);
        }
        
        class CellEditor implements Table.CellWidgetCreator {
            Stop stop;
            int col;

            CellEditor() {
            }

            public String getTheme() {
                return "editor";
            }

            public void applyTheme(ThemeInfo themeInfo) {
            }

            public int getColumnSpan() {
                return 1;
            }

            public int getPreferredHeight() {
                return 18;
            }

            public void setCellData(int row, int column, Object data) {
                this.stop = (Stop)data;
                this.col = column;
            }

            public Widget getCellRenderWidget(int x, int y, int width, int height, boolean isSelected) {
                return null;
            }

            public void positionWidget(Widget widget, int x, int y, int w, int h) {
                widget.setPosition(x, y);
                widget.setSize(w, h);
            }

            public Widget updateWidget(Widget existingWidget) {
                switch(col) {
                    case 0: {
                        ValueAdjusterFloat va = (ValueAdjusterFloat)existingWidget;
                        if(va != null) {
                            va.setModel(stop.getPosModel());
                        } else {
                            va = new ValueAdjusterFloat(stop.getPosModel());
                        }
                        return va;
                    }
                    case 1: {
                        ColorButton cb = (ColorButton)existingWidget;
                        if(cb == null) {
                            cb = new ColorButton();
                            cb.addCallback(new ColorButtonListener(cb));
                        }
                        cb.setColorModel(stop.getColorModel());
                        return cb;
                    }
                    case 2:
                    case 3: {
                        AddRemoveButton btn = (AddRemoveButton)existingWidget;
                        if(btn == null) {
                            btn = new AddRemoveButton(col == 3);
                        }
                        btn.setStop(stop);
                        return btn;
                    }
                    default:
                        return null;
                }
            }
            
        }

    }
    
    static class ColorButtonListener implements Runnable {
        private final ColorButton button;

        ColorButtonListener(ColorButton button) {
            this.button = button;
        }
        
        public void run() {
            final ColorSelector cs = new ColorSelector(new ColorSpaceHSL());
            cs.setUseLabels(false);
            cs.setShowPreview(true);
            cs.setShowHexEditField(true);
            cs.setModel(button.getColorModel());
            PopupWindow popup = new PopupWindow(button);
            popup.setTheme("colorEditorPopup");
            popup.add(cs);
            if(popup.openPopup()) {
                popup.adjustSize();
                popup.setPosition(
                        ColorEditorFactory.computePos(button.getX(), popup.getWidth(), popup.getParent().getInnerRight()),
                        ColorEditorFactory.computePos(button.getY(), popup.getHeight(), popup.getParent().getInnerBottom()));
            }
        }
    }
    
    static class AddRemoveButton extends Button implements Runnable {
        private final boolean isRemove;
        private Stop stop;
        
        @SuppressWarnings("LeakingThisInConstructor")
        AddRemoveButton(boolean isRemove) {
            addCallback(this);
            setTheme(isRemove ? "removebutton" : "addbutton");
            this.isRemove = isRemove;
        }

        public void setStop(Stop stop) {
            this.stop = stop;
        }

        public void run() {
            if(isRemove) {
                stop.remove();
            } else {
                stop.insertBefore();
            }
        }
    }
}
