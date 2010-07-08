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
package de.matthiasmann.twlthemeeditor.fontgen.gui;

import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.FileTable;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.fontgen.FontData;
import de.matthiasmann.twlthemeeditor.gui.MainUI.ExtFilter;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class FontSelectPopup extends PopupWindow {

    public interface Callback {
        public void fontSelected(String fontPath, FontData fontData);
    }

    private static final String PREF_FS = "fontSelector";
    
    private final FileSelector fileSelector;
    private final FontPreview preview;

    private String fontPath;
    private FontData fontData;
    private Callback[] callbacks;

    public FontSelectPopup(Widget owner) {
        super(owner);
        
        this.fileSelector = new FileSelector(Preferences.userNodeForPackage(FontSelectPopup.class), PREF_FS);
        this.preview = new FontPreview();

        fileSelector.setFileSystemModel(JavaFileSystemModel.getInstance());
        fileSelector.addFileFilter(new FileSelector.NamedFileFilter("TrueType fonts", new ExtFilter(".ttf")));
        fileSelector.setAllowMultiSelection(false);
        fileSelector.setUserWidgetBottom(preview);
        fileSelector.getFileTable().addCallback(new FileTable.Callback() {
            public void selectionChanged() {
                updatePreview();
            }
            public void sortingChanged() {
            }
        });
        fileSelector.addCallback(new FileSelector.Callback() {
            public void filesSelected(Object[] files) {
                fireAcceptFont();
            }
            public void canceled() {
                closePopup();
            }
        });

        add(fileSelector);
        setCloseOnClickedOutside(false);
    }

    public void addCallback(Callback cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Callback.class);
    }

    public void removeCallback(Callback cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
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

    void updatePreview() {
        fontPath = null;
        fontData = null;
        FileTable.Entry[] selection = fileSelector.getFileTable().getSelection();
        if(selection.length >= 0) {
            FileTable.Entry entry = selection[0];
            if(!entry.isFolder && (entry.obj instanceof File)) {
                try {
                    fontData = new FontData((File)entry.obj, 32);
                    fontPath = entry.getPath();
                } catch (Throwable ex) {
                    Logger.getLogger(FontSelectPopup.class.getName()).log(Level.SEVERE, "Can't open font: " + entry.getPath(), ex);
                }
            }
        }
        
        if(fontData != null) {
            fileSelector.setOkButtonEnabled(true);
            preview.setFont(fontData.getJavaFont());
            preview.setTooltipContent(fontData.getName());
        } else {
            fileSelector.setOkButtonEnabled(false);
            preview.setFont(null);
            preview.setTooltipContent(null);
        }
    }

    void fireAcceptFont() {
        if(fontData != null) {
            if(callbacks != null) {
                for(Callback cb : callbacks) {
                    cb.fontSelected(fontPath, fontData);
                }
            }
            closePopup();
        }
    }
}
