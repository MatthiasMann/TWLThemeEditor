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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.AnimationState;
import de.matthiasmann.twl.Color;
import de.matthiasmann.twl.Container;
import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.DraggableButton.DragListener;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Menu;
import de.matthiasmann.twl.ScrollPane;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.ValueAdjusterFloat;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.FloatModel;
import de.matthiasmann.twl.model.IntegerModel;
import de.matthiasmann.twl.model.PersistentBooleanModel;
import de.matthiasmann.twl.model.Property;
import de.matthiasmann.twl.model.SimpleBooleanModel;
import de.matthiasmann.twl.model.SimpleFloatModel;
import de.matthiasmann.twl.model.SimpleProperty;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twlthemeeditor.datamodel.ExtRect;
import de.matthiasmann.twlthemeeditor.gui.CollapsiblePanel.Direction;
import de.matthiasmann.twlthemeeditor.gui.TextureViewer.TextureLoadedListener;
import de.matthiasmann.twlthemeeditor.gui.editors.AnimStateEditorFactory;
import java.net.URL;
import java.util.prefs.Preferences;

/**
 *
 * @author Matthias Mann
 */
public final class TextureViewerPane extends DialogLayout {

    public static final StateKey STATE_ERROR                  = StateKey.get("error");
    public static final StateKey STATE_ANIMATED_POSITION_BARS = StateKey.get("animatedPositionBars");

    private final TextureViewer textureViewer;
    private final ScrollPane scrollPane;
    private final Label labelErrorDisplay;
    private final ToggleButton btnShowCompleteTexture;
    private final ToggleButton btnShowSplitPositions;
    private final Label mousePositionDisplay;
    private final Container propertyContainer;
    private final Runnable rectChangeCB;
    
    private final SimpleFloatModel zoomFactorX;
    private final SimpleFloatModel zoomFactorY;
    private final SimpleBooleanModel linkZoomFactors;
    private final SimpleBooleanModel showCompleteTexture;
    private final SimpleBooleanModel showSplitPositions;
    private final PersistentBooleanModel animatedPositionBars;

    private CollapsiblePanel propertiesCollapsePanel;
    
    private URL url;
    private Property<ExtRect> rectProperty;
    private FloatModel[] splitPositionsX;
    private FloatModel[] splitPositionsY;
    private boolean showMousePosition;

    public TextureViewerPane() {
        this.textureViewer = new TextureViewer();
        this.scrollPane = new ScrollPane(textureViewer);
        this.labelErrorDisplay = new Label();
        
        mousePositionDisplay = new Label();
        mousePositionDisplay.setTheme("mousePositionDisplay");
        
        propertyContainer = new Container();
        propertyContainer.setTheme("");
        
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
                .addGroup(createSequentialGroup(scrollPane, propertyContainer))
                .addGroup(horzCtrls)
                .addWidget(labelErrorDisplay));
        setVerticalGroup(createSequentialGroup()
                .addGroup(createParallelGroup(scrollPane, propertyContainer))
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
                updateBars();
                scrollToRect();
            }
        });
        showSplitPositions.addCallback(new Runnable() {
            public void run() {
                updateBars();
            }
        });
        textureViewer.addExceptionCallback(new Runnable() {
            public void run() {
                Throwable ex = textureViewer.getLoadException();
                labelErrorDisplay.setText((ex != null) ? ex.getMessage() : "");
                labelErrorDisplay.getAnimationState().setAnimationState(STATE_ERROR, ex != null);
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
        rectChangeCB = new Runnable() {
            public void run() {
                updateRect();
            }
        };

        updateZoom();
        updateAnimatedPositionBars();
    }

    public void addSettingsMenuItems(Menu settingsMenu) {
        settingsMenu.add("Animate split positions", animatedPositionBars);
    }

    public void setTextureLoadedListener(TextureLoadedListener textureLoadedListener) {
        textureViewer.setTextureLoadedListener(textureLoadedListener);
    }
    
    public void setContext(Context ctx) {
        propertyContainer.removeAllChildren();
        
        if(ctx != null) {
            SimpleProperty<AnimationState> asProperty = new SimpleProperty<AnimationState>
                    (AnimationState.class, "Animation state", textureViewer.getTextureAnimationState());
            AnimStateEditorFactory animStateEditorFactory = new AnimStateEditorFactory(ctx);
            Widget content = animStateEditorFactory.create(asProperty, null);
            Label title = new Label(asProperty.getName());
            title.setTheme("title");
            title.setLabelFor(content);
            DialogLayout l = new DialogLayout();
            l.setTheme("layout");
            l.setHorizontalGroup(l.createParallelGroup(title, content));
            l.setVerticalGroup(l.createSequentialGroup(title, content).addGap());
            propertiesCollapsePanel = new CollapsiblePanel(Direction.HORIZONTAL, null, l, null);
            propertyContainer.add(propertiesCollapsePanel);
        } else {
            propertiesCollapsePanel = null;
        }
    }

    public void setImage(URL url, Property<ExtRect> rectProperty) {
        this.url = url;
        this.rectProperty = rectProperty;
        this.showMousePosition = true;
        btnShowCompleteTexture.setEnabled(rectProperty != null);
        mousePositionDisplay.setText("");
        updateRect();
        if(propertiesCollapsePanel != null) {
            propertiesCollapsePanel.setExpanded(false);
        }
    }

    public void setImage(Image image) {
        this.url = null;
        this.rectProperty = null;
        this.showMousePosition = false;
        btnShowCompleteTexture.setEnabled(false);
        mousePositionDisplay.setText("Preview");
        textureViewer.setImage(image);
        if(propertiesCollapsePanel != null) {
            propertiesCollapsePanel.setExpanded(image != null);
        }
    }

    public void scrollToRect() {
        if(rectProperty != null && showCompleteTexture.getValue()) {
            ExtRect rect = rectProperty.getPropertyValue();
            textureViewer.validateImage();
            scrollPane.validateLayout();
            scrollPane.setScrollPositionX((int)(rect.getCenterX() * zoomFactorX.getValue())
                    + textureViewer.getBorderLeft() - scrollPane.getInnerWidth()/2);
            scrollPane.setScrollPositionY((int)(rect.getCenterY() * zoomFactorY.getValue())
                    + textureViewer.getBorderTop()- scrollPane.getInnerHeight()/2);
        }
    }

    public void setSplitPositions(IntegerModel[] splitPositionsX, IntegerModel[] splitPositionsY) {
        this.splitPositionsX = asFloatModels(splitPositionsX);
        this.splitPositionsY = asFloatModels(splitPositionsY);
        updateBars();
    }

    public void setSplitPositions(FloatModel[] splitPositionsX, FloatModel[] splitPositionsY) {
        this.splitPositionsX = splitPositionsX;
        this.splitPositionsY = splitPositionsY;
        updateBars();
    }
    
    private static FloatModel[] asFloatModels(IntegerModel[] im) {
        if(im != null) {
            FloatModel[] fm = new FloatModel[im.length];
            for(int i=0,n=im.length ; i<n ; i++) {
                fm[i] = new I2F(im[i]);
            }
            return fm;
        }
        return null;
    }

    public void setTintColor(Color tintColor) {
        textureViewer.setTintColor(tintColor);
    }

    void updateZoom() {
        textureViewer.setZoom(zoomFactorX.getValue(), zoomFactorY.getValue());
    }

    void updateRect() {
        if(url != null) {
            if(showCompleteTexture.getValue() || rectProperty == null) {
                textureViewer.setImage(url, null);
            } else {
                textureViewer.setImage(url, rectProperty.getPropertyValue());
            }
        }
    }
    
    void updateBars() {
        if(rectProperty != null) {
            if(showSplitPositions.getValue()) {
                if(showCompleteTexture.getValue()) {
                    textureViewer.setPositionBars(
                            addEdges(splitPositionsY, new EdgeTop(rectProperty), new EdgeBottom(rectProperty)),
                            addEdges(splitPositionsX, new EdgeLeft(rectProperty), new EdgeRight(rectProperty)));
                } else {
                    textureViewer.setPositionBars(splitPositionsY, splitPositionsX);
                }
            } else if(showCompleteTexture.getValue()) {
                textureViewer.setPositionBars(
                        new FloatModel[] { new I2F(new EdgeTop(rectProperty)), new I2F(new EdgeBottom(rectProperty)) },
                        new FloatModel[] { new I2F(new EdgeLeft(rectProperty)), new I2F(new EdgeRight(rectProperty)) });
            } else {
                textureViewer.setPositionBars(null, null);
            }
        } else if(showSplitPositions.getValue()) {
            textureViewer.setPositionBars(splitPositionsY, splitPositionsX);
        } else {
            textureViewer.setPositionBars(null, null);
        }
    }

    private FloatModel[] addEdges(FloatModel[] splits, IntegerModel start, IntegerModel end) {
        return addEdges(splits, new I2F(start), new I2F(end));
    }
    private FloatModel[] addEdges(FloatModel[] splits, FloatModel start, FloatModel end) {
        int count = (splits != null) ? splits.length : 0;
        FloatModel[] result = new FloatModel[count + 2];
        result[0] = start;
        for(int i=0 ; i<count ; i++) {
            result[i+1] = new OffsetM(splits[i], start);
        }
        result[result.length-1] = end;
        return result;
    }

    void updateAnimatedPositionBars() {
        textureViewer.getAnimationState().setAnimationState(
                STATE_ANIMATED_POSITION_BARS, animatedPositionBars.getValue());
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
    
    static final class EdgeLeft extends ExtRect.IM {
        EdgeLeft(Property<ExtRect> property) {
            super(property);
        }
        public int getMaxValue() {
            return getLimit().getX();
        }
        public int getValue() {
            return getRect().x;
        }
        public void setValue(int value) {
            ExtRect rect = getRect();
            setRect(value, rect.y, Math.max(0, rect.getRight()-value), rect.height, false, rect.flipX, rect.flipY);
        }
    }
    static final class EdgeTop extends ExtRect.IM {
        EdgeTop(Property<ExtRect> property) {
            super(property);
        }
        public int getMaxValue() {
            return getLimit().getY();
        }
        public int getValue() {
            return getRect().y;
        }
        public void setValue(int value) {
            ExtRect rect = getRect();
            setRect(rect.x, value, rect.width, Math.max(0, rect.getBottom()-value), false, rect.flipX, rect.flipY);
        }
    }
    static final class EdgeRight extends ExtRect.IM {
        EdgeRight(Property<ExtRect> property) {
            super(property);
        }
        public int getMaxValue() {
            return getLimit().getX();
        }
        public int getValue() {
            return getRect().getRight();
        }
        public void setValue(int value) {
            ExtRect rect = getRect();
            setRect(Math.min(value, rect.x), rect.y, Math.max(0, value-rect.x), rect.height, false, rect.flipX, rect.flipY);
        }
    }
    static final class EdgeBottom extends ExtRect.IM {
        EdgeBottom(Property<ExtRect> property) {
            super(property);
        }
        public int getMaxValue() {
            return getLimit().getY();
        }
        public int getValue() {
            return getRect().getBottom();
        }
        public void setValue(int value) {
            ExtRect rect = getRect();
            setRect(rect.x, Math.min(value, rect.y), rect.width, Math.max(0, value-rect.y), false, rect.flipX, rect.flipY);
        }
    }
    
    static final class I2F implements FloatModel {
        private final IntegerModel im;

        I2F(IntegerModel im) {
            this.im = im;
        }
        public float getMinValue() {
            return im.getMinValue();
        }
        public float getMaxValue() {
            return im.getMaxValue();
        }
        public float getValue() {
            return im.getValue();
        }
        public void setValue(float value) {
            int iValue = Math.round(value);
            im.setValue(Math.max(Math.min(iValue, im.getMaxValue()), im.getMinValue()));
        }
        public void addCallback(Runnable cb) {
            im.addCallback(cb);
        }
        public void removeCallback(Runnable cb) {
            im.removeCallback(cb);
        }
    }
    static final class OffsetM implements FloatModel {
        private final FloatModel base;
        private final FloatModel offset;

        OffsetM(FloatModel base, FloatModel offset) {
            this.base = base;
            this.offset = offset;
        }
        public float getMinValue() {
            return base.getMinValue() + offset.getValue();
        }
        public float getMaxValue() {
            return base.getMaxValue() + offset.getValue();
        }
        public float getValue() {
            return base.getValue() + offset.getValue();
        }
        public void setValue(float value) {
            base.setValue(value - offset.getValue());
        }
        public void addCallback(Runnable cb) {
            base.addCallback(cb);
            offset.addCallback(cb);
        }
        public void removeCallback(Runnable cb) {
            base.removeCallback(cb);
            offset.removeCallback(cb);
        }
    }
}
