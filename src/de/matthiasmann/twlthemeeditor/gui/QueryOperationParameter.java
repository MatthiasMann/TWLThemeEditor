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
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeOperation;
import java.io.File;
import java.net.URI;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class QueryOperationParameter extends DialogLayout {

    final File startDir;
    Object[] results;

    public QueryOperationParameter(File startDir) {
        this.startDir = startDir;
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
                    .addGroup(createSequentialGroup().addWidget(label).addGap());

            switch (p.type) {
                case FILE_SELECTOR:
                    addFileSelector(horzFields, vertRow, i, (ThemeTreeOperation.FileParameter)p);
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
        final PopupWindow popupWindow = new PopupWindow(this);
        final FileSelector fileSelector = new FileSelector(
                Preferences.userNodeForPackage(QueryOperationParameter.class),
                "fileSelector_".concat(fileParameter.name.replace(' ', '_')));

        fileSelector.setFileSystemModel(new JavaFileSystemModel());
        fileSelector.setCurrentFolder(startDir);
        fileSelector.setAllowMultiSelection(false);
        fileSelector.setAllowFolderSelection(false);
        fileSelector.addFileFilter(new FileSelector.NamedFileFilter(fileParameter.name, fileParameter.fileFilter));
        fileSelector.addCallback(new FileSelector.Callback() {
            public void filesSelected(Object[] files) {
                if(files.length == 1 && (files[0] instanceof File)) {
                    URI uri = ((File)files[0]).toURI();
                    if(startDir != null) {
                        uri = startDir.toURI().relativize(uri);
                    }
                    String path = uri.getPath();
                    results[resultIdx] = path;
                    ef.setText(path);
                }
                popupWindow.closePopup();
            }
            public void canceled() {
                popupWindow.closePopup();
            }
        });

        popupWindow.setTheme("fileselector-popup");
        popupWindow.add(fileSelector);

        ef.setTheme("filename");
        ef.setReadOnly(true);

        btn.setTheme("selectFile");
        btn.addCallback(new Runnable() {
            public void run() {
                int width = getGUI().getInnerWidth();
                int height = getGUI().getInnerHeight();
                popupWindow.setSize(width*4/5, height*4/5);
                popupWindow.setPosition(
                        width/2 - popupWindow.getWidth()/2,
                        height/2 - popupWindow.getHeight()/2);
                popupWindow.openPopup();
            }
        });

        horz.addGroup(createSequentialGroup()
                .addWidget(ef)
                .addWidget(btn));
        vert.addWidget(ef).addWidget(btn);
    }
}
