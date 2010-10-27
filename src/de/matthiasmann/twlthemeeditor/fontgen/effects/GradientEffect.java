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
package de.matthiasmann.twlthemeeditor.fontgen.effects;

import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleProperty;
import de.matthiasmann.twlthemeeditor.fontgen.Effect;
import de.matthiasmann.twlthemeeditor.fontgen.FontInfo;
import de.matthiasmann.twlthemeeditor.fontgen.GlyphRect;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

/**
 * An effect to spread a gradient down the text
 * 
 * @author kevin
 * @author Matthias Mann
 */
public class GradientEffect extends Effect {

    private final SimpleProperty<Color> colorTop    = new SimpleProperty<Color>(Color.class, "top color", Color.YELLOW);
    private final SimpleProperty<Color> colorBottom = new SimpleProperty<Color>(Color.class, "bottom color", Color.RED);

    @Override
    public Renderer createRenderer() {
        return new RendererImpl(
                colorTop.getPropertyValue(),
                colorBottom.getPropertyValue());
    }

    @Override
    public Property<?>[] getProperties() {
        return new Property<?>[] {
            new ColorConvertProperty(colorTop),
            new ColorConvertProperty(colorBottom)
        };
    }

    private static class RendererImpl extends Renderer {
        private final Color colorTop;
        private final Color colorBottom;

        public RendererImpl(Color colorTop, Color colorBottom) {
            this.colorTop = colorTop;
            this.colorBottom = colorBottom;
        }

        @Override
        public void preGlyphRender(Graphics2D g, FontInfo fontInfo, GlyphRect glyph) {
            g.setPaint(new GradientPaint(
                    0, -fontInfo.maxGlyphHeight, colorTop,
                    0,  fontInfo.maxGlyphDecent, colorBottom));
        }
    }
}
