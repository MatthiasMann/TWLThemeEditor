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
package de.matthiasmann.twlthemeeditor.fontgen;

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twlthemeeditor.fontgen.FontGenerator.GeneratorMethod;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author Matthias Mann
 */
public abstract class Effect {

    public abstract static class Renderer {
        public Padding getPadding() {
            return null;
        }
    }
    
    public abstract static class AWTRenderer extends Renderer {
        public void prePageRender(Graphics2D g, FontInfo fontInfo) {}
        public void preGlyphRender(Graphics2D g, FontInfo fontInfo, GlyphRect glyph) {}
        public void postGlyphRender(Graphics2D g, FontInfo fontInfo, GlyphRect glyph) {}
        public void postPageRender(Graphics2D g, FontInfo fontInfo) {}
    }
    
    public abstract static class FT2Renderer extends Renderer {
        public void prePageRender(BufferedImage img, FontInfo fontInfo) {}
        public abstract void render(BufferedImage img, FontInfo fontInfo, int xp, int yp, int w, int h, byte[] glyph);
        public void postPageRender(BufferedImage img, FontInfo fontInfo) {}
    }
    
    public abstract boolean supports(GeneratorMethod generator);
    
    public abstract Property<?>[] getProperties();

    public AWTRenderer createAWTRenderer() {
        throw new UnsupportedOperationException();
    }
    
    public FT2Renderer createFT2Renderer() {
        throw new UnsupportedOperationException();
    }

    protected static final class ColorConvertProperty implements Property<Color> {
        private final Property<java.awt.Color> base;

        public ColorConvertProperty(Property<java.awt.Color> base) {
            this.base = base;
        }
        public String getName() {
            return base.getName();
        }
        public boolean isReadOnly() {
            return base.isReadOnly();
        }
        public boolean canBeNull() {
            return base.canBeNull();
        }
        public Color getPropertyValue() {
            java.awt.Color awtColor = base.getPropertyValue();
            if(awtColor != null) {
                return new Color(
                        (byte)awtColor.getRed(),
                        (byte)awtColor.getGreen(),
                        (byte)awtColor.getBlue(),
                        (byte)awtColor.getAlpha());
            } else {
                return null;
            }
        }
        public void setPropertyValue(Color value) throws IllegalArgumentException {
            if(value != null) {
                base.setPropertyValue(new java.awt.Color(
                        value.getRed(),
                        value.getGreen(),
                        value.getBlue(),
                        value.getAlpha()));
            } else {
                base.setPropertyValue(null);
            }
        }
        public Class<Color> getType() {
            return Color.class;
        }
        public void addValueChangedCallback(Runnable cb) {
            base.addValueChangedCallback(cb);
        }
        public void removeValueChangedCallback(Runnable cb) {
            base.removeValueChangedCallback(cb);
        }
    }
}
