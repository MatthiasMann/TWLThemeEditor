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
package de.matthiasmann.twlthemeeditor.imgconv;

import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.renderer.DynamicImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthias Mann
 */
public class ImageDisplay extends Widget {

    private final Executor executor;
    private final Runnable callback;

    private ImageData imageData;
    private boolean skipZeroDelayFrames;

    private boolean pendingUpdate;
    private boolean updateRunning;
    private ByteBuffer buffer;
    private DynamicImage image;
    private ImageGenerator lastImageGen;

    public ImageDisplay(Runnable callback) {
        this.executor = Executors.newSingleThreadExecutor();
        this.callback = callback;
    }

    public void setImageData(ImageData imageData) {
        this.imageData = imageData;
        update();
    }

    public void setSkipZeroDelayFrames(boolean skipZeroDelayFrames) {
        this.skipZeroDelayFrames = skipZeroDelayFrames;
        update();
    }


    public ImageGenerator getLastImageGen() {
        return lastImageGen;
    }

    private void update() {
        GUI gui = getGUI();
        if(gui != null && imageData != null) {
            if(updateRunning) {
                pendingUpdate = true;
            } else {
                executor.execute(new GenImage(gui, imageData, skipZeroDelayFrames));
                updateRunning = true;
            }
        }
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

    void updateImage(ImageGenerator imageGen) {
        this.lastImageGen = imageGen;
        if(image == null || image.getWidth() != imageGen.getWidth() || image.getHeight() != imageGen.getHeight()) {
            destroyImage();
            GUI gui = getGUI();
            if(gui != null) {
                image = gui.getRenderer().createDynamicImage(imageGen.getWidth(), imageGen.getHeight());
            }
        }
        if(image != null) {
            try {
                ByteBuffer bb = getBuffer(imageGen.getWidth() * imageGen.getHeight() * 4);
                IntBuffer ib = bb.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
                imageGen.getTextureData(ib);
                image.update(bb, DynamicImage.Format.BGRA);
            } catch (Throwable ex) {
                Logger.getLogger(ImageDisplay.class.getName()).log(Level.SEVERE, "Unable to update image", ex);
            }
            invalidateLayout();
        }
    }

    void updateDone() {
        updateRunning = false;
        if(pendingUpdate) {
            pendingUpdate = false;
            update();
        }
        callback.run();
    }

    final class GenImage implements Runnable {
        private final GUI gui;
        private final ImageData imageData;
        private final boolean skipZeroDelayFrames;

        public GenImage(GUI gui, ImageData imageData, boolean skipZeroDelayFrames) {
            this.gui = gui;
            this.imageData = imageData;
            this.skipZeroDelayFrames = skipZeroDelayFrames;
        }

        public void run() {
            UploadImage result;

            try {
                ImageGenerator imageGen = new ImageGenerator(imageData, skipZeroDelayFrames);
                result = new UploadImage(imageGen);
            } catch(Throwable ignore) {
                result = new UploadImage(null);
            }

            gui.invokeLater(result);
        }
    }

    class UploadImage implements Runnable {
        private final ImageGenerator imageGen;

        public UploadImage(ImageGenerator imageGen) {
            this.imageGen = imageGen;
        }

        public void run() {
            if(imageGen != null) {
                updateImage(imageGen);
            }
            updateDone();
        }
    }
}
