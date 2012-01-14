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

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twlthemeeditor.properties.HasProperties;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.images.Alias;
import de.matthiasmann.twlthemeeditor.datamodel.images.Animation;
import de.matthiasmann.twlthemeeditor.datamodel.images.Composed;
import de.matthiasmann.twlthemeeditor.datamodel.images.Cursor;
import de.matthiasmann.twlthemeeditor.datamodel.images.CursorRef;
import de.matthiasmann.twlthemeeditor.datamodel.images.Grid;
import de.matthiasmann.twlthemeeditor.datamodel.images.Select;
import de.matthiasmann.twlthemeeditor.datamodel.images.Area;
import de.matthiasmann.twlthemeeditor.datamodel.images.Gradient;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CloneNodeOperation;
import de.matthiasmann.twlthemeeditor.dom.Element;
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

/**
 *
 * @author Matthias Mann
 */
public abstract class Image extends ThemeTreeNode implements HasProperties {

    protected final Images textures;
    protected final NameProperty nameProperty;
    protected ConditionProperty conditionProperty;

    protected Image(Images textures, TreeTableNode parent, Element element) {
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

    protected void addStandardProperties() {
        addProperty(conditionProperty = new ConditionProperty(
                new AttributeProperty(element, "if", "Condition", true),
                new AttributeProperty(element, "unless", "Condition", true), "Condition"));
        addProperty(new BooleanProperty(new AttributeProperty(element, "center", "Centered", true), false));
        addProperty(new BorderProperty(new AttributeProperty(element, "border", "Border", true), 0, false));
        addProperty(new BorderProperty(new AttributeProperty(element, "inset", "Inset", true), Short.MIN_VALUE, false));
        addProperty(new ColorProperty(new AttributeProperty(element, "tint", "Tint color", true), this));
        addProperty(new IntegerProperty(new AttributeProperty(element, "sizeOverwriteH", "Size overwrite horizontal", true), 0, Short.MAX_VALUE));
        addProperty(new IntegerProperty(new AttributeProperty(element, "sizeOverwriteV", "Size overwrite vertical", true), 0, Short.MAX_VALUE));
        addProperty(new BooleanProperty(new AttributeProperty(element, "repeatX", "Repeat horizontal", true), false));
        addProperty(new BooleanProperty(new AttributeProperty(element, "repeatY", "Repeat vertical", true), false));
    }

    public String getName() {
        return (nameProperty != null) ? nameProperty.getPropertyValue() : null;
    }

    public Condition getCondition() {
        return (conditionProperty != null) ? conditionProperty.getPropertyValue() : Condition.NONE;
    }

    public final Images getTextures() {
        return textures;
    }

    public final Image getLimit() {
        Image limit = this;
        while(limit.getParent() instanceof Image) {
            limit = (Image)limit.getParent();
        }
        return limit;
    }

    public void addChildren() throws IOException {
        addChildren(textures.getThemeFile(), element, getImageDomWrapper(textures));
    }

    @Override
    public List<ThemeTreeOperation> getOperations() {
        List<ThemeTreeOperation> operations = super.getOperations();
        operations.add(new CloneNodeOperation(element, this));
        return operations;
    }

    public static void convertToXYWH(ThemeFile themeFile, Element element) {
        if(element.getAttributeValue("xywh") == null) {
            String x = element.getAttributeValue("x");
            String y = element.getAttributeValue("y");
            String w = element.getAttributeValue("width");
            String h = element.getAttributeValue("height");
            element.setAttribute("xywh", x+","+y+","+w+","+h);
            element.removeAttribute("x");
            element.removeAttribute("y");
            element.removeAttribute("width");
            element.removeAttribute("height");
            themeFile.elementUpgraded();
        }
    }

    public static void convertSplitX(ThemeFile themeFile, Element element) {
        if(element.getAttributeValue("splitx") == null) {
            final String borderStr = element.getAttributeValue("border");
            int left = 0;
            int right = 0;
            try {
                Border border = Utils.parseBorder(borderStr);
                if(border != null) {
                    left = border.getBorderLeft();
                    right = border.getBorderRight();
                }
            } catch(IllegalArgumentException ex) {
                themeFile.logError("Could not parse border", borderStr, ex);
            }
            element.setAttribute("splitx", "L"+left+",R"+right);
            themeFile.elementUpgraded();
        }
    }

    public static void convertSplitY(ThemeFile themeFile, Element element) {
        if(element.getAttributeValue("splity") == null) {
            final String borderStr = element.getAttributeValue("border");
            int top = 0;
            int bottom = 0;
            try {
                Border border = Utils.parseBorder(borderStr);
                if(border != null) {
                    top = border.getBorderTop();
                    bottom = border.getBorderBottom();
                }
            } catch(NumberFormatException ex) {
                themeFile.logError("Could not parse border", borderStr, ex);
            }
            element.setAttribute("splity", "T"+top+",B"+bottom);
            themeFile.elementUpgraded();
        }
    }
    
    public static DomWrapper getImageDomWrapper(final Images images) {
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

                if("area".equals(tagName)) {
                    return new Area(images, parent, element);
                }
                if("alias".equals(tagName)) {
                    return new Alias(images, parent, element);
                }
                if("select".equals(tagName)) {
                    return new Select(images, parent, element);
                }
                if("composed".equals(tagName)) {
                    return new Composed(images, parent, element);
                }
                if("grid".equals(tagName)) {
                    return new Grid(images, parent, element);
                }
                if("cursor".equals(tagName)) {
                    if(element.getAttribute("ref") != null) {
                        return new CursorRef(images, parent, element);
                    } else {
                        convertToXYWH(themeFile, element);
                        return new Cursor(images, parent, element);
                    }
                }
                if("animation".equals(tagName)) {
                    return new Animation(images, parent, element);
                }
                if("texture".equals(tagName)) {
                    return upgradeTexture(themeFile, parent, element);
                }
                if("hsplit".equals(tagName)) {
                    return upgradeHSplit(themeFile, parent, element);
                }
                if("vsplit".equals(tagName)) {
                    return upgradeVSplit(themeFile, parent, element);
                }
                if("hvsplit".equals(tagName)) {
                    return upgradeHVSplit(themeFile, parent, element);
                }
                if("gradient".equals(tagName)) {
                    return new Gradient(images, parent, element);
                }
                return null;
            }

            private Image upgradeTexture(ThemeFile themeFile, ThemeTreeNode parent, Element element) {
                element.setName("area");
                convertToXYWH(themeFile, element);
                themeFile.elementUpgraded();
                return new Area(images, parent, element);
            }

            private Image upgradeHSplit(ThemeFile themeFile, ThemeTreeNode parent, Element element) {
                element.setName("area");
                convertToXYWH(themeFile, element);
                convertSplitX(themeFile, element);
                themeFile.elementUpgraded();
                return new Area(images, parent, element);
            }

            private Image upgradeVSplit(ThemeFile themeFile, ThemeTreeNode parent, Element element) {
                element.setName("area");
                convertToXYWH(themeFile, element);
                convertSplitY(themeFile, element);
                themeFile.elementUpgraded();
                return new Area(images, parent, element);
            }

            private Image upgradeHVSplit(ThemeFile themeFile, ThemeTreeNode parent, Element element) {
                element.setName("area");
                convertToXYWH(themeFile, element);
                convertSplitX(themeFile, element);
                convertSplitY(themeFile, element);
                themeFile.elementUpgraded();
                return new Area(images, parent, element);
            }
        };
    }

    protected class ImageNameProperty extends NameProperty {
        public ImageNameProperty(Element element) {
            super(new AttributeProperty(element, "name"), getThemeTreeModel(), getKind(), true);
        }

        @Override
        public void validateName(String name) throws IllegalArgumentException {
            if(name == null || name.length() == 0) {
                throw new IllegalArgumentException("empty name not allowed");
            }
            if("none".equals(name)) {
                throw new IllegalArgumentException("\"none\" is a reserved name");
            }
            if(getThemeTreeModel().findImage(getKind(), name, Image.this) != null) {
                throw new IllegalArgumentException("Name \"" + name + "\" already in use");
            }
        }
    }

    protected class ImageRectProperty extends RectProperty {
        public ImageRectProperty(Element element) {
            super(new AttributeProperty(element, "xywh"), "rect");
        }

        @Override
        public Dimension getLimit() {
            return textures.getTextureDimensions();
        }
    }
}
