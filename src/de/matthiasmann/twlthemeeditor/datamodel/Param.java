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
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewParam;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;
import de.matthiasmann.twlthemeeditor.properties.DimensionProperty;
import de.matthiasmann.twlthemeeditor.properties.ElementTextProperty;
import de.matthiasmann.twlthemeeditor.properties.GapProperty;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twlthemeeditor.properties.ImageReferenceProperty;
import de.matthiasmann.twlthemeeditor.properties.IntegerProperty;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import java.io.IOException;
import java.util.List;
import org.jdom.Content;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Param extends AbstractThemeTreeNode implements HasProperties {

    protected final Theme theme;
    protected final Element element;
    protected final NameProperty nameProperty;
    protected final Element valueElement;

    public Param(Theme theme, TreeTableNode parent, Element element) {
        super(theme.getThemeFile(), parent);
        this.theme = theme;
        this.element = element;
        
        this.nameProperty = new NameProperty(new AttributeProperty(element, "name")) {
            @Override
            public void validateName(String name) throws IllegalArgumentException {
            }
        };
        addProperty(nameProperty);

        Element e = null;
        for(int i=0,n=element.getContentSize() ; i<n ; i++) {
            Content content = element.getContent(i);
            if(content instanceof Element) {
                e = (Element)content;
                break;
            }
        }
        valueElement = e;

        Property<?> property = createProperty(valueElement, this);
        if(property != null) {
            addProperty(property);
        }
    }
    
    protected boolean isMap() {
        return "map".equals(valueElement.getName());
    }

    @Override
    public String getName() {
        return nameProperty.getPropertyValue();
    }

    @Override
    protected String getType() {
        return "param-" + valueElement.getName();
    }

    public void addChildren() throws IOException {
        if(isMap()) {
            Utils.addChildren(theme.getThemeFile(), this, valueElement, new DomWrapper() {
                public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                    if("param".equals(element.getName())) {
                        return new Param(theme, parent, element);
                    }
                    return null;
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    public void addToXPP(DomXPPParser xpp) {
        if(isMap()) {
            xpp.addStartTag(this, element.getName(), element.getAttributes());
            xpp.addStartTag(this, "map");
            Utils.addToXPP(xpp, this);
            xpp.addEndTag("map");
            xpp.addEndTag(element.getName());
        } else {
            xpp.addElement(this, element);
        }
    }

    public Element getDOMElement() {
        return element;
    }

    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> operations = AbstractThemeTreeNode.getDefaultOperations(element, this);
        if(isMap()) {
            addCreateParam(operations, this, valueElement);
        }
        return operations;
    }

    static void addCreateParam(List<ThemeTreeOperation> operations, ThemeTreeNode node, Element element) {
        operations.add(new CreateNewParam(element, "image", node, "none"));
        operations.add(new CreateNewParam(element, "border", node, "0"));
        operations.add(new CreateNewParam(element, "int", node, "0"));
        operations.add(new CreateNewParam(element, "bool", node, "false"));
        operations.add(new CreateNewParam(element, "gap", node, ""));
        operations.add(new CreateNewParam(element, "dimension", node, "0,0"));
        operations.add(new CreateNewParam(element, "map", node, "\n"));
    }

    static Property<?> createProperty(Element e, ThemeTreeNode node) {
        String tagName = e.getName();
        if("image".equals(tagName)) {
            return new ImageReferenceProperty(new ElementTextProperty(e, "image reference"), node, Image.Kind.IMAGE);
        }
        if("border".equals(tagName)) {
            return new BorderProperty(new ElementTextProperty(e, "border"), 0, true);
        }
        if("int".equals(tagName)) {
            return new IntegerProperty(new ElementTextProperty(e, "value"), Short.MIN_VALUE, Short.MAX_VALUE);
        }
        if("bool".equals(tagName)) {
            return new BooleanProperty(new ElementTextProperty(e, "value"), false);
        }
        if("gap".equals(tagName)) {
            return new GapProperty(new ElementTextProperty(e, "layout gap"));
        }
        if("dimension".equals(tagName)) {
            return new DimensionProperty(new ElementTextProperty(e, "dimesnion"));
        }
        return null;
    }
}
