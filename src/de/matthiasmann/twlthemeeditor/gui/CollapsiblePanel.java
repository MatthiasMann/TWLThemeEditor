/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

    public CollapsiblePanel(String title, Widget content, ToggleButton enabled) {
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

        toggleExpand();
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
    private static final int MAX_SPEED = 30;
    private static final int DELAY = 20;
    
    class ContentContainer extends Widget implements Runnable {
        private final Widget content;
        private Timer timer;
        private int prefInnerHeight;
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
