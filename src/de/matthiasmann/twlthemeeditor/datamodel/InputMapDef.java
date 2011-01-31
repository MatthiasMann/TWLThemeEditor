/*
 * Copyright (c) 2008-2011, Matthias Mann
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
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.DerivedNodeReferenceProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import java.io.IOException;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class InputMapDef extends ThemeTreeNode implements HasProperties {

    protected final NameProperty nameProperty;
    protected final DerivedNodeReferenceProperty refProperty;

    public InputMapDef(ThemeFile themeFile, TreeTableNode parent, Element element) {
        super(themeFile, parent, element);

        nameProperty = new NameProperty(new AttributeProperty(element, "name"), getThemeTreeModel(), Kind.INPUTMAP, true) {
            @Override
            public void validateName(String name) throws IllegalArgumentException {
                if(name == null || name.length() == 0) {
                    throw new IllegalArgumentException("Empty name not allowed");
                }
            }
        };
        addProperty(nameProperty);

        refProperty = new DerivedNodeReferenceProperty(
                new AttributeProperty(element, "ref", "Base input map reference", true),
                this, Kind.INPUTMAP);
        addProperty(refProperty);
    }

    @Override
    public void addChildren() throws IOException {
        addChildren(themeFile, element, new DomWrapperImpl());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addToXPP(DomXPPParser xpp) {
        Utils.addToXPP(xpp, element.getName(), this, element.getAttributes());
    }

    @Override
    public Kind getKind() {
        return Kind.INPUTMAP;
    }

    @Override
    public String getName() {
        return nameProperty.getPropertyValue();
    }

    @Override
    protected String getIcon() {
        return "inputmapdef";
    }

    @Override
    public boolean canPasteElement(Element element) {
        return canPasteInputMapDefElement(element);
    }

    public static boolean canPasteInputMapDefElement(Element element) {
        return "action".equals(element.getName());
    }

    @Override
    public boolean childrenNeedName() {
        return true;
    }

    @Override
    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> operations = super.getOperations();
        operations.add(new CloneNodeOperation(element, this));
        return operations;
    }

    @Override
    public List<CreateChildOperation> getCreateChildOperations() {
        List<CreateChildOperation> operations = super.getCreateChildOperations();
        addInputMapActionOperations(operations, this, element);
        return operations;
    }

    static void addInputMapActionOperations(List<CreateChildOperation> operations, ThemeTreeNode parent, Element element) {
        operations.add(new CreateChildOperation("opNewNodeInputMapAction", parent, element) {
            @Override
            public ThemeTreeNode executeAt(Object[] parameter, int pos) throws IOException {
                Element e = new Element("action");
                e.setAttribute("name", "changeMe");
                e.setText("F12");
                return addChild(e, pos);
            }
            @Override
            protected int getBaseIndentation() {
                int baseIndentation = super.getBaseIndentation();
                if(parent instanceof Param) {
                    baseIndentation = Math.max(0, baseIndentation - INDENTATION_SIZE);
                }
                return baseIndentation;
            }
        });
    }

    static class DomWrapperImpl implements DomWrapper {
        public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
            if("action".equals(element.getName())) {
                return new InputMapAction(themeFile, parent, element);
            }
            return null;
        }
    }
}
