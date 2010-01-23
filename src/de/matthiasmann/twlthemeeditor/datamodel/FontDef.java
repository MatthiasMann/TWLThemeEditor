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
package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.TestEnv;
import de.matthiasmann.twlthemeeditor.VirtualFile;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CloneNodeOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewSimple;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class FontDef extends ThemeTreeNode implements HasProperties {

    protected final ArrayList<VirtualFile> virtualFontFiles;
    protected final NameProperty nameProperty;
    protected final Property<String> fileNameProperty;

    public FontDef(ThemeFile themeFile, TreeTableNode parent, Element element) throws IOException {
        super(themeFile, parent, element);
        this.virtualFontFiles = new ArrayList<VirtualFile>();

        nameProperty = new NameProperty(new AttributeProperty(element, "name"), getThemeTreeModel(), Kind.FONT) {
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
                try {
                    registerFontFiles();
                } catch(IOException ignore) {
                }
            }
        });
        addProperty(fileNameProperty);
        
        addProperty(new ColorProperty(new AttributeProperty(element, "color", "Font color", true)));

        registerFontFiles();
    }

    @Override
    public String getName() {
        return nameProperty.getPropertyValue();
    }

    public Kind getKind() {
        return Kind.FONT;
    }

    public URL getFontFileURL() throws MalformedURLException {
        String value = fileNameProperty.getPropertyValue();
        return (value != null) ? themeFile.getURL(value) : null;
    }

    public void addChildren() throws IOException {
        addChildren(themeFile, element, new DomWrapper() {
            public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                if("fontParam".equals(element.getName())) {
                    return new FontParam(themeFile, parent, element);
                }
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void addToXPP(DomXPPParser xpp) {
        Utils.addToXPP(xpp, element.getName(), this, element.getAttributes());
    }

    @Override
    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> operations = super.getOperations();
        operations.add(new CloneNodeOperation(element, this));
        operations.add(new CreateNewSimple(this, element, "fontParam", "if", "hover"));
        return operations;
    }

    protected void registerFontFiles() throws IOException {
        TestEnv env = getThemeFile().getEnv();
        env.unregisterFiles(virtualFontFiles);
        virtualFontFiles.clear();

        URL fontFileURL = getFontFileURL();
        if(fontFileURL != null) {
            virtualFontFiles.add(env.registerFile(fontFileURL));

            Document fontFileDOM = Utils.loadDocument(fontFileURL);
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
        }
    }
}