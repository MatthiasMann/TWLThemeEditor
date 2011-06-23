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

import de.matthiasmann.twl.Clipboard;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.MenuAction;
import de.matthiasmann.twl.MenuCheckbox;
import de.matthiasmann.twl.PopupWindow;
import de.matthiasmann.twl.SimpleDialog;
import de.matthiasmann.twl.TextArea;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.FileSystemModel;
import de.matthiasmann.twl.model.FileSystemModel.FileFilter;
import de.matthiasmann.twl.textarea.HTMLTextAreaModel;
import de.matthiasmann.twl.model.JavaFileSystemModel;
import de.matthiasmann.twl.model.MRUListModel;
import de.matthiasmann.twl.model.PersistentMRUListModel;
import de.matthiasmann.twl.textarea.StyleSheet;
import de.matthiasmann.twlthemeeditor.Main;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.datamodel.Include;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeFile;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeModel;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.fontgen.gui.FontGenDialog;
import de.matthiasmann.twlthemeeditor.imgconv.ConvertImageDialog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author Matthias Mann
 */
public final class MainUI extends DialogLayout {

    private static final String KEY_PROJECT_FILESELECTOR = "projectsFiles";
    private static final String KEY_RECENT_PROJECTS = "recentProjects";

    private static final MessageLog.Category CAT_PROJECT = new MessageLog.Category("Project", MessageLog.CombineMode.NONE, 0);
    private static final MessageLog.Category CAT_PROJECT_WARNING = new MessageLog.Category("Project", MessageLog.CombineMode.NONE, DecoratedText.WARNING);
    private static final MessageLog.Category CAT_PROJECT_ERROR = new MessageLog.Category("Project", MessageLog.CombineMode.NONE, DecoratedText.ERROR);
    
    private final Preferences prefs;
    private final MessageLog messageLog;
    private final EditorArea editorArea;
    private final StatusBar statusBar;
    private final MenuAction btnSaveProject;
    private final Menu recentProjectsMenu;
    private final MRUListModel<String> recentProjectsModel;

    private boolean closeRequested;
    private ThemeTreeModel model;
    private File projectDir;

    public MainUI() {
        this.prefs = Preferences.userNodeForPackage(MainUI.class);
        this.messageLog = new MessageLog();
        this.editorArea = new EditorArea(messageLog);
        this.statusBar = new StatusBar(messageLog);

        Menu menuFile = new Menu("File");
        menuFile.add("New project...", new Runnable() {
            public void run() {
                newProject();
            }
        });
        menuFile.add("Open project...", new Runnable() {
            public void run() {
                openProject();
            }
        });
        menuFile.add(new MenuAction("Start demo mode", new Runnable() {
            public void run() {
                startDemoMode();
            }
        }).setTooltipContent("Opens the TWL Theme Editor in read-only mode"));
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
        menuView.add(new MenuCheckbox("Layout 1", editorArea.getLayoutBooleanModel(EditorArea.Layout.SPLIT_HV)).setTheme("radiobtn"));
        menuView.add(new MenuCheckbox("Layout 2", editorArea.getLayoutBooleanModel(EditorArea.Layout.SPLIT_HHV)).setTheme("radiobtn"));
        
        Menu mainMenu = new Menu();
        mainMenu.setTheme("mainmenu");
        mainMenu.add(menuFile);
        mainMenu.add(menuView);

        editorArea.addMenus(mainMenu);

        Menu menuSettings = new Menu("Settings");
        editorArea.addSettingsMenuItems(menuSettings);
        mainMenu.add(menuSettings);

        Menu menuTools = new Menu("Tools");
        menuTools.add("Create font...", new Runnable() {
            public void run() {
                FontGenDialog fgd = new FontGenDialog(MainUI.this);
                fgd.openPopup();
            }
        });
        menuTools.add("Convert image...", new Runnable() {
            public void run() {
                ConvertImageDialog cid = new ConvertImageDialog(MainUI.this);
                cid.openPopup();
            }
        });
        menuTools.addSpacer();
        menuTools.add("Reload all images", new Runnable() {
            public void run() {
                editorArea.reapplyTheme();
            }
        });
        mainMenu.add(menuTools);

        Menu menuHelp = new Menu("Help");
        menuHelp.add("About", new Runnable() {
            public void run() {
                openAboutDialog();
            }
        });
        mainMenu.add(menuHelp);

        Widget menuBar = mainMenu.createMenuBar();

        recentProjectsModel = new PersistentMRUListModel<String>(5, String.class, prefs, KEY_RECENT_PROJECTS);
        
        btnSaveProject.setEnabled(false);
        
        setHorizontalGroup(createParallelGroup()
                .addWidget(menuBar)
                .addWidget(editorArea)
                .addWidget(statusBar));
        setVerticalGroup(createSequentialGroup()
                .addWidget(menuBar)
                .addGap("menubar-editorarea")
                .addWidget(editorArea)
                .addGap("editorarea-statusbar")
                .addWidget(statusBar));

        popuplateRecentProjectsMenu();

        addActionMapping("reloadTestWidget", "reloadTestWidget");
    }

    @Override
    protected void afterAddToGUI(GUI gui) {
        super.afterAddToGUI(gui);
        messageLog.setGUI(gui);
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        messageLog.setGUI(null);
        super.beforeRemoveFromGUI(gui);
    }

    public void addMessage(MessageLog.Entry entry) {
        messageLog.add(entry);
    }

    public void clearMessages(MessageLog.Category category) {
        messageLog.removeAll(category);
    }

    public void openMessagesDialog() {
        statusBar.openDetailsDialog();
    }

    void newProject() {
        final PopupWindow popupWindow = new PopupWindow(this);
        
        JavaFileSystemModel fsm = JavaFileSystemModel.getInstance();
        NewProjectDialog newProjectDialog = new NewProjectDialog(fsm,
                prefs, KEY_PROJECT_FILESELECTOR, new NewProjectDialog.Listener() {
            public void ok(NewProjectSettings settings) {
                newProject(settings);
                popupWindow.closePopup();
            }
            public void canceled() {
                popupWindow.closePopup();
            }
        });
        
        popupWindow.setTheme("newproject-popup");
        popupWindow.add(newProjectDialog);
        popupWindow.openPopup();
        popupWindow.setSize(getWidth()*4/5, popupWindow.getPreferredHeight());
        popupWindow.setPosition(
                getWidth()/2 - popupWindow.getWidth()/2,
                getHeight()/2 - popupWindow.getHeight()/2);
    }
    
    void openProject() {
        LoadFileSelector lfs = new LoadFileSelector(this,
                prefs, KEY_PROJECT_FILESELECTOR,
                 "XML theme files", ".xml", new LoadFileSelector.Callback() {
            public void fileSelected(File file) {
                openProject(file);
            }
            public void canceled() {
            }
        });
        lfs.openPopup();
    }

    void startDemoMode() {
        closeProject();
        try {
            model = new ThemeTreeModel(messageLog, Main.class.getResource("gui.xml"));
            projectDir = null;
            editorArea.setModel(model);
            btnSaveProject.setEnabled(false);
            editorArea.setDemoMode(true);
            messageLog.add(new MessageLog.Entry(CAT_PROJECT, "Demo mode started", null, null));
        } catch(IOException ex) {
            messageLog.add(new MessageLog.Entry(CAT_PROJECT_ERROR, "Could not start demo mode", null, ex));
        }
    }

    public boolean isCloseRequested() {
        return closeRequested;
    }

    public void closeProject() {
        model = null;
        projectDir = null;
        editorArea.setModel(null);
        editorArea.setDemoMode(false);
    }
    
    public void newProject(NewProjectSettings settings) {
        try {
            File projectFile = settings.createProject();
            messageLog.add(new MessageLog.Entry(CAT_PROJECT, "New project created", projectFile.toString(), null));
            openProject(projectFile);
        } catch(IOException ex) {
            messageLog.add(new MessageLog.Entry(CAT_PROJECT_ERROR, "Could not create project", settings.toString(), ex));
        }
    }

    public void openProject(File file) {
        closeProject();
        file = file.getAbsoluteFile();
        try {
            model = new ThemeTreeModel(messageLog, file.toURI().toURL());
            projectDir = file.getParentFile();
            editorArea.setModel(model);
            btnSaveProject.setEnabled(true);
            editorArea.setDemoMode(false);
            recentProjectsModel.addEntry(file.toString());
            popuplateRecentProjectsMenu();
            if(model.getRootThemeFile().isHadErrors()) {
                messageLog.add(new MessageLog.Entry(CAT_PROJECT_WARNING,
                        "Project loaded with errors", file.toString(), null));
            } else {
                messageLog.add(new MessageLog.Entry(CAT_PROJECT, "Project loaded", file.toString(), null));
            }
        } catch(FileNotFoundException ex) {
            removeFromRecentProjectsList(file);
            messageLog.add(new MessageLog.Entry(CAT_PROJECT_ERROR, "Could not load project", file.toString(), ex));
        } catch(IOException ex) {
            messageLog.add(new MessageLog.Entry(CAT_PROJECT_ERROR, "Could not load project", file.toString(), ex));
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
                    messageLog.add(new MessageLog.Entry(CAT_PROJECT_ERROR, "Could not delete old backup file for " + relative, oldFile.toString(), null));
                    return;
                }
                if(!file.renameTo(oldFile)) {
                    messageLog.add(new MessageLog.Entry(CAT_PROJECT_ERROR, "Could not backup existing file " + relative,
                            file.toString() + "\nBackup file:\n" + oldFile.toString(), null));
                    return;
                }
                if(!tmpFile.renameTo(file)) {
                    messageLog.add(new MessageLog.Entry(CAT_PROJECT_ERROR, "Could not replace file " + relative,
                            file.toString() + "\nNew file:\n" + tmpFile.toString(), null));
                    return;
                }

                messageLog.add(new MessageLog.Entry(CAT_PROJECT, "Wrote " + relative, file.toString(), null));
                themeFile.setModified(false);
            } catch(URISyntaxException ex) {
                messageLog.add(new MessageLog.Entry(CAT_PROJECT_ERROR, "Could not determine file location for: " + themeFile.getURL(), null, null));
            }
        }
    }

    public void reloadTestWidget() {
        editorArea.reloadTestWidget();
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

    void openAboutDialog() {
        StringBuilder sb = new StringBuilder();
        sb.append("<img id=\"logo\" src=\"twl-logo\" alt=\"TWL Logo\"/><br/><br/>" +
                "<p>TWL Theme Editor (c) 2010 Matthias Mann</p><br/>" +
                "<table id=\"sysinfo\" style=\"width:100%\">\n");

        // Java version
        sb.append("<tr><td class=\"parameter\">Java</td><td class=\"value\">");
        appendHTMLEscape(sb, System.getProperty("java.version")).append(" (");
        appendHTMLEscape(sb, System.getProperty("java.vendor")).append(")</td></tr>\n");

        // Platform name
        sb.append("<tr><td class=\"parameter\">Platform</td><td class=\"value\">");
        appendHTMLEscape(sb, LWJGLUtil.getPlatformName()).append("</td></tr>\n");

        // OS version
        sb.append("<tr><td class=\"parameter\">OS</td><td class=\"value\">");
        appendHTMLEscape(sb, System.getProperty("os.name")).append("  ");
        appendHTMLEscape(sb, System.getProperty("os.arch")).append("  Version ");
        appendHTMLEscape(sb, System.getProperty("os.version")).append("</td></tr>\n");

        // LWJGL version
        sb.append("<tr><td class=\"parameter\"><a href=\"http://www.lwjgl.org\">LWJGL</a></td><td class=\"value\">");
        appendHTMLEscape(sb, Sys.getVersion()).append("</td></tr>\n");

        // OpenGL version
        sb.append("<tr><td class=\"parameter\">OpenGL</td><td class=\"value\">");
        appendHTMLEscape(sb, GL11.glGetString(GL11.GL_VERSION)).append("  ");
        appendHTMLEscape(sb, GL11.glGetString(GL11.GL_VENDOR)).append("</td></tr>\n");

        // java.library.path
        sb.append("<tr><td class=\"parameter\">java.library.path</td><td class=\"value\">");
        appendHTMLEscape(sb, System.getProperty("java.library.path")).append("</td></tr>\n");

        // jna.library.path
        sb.append("<tr><td class=\"parameter\">jna.library.path</td><td class=\"value\">");
        appendHTMLEscape(sb, System.getProperty("jna.library.path")).append("</td></tr>\n");

        // org.lwjgl.librarypath
        sb.append("<tr><td class=\"parameter\">org.lwjgl.librarypath</td><td class=\"value\">");
        appendHTMLEscape(sb, System.getProperty("org.lwjgl.librarypath")).append("</td></tr>\n");

        String webStartVerison = System.getProperty("javawebstart.version");
        if(webStartVerison != null) {
            // Webstart version
            sb.append("<tr><td class=\"parameter\">Webstart</td><td class=\"value\">");
            appendHTMLEscape(sb, webStartVerison).append("</td></tr>\n");
        }

        sb.append("<tr><td class=\"parameter\">Icons</td><td class=\"value\"><a href=\"http://www.famfamfam.com/lab/icons/silk/\">www.famfamfam.com</a></td></tr>");

        sb.append("</table><a href=\"javascript:copyToClipboard()\">Copy above report to clipboard</a>");

        StyleSheet styleSheet = new StyleSheet();
        try {
            styleSheet.parse(MainUI.class.getResource("about.css"));
        } catch(IOException ex) {
            Logger.getLogger(MainUI.class.getName()).log(Level.SEVERE, "Can't load style sheet", ex);
        }

        final String aboutHTML = sb.toString();
        HTMLTextAreaModel areaModel = new HTMLTextAreaModel(aboutHTML);
        TextArea textArea = new TextArea(areaModel);
        textArea.setStyleClassResolver(styleSheet);
        textArea.addCallback(new TextArea.Callback() {
            public void handleLinkClicked(String href) {
                if(href.startsWith("javascript:")) {
                    String key = href.substring(11);
                    if("copyToClipboard()".equals(key)) {
                        Clipboard.setClipboard(aboutHTML);
                    }
                } else {
                    Sys.openURL(href);
                }
            }
        });

        SimpleDialog dialog = new SimpleDialog();
        dialog.setMessage(textArea);
        dialog.setTheme("aboutDialog");
        dialog.showDialog(this);
    }

    static final String[] HTML_ESCAPES = {"&amp;", "&lt;", "&gt;"};
    private static StringBuilder appendHTMLEscape(StringBuilder sb, String str) {
        if(str == null) {
            return sb.append(str);
        }
        int start = 0;
        for(int pos=0 ; pos<str.length() ; pos++) {
            char ch = str.charAt(pos);
            int idx = "&<>".indexOf(ch);
            if(idx >= 0) {
                sb.append(str, start, pos).append(HTML_ESCAPES[idx]);
                start = pos + 1;
            }
        }
        return sb.append(str, start, str.length());
    }

    public static class ExtFilter implements FileFilter {
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
