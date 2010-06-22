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
import de.matthiasmann.twl.GUI;
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
import de.matthiasmann.twlthemeeditor.gui.TextureViewer.PositionBarDragListener;
import java.net.URL;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public class TextureViewerPane extends DialogLayout {

    public interface Listener {
        public void dragEdgeTop(int y);
        public void dragEdgeLeft(int x);
        public void dragEdgeRight(int x);
        public void dragEdgeBottom(int y);
        public void dragSplitX(int idx, int x);
        public void dragSplitY(int idx, int y);
    }

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

    private static final int[] EMPTY_INT_ARRAY = {};
    
    private Rect rect;
    private int[] splitPositionsX = EMPTY_INT_ARRAY;
    private int[] splitPositionsY = EMPTY_INT_ARRAY;
    private boolean showMousePosition;

    private Listener listener;

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
        showCompleteTexture.addCallback(new Runnable() {
            public void run() {
                updateRect();
                scrollToRect();
            }
        });
        showSplitPositions.addCallback(new Runnable() {
            public void run() {
                updateRect();
            }
        });
        textureViewer.addExceptionCallback(new Runnable() {
            public void run() {
                Throwable ex = textureViewer.getLoadException();
                labelErrorDisplay.setText((ex != null) ? ex.getMessage() : "");
                labelErrorDisplay.getAnimationState().setAnimationState("error", ex != null);
            }
        });
        textureViewer.setImageDragListener(new DragListener() {
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
        textureViewer.setPositionBarDragListener(new PositionBarDragHandler());
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

    public void setListener(Listener listener) {
        this.listener = listener;
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

    public void scrollToRect() {
        if(rect != null && showCompleteTexture.getValue()) {
            textureViewer.validateImage();
            scrollPane.validateLayout();
            scrollPane.setScrollPositionX((int)(rect.getCenterX() * zoomFactorX.getValue())
                    + textureViewer.getBorderLeft() - scrollPane.getInnerWidth()/2);
            scrollPane.setScrollPositionY((int)(rect.getCenterY() * zoomFactorY.getValue())
                    + textureViewer.getBorderTop()- scrollPane.getInnerHeight()/2);
        }
    }

    public void setSplitPositionsX(int[] splitPositionsX) {
        if(splitPositionsX == null) {
            this.splitPositionsX = EMPTY_INT_ARRAY;
        } else {
            this.splitPositionsX = splitPositionsX;
        }
        updateRect();
    }

    public void setSplitPositionsY(int[] splitPositionsY) {
        if(splitPositionsY == null) {
            this.splitPositionsY = EMPTY_INT_ARRAY;
        } else {
            this.splitPositionsY = splitPositionsY;
        }
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
        } else if(rect != null && showCompleteTexture.getValue()) {
            textureViewer.setPositionBarsVert(new int[]{ rect.getX(), rect.getRight() });
            textureViewer.setPositionBarsHorz(new int[]{ rect.getY(), rect.getBottom() });
        } else {
            textureViewer.setPositionBarsVert(null);
            textureViewer.setPositionBarsHorz(null);
        }
    }

    private int[] addEdges(int[] splits, int start, int end) {
        int[] result = new int[splits.length + 2];
        result[0] = start;
        for(int i=0 ; i<splits.length ; i++) {
            result[i+1] = start + splits[i];
        }
        result[result.length-1] = end;
        return result;
    }

    void updateAnimatedPositionBars() {
        textureViewer.getAnimationState().setAnimationState("animatedPositionBars",
                animatedPositionBars.getValue());
    }

    @Override
    protected void paint(GUI gui) {
        super.paint(gui);
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

    static abstract class DragHandlerAxis {
        int start;
        abstract void drag(int pos);
    }
    class DragHandlerTop extends DragHandlerAxis {
        public DragHandlerTop() {
            start = rect.getY();
        }
        @Override
        void drag(int pos) {
            listener.dragEdgeTop(pos);
        }
    }
    class DragHandlerLeft extends DragHandlerAxis {
        public DragHandlerLeft() {
            start = rect.getX();
        }
        @Override
        void drag(int pos) {
            listener.dragEdgeLeft(pos);
        }
    }
    class DragHandlerRight extends DragHandlerAxis {
        public DragHandlerRight() {
            start = rect.getRight();
        }
        @Override
        void drag(int pos) {
            listener.dragEdgeRight(pos);
        }
    }
    class DragHandlerBottom extends DragHandlerAxis {
        public DragHandlerBottom() {
            start = rect.getBottom();
        }
        @Override
        void drag(int pos) {
            listener.dragEdgeBottom(pos);
        }
    }
    class DragHandlerSplitX extends DragHandlerAxis {
        final int idx;
        public DragHandlerSplitX(int idx) {
            this.idx = idx;
            this.start = splitPositionsX[idx];
        }
        @Override
        void drag(int pos) {
            listener.dragSplitX(idx, pos);
        }
    }
    class DragHandlerSplitY extends DragHandlerAxis {
        final int idx;
        public DragHandlerSplitY(int idx) {
            this.idx = idx;
            this.start = splitPositionsY[idx];
        }
        @Override
        void drag(int pos) {
            listener.dragSplitY(idx, pos);
        }
    }

    class PositionBarDragHandler implements PositionBarDragListener {
        DragHandlerAxis axisX;
        DragHandlerAxis axisY;

        public void dragStarted(int posBarHorz, int posBarVert) {
            if(rect != null) {
                boolean all = showCompleteTexture.getValue();
                boolean showSplits = showSplitPositions.getValue();

                axisX = null;
                axisY = null;

                if(all) {
                    if(showSplits) {
                        if(posBarVert == 0) {
                            axisX = new DragHandlerLeft();
                        } else if(posBarVert == splitPositionsX.length + 1) {
                            axisX = new DragHandlerRight();
                        } else if(posBarVert > 0) {
                            axisX = new DragHandlerSplitX(posBarVert - 1);
                        }
                        if(posBarHorz == 0) {
                            axisY = new DragHandlerTop();
                        } else if(posBarHorz == splitPositionsY.length + 1) {
                            axisY = new DragHandlerBottom();
                        } else if(posBarHorz > 0) {
                            axisY = new DragHandlerSplitY(posBarHorz - 1);
                        }
                    } else {
                        if(posBarVert == 0) {
                            axisX = new DragHandlerLeft();
                        } else if(posBarVert == 1) {
                            axisX = new DragHandlerRight();
                        }
                        if(posBarHorz == 0) {
                            axisY = new DragHandlerTop();
                        } else if(posBarHorz == 1) {
                            axisY = new DragHandlerBottom();
                        }
                    }
                } else if(showSplits) {
                    if(posBarVert >= 0) {
                        axisX = new DragHandlerSplitX(posBarVert);
                    }
                    if(posBarHorz >= 0) {
                        axisY = new DragHandlerSplitY(posBarHorz);
                    }
                }
            }
        }

        public void dragged(int deltaX, int deltaY) {
            if(listener != null) {
                if(axisX != null) {
                    axisX.drag(axisX.start + deltaX);
                }
                if(axisY != null) {
                    axisY.drag(axisY.start + deltaY);
                }
            }
        }

        public void dragStopped() {
        }
    }
}
