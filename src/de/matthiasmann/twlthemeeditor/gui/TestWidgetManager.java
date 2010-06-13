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
import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.MenuCheckbox;
import de.matthiasmann.twl.Scrollbar;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.model.HasCallback;
import de.matthiasmann.twlthemeeditor.datamodel.DecoratedText;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.PreviewWidgets;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestComboBox;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestFrameWithWidgets;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestLabel;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestScrollPane;
import de.matthiasmann.twlthemeeditor.gui.testwidgets.TestScrollbar;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *
 * @author Matthias Mann
 */
public class TestWidgetManager {

    private static final MessageLog.Category CAT_ERROR   = new MessageLog.Category("Test widgets", MessageLog.CombineMode.NONE, DecoratedText.ERROR);
    private static final MessageLog.Category CAT_WARNING = new MessageLog.Category("Test widgets", MessageLog.CombineMode.NONE, DecoratedText.WARNING);
    private static final MessageLog.Category CAT_INFO    = new MessageLog.Category("Test widgets", MessageLog.CombineMode.NONE, 0);

    private final MessageLog messageLog;
    private final ArrayList<TestWidgetFactory> builtinWidgets;
    private final HashMap<ArrayList<String>, ArrayList<TestWidgetFactory>> userWidgets;

    private Runnable callback;
    private TestWidgetFactory currentTestWidgetFactory;

    public TestWidgetManager(MessageLog messageLog) {
        this.messageLog = messageLog;
        this.builtinWidgets = new ArrayList<TestWidgetFactory>();
        this.userWidgets = new LinkedHashMap<ArrayList<String>, ArrayList<TestWidgetFactory>>();

        builtinWidgets.add(new TestWidgetFactory(Widget.class, "Widget"));
        builtinWidgets.add(new TestWidgetFactory(TestLabel.class, "Label"));
        builtinWidgets.add(new TestWidgetFactory(Button.class, "Button", "Press me !"));
        builtinWidgets.add(new TestWidgetFactory(ToggleButton.class, "ToggleButton", "Toggle me !"));
        builtinWidgets.add(new TestWidgetFactory(EditField.class, "EditField"));
        builtinWidgets.add(new TestWidgetFactory(TestScrollbar.class, "HScrollbar", Scrollbar.Orientation.HORIZONTAL));
        builtinWidgets.add(new TestWidgetFactory(TestScrollbar.class, "VScrollbar", Scrollbar.Orientation.VERTICAL));
        builtinWidgets.add(new TestWidgetFactory(TestComboBox.class, "ComboBox"));
        builtinWidgets.add(new TestWidgetFactory(PreviewWidgets.class, "Widgets"));
        builtinWidgets.add(new TestWidgetFactory(TestFrameWithWidgets.class, "Frame with Widgets"));
        builtinWidgets.add(new TestWidgetFactory(TestScrollPane.class, "TextArea"));

        currentTestWidgetFactory = builtinWidgets.get(0);
    }

    public Runnable getCallback() {
        return callback;
    }

    public void setCallback(Runnable callback) {
        this.callback = callback;
    }

    public TestWidgetFactory getCurrentTestWidgetFactory() {
        return currentTestWidgetFactory;
    }

    public void clearCache() {
        clearCache(builtinWidgets);
        for(ArrayList<TestWidgetFactory> factories : userWidgets.values()) {
            clearCache(factories);
        }
    }

    private void clearCache(ArrayList<TestWidgetFactory> factories) {
        for(TestWidgetFactory factory : factories) {
            factory.clearCache();
        }
    }
    
    public boolean loadUserWidgets(Object[] files) {
        ArrayList<File> jarFiles = new ArrayList<File>(files.length);
        for(Object o : files) {
            if(o instanceof File) {
                jarFiles.add((File)o);
            }
        }
        if(jarFiles.size() > 0) {
            return loadUserWidgets(jarFiles);
        }
        return false;
    }

    public void updateMenu(Menu menu) {
        addMenu(menu, "built-in widgets", builtinWidgets);
        for(Map.Entry<ArrayList<String>, ArrayList<TestWidgetFactory>> entry : userWidgets.entrySet()) {
            String name = entry.getKey().get(0);
            name = name.substring(name.lastIndexOf('/') + 1);
            addMenu(menu, name, entry.getValue());
        }
    }

    private void addMenu(Menu parent, String name, ArrayList<TestWidgetFactory> factories) {
        Menu menu = new Menu(name);
        for(TestWidgetFactory f : factories) {
            menu.add(new MenuCheckbox(f.getName(), new SelectedWidgetModel(f)).setTheme("radiobtn"));
        }
        parent.add(menu);
    }

    private boolean loadUserWidgets(ArrayList<File> files) {
        try {
            StringBuilder infoMsg = new StringBuilder();
            infoMsg.append("Loaded from the following JAr files:\n");
            
            URL[] urls = new URL[files.size()];
            ArrayList<String> key = new ArrayList<String>();
            for(int i=0,n=urls.length ; i<n ; i++) {
                final File file = files.get(i);
                urls[i] = file.toURI().toURL();
                key.add(urls[i].toString());
                infoMsg.append(file.toString()).append("\n");
            }

            ArrayList<TestWidgetFactory> testWidgetFactories = new ArrayList<TestWidgetFactory>();
            URLClassLoader classLoader = new URLClassLoader(urls);
            for(File f : files) {
                scanJARFile(classLoader, f, testWidgetFactories);
            }

            if(!testWidgetFactories.isEmpty()) {
                userWidgets.put(key, testWidgetFactories);

                infoMsg.append("\nThe following classes have been loaded:\n");
                for(TestWidgetFactory twf : testWidgetFactories) {
                    infoMsg.append(twf.getClazz().getName()).append("\n");
                }
            }

            messageLog.add(new MessageLog.Entry(
                    testWidgetFactories.isEmpty() ? CAT_WARNING : CAT_INFO,
                    "Loaded " + testWidgetFactories.size() + " user widgets", infoMsg.toString(), null));
            
            return true;
        } catch (Throwable ex) {
            messageLog.add(new MessageLog.Entry(CAT_ERROR, "Can't load user classes", null, ex));
            return false;
        }
    }

    private void scanJARFile(ClassLoader classLoader, File file, ArrayList<TestWidgetFactory> testWidgetFactories) {
        try {
            JarFile jarFile = new JarFile(file);
            try {
                StringBuilder warnings = new StringBuilder();
                Enumeration<JarEntry> entries = jarFile.entries();
                while(entries.hasMoreElements()) {
                    JarEntry e = entries.nextElement();
                    String name = e.getName();
                    if(name.endsWith(".class") && !name.startsWith("de/matthiasmann/twl/")) {
                        name = name.substring(0, name.length()-6).replace('/', '.');
                        testClass(classLoader, name, testWidgetFactories, warnings);
                    }
                }
                if(warnings.length() > 0) {
                    messageLog.add(new MessageLog.Entry(CAT_WARNING, "Warnings while scanning JAR file: " + file.getName(), warnings.toString(), null));
                }
            } finally {
                jarFile.close();
            }
        } catch (IOException ex) {
            messageLog.add(new MessageLog.Entry(CAT_ERROR, "Can't scan JAR file: " + file.getName(), file.toString(), ex));
        }
    }

    private void testClass(ClassLoader classLoader, String name, ArrayList<TestWidgetFactory> testWidgetFactories, StringBuilder warnings) {
        try {
            Class<?> clazz = Class.forName(name, false, classLoader);
            if(Widget.class.isAssignableFrom(clazz) && !clazz.isMemberClass() && !clazz.isLocalClass()) {
                @SuppressWarnings("unchecked")
                Class<? extends Widget> widgetClazz = (Class<? extends Widget>)clazz;
                Constructor<?> c = clazz.getConstructor();
                if(Modifier.isPublic(c.getModifiers())) {
                    testWidgetFactories.add(new TestWidgetFactory(
                            widgetClazz, clazz.getSimpleName()));
                } else {
                    warnings.append(name).append(": default constructor is not public\n");
                }
            }
        } catch(NoSuchMethodException ignore) {
            warnings.append(name).append(": no default constructor\n");
        } catch(Throwable ex) {
            warnings.append(name).append(": can't load class: ").append(ex.getMessage()).append("\n");
        }
    }

    class SelectedWidgetModel extends HasCallback implements BooleanModel {
        private final TestWidgetFactory factory;

        public SelectedWidgetModel(TestWidgetFactory factory) {
            this.factory = factory;
        }

        public boolean getValue() {
            return currentTestWidgetFactory == factory;
        }

        public void setValue(boolean value) {
            if(value && currentTestWidgetFactory != factory) {
                currentTestWidgetFactory = factory;
                if(callback != null) {
                    callback.run();
                }
                doCallback();
            }
        }
    }
}