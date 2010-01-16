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
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.images.Alias;
import de.matthiasmann.twlthemeeditor.datamodel.images.Animation;
import de.matthiasmann.twlthemeeditor.datamodel.images.Composed;
import de.matthiasmann.twlthemeeditor.datamodel.images.Cursor;
import de.matthiasmann.twlthemeeditor.datamodel.images.CursorRef;
import de.matthiasmann.twlthemeeditor.datamodel.images.Grid;
import de.matthiasmann.twlthemeeditor.datamodel.images.HSplit;
import de.matthiasmann.twlthemeeditor.datamodel.images.HVSplit;
import de.matthiasmann.twlthemeeditor.datamodel.images.Select;
import de.matthiasmann.twlthemeeditor.datamodel.images.Texture;
import de.matthiasmann.twlthemeeditor.datamodel.images.VSplit;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.BooleanProperty;
import de.matthiasmann.twlthemeeditor.properties.BorderProperty;
import de.matthiasmann.twlthemeeditor.properties.ColorProperty;
import de.matthiasmann.twlthemeeditor.properties.ConditionProperty;
import de.matthiasmann.twlthemeeditor.properties.IntegerProperty;
import de.matthiasmann.twlthemeeditor.properties.NameProperty;
import de.matthiasmann.twlthemeeditor.properties.RectProperty;
import java.io.IOException;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public abstract class Image extends AbstractThemeTreeNode implements HasProperties {

    protected final Textures textures;
    protected final NameProperty nameProperty;
    protected ConditionProperty conditionProperty;

    protected Image(Textures textures, TreeTableNode parent, Element element) {
        super(textures.getThemeFile(), parent, element);
        this.textures = textures;

        if(parent == textures) {
            this.nameProperty = new ImageNameProperty(element);
            addProperty(nameProperty);
        } else {
            this.nameProperty = null;
        }
    }

    public Kind getKind() {
        return Kind.IMAGE;
    }

    public void addToXPP(DomXPPParser xpp) {
        xpp.addElement(this, element);
    }

    public List<ThemeTreeOperation> getOperations() {
        return AbstractThemeTreeNode.getDefaultOperations(element, this);
    }

    protected void addStandardProperties() {
        addProperty(conditionProperty = new ConditionProperty(
                new AttributeProperty(element, "if", "Condition", true),
                new AttributeProperty(element, "unless", "Condition", true), "Condition"));
        addProperty(new BooleanProperty(new AttributeProperty(element, "center", "Centered", true), false));
        addProperty(new BorderProperty(new AttributeProperty(element, "border", "Border", true), 0, false));
        addProperty(new BorderProperty(new AttributeProperty(element, "inset", "Inset", true), Short.MIN_VALUE, false));
        addProperty(new IntegerProperty(new AttributeProperty(element, "sizeOverwriteH", "Size overwrite horizontal", true), 0, Short.MAX_VALUE));
        addProperty(new IntegerProperty(new AttributeProperty(element, "sizeOverwriteV", "Size overwrite vertical", true), 0, Short.MAX_VALUE));
        addProperty(new BooleanProperty(new AttributeProperty(element, "repeatX", "Repeat horizontal", true), false));
        addProperty(new BooleanProperty(new AttributeProperty(element, "repeatY", "Repeat vertical", true), false));
        addProperty(new ColorProperty(new AttributeProperty(element, "tint", "Tint color", true)));
    }

    public String getName() {
        return (nameProperty != null) ? nameProperty.getPropertyValue() : null;
    }

    public Condition getCondition() {
        return (conditionProperty != null) ? conditionProperty.getPropertyValue() : Condition.NONE;
    }

    public final Textures getTextures() {
        return textures;
    }

    public final Image getLimit() {
        Image limit = this;
        while(limit.getParent() instanceof Image) {
            limit = (Image)limit.getParent();
        }
        return limit;
    }
    
    @Override
    public String toString() {
        return getName();
    }

    public void addChildren() throws IOException {
        addChildren(textures.getThemeFile(), element, getImageDomWrapper(textures));
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
                if("hsplit".equals(tagName)) {
                    return new HSplit(textures, parent, element);
                }
                if("vsplit".equals(tagName)) {
                    return new VSplit(textures, parent, element);
                }
                if("hvsplit".equals(tagName)) {
                    return new HVSplit(textures, parent, element);
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

    protected class ImageNameProperty extends NameProperty {
        public ImageNameProperty(Element element) {
            super(new AttributeProperty(element, "name"), getThemeTreeModel(), getKind());
        }

        @Override
        public void validateName(String name) throws IllegalArgumentException {
            if(name == null || name.length() == 0) {
                throw new IllegalArgumentException("empty name not allowed");
            }
            if("none".equals(name)) {
                throw new IllegalArgumentException("\"none\" is a reserved name");
            }
            for(Image img : getThemeTreeModel().getImages()) {
                if(img != Image.this && img.getKind() == getKind()) {
                    String imgName = img.getName();
                    if(name.equals(imgName)) {
                        throw new IllegalArgumentException("Name \"" + name + "\" already in use");
                    }
                }
            }
        }
    }

    protected class ImageRectProperty extends RectProperty {
        public ImageRectProperty(Element element) {
            super(new AttributeProperty(element, "x"),
                    new AttributeProperty(element, "y"),
                    new AttributeProperty(element, "width"),
                    new AttributeProperty(element, "height"),
                    "rect");
        }

        @Override
        public Dimension getLimit() {
            return textures.getTextureDimensions();
        }
    }
}
