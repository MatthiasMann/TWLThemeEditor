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

import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CloneNodeOperation;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.NodeReferenceProperty;
import java.io.IOException;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Theme extends ThemeTreeNode implements HasProperties {

    protected final NameProperty nameProperty;
    protected final BooleanProperty allowWildcardProperty;
    protected final BooleanProperty mergeProperty;
    protected final NodeReferenceProperty refProperty;

    public Theme(ThemeFile themeFile, TreeTableNode parent, Element element) {
        super(themeFile, parent, element);

        this.nameProperty = new NameProperty(new AttributeProperty(element, "name"), getThemeTreeModel(), Kind.THEME) {
            @Override
            public void validateName(String name) throws IllegalArgumentException {
                if(name == null || name.length() == 0) {
                    throw new IllegalArgumentException("empty name not allowed");
                }
                if(getThemeTreeModel().findTopLevelNodes(Theme.class, name, Theme.this) != null) {
                    throw new IllegalArgumentException("Name \"" + name + "\" already in use");
                }
            }
        };
        addProperty(nameProperty);

        if(element.getParentElement().isRootElement()) {
            allowWildcardProperty = new BooleanProperty(
                    new AttributeProperty(element, "allowWildcard", "Allow Wildcard", true), false);
            addProperty(allowWildcardProperty);
            mergeProperty = null;
        } else {
            allowWildcardProperty = null;
            mergeProperty = new BooleanProperty(
                    new AttributeProperty(element, "merge", "Merge", true), false);
            addProperty(mergeProperty);
        }

        addProperty(refProperty = new NodeReferenceProperty(
                new AttributeProperty(element, "ref", "Base theme reference", true),
                this, Kind.THEME));
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

    public boolean isWildcard() {
        return (nameProperty != null) && nameProperty.getPropertyValue().isEmpty();
    }

    public boolean isAllowWildcard() {
        return allowWildcardProperty != null && allowWildcardProperty.getValue();
    }

    public boolean isMerge() {
        return mergeProperty != null && mergeProperty.getValue();
    }

    public boolean matchName(String name) {
        return nameProperty != null && nameProperty.getPropertyValue().equals(name);
    }

    public NodeReference getRef() {
        return refProperty.getPropertyValue();
    }
    
    public Kind getKind() {
        return Kind.THEME;
    }

    public final Theme getLimit() {
        Theme node = this;
        while(node.getParent() instanceof Theme) {
            node = (Theme)node.getParent();
        }
        return node;
    }
    
    public void addChildren() throws IOException {
        addChildren(themeFile, element, new DomWrapper() {
            public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                String tagName = element.getName();
                if("theme".equals(tagName)) {
                    return new Theme(themeFile, parent, element);
                }
                if("param".equals(tagName)) {
                    return new Param(Theme.this, parent, element);
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
        ThemeFile.addCreateThemeOperation(operations, this, element);
        Param.addCreateParam(operations, this, element);
        return operations;
    }
}
