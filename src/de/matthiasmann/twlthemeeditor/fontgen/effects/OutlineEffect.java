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
package de.matthiasmann.twlthemeeditor.fontgen.effects;

import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleProperty;
import de.matthiasmann.twlthemeeditor.fontgen.Effect;
import de.matthiasmann.twlthemeeditor.fontgen.FontGenerator.GeneratorMethod;
import de.matthiasmann.twlthemeeditor.fontgen.FontInfo;
import de.matthiasmann.twlthemeeditor.fontgen.GlyphRect;
import de.matthiasmann.twlthemeeditor.fontgen.Padding;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 *
 * @author Matthias Mann
 */
public class OutlineEffect extends Effect {

    private final SimpleProperty<Float> width = new SimpleProperty<Float>(Float.class, "width", 2.0f);
    private final SimpleProperty<Color> color = new SimpleProperty<Color>(Color.class, "color", Color.GRAY);
    private final SimpleProperty<Join> join = new SimpleProperty<Join>(Join.class, "join", Join.BEVEL);

    @Override
    public AWTRenderer createAWTRenderer() {
        return new RendererImpl(
                width.getPropertyValue(),
                join.getPropertyValue().awtJoin,
                color.getPropertyValue());
    }

    @Override
    public boolean supports(GeneratorMethod generator) {
        switch(generator) {
            case AWT_VECTOR:
                return true;
            default:
                return false;
        }
    }

    @Override
    public Property<?>[] getProperties() {
        return new Property<?>[] {
            width,
            join,
            new ColorConvertProperty(color)
        };
    }

    public static enum Join {
        BEVEL(BasicStroke.JOIN_BEVEL),
        MITER(BasicStroke.JOIN_MITER),
        ROUND(BasicStroke.JOIN_ROUND);
        
        int awtJoin;
        private Join(int awtJoin) {
            this.awtJoin = awtJoin;
        }
    }

    private static class RendererImpl extends AWTRenderer {
        private final float width;
        private final int awtJoin;
        private final Color color;
        private BasicStroke stroke;

        public RendererImpl(float width, int awtJoin, Color color) {
            this.width = width;
            this.awtJoin = awtJoin;
            this.color = color;
        }

        @Override
        public void prePageRender(Graphics2D g, FontInfo fontInfo) {
            stroke = new BasicStroke(Math.max(0.001f, width), BasicStroke.CAP_SQUARE, awtJoin);
        }

        @Override
        public void postGlyphRender(Graphics2D g, FontInfo fontInfo, GlyphRect glyph) {
            int offY = glyph.yDrawOffset - glyph.yoffset;
            g.setColor(color);
            g.setStroke(stroke);
            g.setPaint(null);
            g.translate(glyph.xDrawOffset, offY);
            g.draw(glyph.glyphShape);
            g.translate(-glyph.xDrawOffset, -offY);
        }

        @Override
        public Padding getPadding() {
            int advance = Math.round(width);
            int padding = Math.round(width * 0.5f);
            return new Padding(padding, padding, padding, padding, advance);
        }
    }
}
