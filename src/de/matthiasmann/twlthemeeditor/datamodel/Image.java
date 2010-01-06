/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.Rect;
import org.jdom.Element;

/**
 *
 * @author MannMat
 */
public abstract class Image extends NodeWrapper {

    Image(ThemeFile themeFile, Element root) {
        super(themeFile, root);
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
    public Integer getSizeOverwriteH() {
        String value = getAttribute("sizeOverwriteH");
        return (value == null) ? null : Integer.valueOf(value);
    }

    public void setSizeOverwriteH(Integer sizeOverwriteH) {
        setAttribute("sizeOverwriteH", toStringOrNull(sizeOverwriteH));
    }

    @Optional
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
            super(textures.themeFile, root);
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
        Alias(ThemeFile themeFile, Element root) {
            super(themeFile, root);
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
