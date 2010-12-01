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
package de.matthiasmann.twlthemeeditor.fontgen.gui;

import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.DynamicImage;
import de.matthiasmann.twlthemeeditor.DelayedAction;
import de.matthiasmann.twlthemeeditor.fontgen.CharSet;
import de.matthiasmann.twlthemeeditor.fontgen.Effect;
import de.matthiasmann.twlthemeeditor.fontgen.FontData;
import de.matthiasmann.twlthemeeditor.fontgen.FontGenerator;
import de.matthiasmann.twlthemeeditor.fontgen.FontGenerator.GeneratorMethod;
import de.matthiasmann.twlthemeeditor.fontgen.Padding;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class FontDisplay extends Widget {

    private final Runnable callback;
    private final GUI.AsyncCompletionListener<FontGenerator> completionHandler;
    private DelayedAction delayedAction;

    private int textureSize;
    private FontData fontData;
    private Padding padding;
    private boolean paddingAutomatic;
    private boolean useAA;
    private CharSet charSet;
    private Effect.Renderer[] effects;
    private FontGenerator.GeneratorMethod generatorMethod;

    private boolean pendingUpdate;
    private boolean updateRunning;
    private ByteBuffer buffer;
    private DynamicImage image;
    private FontGenerator lastFontGen;

    public FontDisplay(Runnable callback) {
        this.callback = callback;
        this.completionHandler = new GUI.AsyncCompletionListener<FontGenerator>() {
            public void completed(FontGenerator fontGen) {
                updateImage(fontGen);
            }

            public void failed(Exception ex) {
                Logger.getLogger(FontDisplay.class.getName()).log(Level.SEVERE, "Can't generate font", ex);
                updateDone();
            }
        };
    }

    public void setTextureSize(int textureSize) {
        this.textureSize = textureSize;
        update();
    }

    public void setFontData(FontData fontData) {
        this.fontData = fontData;
        update();
    }

    public void setPaddingManual(Padding padding) {
        this.padding = padding;
        this.paddingAutomatic = false;
        update();
    }

    public void setPaddingAutomatic() {
        this.padding = null;
        this.paddingAutomatic = true;
        update();
    }

    public void setUseAA(boolean useAA) {
        this.useAA = useAA;
        update();
    }

    public void setGeneratorMethod(GeneratorMethod generatorMethod) {
        this.generatorMethod = generatorMethod;
        update();
    }

    public void setCharSet(CharSet charSet) {
        this.charSet = new CharSet(charSet);
        update();
    }

    public void setEffects(Effect[] effects) {
        Effect.Renderer[] tmp = new Effect.Renderer[effects.length];
        for(int i=0 ; i<effects.length ; i++) {
            tmp[i] = effects[i].createRenderer();
        }
        this.effects = tmp;
        update();
    }

    public FontGenerator getLastFontGen() {
        return lastFontGen;
    }

    void update() {
        if(delayedAction != null) {
            delayedAction.run();
        }
    }
    
    void doUpdate() {
        GUI gui = getGUI();
        if(gui != null && textureSize > 0 && fontData != null && (paddingAutomatic || padding != null) && 
                charSet != null && effects != null && generatorMethod != null) {
            if(updateRunning) {
                pendingUpdate = true;
            } else {
                gui.invokeAsync(new GenFont(textureSize, fontData, computePadding(), charSet, effects, useAA, generatorMethod), completionHandler);
                updateRunning = true;
            }
        }
    }

    private Padding computePadding() {
        if(paddingAutomatic) {
            Padding p = Padding.ZERO;
            for(Effect.Renderer effect : effects) {
                Padding ep = effect.getPadding();
                if(ep != null) {
                    p = p.max(ep);
                }
            }
            return p;
        } else {
            assert padding != null : "padding is null";
            return padding;
        }
    }

    @Override
    protected void afterAddToGUI(GUI gui) {
        super.afterAddToGUI(gui);
        delayedAction = new DelayedAction(gui, new Runnable() {
            public void run() {
                doUpdate();
            }
        });
    }

    @Override
    protected void beforeRemoveFromGUI(GUI gui) {
        delayedAction = null;
        super.beforeRemoveFromGUI(gui);
    }

    @Override
    public void destroy() {
        super.destroy();
        destroyImage();
    }

    @Override
    public int getPreferredInnerWidth() {
        return (image != null) ? image.getWidth() : 0;
    }

    @Override
    public int getPreferredInnerHeight() {
        return (image != null) ? image.getHeight() : 0;
    }

    @Override
    protected void paintWidget(GUI gui) {
        if(image != null) {
            image.draw(getAnimationState(), getInnerX(), getInnerY());
        }
    }

    private void destroyImage() {
        if(image != null) {
            image.destroy();
            image = null;
        }
    }

    private ByteBuffer getBuffer(int size) {
        if(buffer == null || buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size);
        }
        buffer.clear().limit(size);
        return buffer;
    }

    void updateImage(FontGenerator fontGen) {
        this.lastFontGen = fontGen;
        int width = fontGen.getImageWidth();
        if(image == null || image.getWidth() != width) {
            destroyImage();
            GUI gui = getGUI();
            if(gui != null) {
                // the size of the FontGenerator is always square - see doUpdate
                image = gui.getRenderer().createDynamicImage(width, width);
            }
        }
        if(image != null) {
            try {
                ByteBuffer bb = getBuffer(textureSize * textureSize * 4);
                IntBuffer ib = bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
                fontGen.getTextureData(ib);
                image.update(bb, DynamicImage.Format.BGRA);
            } catch (Throwable ex) {
                Logger.getLogger(FontDisplay.class.getName()).log(Level.SEVERE, "Unable to update image", ex);
            }
            invalidateLayout();
        }
        updateDone();
    }

    void updateDone() {
        updateRunning = false;
        if(pendingUpdate) {
            pendingUpdate = false;
            update();
        }
        callback.run();
    }

    final class GenFont implements Callable<FontGenerator> {
        private final int textureSize;
        private final FontGenerator fontGen;
        private final Padding padding;
        private final CharSet charSet;
        private final Effect.Renderer[] effects;
        private final boolean useAA;

        public GenFont(int textureSize, FontData fontData, Padding padding, CharSet charSet,
                Effect.Renderer[] effects, boolean useAA, FontGenerator.GeneratorMethod generatorMethod) {
            this.textureSize = textureSize;
            this.fontGen = new FontGenerator(fontData, generatorMethod);
            this.padding = padding;
            this.charSet = charSet;
            this.effects = effects;
            this.useAA = useAA;
        }

        public FontGenerator call() throws Exception {
            fontGen.generate(textureSize, textureSize, charSet, padding, effects, useAA);
            return fontGen;
        }
    }
}
