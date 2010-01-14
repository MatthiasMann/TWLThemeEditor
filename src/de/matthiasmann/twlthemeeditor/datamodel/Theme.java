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
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.ThemeReferenceProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Theme extends AbstractThemeTreeNode implements HasProperties {

    protected final ThemeFile themeFile;
    protected final ArrayList<Property<?>> properties;
    protected final Element element;
    protected final NameProperty nameProperty;
    protected final ThemeReferenceProperty refProperty;

    public Theme(ThemeFile themeFile, TreeTableNode parent, Element element) {
        super(parent);
        this.themeFile = themeFile;
        this.properties = new ArrayList<Property<?>>();
        this.element = element;

        this.nameProperty = new NameProperty(new AttributeProperty(element, "name")) {
            @Override
            public void validateName(String name) throws IllegalArgumentException {
                if(name == null || name.length() == 0) {
                    throw new IllegalArgumentException("empty name not allowed");
                }
                for(Theme theme : getThemeTreeModel().getThemes()) {
                    if(theme != Theme.this) {
                        String themeName = theme.getName();
                        if(name.equals(themeName)) {
                            throw new IllegalArgumentException("Name \"" + name + "\" already in use");
                        }
                    }
                }
            }

            @Override
            public void setPropertyValue(String value) throws IllegalArgumentException {
                validateName(value);
                String prevName = getPropertyValue();
                if(!prevName.equals(value)) {
                    for(Theme theme : getThemeTreeModel().getThemes()) {
                        theme.handleThemeRenamed(prevName, value);
                    }
                    super.setPropertyValue(value);
                }
            }
        };
        addProperty(nameProperty);

        if(!element.getParentElement().isRootElement()) {
            addProperty(new BooleanProperty(new AttributeProperty(element, "merge", "Merge", true), false));
        }

        refProperty = new ThemeReferenceProperty(new AttributeProperty(element, "ref", "Base theme reference", true), this);
        addProperty(refProperty);
    }

    protected final void addProperty(Property<?> property) {
        themeFile.registerProperty(property);
        properties.add(property);
    }

    public String getName() {
        if(nameProperty == null) {
            return null;
        }
        String name = nameProperty.getPropertyValue();
        if("".equals(name)) {
            return "WILDCARD";
        }
        return name;
    }

    protected String getType() {
        return "theme";
    }

    void handleThemeRenamed(String from, String to) {
        for(Theme theme : getChildren(Theme.class)) {
            theme.handleThemeRenamed(from, to);
        }
        ThemeReference ref = refProperty.getPropertyValue();
        if(ref != null && from.equals(ref.getName())) {
            refProperty.setPropertyValue(new ThemeReference(to));
        }
    }

    public void addChildren() throws IOException {
        Utils.addChildren(themeFile, this, element, new DomWrapper() {
            public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                String tagName = element.getName();
                if("theme".equals(tagName)) {
                    return new Theme(themeFile, parent, element);
                }
                return null;
            }
        });
    }

    public void addToXPP(DomXPPParser xpp) {
        xpp.addElement(this, element);
    }

    public Element getDOMElement() {
        return element;
    }

    public List<ThemeTreeOperation> getOperations() {
        return Collections.<ThemeTreeOperation>emptyList();
    }

    public Property<?>[] getProperties() {
        return properties.toArray(new Property<?>[properties.size()]);
    }

}
