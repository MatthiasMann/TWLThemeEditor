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
                    .addWidget(label, Alignment.TOPLEFT);

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
}
