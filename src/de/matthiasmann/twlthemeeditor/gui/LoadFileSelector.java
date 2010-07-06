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

import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.FileSystemModel.FileFilter;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import java.io.File;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class LoadFileSelector {

    public interface Callback {
        public void fileSelected(File file);
        public void canceled();
    }

    final Callback callback;
    final FileSelector fileSelector;
    final PopupWindow popupWindow;

    public LoadFileSelector(Widget owner, Preferences prefs, String prefsKey,
            String description, String extension, Callback callback) {
        this(owner, prefs, prefsKey, description, new MainUI.ExtFilter(extension), callback);
    }
    
    public LoadFileSelector(Widget owner, Preferences prefs, String prefsKey,
            String description, FileFilter fileFilter, Callback callback) {
        this.callback = callback;

        if(callback == null) {
            throw new NullPointerException("callback");
        }

        FileSelector.NamedFileFilter filter = new FileSelector.NamedFileFilter(description, fileFilter);

        fileSelector = new FileSelector(prefs, prefsKey);
        fileSelector.setFileSystemModel(JavaFileSystemModel.getInstance());
        fileSelector.setAllowMultiSelection(false);
        fileSelector.addCallback(new CB());
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

    class CB implements FileSelector.Callback {
        public void filesSelected(Object[] files) {
            if(files.length == 1 && (files[0] instanceof File)) {
                popupWindow.closePopup();
                callback.fileSelected((File)files[0]);
            }
        }

        public void canceled() {
            popupWindow.closePopup();
            callback.canceled();
        }
    }
}
