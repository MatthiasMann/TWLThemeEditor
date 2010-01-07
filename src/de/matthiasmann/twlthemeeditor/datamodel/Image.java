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

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.model.AbstractTreeTableNode;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public abstract class Image extends NodeWrapper {

    Image(AbstractTreeTableNode parent, Element root) {
        super(parent, root);
    }

    public String getName() {
        return getAttribute("name");
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
        setAttribute("sizeOverwriteH", toStringOrNull(sizeOverwriteH));
    }

    @Optional
    @MinValueI(0)
    public Integer getSizeOverwriteV() {
        String value = getAttribute("sizeOverwriteV");
        return (value == null) ? null : Integer.valueOf(value);
    }

    public void setSizeOverwriteV(Integer sizeOverwriteV) {
        setAttribute("sizeOverwriteV", toStringOrNull(sizeOverwriteV));
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
        setAttribute("tint", toStringOrNull(tint));
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

    public ImageReference makeReference() {
        return new ImageReference(getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    public Object getData(int column) {
        switch (column) {
            case 0:
                return getName();
            case 1:
                return getClass().getSimpleName();
            default:
                return "";
        }
    }

    public String debugGetProps() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(getClass().getSimpleName());
        sb.append(" name=\"").append(getName()).append('\"');
        sb.append(" centered=").append(isCentered());
        sb.append(" border=").append(getBorder());
        sb.append(" inset=").append(getInset());
        appendChildProps(sb);
        return sb.append(']').toString();
    }

    static String toStringOrNull(Object o) {
        return (o == null) ? null : o.toString();
    }
    
    protected abstract void appendChildProps(StringBuilder sb);

    public static class Texture extends Image implements HasTextureDimensions {
        private final Textures textures;

        Texture(Textures textures, Element root) {
            super(textures, root);
            this.textures = textures;
        }

        public Rect getRect() {
            return new Rect(
                    parseIntFromAttribute("x"),
                    parseIntFromAttribute("y"),
                    parseIntFromAttribute("width"),
                    parseIntFromAttribute("height"));
        }

        public void setRect(Rect rect) {
            setAttribute("x", rect.getX());
            setAttribute("y", rect.getY());
            setAttribute("width", rect.getWidth());
            setAttribute("height", rect.getHeight());
        }

        public Dimension getTextureDimensions() {
            return textures.getTextureDimensions();
        }

        @Override
        protected void appendChildProps(StringBuilder sb) {
            sb.append(" rect=").append(getRect());
        }
    }

    public static class Alias extends Image {
        Alias(Textures textures, Element root) {
            super(textures, root);
        }

        public ImageReference getRef() {
            return new ImageReference(getAttribute("ref"));
        }

        public void setRef(ImageReference ref) {
            setAttribute("ref", ref.getName());
        }
        
        @Override
        protected void appendChildProps(StringBuilder sb) {
            sb.append(" ref=").append(getRef());
        }
    }

}
