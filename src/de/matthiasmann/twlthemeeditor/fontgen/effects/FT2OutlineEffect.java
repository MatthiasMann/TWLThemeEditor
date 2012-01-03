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

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleProperty;
import de.matthiasmann.twlthemeeditor.fontgen.Effect;
import de.matthiasmann.twlthemeeditor.fontgen.FontGenerator.GeneratorMethod;
import de.matthiasmann.twlthemeeditor.fontgen.FontInfo;
import de.matthiasmann.twlthemeeditor.fontgen.Padding;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;

/**
 *
 * @author Matthias Mann
 */
public class FT2OutlineEffect extends Effect {
    
    private final SimpleProperty<Color> glyphColor = new SimpleProperty<Color>(Color.class, "glyphColor", Color.WHITE);
    private final SimpleProperty<Color> outlineColor = new SimpleProperty<Color>(Color.class, "outlineColor", Color.BLACK);
    
    @Override
    public boolean supports(GeneratorMethod generator) {
        switch(generator) {
            case FREETYPE2:
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public Property<?>[] getProperties() {
        return new Property<?>[] {
            glyphColor,
            outlineColor
        };
    }

    @Override
    public FT2Renderer createFT2Renderer() {
        return new RendererImpl(
                glyphColor.getPropertyValue(),
                outlineColor.getPropertyValue());
    }
    
    static class RendererImpl extends FT2Renderer {
        final Color glyphColor;
        final Color outlineColor;
        
        int[] data;
        int stride;
        int offset;
        int[] colors;

        public RendererImpl(Color glyphColor, Color outlineColor) {
            this.glyphColor = glyphColor;
            this.outlineColor = outlineColor;
        }

        @Override
        public Padding getPadding() {
            return new Padding(1, 1, 1, 1, 1);
        }
        
        @Override
        public void prePageRender(BufferedImage image, FontInfo fontInfo) {
            DataBufferInt dataBuffer = (DataBufferInt)image.getRaster().getDataBuffer();
            data = dataBuffer.getData();
            stride = ((SinglePixelPackedSampleModel)image.getSampleModel()).getScanlineStride();
            offset = dataBuffer.getOffset();
            
            int oR = outlineColor.getRed();
            int oG = outlineColor.getGreen();
            int oB = outlineColor.getBlue();
            int dR = glyphColor.getRed() - oR;
            int dG = glyphColor.getGreen() - oG;
            int dB = glyphColor.getBlue() - oB;
            
            colors = new int[256];
            for(int i=0 ; i<256 ; i++) {
                int r = oR + dR * i / 255;
                int g = oG + dG * i / 255;
                int b = oB + dB * i / 255;
                colors[i] = (r << 16) | (g << 8) | b;
            }
        }

        @Override
        public void render(BufferedImage image, FontInfo fontInfo, int xp, int yp, int width, int height, byte[] glyph) {
            if(xp + width - 2 > image.getWidth()) {
                return;
            }
            if(yp + height - 2 > image.getHeight()) {
                return;
            }
            int dataOff = offset + yp * stride + xp - 2;

            int off = width + 1;
            for(int y=2 ; y<height ; y++) {
                for(int x=2 ; x<width ; x++,off++) {
                    int outline = max(
                      max3H(glyph, off-width),
                      max3H(glyph, off      ),
                      max3H(glyph, off+width));
                    data[dataOff+x] = colors[glyph[off] & 255] | (outline << 24);
                }
                dataOff += stride;
                off += 2;
            }
        }

        private static int max3H(byte[] bb, int off) {
            int a = bb[off-1] & 255;
            int b = bb[off  ] & 255;
            int c = bb[off+1] & 255;
            return max(a, b, c);
        }

        private static int max(int a, int b, int c) {
            if(b > a) a = b;
            if(c > a) a = c;
            return a;
        }
    }
}
