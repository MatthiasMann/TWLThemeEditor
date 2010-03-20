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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.FileSelector;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.MenuAction;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.SimpleDialog;
import de.matthiasmann.twl.TextArea;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.FileSystemModel;
import de.matthiasmann.twl.model.FileSystemModel.FileFilter;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twl.model.MRUListModel;
import de.matthiasmann.twl.model.PersistentMRUListModel;
import de.matthiasmann.twl.model.SimpleTextAreaModel;
import de.matthiasmann.twlthemeeditor.datamodel.Include;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeFile;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.gui.EditorArea.Layout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    private static final String KEY_RECENT_PROJECTS = "recentProjects";

    private final Preferences prefs;
    private final EditorArea editorArea;
    private final MenuAction btnSaveProject;
    private final Menu recentProjectsMenu;
    private final MRUListModel<String> recentProjectsModel;

    private boolean closeRequested;
    private ThemeTreeModel model;
    private File projectDir;

    public MainUI() {
        this.prefs = Preferences.userNodeForPackage(MainUI.class);
        this.editorArea = new EditorArea();

        Menu menuFile = new Menu("File");
        menuFile.add("Open project", new Runnable() {
            public void run() {
                openProject();
            }
        });
        menuFile.add(recentProjectsMenu = new Menu("Open recent Project"));
        menuFile.add(btnSaveProject = new MenuAction("Save project", new Runnable() {
            public void run() {
                saveProject();
            }
        }));
        
        menuFile.addSpacer();
        menuFile.add("Exit", new Runnable() {
            public void run() {
                exit();
            }
        });

        Menu menuView = new Menu("View");
        menuView.add("Layout 1", editorArea.new LayoutModel(EditorArea.Layout.SPLIT_HV));
        menuView.add("Layout 2", editorArea.new LayoutModel(EditorArea.Layout.SPLIT_HHV));
        
        Menu mainMenu = new Menu();
        mainMenu.setTheme("mainmenu");
        mainMenu.add(menuFile);
        mainMenu.add(menuView);

        editorArea.addMenus(mainMenu);
        
        Widget menuBar = mainMenu.createMenuBar();

        recentProjectsModel = new PersistentMRUListModel<String>(5, String.class, prefs, KEY_RECENT_PROJECTS);
        
        btnSaveProject.setEnabled(false);
        
        setHorizontalGroup(createParallelGroup()
                .addWidget(menuBar)
                .addWidget(editorArea));
        setVerticalGroup(createSequentialGroup()
                .addWidget(menuBar)
                .addGap("menubar-editorarea")
                .addWidget(editorArea));

        popuplateRecentProjectsMenu();
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
            btnSaveProject.setEnabled(true);
            recentProjectsModel.addEntry(file.toString());
            popuplateRecentProjectsMenu();
        } catch(FileNotFoundException ex) {
            removeFromRecentProjectsList(file);
            showErrorMessage("Can't load project", ex);
        } catch(IOException ex) {
            showErrorMessage("Can't load project", ex);
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

    void showErrorMessage(String msg, Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.flush();

        TextArea textArea = new TextArea(new SimpleTextAreaModel(sw.toString()));
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFixed(ScrollPane.Fixed.HORIZONTAL);

        SimpleDialog dlg = new SimpleDialog();
        dlg.setTheme("errorMsgDialog");
        dlg.setMessage(scrollPane);
        dlg.setTitle(msg);
        dlg.showDialog(this);
    }

    void setLayout(Layout layout) {
        editorArea.setLayout(layout);
    }

    void exit() {
        closeRequested = true;
    }

    void popuplateRecentProjectsMenu() {
        recentProjectsMenu.clear();
        int numEntries = recentProjectsModel.getNumEntries();
        for(int i=0 ; i<numEntries ; i++) {
            final String entry = recentProjectsModel.getEntry(i);
            recentProjectsMenu.add(entry, new Runnable() {
                public void run() {
                    openProject(new File(entry));
                }
            });
        }
        recentProjectsMenu.setEnabled(numEntries > 0);
    }

    private void removeFromRecentProjectsList(File file) {
        int idx = Utils.find(recentProjectsModel, file.toString());
        if(idx >= 0) {
            recentProjectsModel.removeEntry(idx);
            popuplateRecentProjectsMenu();
        }
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

}
