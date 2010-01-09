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

import de.matthiasmann.twlthemeeditor.properties.MinValueI;
import de.matthiasmann.twlthemeeditor.properties.Optional;
import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.Color;
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
import java.io.IOException;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public abstract class Image extends ThemeTreeNode implements HasProperties {

    protected final Textures textures;
    protected BaseProperties properties;
    protected final Element element;

    protected Image(Textures textures, TreeTableNode parent, Element element) {
        super(parent);
        this.textures = textures;
        this.element = element;
    }

    public BaseProperties getProperties() {
        return properties;
    }

    public void addToXPP(DomXPPParser xpp) {
        xpp.addElement(element);
    }

    public class BaseProperties extends NodeWrapper {
        protected final Textures textures;

        protected BaseProperties(Textures textures, Element node) {
            super(textures.getThemeFile(), node);
            this.textures = textures;
        }

        public String getName() {
            return getAttribute("name");
        }
    }

    public class ImageProperties extends BaseProperties {
        public ImageProperties(Textures textures, Element node) {
            super(textures, node);
        }

        public boolean isCentered() {
            return parseBoolFromAttribute("center", false);
        }

        public void setCentered(boolean centered) {
            setAttribute("center", centered, false);
        }

        @Optional
        public Border getBorder() {
            return Utils.parseBorder(getAttribute("border"));
        }

        public void setBorder(Border border) {
            setAttribute("border", Utils.toString(border));
        }

        @Optional
        public Border getInset() {
            return Utils.parseBorder(getAttribute("inset"));
        }

        public void setInset(Border inset) {
            setAttribute("inset", Utils.toString(inset));
        }

        @Optional
        @MinValueI(0)
        public Integer getSizeOverwriteH() {
            String value = getAttribute("sizeOverwriteH");
            return (value == null) ? null : Integer.valueOf(value);
        }

        public void setSizeOverwriteH(Integer sizeOverwriteH) {
            setAttribute("sizeOverwriteH", Utils.toStringOrNull(sizeOverwriteH));
        }

        @Optional
        @MinValueI(0)
        public Integer getSizeOverwriteV() {
            String value = getAttribute("sizeOverwriteV");
            return (value == null) ? null : Integer.valueOf(value);
        }

        public void setSizeOverwriteV(Integer sizeOverwriteV) {
            setAttribute("sizeOverwriteV", Utils.toStringOrNull(sizeOverwriteV));
        }

        public boolean isRepeatX() {
            return parseBoolFromAttribute("repeatX", false);
        }

        public void setRepeatX(boolean repeatX) {
            setAttribute("repeatX", repeatX, false);
        }

        public boolean isRepeatY() {
            return parseBoolFromAttribute("repeatY", false);
        }

        public void setRepeatY(boolean repeatY) {
            setAttribute("repeatY", repeatY, false);
        }

        @Optional
        public Color getTint() {
            String value = getAttribute("tint");
            return (value == null) ? null : Color.parserColor(value);
        }

        public void setTint(Color tint) {
            setAttribute("tint", Utils.toStringOrNull(tint));
        }

        public Condition getCondition() {
            String cond = getAttribute("if");
            if(cond != null) {
                return new Condition(Condition.Type.IF, cond);
            }
            cond = getAttribute("unless");
            if(cond != null) {
                return new Condition(Condition.Type.UNLESS, cond);
            }
            return Condition.NONE;
        }

        public void setCondition(Condition condition) {
            setAttribute("if", (condition.getType() == Condition.Type.IF) ? condition.getCondition() : null);
            setAttribute("unless", (condition.getType() == Condition.Type.UNLESS) ? condition.getCondition() : null);
        }
    }

    public ImageReference makeReference() {
        return new ImageReference(properties.getName());
    }

    public String getName() {
        return properties.getName();
    }

    public Textures getTextures() {
        return textures;
    }

    @Override
    public String toString() {
        return properties.getName();
    }

    public Object getData(int column) {
        switch (column) {
            case 0: {
                String name = properties.getName();
                if(name != null) {
                    return name;
                }
                return "Unnamed #" + (1+getParent().getChildIndex(this));
            }
            case 1:
                return getClass().getSimpleName();
            default:
                return "";
        }
    }

    public static void addChildImages(final Textures textures, ModifyableTreeTableNode parent, Element node) throws IOException {
        Utils.addChildren(textures.getThemeFile(), parent, node, new DomWrapper() {
            public TreeTableNode wrap(ThemeFile themeFile, ModifyableTreeTableNode parent, Element element) throws IOException {
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
        });
    }

}
