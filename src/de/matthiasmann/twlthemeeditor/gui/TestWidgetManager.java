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
import de.matthiasmann.twl.ColorSelector;
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.MenuCheckbox;
import de.matthiasmann.twl.ProgressBar;
import de.matthiasmann.twl.Scrollbar;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterFloat;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.ColorSpaceHSL;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twl.utils.XMLParser;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.PreviewWidgets;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestComboBox;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestFormular;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestFrameWithWidgets;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestLabel;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestListBox;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestScrollPane;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestScrollbar;
import de.matthiasmann.twlthemeeditor.util.SolidFileClassLoader;
import de.matthiasmann.twlthemeeditor.util.SolidFileInspector;
import de.matthiasmann.twlthemeeditor.util.SolidFileWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author Matthias Mann
 */
public class TestWidgetManager {

    public interface Callback {
        public void testWidgetChanged();
        public void newWidgetsLoaded();
    }
    
    private static final MessageLog.Category CAT_ERROR   = new MessageLog.Category("Test widgets", MessageLog.CombineMode.NONE, DecoratedText.ERROR);
    private static final MessageLog.Category CAT_WARNING = new MessageLog.Category("Test widgets", MessageLog.CombineMode.NONE, DecoratedText.WARNING);
    private static final MessageLog.Category CAT_INFO    = new MessageLog.Category("Test widgets", MessageLog.CombineMode.NONE, 0);

    private final MessageLog messageLog;
    private final ArrayList<TestWidgetFactory> builtinWidgets;
    private final HashMap<ArrayList<String>, LoadedUserWidgets> userWidgets;

    private Callback callback;
    private TestWidgetFactory demoModeTestWidgetFactory;
    private TestWidgetFactory currentTestWidgetFactory;
    private LoadedUserWidgets currentUserWidgets;
    private ProgressDialog progressDialog;

    public TestWidgetManager(MessageLog messageLog) {
        this.messageLog = messageLog;
        this.builtinWidgets = new ArrayList<TestWidgetFactory>();
        this.userWidgets = new LinkedHashMap<ArrayList<String>, LoadedUserWidgets>();

        builtinWidgets.add(new TestWidgetFactory(Widget.class, "Widget"));
        builtinWidgets.add(new TestWidgetFactory(TestLabel.class, "Label"));
        builtinWidgets.add(new TestWidgetFactory(Button.class, "Button", "Press me !"));
        builtinWidgets.add(new TestWidgetFactory(ToggleButton.class, "ToggleButton", "Toggle me !"));
        builtinWidgets.add(new TestWidgetFactory(EditField.class, "EditField"));
        builtinWidgets.add(new TestWidgetFactory(ValueAdjusterFloat.class, "ValueAdjusterFloat"));
        builtinWidgets.add(new TestWidgetFactory(TestScrollbar.class, "HScrollbar", Scrollbar.Orientation.HORIZONTAL));
        builtinWidgets.add(new TestWidgetFactory(TestScrollbar.class, "VScrollbar", Scrollbar.Orientation.VERTICAL));
        builtinWidgets.add(new TestWidgetFactory(ProgressBar.class, "ProgressBar"));
        builtinWidgets.add(new TestWidgetFactory(TestListBox.class, "ListBox"));
        builtinWidgets.add(new TestWidgetFactory(TestComboBox.class, "ComboBox"));
        builtinWidgets.add(new TestWidgetFactory(PreviewWidgets.class, "Widgets"));
        builtinWidgets.add(new TestWidgetFactory(TestFormular.class, "Example formular"));
        builtinWidgets.add(new TestWidgetFactory(TestFrameWithWidgets.class, "Frame with Widgets"));
        builtinWidgets.add(new TestWidgetFactory(TestScrollPane.class, "TextArea"));
        builtinWidgets.add(new TestWidgetFactory(ColorSelector.class, "ColorSelector", new ColorSpaceHSL()));

        currentTestWidgetFactory = builtinWidgets.get(0);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setProgressDialog(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
    }

    public TestWidgetFactory getCurrentTestWidgetFactory() {
        return currentTestWidgetFactory;
    }

    public void setDemoMode(boolean demoMode) {
        if(demoMode) {
            demoModeTestWidgetFactory = new TestWidgetFactory(MainUI.class, "Theme Editor");
            setCurrentTestWidgetFactory(demoModeTestWidgetFactory, null);
        } else {
            if(currentTestWidgetFactory == demoModeTestWidgetFactory) {
                setCurrentTestWidgetFactory(null, null);
            }
            demoModeTestWidgetFactory = null;
        }
        callback.newWidgetsLoaded();
    }

    public void clearCache() {
        clearCache(builtinWidgets);
        for(LoadedUserWidgets luw : userWidgets.values()) {
            clearCache(luw.factories);
        }
    }

    public void reloadCurrentWidget() {
        final LoadedUserWidgets luw = currentUserWidgets;
        if(luw != null) {
            final String currentClassName = currentTestWidgetFactory.getClazz().getName();
            loadUserWidgets(luw.toScan, luw.dependencies, new Runnable() {
                public void run() {
                    callback.newWidgetsLoaded();
                    LoadedUserWidgets newLuw = userWidgets.get(luw.key);
                    TestWidgetFactory newFactory;
                    if(newLuw == null) {
                        newFactory = null;
                    } else {
                        newFactory = newLuw.findFactory(currentClassName);
                    }
                    if(newFactory != null) {
                        setCurrentTestWidgetFactory(newFactory, newLuw);
                    } else {
                        setCurrentTestWidgetFactory(null, null);
                    }
                }
            });
        } else if(currentTestWidgetFactory != null) {
            currentTestWidgetFactory.clearCache();
            callback.testWidgetChanged();
        }
    }

    private void clearCache(ArrayList<TestWidgetFactory> factories) {
        for(TestWidgetFactory factory : factories) {
            factory.clearCache();
        }
    }
    
    public boolean loadUserWidgets(File file) {
        try {
            URI base = file.getParentFile().toURI();
            XMLParser xmlp = new XMLParser(file.toURI().toURL());
            xmlp.nextTag();
            xmlp.require(XmlPullParser.START_TAG, null, "classpath");
            xmlp.nextTag();
            ArrayList<URI> toScan = readEntries(xmlp, base, "entriesToScan");
            xmlp.nextTag();
            ArrayList<URI> dependencies = readEntries(xmlp, base, "entriesDependencies");
            xmlp.nextTag();
            xmlp.require(XmlPullParser.END_TAG, null, "classpath");
            if(toScan.size() > 0) {
                loadUserWidgets(toScan, dependencies, new Runnable() {
                    public void run() {
                        callback.newWidgetsLoaded();
                    }
                });
                return true;
            }
        } catch(Exception ex) {
            messageLog.add(new MessageLog.Entry(CAT_ERROR, "Can't load class path", null, ex));
        }
        return false;
    }

    private ArrayList<URI> readEntries(XMLParser xmlp, URI base, String tag) throws XmlPullParserException, IOException {
        ArrayList<URI> result = new ArrayList<URI>();
        xmlp.require(XmlPullParser.START_TAG, null, tag);
        xmlp.nextTag();
        while(!xmlp.isEndTag()) {
            xmlp.require(XmlPullParser.START_TAG, null, "entry");
            result.add(base.resolve(xmlp.nextText()));
            xmlp.require(XmlPullParser.END_TAG, null, "entry");
            xmlp.nextTag();
        }
        xmlp.require(XmlPullParser.END_TAG, null, tag);
        return result;
    }

    public void updateMenu(Menu menu) {
        addMenu(menu, "Built-in widgets", builtinWidgets, null);
        if(demoModeTestWidgetFactory != null) {
            addMenu(menu, "Demo mode", Collections.singletonList(demoModeTestWidgetFactory), null);
        }
        for(Map.Entry<ArrayList<String>,LoadedUserWidgets> entry : userWidgets.entrySet()) {
            String name = entry.getKey().get(0);
            if(!name.endsWith("/")) {
                name = name.substring(name.lastIndexOf('/') + 1);
            }
            LoadedUserWidgets luw = entry.getValue();
            addMenu(menu, name, luw.factories, luw);
        }
    }

    void setCurrentTestWidgetFactory(TestWidgetFactory factory, LoadedUserWidgets userWidgets) {
        currentTestWidgetFactory = factory;
        currentUserWidgets = userWidgets;
        callback.testWidgetChanged();
    }

    private void addMenu(Menu parent, String name, List<TestWidgetFactory> factories, LoadedUserWidgets userWidgets) {
        Menu menu = new Menu(name);
        for(TestWidgetFactory f : factories) {
            addMenuItem(menu, f, userWidgets);
        }
        parent.add(menu);
    }

    private void addMenuItem(Menu menu, TestWidgetFactory f, LoadedUserWidgets userWidgets) {
        menu.add(new MenuCheckbox(f.getName(), new SelectedWidgetModel(f, userWidgets)).setTheme("radiobtn"));
    }

    private void loadUserWidgets(ArrayList<URI> toScan, ArrayList<URI> dependencies, final Runnable cb) {
        progressDialog.setTitle("Scanning class files");
        progressDialog.setMessage("");
        progressDialog.setIndeterminate("");
        progressDialog.openPopupCentered();
        
        final Inspector inspector = new Inspector(toScan, dependencies,
                progressDialog, getClass().getClassLoader());

        GUI.AsyncCompletionListener<LoadedUserWidgets> acl =
                new GUI.AsyncCompletionListener<LoadedUserWidgets>() {
            public void completed(LoadedUserWidgets luw) {
                progressDialog.closePopup();
                if(luw != null) {
                    LoadedUserWidgets old = userWidgets.put(inspector.key, luw);
                    if(old != null) {
                        old.classLoader.close();
                    }

                    messageLog.add(new MessageLog.Entry(
                            inspector.foundCriticalClasses ? CAT_WARNING : CAT_INFO,
                            "Loaded " + luw.factories.size() + " user widgets",
                            inspector.infoMsg.toString(), null));

                    if(cb != null) {
                        cb.run();
                    }
                } else {
                    messageLog.add(new MessageLog.Entry(CAT_WARNING,
                            "Could not loaded any user widget",
                            inspector.infoMsg.toString(), null));
                }
            }

            public void failed(Exception ex) {
                progressDialog.closePopup();
                messageLog.add(new MessageLog.Entry(CAT_ERROR,
                        "Can't load user classes", null, ex));
            }
        };

        if(progressDialog.isOpen()) {
            progressDialog.getGUI().invokeAsync(inspector, acl);
        } else {
            try {
                acl.completed(inspector.call());
            } catch(Exception ex) {
                acl.failed(ex);
            }
        }
    }

    class SelectedWidgetModel extends HasCallback implements BooleanModel {
        private final TestWidgetFactory factory;
        private final LoadedUserWidgets userWidgets;

        public SelectedWidgetModel(TestWidgetFactory factory, LoadedUserWidgets userWidgets) {
            this.factory = factory;
            this.userWidgets = userWidgets;
        }

        public boolean getValue() {
            return currentTestWidgetFactory == factory;
        }

        public void setValue(boolean value) {
            if(value && currentTestWidgetFactory != factory) {
                setCurrentTestWidgetFactory(factory, userWidgets);
                doCallback();
            }
        }
    }

    static class LoadedUserWidgets {
        final ArrayList<URI> toScan;
        final ArrayList<URI> dependencies;
        final ArrayList<String> key;
        final ArrayList<TestWidgetFactory> factories;
        final SolidFileClassLoader classLoader;

        public LoadedUserWidgets(ArrayList<URI> toScan, ArrayList<URI> dependencies, ArrayList<String> key, ArrayList<TestWidgetFactory> factories, SolidFileClassLoader classLoader) {
            this.toScan = toScan;
            this.dependencies = dependencies;
            this.key = key;
            this.factories = factories;
            this.classLoader = classLoader;
        }

        TestWidgetFactory findFactory(String className) {
            for(TestWidgetFactory factory : factories) {
                if(className.equals(factory.getClazz().getName())) {
                    return factory;
                }
            }
            return null;
        }
    }

    static enum FailReason {
        NOT_PUBLIC(" : not public"),
        NO_DEFAULT_CONSTRUCTOR(" : no default constructor"),
        DEFAULT_CONSTRUCTOR_NOT_PUBLIC(" : non public default constructor"),
        ABSTRACT(" : is abstract");
        
        final String msg;
        private FailReason(String msg) {
            this.msg = msg;
        }
    }
    
    static class Inspector implements Callable<LoadedUserWidgets>, SolidFileInspector, ClassVisitor {
        final ArrayList<URI> toScan;
        final ArrayList<URI> dependencies;
        final ProgressDialog progressDialog;
        final ClassLoader parentClassLoader;

        final ArrayList<String> key;
        final HashMap<String, Object> superClassMap;
        final HashSet<String> subClassSet;
        final TreeMap<String, FailReason> failedMap;
        final ArrayList<String> candidates;
        final StringBuilder infoMsg;

        String curClassName;
        boolean isPublic;
        boolean isAbstract;
        boolean foundDefaultConstructor;
        boolean isCandidate;
        boolean isInnerClass;
        boolean foundLWJGLclasses;
        boolean foundTWLclasses;
        boolean foundXPPclasses;
        boolean foundCriticalClasses;
        byte[] buffer;

        public Inspector(ArrayList<URI> toScan, ArrayList<URI> dependencies, ProgressDialog progressDialog, ClassLoader parentClassLoader) {
            this.toScan = toScan;
            this.dependencies = dependencies;
            this.progressDialog = progressDialog;
            this.parentClassLoader = parentClassLoader;

            this.key = new ArrayList<String>();
            this.superClassMap = new HashMap<String, Object>();
            this.subClassSet = new HashSet<String>();
            this.failedMap = new TreeMap<String, FailReason>();
            this.candidates = new ArrayList<String>();
            this.buffer = new byte[4096];
            this.infoMsg = new StringBuilder();
        }

        public LoadedUserWidgets call() throws Exception {
            infoMsg.append("Loaded from the following class path:\n");

            File roots[] = new File[toScan.size() + dependencies.size()];
            for(int i=0,n=toScan.size() ; i<n ; i++) {
                URI fileUri = toScan.get(i);
                File file = new File(fileUri);
                roots[i] = file;
                String path = file.toString();
                key.add(path);
                infoMsg.append(path).append("\n");
            }
            if(!dependencies.isEmpty()) {
                infoMsg.append("Additional class path:\n");
                for(int i=0,n=dependencies.size() ; i<n ; i++) {
                    URI fileUri = dependencies.get(i);
                    File file = new File(fileUri);
                    roots[i+toScan.size()] = file;
                    infoMsg.append(file.getPath()).append("\n");
                }
            }

            ArrayList<TestWidgetFactory> testWidgetFactories = new ArrayList<TestWidgetFactory>();
            SolidFileClassLoader classLoader = SolidFileClassLoader.create(parentClassLoader, this, roots);
            StringBuilder warnings = new StringBuilder();

            if(foundLWJGLclasses) {
                infoMsg.append("\nCRITICAL: LWJGL classes (org.lwjgl.*) found on class path");
                foundCriticalClasses = true;
            }
            if(foundTWLclasses) {
                infoMsg.append("\nCRITICAL: TWL classes (de.matthiasmann.twl.*) found on class path");
                foundCriticalClasses = true;
            }
            if(foundXPPclasses) {
                infoMsg.append("\nCRITICAL: XPP classes (org.xmlpull.v1.*) found on class path");
                foundCriticalClasses = true;
            }
            if(foundCriticalClasses) {
                infoMsg.append("\nIncluding classes from these package in the user widget class path"
                        + " may cause class loading issues and random failures\n");
            }

            for(String candidate : candidates) {
                if(checkSuperClass(candidate)) {
                    String candidateClassName = candidate.replace('/', '.');
                    try {
                        Class<?> clz = Class.forName(candidateClassName, false, classLoader);
                        @SuppressWarnings("unchecked")
                        Class<? extends Widget> widgetClazz = (Class<? extends Widget>)clz;
                        testWidgetFactories.add(new TestWidgetFactory(
                                widgetClazz, widgetClazz.getSimpleName()));
                    } catch(Exception ex) {
                        warnings.append(candidateClassName).append(" : can't load class: ").append(ex.getMessage()).append("\n");
                    }
                }
            }
            failedMap.keySet().removeAll(subClassSet);
            for(Map.Entry<String, FailReason> e : failedMap.entrySet()) {
                if(checkSuperClass(e.getKey())) {
                    warnings.append(e.getKey().replace('/', '.')).append(e.getValue().msg).append("\n");
                }
            }

            LoadedUserWidgets luw;

            if(!testWidgetFactories.isEmpty()) {
                luw = new LoadedUserWidgets(toScan, dependencies, key,
                        testWidgetFactories, classLoader);

                infoMsg.append("\nThe following classes have been loaded:\n");
                for(TestWidgetFactory twf : testWidgetFactories) {
                    infoMsg.append(twf.getClazz().getName()).append("\n");
                }
            } else {
                luw = null;
                classLoader.close();
            }

            if(warnings.length() > 0) {
                infoMsg.append("\nThe following class could not be loaded:\n")
                        .append(warnings);
            }

            return luw;
        }

        public void processingRoot(final File root) {
            if(progressDialog != null) {
                progressDialog.setMessage(root.toString());
            }
        }

        public boolean shouldInspectFile(String name) {
            if(!name.endsWith(".class")) {
                return false;
            }
            if(name.startsWith("de/matthiasmann/twl/")) {
                foundTWLclasses = true;
                return false;
            }
            if(name.startsWith("org/lwjgl/")) {
                foundLWJGLclasses = true;
                return false;
            }
            if(name.startsWith("org/xmlpull/v1/")) {
                foundXPPclasses = true;
                return false;
            }
            return true;
        }

        public void inspectFile(SolidFileWriter writer, String name, InputStream is) throws IOException {
            int size = readFile(is);
            writer.addEntry(name, buffer, 0, size);
            ClassReader cr = new ClassReader(buffer, 0, size);
            cr.accept(this, ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        }

        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            superClassMap.put(name, superName);
            subClassSet.add(superName);
            curClassName = name;
            isPublic = (access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC;
            isAbstract = (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
            isCandidate = false;
            isInnerClass = false;
            foundDefaultConstructor = false;
        }

        public void visitSource(String source, String debug) {
        }

        public void visitOuterClass(String owner, String name, String desc) {
            isInnerClass = true;
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return null;
        }

        public void visitAttribute(Attribute attr) {
        }

        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if(!isInnerClass && curClassName.equals(name)) {
                isInnerClass = true;
            }
        }

        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            return null;
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if(isPublic && !isAbstract && "<init>".equals(name)) {
                if("()V".equals(desc)) {
                    foundDefaultConstructor = true;
                    if((access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC) {
                        candidates.add(curClassName);
                        isCandidate = true;
                    }
                }
            }
            return null;
        }

        public void visitEnd() {
            if(!isCandidate && !isInnerClass) {
                FailReason reason;
                if(!isPublic) {
                    reason = FailReason.NOT_PUBLIC;
                } else if(isAbstract) {
                    reason = FailReason.ABSTRACT;
                } else if(foundDefaultConstructor) {
                    reason = FailReason.DEFAULT_CONSTRUCTOR_NOT_PUBLIC;
                } else {
                    reason = FailReason.NO_DEFAULT_CONSTRUCTOR;
                }
                failedMap.put(curClassName, reason);
            }
            curClassName = null;
        }

        private int readFile(InputStream is) throws IOException {
            int pos = 0;
            int read;
            while((read=is.read(buffer, pos, buffer.length - pos)) > 0) {
                pos += read;
                if(pos == buffer.length) {
                    buffer = Arrays.copyOf(buffer, pos*2);
                }
            }
            return pos;
        }

        private boolean checkSuperClass(String name) {
            Object o = superClassMap.get(name);
            if(o instanceof Boolean) {
                return (Boolean)o;
            }
            boolean result;
            if(o == null) {
                result = checkTWLWidget(name);
            } else {
                result = checkSuperClass((String)o);
            }
            superClassMap.put(name, result);
            return result;
        }

        private boolean checkTWLWidget(String name) {
            if(name.startsWith("de/matthiasmann/twl/")) {
                name = name.replace('/', '.');
                try {
                    return Widget.class.isAssignableFrom(Class.forName(name, false, Widget.class.getClassLoader()));
                } catch(ClassNotFoundException ex) {
                    return false;
                }
            }
            return false;
        }
    }
}
