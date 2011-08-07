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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Timer;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;
import de.matthiasmann.twl.model.BooleanModel;
import de.matthiasmann.twl.renderer.AnimationState.StateKey;

/**
 *
 * @author Matthias Mann
 */
public final class CollapsiblePanel extends DialogLayout {

    public enum Direction {
        HORIZONTAL,
        VERTICAL
    }
    
    public static final StateKey STATE_EXPANDED = StateKey.get("expanded");

    private final Direction direction;
    private final Arrow arrow;
    private final ContentContainer container;

    private boolean expanded;

    public CollapsiblePanel(Direction direction, String title, Widget content, final BooleanModel activeModel) {
        this.direction = direction;
        this.arrow = new Arrow();
        this.container = new ContentContainer(content);

        Group horzTitle, vertTitle;
        
        if(direction == Direction.VERTICAL) {
            horzTitle = createSequentialGroup();
            vertTitle = createParallelGroup();
        } else {
            horzTitle = createParallelGroup();
            vertTitle = createSequentialGroup();
        }

        horzTitle.addWidget(arrow);
        vertTitle.addWidget(arrow);

        if(activeModel != null) {
            ToggleButton btnActive = new ToggleButton(activeModel);
            btnActive.setTheme("active");

            horzTitle.addWidget(btnActive);
            vertTitle.addWidget(btnActive);
        }

        if(title != null && title.length() > 0) {
            Label titleLabel = new Label(title);
            titleLabel.setTheme("title");
            titleLabel.setLabelFor(content);

            horzTitle.addWidget(titleLabel);
            vertTitle.addWidget(titleLabel);
        }

        if(direction == Direction.VERTICAL) {
            horzTitle.addGap();

            setHorizontalGroup(createParallelGroup().addGroup(horzTitle).addWidget(container));
            setVerticalGroup(createSequentialGroup().addGroup(vertTitle).addGap("title-content").addWidget(container));
        } else {
            setHorizontalGroup(createSequentialGroup().addGroup(horzTitle).addGap("title-content").addWidget(container));
            setVerticalGroup(createParallelGroup().addGroup(vertTitle).addWidget(container));
        }

        if(activeModel == null || activeModel.getValue()) {
            toggleExpand();
        } else {
            container.setEnabled(false);
        }

        if(activeModel != null) {
            activeModel.addCallback(new Runnable() {
                public void run() {
                    if(activeModel.getValue() && !expanded) {
                        toggleExpand();
                    }
                }
            });
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if(this.expanded != expanded) {
            toggleExpand();
        }
    }
    
    public Widget getContent() {
        return container.content;
    }

    @Override
    protected void widgetDisabled() {
        super.widgetDisabled();
        setExpanded(false);
    }

    void toggleExpand() {
        expanded ^= true;
        arrow.getAnimationState().setAnimationState(STATE_EXPANDED, expanded);
        container.startAnimate();
        container.setEnabled(expanded);
    }

    class Arrow extends Label {
        @Override
        public boolean handleEvent(Event evt) {
            if(evt.getType() == Event.Type.MOUSE_CLICKED && evt.getMouseClickCount() == 1) {
                toggleExpand();
                return true;
            }
            return evt.isMouseEventNoWheel();
        }
    }

    private static final int STEP = 2;
    private static final int MAX_SPEED = 25;
    private static final int DELAY = 20;
    
    class ContentContainer extends Widget implements Runnable {
        final Widget content;
        private Timer timer;
        private int prefInnerSize = -1;
        private int speed;

        public ContentContainer(Widget content) {
            this.content = content;
            add(content);
            setClip(true);
        }

        @Override
        protected void afterAddToGUI(GUI gui) {
            super.afterAddToGUI(gui);
            timer = gui.createTimer();
            timer.setContinuous(true);
            timer.setDelay(DELAY);
            timer.setCallback(this);
        }

        @Override
        protected void beforeRemoveFromGUI(GUI gui) {
            timer.stop();
            timer = null;
            super.beforeRemoveFromGUI(gui);
        }

        @Override
        public int getMinWidth() {
            int minWidth = super.getMinWidth();
            if(direction == Direction.VERTICAL) {
                minWidth = Math.max(minWidth, getBorderHorizontal() + content.getMinWidth());
            }
            return minWidth;
        }

        @Override
        public int getMinHeight() {
            int minHeight = super.getMinHeight();
            if(direction == Direction.HORIZONTAL) {
                minHeight = Math.max(minHeight, getBorderVertical() + content.getMinHeight());
            }
            return minHeight;
        }

        @Override
        public int getPreferredInnerWidth() {
            if(direction == Direction.VERTICAL) {
                return content.getPreferredWidth();
            }
            if(prefInnerSize < 0) {
                prefInnerSize = computePreferredInnerSize();
            }
            return prefInnerSize;
        }

        @Override
        public int getPreferredInnerHeight() {
            if(direction == Direction.HORIZONTAL) {
                return content.getPreferredHeight();
            }
            if(prefInnerSize < 0) {
                prefInnerSize = computePreferredInnerSize();
            }
            return prefInnerSize;
        }

        public void run() {
            int pref = computePreferredInnerSize();
            if(pref == prefInnerSize) {
                timer.stop();
            } else if(prefInnerSize < pref) {
                prefInnerSize = Math.min(prefInnerSize + speed, pref);
            } else {
                prefInnerSize = Math.max(prefInnerSize - speed, pref);
            }
            speed = Math.min(speed + STEP, MAX_SPEED);
            checkSize();
        }

        private int computePreferredInnerSize() {
            if(!expanded) {
                return 0;
            }
            if(direction == Direction.VERTICAL) {
                return computeSize(0, content.getPreferredHeight(), content.getMaxHeight());
            } else {
                return computeSize(0, content.getPreferredWidth(), content.getMaxWidth());
            }
        }

        private void checkSize() {
            int size = (direction == Direction.VERTICAL) ? getInnerHeight() : getInnerWidth();
            if(size != prefInnerSize) {
                super.invalidateLayout();
            }
        }

        @Override
        protected void paintWidget(GUI gui) {
            checkSize();
        }

        @Override
        protected void childInvalidateLayout(Widget child) {
            startAnimate();
        }

        void startAnimate() {
            if(timer != null && !timer.isRunning()) {
                speed = STEP;
                timer.start();
            }
        }

        @Override
        protected void layout() {
            layoutChildFullInnerArea(content);
        }
    }
}
