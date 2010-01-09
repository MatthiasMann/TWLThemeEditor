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

import de.matthiasmann.twl.DialogLayout;
import de.matthiasmann.twl.Event;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.Label;
import de.matthiasmann.twl.Timer;
import de.matthiasmann.twl.ToggleButton;
import de.matthiasmann.twl.Widget;

/**
 *
 * @author Matthias Mann
 */
public class CollapsiblePanel extends DialogLayout {

    public static final String STATE_EXPANDED = "expanded";
    
    private final Arrow arrow;
    private final ContentContainer container;

    private boolean expanded;

    public CollapsiblePanel(String title, Widget content, final ToggleButton enabled) {
        this.arrow = new Arrow();
        this.container = new ContentContainer(content);

        Label titleLabel = new Label(title);
        titleLabel.setTheme("title");
        titleLabel.setLabelFor(content);

        Group horzTitle = createSequentialGroup().addWidget(arrow);
        Group vertTitle = createParallelGroup().addWidget(arrow);
        if(enabled != null) {
            horzTitle.addWidget(enabled);
            vertTitle.addWidget(enabled);
        }
        horzTitle.addWidget(titleLabel).addGap();
        vertTitle.addWidget(titleLabel);

        setHorizontalGroup(createParallelGroup().addGroup(horzTitle).addWidget(container));
        setVerticalGroup(createSequentialGroup().addGroup(vertTitle).addWidget(container));

        if(enabled == null || enabled.isActive()) {
            toggleExpand();
        }

        if(enabled != null) {
            enabled.addCallback(new Runnable() {
                public void run() {
                    if(enabled.isActive() && !expanded) {
                        toggleExpand();
                    }
                }
            });
        }
    }

    void toggleExpand() {
        expanded ^= true;
        arrow.getAnimationState().setAnimationState(STATE_EXPANDED, expanded);
        container.startAnimate();
    }

    class Arrow extends Widget {
        @Override
        protected boolean handleEvent(Event evt) {
            if(evt.getType() == Event.Type.MOUSE_CLICKED && evt.getMouseClickCount() == 1) {
                toggleExpand();
                return true;
            }
            return evt.isMouseEvent() && evt.getType() != Event.Type.MOUSE_WHEEL;
        }
    }

    private static final int STEP = 2;
    private static final int MAX_SPEED = 25;
    private static final int DELAY = 20;
    
    class ContentContainer extends Widget implements Runnable {
        private final Widget content;
        private Timer timer;
        private int prefInnerHeight = -1;
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
        public int getPreferredInnerHeight() {
            if(prefInnerHeight < 0) {
                prefInnerHeight = computPrefferedInnerHeight();
            }
            return prefInnerHeight;
        }

        public void run() {
            int pref = computPrefferedInnerHeight();
            if(pref == prefInnerHeight) {
                timer.stop();
            } else if(prefInnerHeight < pref) {
                prefInnerHeight = Math.min(prefInnerHeight + speed, pref);
            } else {
                prefInnerHeight = Math.max(prefInnerHeight - speed, pref);
            }
            speed = Math.min(speed + STEP, MAX_SPEED);
            checkSize();
        }

        private int computPrefferedInnerHeight() {
            return expanded ? content.getPreferredHeight() : 0;
        }

        private void checkSize() {
            if(getInnerHeight() != prefInnerHeight) {
                invalidateLayoutTree();
            }
        }

        @Override
        protected void paintWidget(GUI gui) {
            checkSize();
        }

        @Override
        protected void childChangedSize(Widget child) {
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
