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
import de.matthiasmann.twl.CallbackWithReason;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.DialogLayout.Group;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ListBox;
import de.matthiasmann.twl.ListBox.CallbackReason;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SimpleDialog;
import de.matthiasmann.twl.TableRowSelectionManager;
import de.matthiasmann.twl.TreeTable;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.FileSystemTreeModel;
import de.matthiasmann.twl.model.FileSystemTreeModel.FolderNode;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twl.model.ListModel.ChangeListener;
import de.matthiasmann.twl.model.SimpleChangableListModel;
import de.matthiasmann.twl.model.TableSingleSelectionModel;
import de.matthiasmann.twl.model.TreeTableNode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author Matthias Mann
 */
public class NewClasspathDialog extends PopupWindow {

    public interface Callback {
        public void classpathCreated(File file);
    }

    private static final String PREF_SELECT_JAR = "userWidgetJARs";
    private static final String PREF_SAVE_CLASSPATH = "selectClasspath";
    private static final String PREF_LAST_FOLDER = "lastFolder";

    private final Callback callback;
    private final Preferences prefs;
    private final DialogLayout layout;
    
    final SimpleChangableListModel<File> entriesToScan;
    final SimpleChangableListModel<File> entriesDependencies;

    public NewClasspathDialog(Widget owner, Callback callback) {
        super(owner);

        this.callback = callback;
        this.prefs = Preferences.userNodeForPackage(NewClasspathDialog.class);
        this.layout = new DialogLayout();
        
        Group vRows = layout.createSequentialGroup();
        Group hTable = layout.createParallelGroup();
        Group hTableButtons = layout.createParallelGroup();

        Group hEntries = layout.createSequentialGroup()
                .addGroup(hTable)
                .addGroup(hTableButtons);
        Group hMain = layout.createParallelGroup()
                .addGroup(hEntries);
        
        this.entriesToScan = createList(vRows, hMain, hTable, hTableButtons, "Entries to Scan");
        this.entriesDependencies = createList(vRows, hMain, hTable, hTableButtons,
                "Dependencies (don't add LWJGL or TWL!)");

        final Button btnSave = new Button("Save");
        btnSave.setEnabled(false);
        btnSave.addCallback(new Runnable() {
            public void run() {
                save();
            }
        });

        Button btnCancel = new Button("Cancel");
        btnCancel.addCallback(new Runnable() {
            public void run() {
                cancel();
            }
        });

        entriesToScan.addChangeListener(new ChangeListener() {
            public void entriesInserted(int first, int last) {
                allChanged();
            }
            public void entriesDeleted(int first, int last) {
                allChanged();
            }
            public void entriesChanged(int first, int last) {
                allChanged();
            }
            public void allChanged() {
                btnSave.setEnabled(entriesToScan.getNumEntries() > 0);
            }
        });
        
        Group hButtons = layout.createSequentialGroup()
                .addGap()
                .addWidget(btnSave)
                .addWidget(btnCancel);
        Group vButtons = layout.createParallelGroup()
                .addWidget(btnSave)
                .addWidget(btnCancel);

        layout.setHorizontalGroup(hMain
                .addGroup(hButtons));
        layout.setVerticalGroup(vRows
                .addGroup(vButtons));

        add(layout);
    }

    @Override
    public boolean openPopup() {
        if(super.openPopup()) {
            int width = getGUI().getInnerWidth();
            int height = getGUI().getInnerHeight();
            setSize(width*4/5, height*4/5);
            setPosition(
                    width/2 - getWidth()/2,
                    height/2 - getHeight()/2);
            return true;
        }
        return false;
    }

    void save() {
        if(entriesToScan.getNumEntries() > 0) {
            SaveFileSelector sfs = new SaveFileSelector(this, prefs, PREF_SAVE_CLASSPATH,
                    "TWL Theme Editor classpath file", ".classpath", new SaveFileSelector.Callback() {
                public File[] getFilesCreatedForFileName(File file) {
                    return new File[] { file };
                }
                public void fileNameSelected(File file) {
                    save(file);
                }
                public void canceled() {
                }
            });
            sfs.openPopup();
        }
    }

    void cancel() {
        closePopup();
    }

    void save(File file) {
        try {
            URI base = file.getParentFile().toURI();
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlSerializer xs = factory.newSerializer();
            
            FileOutputStream fos = new FileOutputStream(file);
            try {
                xs.setOutput(fos, "UTF8");
                xs.startDocument("UTF8", Boolean.TRUE);
                xs.text("\n");
                xs.startTag(null, "classpath");
                xs.text("\n    ");
                xs.startTag(null, "entriesToScan");
                writeEntries(xs, base, entriesToScan);
                xs.text("\n    ");
                xs.endTag(null, "entriesToScan");
                xs.text("\n    ");
                xs.startTag(null, "entriesDependencies");
                writeEntries(xs, base, entriesDependencies);
                xs.text("\n    ");
                xs.endTag(null, "entriesDependencies");
                xs.text("\n");
                xs.endTag(null, "classpath");
                xs.endDocument();
            } finally {
                fos.close();
            }

            callback.classpathCreated(file);
            closePopup();
        } catch (Exception ex) {
            Logger.getLogger(NewClasspathDialog.class.getName()).log(Level.SEVERE,
                    "can't write classpath", ex);
        }
    }

    private void writeEntries(XmlSerializer xs, URI base, SimpleChangableListModel<File> entries) throws IOException {
        for(int i=0 ; i<entries.getNumEntries() ; i++) {
            URI entry = entries.getEntry(i).toURI();
            xs.text("\n        ");
            xs.startTag(null, "entry");
            xs.text(base.relativize(entry).toASCIIString());
            xs.endTag(null, "entry");
        }
    }
    private SimpleChangableListModel<File> createList(Group vRows, Group hMain, Group hTable, Group hButtons, String title) {
        final SimpleChangableListModel<File> model = new SimpleChangableListModel<File>();
        final ListBox<File> listBox = new ListBox<File>(model);
        final Button btnAddFolder = new Button("Add Folder");
        final Button btnAddJAR = new Button("Add JAR");
        final Button btnRemove = new Button("Remove");

        listBox.addCallback(new CallbackWithReason<ListBox.CallbackReason>() {
            public void callback(CallbackReason reason) {
                btnRemove.setEnabled(listBox.getSelected() >= 0);
            }
        });
        btnAddFolder.addCallback(new Runnable() {
            public void run() {
                addFolder(model, btnAddFolder);
            }
        });
        btnAddJAR.addCallback(new Runnable() {
            public void run() {
                addJAR(model, btnAddJAR);
            }
        });
        btnRemove.setEnabled(false);
        btnRemove.addCallback(new Runnable() {
            public void run() {
                int idx = listBox.getSelected();
                if(idx >= 0) {
                    model.removeElement(idx);
                }
            }
        });

        Label labelTitle = new Label(title);
        labelTitle.setTheme("title");

        Group vButtons = layout.createSequentialGroup()
                .addWidget(btnAddFolder)
                .addWidget(btnAddJAR)
                .addWidget(btnRemove)
                .addGap();
        Group vGroup = layout.createParallelGroup()
                .addWidget(listBox)
                .addGroup(vButtons);
        
        vRows.addWidget(labelTitle).addGroup(vGroup).addGap(DialogLayout.MEDIUM_GAP);
        hMain.addWidget(labelTitle);
        hTable.addWidget(listBox);
        hButtons.addWidget(btnAddFolder)
                .addWidget(btnAddJAR)
                .addWidget(btnRemove);
        return model;
    }

    void addFolder(final SimpleChangableListModel<File> model, Button btn) {
        final FileSystemTreeModel fstm = new FileSystemTreeModel(JavaFileSystemModel.getInstance());
        fstm.setSorter(new FileSelector.NameSorter(JavaFileSystemModel.getInstance()));

        final TableSingleSelectionModel selectionModel = new TableSingleSelectionModel();
        final TreeTable treeTable = new TreeTable(fstm);
        final ScrollPane scrollPane = new ScrollPane(treeTable);
        final SimpleDialog dialog = new SimpleDialog();

        treeTable.setSelectionManager(new TableRowSelectionManager(selectionModel));
        treeTable.addCallback(new de.matthiasmann.twl.TableBase.Callback() {
            public void mouseDoubleClicked(int row, int column) {
                if(row >= 0 && row < treeTable.getNumRows()) {
                    treeTable.setRowExpanded(row, true);
                }
            }
            public void mouseRightClick(int row, int column, Event evt) {
            }
            public void columnHeaderClicked(int column) {
            }
        });

        scrollPane.setExpandContentSize(true);
        scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

        dialog.setTheme("selectfolder");
        dialog.setMessage(scrollPane);
        dialog.setOkCallback(new Runnable() {
            public void run() {
                int idx = selectionModel.getFirstSelected();
                if(idx >= 0) {
                    TreeTableNode ttn = treeTable.getNodeFromRow(idx);
                    if(ttn instanceof FolderNode) {
                        File file = (File)((FolderNode)ttn).getFolder();
                        prefs.put(PREF_LAST_FOLDER, file.toString());
                        model.addElement(file);
                    }
                }
            }
        });

        dialog.showDialog(btn);
        
        String lastFolder = prefs.get(PREF_LAST_FOLDER, null);
        if(lastFolder != null) {
            File file = new File(lastFolder);
            FolderNode node = fstm.getNodeForFolder(file);
            if(node != null) {
                int row = treeTable.getRowFromNodeExpand(node);
                if(row >= 0) {
                    treeTable.scrollToRow(row);
                    selectionModel.setSelection(row, row);
                }
            }
        }
    }

    void addJAR(final SimpleChangableListModel<File> model, Button btn) {
        LoadFileSelector lfs = new LoadFileSelector(btn, prefs, PREF_SELECT_JAR,
                "Java JAR archive", ".jar", new LoadFileSelector.Callback() {
            public void fileSelected(File file) {
                if(file.isFile() && file.canRead()) {
                    model.addElement(file);
                }
            }
            public void canceled() {
            }
        });
        lfs.openPopup();
    }
}
