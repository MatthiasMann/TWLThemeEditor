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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.DraggableButton.DragListener;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterFloat;
import de.matthiasmann.twl.model.FloatModel;
import de.matthiasmann.twl.model.PersistentBooleanModel;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twl.model.SimpleFloatModel;
import de.matthiasmann.twl.renderer.Image;
import java.net.URL;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class TextureViewerPane extends DialogLayout {

    private final TextureViewer textureViewer;
    private final ScrollPane scrollPane;
    private final Label labelErrorDisplay;
    private final ToggleButton btnShowCompleteTexture;
    private final ToggleButton btnShowSplitPositions;
    private final Label mousePositionDisplay;
    
    private final SimpleFloatModel zoomFactorX;
    private final SimpleFloatModel zoomFactorY;
    private final SimpleBooleanModel linkZoomFactors;
    private final SimpleBooleanModel showCompleteTexture;
    private final SimpleBooleanModel showSplitPositions;
    private final PersistentBooleanModel animatedPositionBars;

    private Rect rect;
    private int[] splitPositionsX;
    private int[] splitPositionsY;
    private boolean showMousePosition;

    public TextureViewerPane() {
        this.textureViewer = new TextureViewer();
        this.scrollPane = new ScrollPane(textureViewer);
        this.labelErrorDisplay = new Label();
        
        mousePositionDisplay = new Label();
        mousePositionDisplay.setTheme("mousePositionDisplay");
        
        this.zoomFactorX = new SimpleFloatModel(0.1f, 20.0f, 1.0f);
        this.zoomFactorY = new SimpleFloatModel(0.1f, 20.0f, 1.0f);
        this.linkZoomFactors = new SimpleBooleanModel(true);
        this.showCompleteTexture = new SimpleBooleanModel(false);
        this.showSplitPositions = new SimpleBooleanModel(true);
        this.animatedPositionBars = new PersistentBooleanModel(
                Preferences.userNodeForPackage(TextureViewerPane.class),
                "animated-positionbars", true);

        ValueAdjusterFloat adjusterZoomX = new ZoomAdjuster(zoomFactorX);
        adjusterZoomX.setDisplayPrefix("X: ");
        adjusterZoomX.setFormat("%2.1f");
        ValueAdjusterFloat adjusterZoomY = new ZoomAdjuster(zoomFactorY);
        adjusterZoomY.setDisplayPrefix("Y: ");
        adjusterZoomY.setFormat("%2.1f");

        ToggleButton btnLinkZoomFactors = new ToggleButton(linkZoomFactors);
        btnLinkZoomFactors.setTheme("linkZoomFactors");

        btnShowCompleteTexture = new ToggleButton(showCompleteTexture);
        btnShowCompleteTexture.setTheme("showCompleteTexture");

        btnShowSplitPositions = new ToggleButton(showSplitPositions);
        btnShowSplitPositions.setTheme("showSplitPositions");

        Group horzCtrls = createSequentialGroup()
                .addWidget(mousePositionDisplay)
                .addGap()
                .addWidgets(adjusterZoomX, btnLinkZoomFactors, adjusterZoomY)
                .addGap()
                .addWidget(btnShowCompleteTexture)
                .addWidget(btnShowSplitPositions)
                .addGap();
        Group vertCtrls = createParallelGroup(mousePositionDisplay,
                adjusterZoomX, btnLinkZoomFactors,
                adjusterZoomY, btnShowCompleteTexture,
                btnShowSplitPositions);

        setClip(true);
        setHorizontalGroup(createParallelGroup()
                .addWidget(scrollPane)
                .addGroup(horzCtrls)
                .addWidget(labelErrorDisplay));
        setVerticalGroup(createSequentialGroup()
                .addWidget(scrollPane)
                .addGroup(vertCtrls)
                .addWidget(labelErrorDisplay));

        zoomFactorX.addCallback(new Runnable() {
            public void run() {
                if(linkZoomFactors.getValue()) {
                    zoomFactorY.setValue(zoomFactorX.getValue());
                }
                updateZoom();
            }
        });
        zoomFactorY.addCallback(new Runnable() {
            public void run() {
                if(linkZoomFactors.getValue()) {
                    zoomFactorX.setValue(zoomFactorY.getValue());
                }
                updateZoom();
            }
        });
        Runnable updateRectCB = new Runnable() {
            public void run() {
                updateRect();
            }
        };
        showCompleteTexture.addCallback(updateRectCB);
        showSplitPositions.addCallback(updateRectCB);
        textureViewer.addExceptionCallback(new Runnable() {
            public void run() {
                Throwable ex = textureViewer.getLoadException();
                labelErrorDisplay.setText((ex != null) ? ex.getMessage() : "");
                labelErrorDisplay.getAnimationState().setAnimationState("error", ex != null);
            }
        });
        textureViewer.setListener(new DragListener() {
            int scrollStartX;
            int scrollStartY;
            
            public void dragStarted() {
                scrollStartX = scrollPane.getScrollPositionX();
                scrollStartY = scrollPane.getScrollPositionY();
            }
            public void dragged(int deltaX, int deltaY) {
                scrollPane.setScrollPositionX(scrollStartX - deltaX);
                scrollPane.setScrollPositionY(scrollStartY - deltaY);
            }
            public void dragStopped() {
            }
        });
        textureViewer.setMouseOverListener(new TextureViewer.MouseOverListener() {
            public void mousePosition(int x, int y) {
                if(showMousePosition) {
                    mousePositionDisplay.setText("X:"+x+" Y:"+y);
                }
            }
            public void mouseExited() {
                if(showMousePosition) {
                    mousePositionDisplay.setText("");
                }
            }
        });
        animatedPositionBars.addCallback(new Runnable() {
            public void run() {
                updateAnimatedPositionBars();
            }
        });

        updateZoom();
        updateAnimatedPositionBars();
    }

    public void addSettingsMenuItems(Menu settingsMenu) {
        settingsMenu.add("Animate split positions", animatedPositionBars);
    }

    public void setUrl(URL url) {
        textureViewer.setUrl(url);
    }

    public void setRect(Rect rect) {
        this.rect = rect;
        this.showMousePosition = true;
        btnShowCompleteTexture.setEnabled(rect != null);
        mousePositionDisplay.setText("");
        updateRect();
    }

    public void setSplitPositionsX(int[] splitPositionsX) {
        this.splitPositionsX = splitPositionsX;
        updateRect();
    }

    public void setSplitPositionsY(int[] splitPositionsY) {
        this.splitPositionsY = splitPositionsY;
        updateRect();
    }

    public void setImage(Image image) {
        this.rect = null;
        this.showMousePosition = false;
        textureViewer.setImage(image);
        btnShowCompleteTexture.setEnabled(false);
        mousePositionDisplay.setText("Preview");
        updateRect();
    }

    public void setTintColor(Color tintColor) {
        textureViewer.setTintColor(tintColor);
    }

    void updateZoom() {
        textureViewer.setZoom(zoomFactorX.getValue(), zoomFactorY.getValue());
    }

    void updateRect() {
        textureViewer.setRect(showCompleteTexture.getValue() ? null : rect);
        if(rect != null && showSplitPositions.getValue()) {
            if(showCompleteTexture.getValue()) {
                textureViewer.setPositionBarsVert(addEdges(splitPositionsX, rect.getX(), rect.getRight()));
                textureViewer.setPositionBarsHorz(addEdges(splitPositionsY, rect.getY(), rect.getBottom()));
            } else {
                textureViewer.setPositionBarsVert(splitPositionsX);
                textureViewer.setPositionBarsHorz(splitPositionsY);
            }
        } else {
            textureViewer.setPositionBarsVert(null);
            textureViewer.setPositionBarsHorz(null);
        }
    }

    private int[] addEdges(int[] splits, int start, int end) {
        int[] result = new int[(splits != null) ? splits.length+2 : 2];
        result[0] = start;
        if(splits != null) {
            for(int i=0 ; i<splits.length ; i++) {
                result[i+1] = start + splits[i];
            }
        }
        result[result.length-1] = end;
        return result;
    }

    void updateAnimatedPositionBars() {
        textureViewer.getAnimationState().setAnimationState("animatedPositionBars",
                animatedPositionBars.getValue());
    }
    
    static class ZoomAdjuster extends ValueAdjusterFloat {
        public ZoomAdjuster(FloatModel model) {
            super(model);
        }

        @Override
        protected void doDecrement() {
            float value = getValue();
            float stepSize = (value > 1f) ? 1f : 0.1f;

            float floor = (float)Math.floor(value / stepSize) * stepSize;
            setValue((floor < value) ? floor : floor - stepSize);
        }

        @Override
        protected void doIncrement() {
            float value = getValue();
            float stepSize = (value < 1f) ? 0.1f : 1f;

            float ceil = (float)Math.ceil(value / stepSize) * stepSize;
            setValue((ceil > value) ? ceil : ceil + stepSize);
        }
    }
}
