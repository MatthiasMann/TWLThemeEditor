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
package de.matthiasmann.twlthemeeditor.fontgen.effects;

import de.matthiasmann.javafreetype.FreeTypeGlyphInfo;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleProperty;
import de.matthiasmann.twlthemeeditor.fontgen.Effect;
import de.matthiasmann.twlthemeeditor.fontgen.FontGenerator.GeneratorMethod;
import de.matthiasmann.twlthemeeditor.fontgen.FontInfo;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;

/**
 *
 * @author Matthias Mann
 */
public class FT2GradientEffect extends Effect {
    
    private final SimpleProperty<Color> colorTop    = new SimpleProperty<Color>(Color.class, "top color", Color.YELLOW);
    private final SimpleProperty<Color> colorBottom = new SimpleProperty<Color>(Color.class, "bottom color", Color.RED);

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
            colorTop,
            colorBottom
        };
    }
    
    @Override
    public FT2Renderer createFT2Renderer() {
        return new RendererImpl(
                colorTop.getPropertyValue(),
                colorBottom.getPropertyValue());
    }
    
    static Object KEY_GRADIENT_DATA = new Object();
    
    static class RendererImpl extends FT2Renderer {
        final Color colorTop;
        final Color colorBottom;

        int[] data;
        int stride;
        int offset;
        int[] colors;

        public RendererImpl(Color colorTop, Color colorBottom) {
            this.colorTop = colorTop;
            this.colorBottom = colorBottom;
        }
        
        @Override
        public void prePageRender(BufferedImage image, FontInfo fontInfo) {
            DataBufferInt dataBuffer = (DataBufferInt)image.getRaster().getDataBuffer();
            data = dataBuffer.getData();
            stride = ((SinglePixelPackedSampleModel)image.getSampleModel()).getScanlineStride();
            offset = dataBuffer.getOffset();
            
            int tR = colorTop.getRed();
            int tG = colorTop.getGreen();
            int tB = colorTop.getBlue();
            int bR = colorBottom.getRed() - tR;
            int bG = colorBottom.getGreen() - tG;
            int bB = colorBottom.getBlue() - tB;
            
            int height = fontInfo.maxGlyphDecent + fontInfo.maxGlyphHeight;
            if(height < 2) {
                height = 2;
            }
            
            colors = new int[height+2];
            colors[0]        = (tR << 16) | (tG << 8) | tB;
            colors[height+1] = (bR << 16) | (bG << 8) | bB;
            for(int i=1 ; i<=height ; i++) {
                int r = tR + bR * i / (height-1);
                int g = tG + bG * i / (height-1);
                int b = tB + bB * i / (height-1);
                colors[i] = (r << 16) | (g << 8) | b;
            }
            
            fontInfo.effectData.put(KEY_GRADIENT_DATA, colors);
        }

        @Override
        public void render(BufferedImage image, FontInfo fontInfo, int xp, int yp, int width, int height, byte[] glyph, FreeTypeGlyphInfo glyphInfo) {
            if(xp + width > image.getWidth()) {
                return;
            }
            if(yp + height > image.getHeight()) {
                return;
            }
            if(fontInfo.effectData.containsKey(FT2OutlineEffect.KEY_OUTLINE_ACTIVE)) {
                return;
            }
            
            int dataOff = offset + yp * stride + xp - 2;

            int row = 1 + fontInfo.maxGlyphAscent - glyphInfo.getOffsetY();
            int off = width*2 + 2;
            for(int y=2 ; y<height ; y++,row++) {
                for(int x=2 ; x<width ; x++,off++) {
                    final int alpha = glyph[off] & 255;
                    data[dataOff+x] = colors[row] | ((alpha) << 24);
                }
                dataOff += stride;
                off += 2;
            }
        }
    }
    
}
