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

import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.FileTable.Entry;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import java.io.File;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class SaveFileSelector {

    public interface Callback {
        public File[] getFilesCreatedForFileName(File file);
        public void fileNameSelected(File file);
        public void canceled();
    }

    final EditField editField;
    final FileSelector fileSelector;
    final PopupWindow popupWindow;
    final Callback callback;
    final String extension;

    public SaveFileSelector(Widget owner, Preferences prefs, String prefsKey,
            String description, String extension, Callback callback) {
        this.callback = callback;
        this.extension = extension;

        if(callback == null) {
            throw new NullPointerException("callback");
        }
        
        CB cb = new CB();
        
        editField = new EditField();
        editField.addCallback(cb);

        FileSelector.NamedFileFilter filter = new FileSelector.NamedFileFilter(description, new MainUI.ExtFilter(extension));
        
        fileSelector = new FileSelector(prefs, prefsKey);
        fileSelector.setFileSystemModel(JavaFileSystemModel.getInstance());
        fileSelector.setAllowMultiSelection(false);
        fileSelector.setUserWidgetBottom(editField);
        fileSelector.addCallback(cb);
        fileSelector.addFileFilter(FileSelector.AllFilesFilter);
        fileSelector.addFileFilter(filter);
        fileSelector.setFileFilter(filter);

        popupWindow = new PopupWindow(owner);
        popupWindow.setTheme("fileselector-popup");
        popupWindow.add(fileSelector);
    }

    public void openPopup() {
        if(popupWindow.openPopup()) {
            GUI gui = popupWindow.getGUI();
            popupWindow.setSize(gui.getWidth()*4/5, gui.getHeight()*4/5);
            popupWindow.setPosition(
                    (gui.getWidth() - popupWindow.getWidth())/2,
                    (gui.getHeight() - popupWindow.getHeight())/2);
        }
    }

    void checkName() {
        String name = editField.getText();
        if(name.length() == 0) {
            fileSelector.setOkButtonEnabled(false);
            editField.setErrorMessage(null);
            return;
        }

        File folder = (File)fileSelector.getCurrentFolder();
        if(folder == null) {
            fileSelector.setOkButtonEnabled(false);
            editField.setErrorMessage(null);
            return;
        }

        if(!folder.canWrite()) {
            fileSelector.setOkButtonEnabled(false);
            editField.setErrorMessage("Can't write to folder " + folder);
            return;
        }

        StringBuilder msg = new StringBuilder();
        File file = new File(folder, name);
        for(File fileToCheck : callback.getFilesCreatedForFileName(file)) {
            if(fileToCheck.exists()) {
                msg.append(fileToCheck).append(" exists and will be overwritten\n");
            }
        }

        fileSelector.setOkButtonEnabled(true);
        if(msg.length() > 0) {
            editField.setErrorMessage(msg.substring(0, msg.length()-1));
        } else {
            editField.setErrorMessage(null);
        }
    }
    
    class CB implements FileSelector.Callback2, EditField.Callback {
        boolean inSelectionChanged;
        public void filesSelected(Object[] files) {
            File folder = (File)fileSelector.getCurrentFolder();
            String name = editField.getText();
            if(name.length() > 0 && folder != null) {
                File file = new File(folder, name);
                popupWindow.closePopup();
                callback.fileNameSelected(file);
            }
        }

        public void canceled() {
            popupWindow.closePopup();
            callback.canceled();
        }

        public void folderChanged(Object folder) {
            try {
                checkName();
            } finally {
                inSelectionChanged = false;
            }
        }

        public void selectionChanged(Entry[] selection) {
            if(selection.length == 1 && !selection[0].isFolder) {
                inSelectionChanged = true;
                try {
                    editField.setText(selection[0].name);
                } finally {
                    inSelectionChanged = false;
                }
            }
        }

        public void callback(int key) {
            checkName();
            if(!editField.hasSelection() && editField.getCursorPos() == editField.getTextLength() && key == Event.KEY_NONE) {
                String name = editField.getText();
                if(!name.endsWith(extension)) {
                    String newName = name.concat(extension);
                    editField.setText(newName);
                    editField.setSelection(name.length(), newName.length());
                }
            }
            if(!inSelectionChanged) {
                fileSelector.clearSelection();
            }
        }
    }

}
