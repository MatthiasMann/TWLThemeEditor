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

import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.images.Alias;
import de.matthiasmann.twlthemeeditor.datamodel.images.Animation;
import de.matthiasmann.twlthemeeditor.datamodel.images.Composed;
import de.matthiasmann.twlthemeeditor.datamodel.images.Cursor;
import de.matthiasmann.twlthemeeditor.datamodel.images.CursorRef;
import de.matthiasmann.twlthemeeditor.datamodel.images.Grid;
import de.matthiasmann.twlthemeeditor.datamodel.images.HSplitSimple;
import de.matthiasmann.twlthemeeditor.datamodel.images.HVSplitSimple;
import de.matthiasmann.twlthemeeditor.datamodel.images.Select;
import de.matthiasmann.twlthemeeditor.datamodel.images.Texture;
import de.matthiasmann.twlthemeeditor.datamodel.images.VSplitSimple;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;
import de.matthiasmann.twlthemeeditor.properties.ConditionProperty;
import de.matthiasmann.twlthemeeditor.properties.IntegerProperty;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public abstract class Image extends AbstractThemeTreeNode implements HasProperties {

    public enum Kind {
        IMAGE,
        CURSOR
    }

    protected final Textures textures;
    protected final ArrayList<Property<?>> properties;
    protected final Element element;
    protected final AttributeProperty nameProperty;
    protected ConditionProperty conditionProperty;

    protected Image(Textures textures, TreeTableNode parent, Element element) {
        super(parent);
        this.textures = textures;
        this.properties = new ArrayList<Property<?>>();
        this.element = element;

        if(parent == textures) {
            this.nameProperty = new AttributeProperty(element, "name");
            addProperty(nameProperty);
        } else {
            this.nameProperty = null;
        }
    }

    public Kind getKind() {
        return Kind.IMAGE;
    }

    public Property<?>[] getProperties() {
        return properties.toArray(new Property[properties.size()]);
    }

    protected final void addProperty(Property<?> property) {
        textures.getThemeFile().registerProperty(property);
        properties.add(property);
    }

    public void addToXPP(DomXPPParser xpp) {
        xpp.addElement(this, element);
    }

    public List<ThemeTreeOperation> getOperations() {
        return AbstractThemeTreeNode.getDefaultOperations(element, this);
    }

    protected void addStandardProperties() {
        addProperty(conditionProperty = new ConditionProperty(element, "Condition"));
        addProperty(new BooleanProperty(new AttributeProperty(element, "center", "Centered", true), false));
        addProperty(new BorderProperty(new AttributeProperty(element, "border", "Border", true), 0));
        addProperty(new BorderProperty(new AttributeProperty(element, "inset", "Inset", true), Short.MIN_VALUE));
        addProperty(new IntegerProperty(new AttributeProperty(element, "sizeOverwriteH", "Size overwrite horizontal", true), 0, Short.MAX_VALUE));
        addProperty(new IntegerProperty(new AttributeProperty(element, "sizeOverwriteV", "Size overwrite vertical", true), 0, Short.MAX_VALUE));
        addProperty(new BooleanProperty(new AttributeProperty(element, "repeatX", "Repeat horizontal (deprecated)", true), false));
        addProperty(new BooleanProperty(new AttributeProperty(element, "repeatY", "Repeat vertical (deprecated)", true), false));
        addProperty(new ColorProperty(new AttributeProperty(element, "tint", "Tint color", true)));
    }

    public String getName() {
        return (nameProperty != null) ? nameProperty.getPropertyValue() : null;
    }

    public Condition getCondition() {
        return (conditionProperty != null) ? conditionProperty.getPropertyValue() : Condition.NONE;
    }

    public Textures getTextures() {
        return textures;
    }

    @Override
    public String toString() {
        return getName();
    }

    public Object getData(int column) {
        switch (column) {
            case 0: {
                String displayName = getDisplayName();
                return error ? new NodeNameWithError(displayName) : displayName;
            }
            case 1:
                return getType();
            default:
                return "";
        }
    }

    public String getDisplayName() {
        String name = getName();
        if(name != null) {
            return name;
        }
        if(getParent() instanceof NameGenerator) {
            return ((NameGenerator)getParent()).generateName(this);
        }
        return "Unnamed #" + (1+getParent().getChildIndex(this));
    }

    protected String getType() {
        return element.getName();
    }

    public Element getDOMElement() {
        return element;
    }

    public void addChildren() throws IOException {
        Utils.addChildren(textures.getThemeFile(), this, element, getImageDomWrapper(textures));
    }
    
    public static DomWrapper getImageDomWrapper(final Textures textures) {
        return new DomWrapper() {
            public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                Image image = createChild(themeFile, parent, element);
                if(image != null && image.getKind() != Kind.CURSOR) {
                    image.addStandardProperties();
                }
                return image;
            }
            public Image createChild(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                String tagName = element.getName();

                if("texture".equals(tagName)) {
                    return new Texture(textures, parent, element);
                }
                if("alias".equals(tagName)) {
                    return new Alias(textures, parent, element);
                }
                if("select".equals(tagName)) {
                    return new Select(textures, parent, element);
                }
                if("composed".equals(tagName)) {
                    return new Composed(textures, parent, element);
                }
                if("grid".equals(tagName)) {
                    return new Grid(textures, parent, element);
                }
                if("hsplit".equals(tagName) && element.getAttribute("splitx") != null) {
                    return new HSplitSimple(textures, parent, element);
                }
                if("vsplit".equals(tagName) && element.getAttribute("splity") != null) {
                    return new VSplitSimple(textures, parent, element);
                }
                if("hvsplit".equals(tagName) && element.getAttribute("splitx") != null && element.getAttribute("splity") != null) {
                    return new HVSplitSimple(textures, parent, element);
                }
                if("cursor".equals(tagName)) {
                    if(element.getAttribute("ref") != null) {
                        return new CursorRef(textures, parent, element);
                    } else {
                        return new Cursor(textures, parent, element);
                    }
                }
                if("animation".equals(tagName)) {
                    return new Animation(textures, parent, element);
                }
                return null;
            }
        };
    }

    protected class ImageRectProperty extends RectProperty {
        public ImageRectProperty(Element element) {
            super(element, "rect");
        }

        @Override
        public Dimension getLimit() {
            return textures.getTextureDimensions();
        }
    }
}
