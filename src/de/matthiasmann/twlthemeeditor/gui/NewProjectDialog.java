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
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.EditFieldAutoCompletionWindow;
import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.ListBox;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.TreeComboBox;
import de.matthiasmann.twl.model.FileSystemAutoCompletionDataSource;
import de.matthiasmann.twl.model.FileSystemModel;
import de.matthiasmann.twl.model.FileSystemTreeModel;
import de.matthiasmann.twl.model.MRUListModel;
import de.matthiasmann.twl.model.PersistentMRUListModel;
import de.matthiasmann.twl.model.SimpleMRUListModel;
import de.matthiasmann.twl.model.TreeTableModel;
import de.matthiasmann.twl.model.TreeTableNode;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import org.lwjgl.input.Keyboard;

/**
 *
 * @author Matthias Mann
 */
public class NewProjectDialog extends DialogLayout {

    public interface Listener {
        public void ok(NewProjectSettings settings);
        public void canceled();
    }

    private final Listener listener;
    private final FileSystemModel fsm;
    private final FileSystemTreeModel model;
    private final MRUListModel<String> folderMRU;
    private final TreeComboBox currentFolder;
    private final Button btnFolderMRU;
    private final EditField efProjectName;
    private final Label labelInfo;
    private final Button btnOk;
    private final Button btnCancel;
    private final EditFieldAutoCompletionWindow autoCompletion;

    private NewProjectSettings settings;
    
    public NewProjectDialog(FileSystemModel fsm, Preferences prefs, String prefsKey, Listener listener) {
        this.listener = listener;
        this.fsm = fsm;
        this.model = new FileSystemTreeModel(fsm);
        
        if(listener == null) {
            throw new NullPointerException("listener");
        }
        
        if((prefs == null) != (prefsKey == null)) {
            throw new IllegalArgumentException("'prefs' and 'prefsKey' must both be valid or both null");
        }

        if(prefs != null) {
            folderMRU = new PersistentMRUListModel<String>(10, String.class, prefs, prefsKey.concat("_foldersMRU"));
        } else {
            folderMRU = new SimpleMRUListModel<String>(10);
        }

        model.setSorter(new FileSelector.NameSorter(fsm));

        currentFolder = new TreeComboBox();
        currentFolder.setTheme("currentFolder");

        btnFolderMRU = new Button();
        btnFolderMRU.setTheme("buttonFoldersMRU");
        btnFolderMRU.addCallback(new Runnable() {
            public void run() {
                showFolderMRU();
            }
        });

        efProjectName = new EditField();
        efProjectName.setTheme("editfieldProjectName");
        efProjectName.addCallback(new EditField.Callback() {
            public void callback(int key) {
                handleEditFieldKey(key);
            }
        });

        labelInfo = new Label();
        labelInfo.setTheme("labelInfo");
        
        btnOk = new Button();
        btnOk.setTheme("buttonOk");
        btnOk.setEnabled(false);
        btnOk.addCallback(new Runnable() {
            public void run() {
                acceptSelection();
            }
        });

        btnCancel = new Button();
        btnCancel.setTheme("buttonCancel");
        btnCancel.addCallback(new Runnable() {
            public void run() {
                canceled();
            }
        });

        autoCompletion = new EditFieldAutoCompletionWindow(currentFolder.getEditField());
        autoCompletion.setExecutorService(Executors.newSingleThreadExecutor());
        autoCompletion.setDataSource(new FileSystemAutoCompletionDataSource(fsm,
                FileSystemTreeModel.FolderFilter.instance));

        currentFolder.setPathResolver(new TreeComboBox.PathResolver() {
            public TreeTableNode resolvePath(TreeTableModel model, String path) throws IllegalArgumentException {
                return NewProjectDialog.this.resolvePath(path);
            }
        });
        currentFolder.addCallback(new TreeComboBox.Callback() {
            public void selectedNodeChanged(TreeTableNode node, TreeTableNode previousChildNode) {
                checkProjectSettings();
                setCurrentNode(node);
            }
        });
        currentFolder.setModel(model);
        currentFolder.setSeparator(fsm.getSeparator());
        currentFolder.getEditField().setAutoCompletionWindow(autoCompletion);

        Label labelCurrentFolder = new Label("Folder");
        labelCurrentFolder.setLabelFor(currentFolder);

        Label labelProjectName = new Label("Project Name");
        labelProjectName.setLabelFor(efProjectName);

        add(labelCurrentFolder);
        add(currentFolder);
        add(btnFolderMRU);
        add(efProjectName);
        add(btnOk);
        add(btnCancel);

        Group hCurrentFolder = createSequentialGroup()
                .addWidget(currentFolder)
                .addWidget(btnFolderMRU);
        Group vCurrentFolder = createParallelGroup()
                .addWidget(labelCurrentFolder)
                .addWidget(currentFolder)
                .addWidget(btnFolderMRU);

        Group hLabels = createParallelGroup()
                .addWidget(labelCurrentFolder)
                .addWidget(labelProjectName);

        Group hFields = createParallelGroup()
                .addGroup(hCurrentFolder)
                .addWidget(efProjectName);

        Group hMain = createSequentialGroup(hLabels, hFields);
        Group vMain = createSequentialGroup()
                .addGroup(vCurrentFolder)
                .addGroup(createParallelGroup(labelProjectName, efProjectName));

        Group hButtonGroup = createSequentialGroup()
                .addGap("buttonBarLeft")
                .addWidget(btnOk)
                .addGap("buttonBarSpacer")
                .addWidget(btnCancel)
                .addGap("buttonBarRight");
        Group vButtonGroup = createParallelGroup()
                .addWidget(btnOk)
                .addWidget(btnCancel);

        setHorizontalGroup(createParallelGroup()
                .addGroup(hMain)
                .addWidget(labelInfo)
                .addGroup(hButtonGroup));
        setVerticalGroup(createSequentialGroup()
                .addGroup(vMain)
                .addGap("settings-info")
                .addWidget(labelInfo)
                .addGroup(vButtonGroup));

        setCurrentNode(model);
        if(folderMRU.getNumEntries() > 0) {
            gotoFolderFromMRU(0);
        }

        checkProjectSettings();
    }

    final void setCurrentNode(TreeTableNode node) {
        currentFolder.setCurrentNode(node);
    }

    public Object getCurrentFolder() {
        Object node = currentFolder.getCurrentNode();
        if(node instanceof FileSystemTreeModel.FolderNode) {
            return ((FileSystemTreeModel.FolderNode)node).getFolder();
        } else {
            return null;
        }
    }

    TreeTableNode resolvePath(String path) throws IllegalArgumentException {
        Object obj = fsm.getFile(path);
        if(obj != null) {
            FileSystemTreeModel.FolderNode node = model.getNodeForFolder(obj);
            if(node != null) {
                return node;
            }
        }
        throw new IllegalArgumentException("Could not resolve: " + path);
    }

    void showFolderMRU() {
        final PopupWindow popup = new PopupWindow(this);
        final ListBox<String> listBox = new ListBox<String>(folderMRU);
        popup.setTheme("fileselector-folderMRUpopup");
        popup.add(listBox);
        if(popup.openPopup()) {
            popup.setInnerSize(getInnerWidth()*2/3, getInnerHeight()*2/3);
            popup.setPosition(btnFolderMRU.getX() - popup.getWidth(), btnFolderMRU.getY());
            listBox.addCallback(new CallbackWithReason<ListBox.CallbackReason>() {
                public void callback(ListBox.CallbackReason reason) {
                    if(reason.actionRequested()) {
                        popup.closePopup();
                        int idx = listBox.getSelected();
                        if(idx >= 0) {
                            gotoFolderFromMRU(idx);
                        }
                    }
                }
            });
        }
    }

    final void gotoFolderFromMRU(int idx) {
        String path = folderMRU.getEntry(idx);
        try {
            TreeTableNode node = resolvePath(path);
            setCurrentNode(node);
        } catch(IllegalArgumentException ex) {
            folderMRU.removeEntry(idx);
        }
    }
    
    void acceptSelection() {
        if(settings != null) {
            folderMRU.addEntry(settings.getFolder().getPath());
            listener.ok(settings);
        }
    }

    void canceled() {
        listener.canceled();
    }

    void handleEditFieldKey(int key) {
        if(key == Keyboard.KEY_ESCAPE) {
            canceled();
        } else {
            checkProjectSettings();
            if(key == Keyboard.KEY_RETURN && btnOk.isEnabled()) {
                acceptSelection();
            }
        }
    }

    private boolean checkProjectName(String projectName) {
        char prev = '.';
        for(int i=0,n=projectName.length()-4 ; i<n ; i++) {
            char ch = projectName.charAt(i);
            if(ch == '.') {
                if(prev == '.') {
                    return false;
                }
            } else if(!Character.isLetterOrDigit(ch) && ch != '_' && ch != '-') {
                return false;
            }
            prev = ch;
        }
        return prev != '.';
    }

    private void setMessage(String msg) {
        labelInfo.setText(msg);
        labelInfo.getAnimationState().setAnimationState("error", settings == null);
        labelInfo.getAnimationState().setAnimationState("warning", msg.length() > 0);
        btnOk.setEnabled(settings != null);
    }

    final void checkProjectSettings() {
        settings = null;

        Object folder = getCurrentFolder();
        if(!(folder instanceof File)) {
            setMessage("Selected folder is not usable");
            return;
        }

        if(!((File)folder).canWrite()) {
            setMessage("Selected folder is write protected");
            return;
        }

        String projectName = efProjectName.getText();
        if(projectName.length() == 0) {
            setMessage("Project name required");
            return;

        }

        if(!projectName.toLowerCase().endsWith(".xml")) {
            projectName = projectName.concat(".xml");
        }
        if(!checkProjectName(projectName)) {
            setMessage("Invalid project name. Must only contain alpha numeric characters, '-', '_' and single '.'");
            return;
        }

        settings = new NewProjectSettings((File)folder, projectName);

        StringBuilder msgs = new StringBuilder();
        for(File file : settings.getFileList()) {
            if(file.exists()) {
                if(msgs.length() == 0) {
                    msgs.append("The following files already exist and will be overwritten:");
                }
                msgs.append("\n").append(file.getName());
            }
        }
        setMessage(msgs.toString());
    }
}
