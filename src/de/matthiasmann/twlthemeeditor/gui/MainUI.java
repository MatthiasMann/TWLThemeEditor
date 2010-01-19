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
import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.SubMenu;
import de.matthiasmann.twl.model.FileSystemModel;
import de.matthiasmann.twl.model.FileSystemModel.FileFilter;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twlthemeeditor.datamodel.Include;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeFile;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.gui.EditorArea.Layout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class MainUI extends DialogLayout {

    private static final String KEY_PROJECT_FILESELECTOR = "projectsFiles";

    private final Preferences prefs;
    private final EditorArea editorArea;
    private final Button btnSaveProject;

    private boolean closeRequested;
    private ThemeTreeModel model;
    private File projectDir;

    public MainUI() {
        this.prefs = Preferences.userNodeForPackage(MainUI.class);
        this.editorArea = new EditorArea();

        MainMenu menuFile = new MainMenu("File");
        menuFile.getPopupMenu().add(createMenuItem("Open project", new Runnable() {
            public void run() {
                openProject();
            }
        }));
        menuFile.getPopupMenu().add(btnSaveProject = createMenuItem("Save project", new Runnable() {
            public void run() {
                saveProject();
            }
        }));
        menuFile.getPopupMenu().addSpacer();
        menuFile.getPopupMenu().add(createMenuItem("Exit", new Runnable() {
            public void run() {
                exit();
            }
        }));

        MainMenu menuView = new MainMenu("View");
        menuView.getPopupMenu().add(createMenuItem("Layout 1", new Runnable() {
            public void run() {
                setLayout(EditorArea.Layout.SPLIT_HV);
            }
        }));
        menuView.getPopupMenu().add(createMenuItem("Layout 2", new Runnable() {
            public void run() {
                setLayout(EditorArea.Layout.SPLIT_HHV);
            }
        }));

        setHorizontalGroup(createParallelGroup()
                .addGroup(createSequentialGroup(menuFile, menuView).addGap())
                .addWidget(editorArea));
        setVerticalGroup(createSequentialGroup()
                .addGroup(createParallelGroup(menuFile, menuView))
                .addWidget(editorArea));
    }

    void openProject() {
        final PopupWindow popupWindow = new PopupWindow(this);
        JavaFileSystemModel fsm = new JavaFileSystemModel();
        FileSelector.NamedFileFilter filter = new FileSelector.NamedFileFilter(
                "XML files", new ExtFilter(".xml"));
        FileSelector fileSelector = new FileSelector(prefs, KEY_PROJECT_FILESELECTOR);
        fileSelector.setFileSystemModel(fsm);
        fileSelector.addFileFilter(FileSelector.AllFilesFilter);
        fileSelector.addFileFilter(filter);
        fileSelector.setFileFilter(filter);
        fileSelector.setAllowMultiSelection(false);
        fileSelector.addCallback(new FileSelector.Callback() {
            public void filesSelected(Object[] files) {
                if(files.length == 1 && (files[0] instanceof File)) {
                    openProject((File)files[0]);
                }
                popupWindow.closePopup();
            }
            public void canceled() {
                popupWindow.closePopup();
            }
        });
        popupWindow.setTheme("fileselector-popup");
        popupWindow.add(fileSelector);
        popupWindow.setSize(getWidth()*4/5, getHeight()*4/5);
        popupWindow.setPosition(
                getWidth()/2 - popupWindow.getWidth()/2,
                getHeight()/2 - popupWindow.getHeight()/2);
        popupWindow.openPopup();
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    public void closeProject() {
        model = null;
        projectDir = null;
        editorArea.setModel(null);
    }
    
    public void openProject(File file) {
        closeProject();
        file = file.getAbsoluteFile();
        try {
            model = new ThemeTreeModel(file.toURI().toURL());
            projectDir = file.getParentFile();
            editorArea.setModel(model);
        } catch(IOException ex) {
            Logger.getLogger(MainUI.class.getName()).log(Level.SEVERE,
                    "Can't load project", ex);
        }
    }

    public void saveProject() {
        if(model != null && projectDir != null) {
            saveThemeFile(model.getRootThemeFile());
            for(Include inc : model.getTopLevelNodes(Include.class, null)) {
                saveThemeFile(inc.getIncludedThemeFile());
            }
        }
    }

    void saveThemeFile(ThemeFile themeFile) {
        if(themeFile.isModified()) {
            try {
                String relative = projectDir.toURI().relativize(themeFile.getURL().toURI()).getPath();

                File file = new File(projectDir, relative);
                File oldFile = new File(projectDir, relative.concat(".old"));
                File tmpFile = new File(projectDir, relative.concat(".tmp"));
                try {
                    FileOutputStream fos = new FileOutputStream(tmpFile);
                    try {
                        themeFile.writeTo(fos);
                    } finally {
                        fos.close();
                    }
                } catch(IOException ex) {
                    Logger.getLogger(MainUI.class.getName()).log(Level.SEVERE,
                            "Can't write XML to file: " + tmpFile, ex);
                    return;
                }

                if(oldFile.exists() && !oldFile.delete()) {
                    Logger.getLogger(MainUI.class.getName()).log(Level.WARNING,
                            "Can't delete old file: " + oldFile);
                }
                if(!file.renameTo(oldFile)) {
                    Logger.getLogger(MainUI.class.getName()).log(Level.WARNING,
                            "Can't rename file: " + file + " to " + oldFile);
                }
                if(!tmpFile.renameTo(file)) {
                    Logger.getLogger(MainUI.class.getName()).log(Level.WARNING,
                            "Can't rename file: " + tmpFile + " to " + file);
                }

                themeFile.setModified(false);
            } catch(URISyntaxException ex) {
                Logger.getLogger(MainUI.class.getName()).log(Level.SEVERE,
                        "Can't determine file location for: " + themeFile.getURL(), ex);
            }
        }
    }

    void setLayout(Layout layout) {
        editorArea.setLayout(layout);
    }

    void exit() {
        closeRequested = true;
    }

    private Button createMenuItem(String text, Runnable cb) {
        Button btn = new Button(text);
        btn.setTheme("menuitem");
        btn.addCallback(cb);
        return btn;
    }

    static class ExtFilter implements FileFilter {
        private final String[] extensions;
        public ExtFilter(String ... extensions) {
            this.extensions = extensions;
        }
        public boolean accept(FileSystemModel fsm, Object file) {
            String name = fsm.getName(file).toLowerCase();
            for(String extension : extensions) {
                if(name.endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    static class MainMenu extends SubMenu {
        public MainMenu(String text) {
            super(text);
            getPopupMenu().setTheme("mainMenu-popupMenu");
        }

        @Override
        protected void buttonAction() {
            getPopupMenu().showPopup(getX(), getBottom());
        }
    }
}
