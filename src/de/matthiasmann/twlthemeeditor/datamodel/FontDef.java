/*
 * Copyright (c) 2008-2012, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twl.utils.ParameterStringParser;
import de.matthiasmann.twlthemeeditor.TestEnv;
import de.matthiasmann.twlthemeeditor.VirtualFile;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CloneNodeOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewSimple;
import de.matthiasmann.twlthemeeditor.dom.Document;
import de.matthiasmann.twlthemeeditor.dom.Element;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.IntegerProperty;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Matthias Mann
 */
public class FontDef extends ThemeTreeNode implements HasProperties {

    protected final ArrayList<VirtualFile> virtualFontFiles;
    protected final NameProperty nameProperty;
    protected final Property<String> fileNameProperty;
    protected final BooleanProperty defaultProperty;

    @SuppressWarnings("LeakingThisInConstructor")
    public FontDef(ThemeFile themeFile, TreeTableNode parent, Element element) throws IOException {
        super(themeFile, parent, element);
        this.virtualFontFiles = new ArrayList<VirtualFile>();

        nameProperty = new NameProperty(new AttributeProperty(element, "name"), getThemeTreeModel(), Kind.FONT, true) {
            @Override
            public void validateName(String name) throws IllegalArgumentException {
                if(name == null || name.length() == 0) {
                    throw new IllegalArgumentException("Empty name not allowed");
                }
            }
        };
        addProperty(nameProperty);

        fileNameProperty = new AttributeProperty(element, "filename", "Font file name", true);
        fileNameProperty.addValueChangedCallback(new Runnable() {
            public void run() {
                registerFontFiles();
            }
        });
        addProperty(fileNameProperty);

        defaultProperty = new BooleanProperty(new AttributeProperty(element, "default", "Default font", true), false);

        addProperty(defaultProperty);
        addCommonFontDefProperties(this, element);

        registerFontFiles();
    }

    @Override
    public String getDisplayName() {
        String name = getName();
        if(name != null) {
            if(defaultProperty.getValue()) {
                return name + " *";
            } else {
                return name;
            }
        }
        return super.getDisplayName();
    }

    @Override
    public String getName() {
        return nameProperty.getPropertyValue();
    }

    @Override
    protected String getIcon() {
        return "fontdef";
    }

    public Kind getKind() {
        return Kind.FONT;
    }

    public URL getFontFileURL() throws MalformedURLException {
        String value = fileNameProperty.getPropertyValue();
        return (value != null) ? themeFile.getURL(value) : null;
    }

    public void addChildren() throws IOException {
        addChildren(themeFile, element, new DomWrapperImpl());
    }

    @SuppressWarnings("unchecked")
    public void addToXPP(DomXPPParser xpp) {
        Utils.addToXPP(xpp, element.getName(), this, element.getAttributes());
    }

    @Override
    public boolean canPasteElement(Element element) {
        return canPasteFontDefElement(element);
    }

    public static boolean canPasteFontDefElement(Element element) {
        return "fontParam".equals(element.getName());
    }

    @Override
    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> operations = super.getOperations();
        operations.add(new CloneNodeOperation(element, this) {
            @Override
            protected void adjustClonedElement(Element clonedElement) {
                super.adjustClonedElement(clonedElement);
                clonedElement.removeAttribute("default");
            }
        });
        return operations;
    }

    @Override
    public List<CreateChildOperation> getCreateChildOperations() {
        List<CreateChildOperation> operations = super.getCreateChildOperations();
        addFontParamOperations(operations, this, element);
        return operations;
    }

    private void registerFontFiles() {
        try {
            registerFontFiles(getThemeFile().getEnv(), virtualFontFiles, getFontFileURL());
        } catch(IOException ex) {
            getRootThemeFile().logError("Could not load font", null, ex);
        }
    }

    static void addCommonFontDefProperties(ThemeTreeNode node, Element element) {
        node.addProperty(new ColorProperty(new AttributeProperty(element, "color", "Font color", true), node));
        node.addProperty(new IntegerProperty(new AttributeProperty(element, "offsetX", "Offset X", true), -100, 100));
        node.addProperty(new IntegerProperty(new AttributeProperty(element, "offsetY", "Offset Y", true), -100, 100));
        node.addProperty(new BooleanProperty(new AttributeProperty(element, "linethrough", "line through / striked", true), false));
        node.addProperty(new BooleanProperty(new AttributeProperty(element, "underline", "Underlined", true), false));
        node.addProperty(new IntegerProperty(new AttributeProperty(element, "underlineOffset", "Underline offset", true), -100, 100));
    }

    static void addFontParamOperations(List<CreateChildOperation> operations, ThemeTreeNode parent, Element element) {
        operations.add(new CreateNewSimple(parent, element, "fontParam", "if", "hover"));
    }

    static void registerFontFiles(TestEnv env, ArrayList<VirtualFile> virtualFontFiles, URL fontFileURL) throws IOException {
        env.unregisterFiles(virtualFontFiles);
        virtualFontFiles.clear();

        if(fontFileURL != null) {
            virtualFontFiles.add(env.registerFile(fontFileURL));

            InputStream is = fontFileURL.openStream();
            try {
                BufferedInputStream bis = new BufferedInputStream(is);
                bis.mark(1);
                if(bis.read() != 'i') {
                    bis.reset();
                    Document fontFileDOM = Utils.loadDocument(bis);
                    Element pages = fontFileDOM.getRootElement().getChild("pages");
                    if(pages != null) {
                        for(Object obj : pages.getChildren("page")) {
                            Element page = (Element)obj;
                            String file = page.getAttributeValue("file");
                            if(file != null) {
                                virtualFontFiles.add(env.registerFile(new URL(fontFileURL, file)));
                            }
                        }
                    }
                } else {
                    bis.reset();
                    InputStreamReader isr = new InputStreamReader(bis);
                    BufferedReader br = new BufferedReader(isr);
                    String line;
                    while((line=br.readLine()) != null) {
                        if(line.startsWith("page ")) {
                            ParameterStringParser psp = new ParameterStringParser(line, ' ', '=');
                            while(psp.next()) {
                                if("file".equals(psp.getKey())) {
                                    virtualFontFiles.add(env.registerFile(new URL(fontFileURL, psp.getValue())));
                                }
                            }
                        }
                        if(line.startsWith("char")) {
                            break;
                        }
                    }
                }
            } finally {
                is.close();
            }
        }
    }

    static class DomWrapperImpl implements DomWrapper {
        public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
            if("fontParam".equals(element.getName())) {
                return new FontParam(themeFile, parent, element);
            }
            return null;
        }
    }
}
