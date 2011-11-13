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

import de.matthiasmann.twl.Alignment;
import de.matthiasmann.twl.Button;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.TableBase;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.model.AbstractTreeTableModel;
import de.matthiasmann.twl.model.AbstractTreeTableNode;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeOperation;
import de.matthiasmann.twlthemeeditor.themeparams.ClassDatabase;
import de.matthiasmann.twlthemeeditor.themeparams.ClassThemeParamInfo;
import de.matthiasmann.twlthemeeditor.themeparams.ThemeParamInfo;
import java.io.File;
import java.net.URI;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class QueryOperationParameter extends DialogLayout {

    final File startDir;
    final TestWidgetManager testWidgetManager;
    Object[] results;

    public QueryOperationParameter(File startDir, TestWidgetManager testWidgetManager) {
        this.startDir = startDir;
        this.testWidgetManager = testWidgetManager;
    }

    public Object[] getResults() {
        return results.clone();
    }

    public void setParameter(ThemeTreeOperation.Parameter ... parameter) {
        setHorizontalGroup(null);
        setVerticalGroup(null);
        super.removeAllChildren();

        results = new Object[parameter.length];
        Group horzLabel = createParallelGroup();
        Group horzFields = createParallelGroup();
        Group vert = createSequentialGroup();

        for(int i=0,n=parameter.length ; i<n ; i++) {
            ThemeTreeOperation.Parameter p = parameter[i];
            
            Label label = new Label(p.name);
            Group vertRow = createParallelGroup()
                    .addWidget(label, Alignment.TOPLEFT);

            switch (p.type) {
                case FILE_SELECTOR:
                    addFileSelector(horzFields, vertRow, i, (ThemeTreeOperation.FileParameter)p);
                    break;
                case THEME_PARAMETER_INFO:
                    addThemeParameterInfoSelector(horzFields, vertRow, i);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            vert.addGroup(vertRow);
            horzLabel.addWidget(label);
        }
        
        setHorizontalGroup(createSequentialGroup()
                .addGroup(horzLabel)
                .addGroup(horzFields));
        setVerticalGroup(vert);
    }

    private void addFileSelector(Group horz, Group vert, final int resultIdx, ThemeTreeOperation.FileParameter fileParameter) {
        final EditField ef = new EditField();
        final Button btn = new Button();
        final LoadFileSelector lfs = new LoadFileSelector(this,
                Preferences.userNodeForPackage(QueryOperationParameter.class),
                "fileSelector_".concat(fileParameter.name.replace(' ', '_')),
                fileParameter.name, fileParameter.fileFilter, new LoadFileSelector.Callback() {
            public void fileSelected(File file) {
                URI uri = file.toURI();
                if(startDir != null) {
                    uri = startDir.toURI().relativize(uri);
                }
                String path = uri.getPath();
                results[resultIdx] = path;
                ef.setText(path);
            }

            public void canceled() {
            }
        });

        ef.setTheme("filename");
        ef.setReadOnly(true);

        btn.setTheme("selectFile");
        btn.addCallback(new Runnable() {
            public void run() {
                lfs.openPopup();
            }
        });

        horz.addGroup(createSequentialGroup()
                .addWidget(ef)
                .addWidget(btn));
        vert.addWidget(ef).addWidget(btn);
    }

    private void addThemeParameterInfoSelector(Group horz, Group vert, final int resultIdx) {
        CDBTreeModel model = new CDBTreeModel();
        for(ClassDatabase cdb :  testWidgetManager.getClassDatabases()) {
            model.addDatabase(cdb);
        }
        
        final TableSingleSelectionModel selModel = new TableSingleSelectionModel();
        final TreeTable table = new TreeTable(model);
        table.registerCellRenderer(FolderName.class, new FolderRenderer());
        table.setSelectionManager(new TableRowSelectionManager(selModel));
        
        for(int i=0 ; i<model.getNumChildren() ; i++) {
            table.setRowExpanded(table.getRowFromNode(model.getChild(i)), true);
        }
        
        selModel.addSelectionChangeListener(new Runnable() {
            public void run() {
                int row = selModel.getFirstSelected();
                ThemeParamInfo paramInfo = null;
                if(row >= 0) {
                    TreeTableNode node = table.getNodeFromRow(row);
                    if(node instanceof CDBTreeNode) {
                        paramInfo = ((CDBTreeNode)node).info;
                    }
                }
                results[resultIdx] = paramInfo;
            }
        });
        
        ScrollPane sp = new ScrollPane(table);
        sp.setFixed(ScrollPane.Fixed.HORIZONTAL);
        sp.setTheme("selectThemeParameterInfo");
        
        horz.addWidget(sp);
        vert.addWidget(sp);
    }
    
    static String lastPart(String name, char separator) {
        int idx = name.lastIndexOf(separator);
        return name.substring(idx+1);
    }
    
    static class FolderRenderer extends TableBase.StringCellRenderer {
        @Override
        public int getColumnSpan() {
            return 3;
        }
    }
    
    static class CDBTreeModel extends AbstractTreeTableModel {
        private static final String[] COLUMN_HEADER = {
            "Name", "Type", "Default"
        };
        
        public String getColumnHeaderText(int column) {
            return COLUMN_HEADER[column];
        }

        public int getNumColumns() {
            return COLUMN_HEADER.length;
        }
        
        public void addDatabase(ClassDatabase cdb) {
            CDBTreeNode cdbNode = new CDBTreeNode(cdb.getDisplayName(), this);
            
            for(ClassThemeParamInfo classInfo : cdb.getClassInfos()) {
                CDBTreeNode classNode = new CDBTreeNode(lastPart(classInfo.getClassName(), '/'), cdbNode);
                for(ThemeParamInfo tpi : cdb.collectParametersForClass(classInfo.getClassName())) {
                    CDBTreeNode node = new CDBTreeNode(tpi, classNode);
                    classNode.add(node);
                }
                if(classNode.getNumChildren() > 0) {
                    cdbNode.add(classNode);
                }
            }
            
            if(cdbNode.getNumChildren() > 0) {
                insertChild(cdbNode, getNumChildren());
            }
        }
    }
    
    static class FolderName {
        final String name;

        public FolderName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    
    static class CDBTreeNode extends AbstractTreeTableNode {
        final FolderName folderName;
        final ThemeParamInfo info;

        public CDBTreeNode(ThemeParamInfo info, TreeTableNode parent) {
            super(parent);
            this.folderName = null;
            this.info = info;
            setLeaf(true);
        }

        public CDBTreeNode(String folderName, TreeTableNode parent) {
            super(parent);
            this.folderName = new FolderName(folderName);
            this.info = null;
        }
        
        public Object getData(int column) {
            if(folderName != null) {
                return folderName;
            }
            switch(column) {
                case 0:
                    return info.getName();
                case 1:
                    if(info.isEnum()) {
                        return "enum (" + lastPart(info.getEnumType(), '.') + ")";
                    }
                    return info.getType();
                case 2:
                    return info.getDefaultValue();
                default:
                    return "???";
            }
        }
        
        public void add(CDBTreeNode node) {
            insertChild(node, getNumChildren());
        }
    }
}
