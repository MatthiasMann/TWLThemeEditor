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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Shadow effect
 *
 * @author kevin
 * @author Matthias Mann
 */
public class BlurShadowEffect extends Effect {

    /** The number of kernels to apply */
    static final int NUM_KERNELS = 16;
    /** The blur kernels applied across the effect */
    static final float[][] GAUSSIAN_BLUR_KERNELS = generateGaussianBlurKernels(NUM_KERNELS);
    /** Hints for ConvolveOp */
    static final RenderingHints hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    private final SimpleProperty<Float> distance = new SimpleProperty<Float>(Float.class, "distance", 3.0f);
    private final SimpleProperty<Integer> kernelsize = new SimpleProperty<Integer>(Integer.class, "kernel size", 3);
    private final SimpleProperty<Integer> passes = new SimpleProperty<Integer>(Integer.class, "num passes", 1);
    private final SimpleProperty<Color> color = new SimpleProperty<Color>(Color.class, "color", new Color(0, 0, 0, 128));

    @Override
    public AWTRenderer createAWTRenderer() {
        return new RendererImpl(
                kernelsize.getPropertyValue(),
                passes.getPropertyValue(),
                color.getPropertyValue(),
                distance.getPropertyValue());
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
            distance,
            kernelsize,
            passes,
            new ColorConvertProperty(color)
        };
    }

    private static class RendererImpl extends AWTRenderer {
        private final int kernelSize;
        private final int numPasses;
        private final Color color;
        private final float dist;

        public RendererImpl(int kernelSize, int numPasses, Color color, float dist) {
            this.kernelSize = kernelSize;
            this.numPasses = numPasses;
            this.color = color;
            this.dist = dist;
        }

        private ConvolveOp filterH;
        private ConvolveOp filterV;

        @Override
        public void prePageRender(Graphics2D g, FontInfo fontInfo) {
            if(kernelSize > 1 && numPasses >= 1) {
                final float[] matrix = GAUSSIAN_BLUR_KERNELS[Math.min(NUM_KERNELS, kernelSize) - 1];
                Kernel kernelH = new Kernel(matrix.length, 1, matrix);
                Kernel kernelV = new Kernel(1, matrix.length, matrix);
                filterH = new ConvolveOp(kernelH, ConvolveOp.EDGE_NO_OP, hints);
                filterV = new ConvolveOp(kernelV, ConvolveOp.EDGE_NO_OP, hints);
            } else {
                filterH = null;
                filterV = null;
            }
        }

        @Override
        public void preGlyphRender(Graphics2D g, FontInfo context, GlyphRect glyph) {
            BufferedImage image1 = new BufferedImage(glyph.width, glyph.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g1 = image1.createGraphics();
            g1.setColor(color);
            g1.translate(glyph.xDrawOffset + dist, glyph.yDrawOffset - glyph.yoffset + dist);
            g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g1.fill(glyph.glyphShape);
            g1.dispose();
            if(filterH != null && filterV != null) {
                BufferedImage image2 = new BufferedImage(glyph.width, glyph.height, BufferedImage.TYPE_INT_ARGB);
                for(int i = 0; i < numPasses; i++) {
                    filterH.filter(image1, image2);
                    filterV.filter(image2, image1);
                }
            }
            g.drawImage(image1, 0, 0, null);
        }

        @Override
        public void postPageRender(Graphics2D g, FontInfo fontInfo) {
            filterH = null;
            filterV = null;
        }

        @Override
        public Padding getPadding() {
            int padding = Math.round((float)Math.ceil(dist));
            return new Padding(0, 0, padding, padding, 0);
        }
    }

    /**
     * Generate the blur kernels which will be repeatedly applied when blurring images
     *
     * @param level The number of kernels to generate
     * @return The kernels generated
     */
    private static float[][] generateGaussianBlurKernels(int level) {
        float[][] pascalsTriangle = generatePascalsTriangle(level);
        float[][] gaussianTriangle = new float[pascalsTriangle.length][];

        for (int i = 0; i < gaussianTriangle.length; i++) {
            float total = 0.0f;
            gaussianTriangle[i] = new float[pascalsTriangle[i].length];

            for (int j = 0; j < pascalsTriangle[i].length; j++) {
                total += pascalsTriangle[i][j];
            }

            float coefficient = 1 / total;
            for (int j = 0; j < pascalsTriangle[i].length; j++) {
                gaussianTriangle[i][j] = coefficient * pascalsTriangle[i][j];
            }

        }

        return gaussianTriangle;
    }

    /**
     * Generate Pascal's triangle
     *
     * @param level The level of the triangle to generate
     * @return The Pascal's triangle
     */
    private static float[][] generatePascalsTriangle(int level) {
        if (level < 2) {
            level = 2;
        }

        float[][] triangle = new float[level][];
        triangle[0] = new float[] { 1.0f };
        triangle[1] = new float[] { 1.0f, 1.0f };

        for (int i = 2; i < level; i++) {
            triangle[i] = new float[i + 1];
            triangle[i][0] = 1.0f;
            triangle[i][i] = 1.0f;
            for (int j = 1; j < triangle[i].length - 1; j++) {
                triangle[i][j] = triangle[i - 1][j - 1] + triangle[i - 1][j];
            }
        }

        return triangle;
    }
}
