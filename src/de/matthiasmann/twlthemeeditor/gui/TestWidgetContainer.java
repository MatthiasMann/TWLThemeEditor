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

import de.matthiasmann.twl.DesktopArea;
import de.matthiasmann.twl.Widget;

/**
 *
 * @author Matthias Mann
 */
public class TestWidgetContainer extends DesktopArea {
    
    public TestWidgetContainer() {
        setTheme("");
    }

    @Override
    protected void restrictChildrenToInnerArea() {
        final int top = getInnerY();
        final int left = getInnerX();
        final int right = getInnerRight();
        final int bottom = getInnerBottom();
        final int width = Math.max(0, right-left);
        final int height = Math.max(0, bottom-top);

        for(int i=0,n=getNumChildren() ; i<n ; i++) {
            Widget w = getChild(i);
            int minWidth = w.getMinWidth();
            int minHeight = w.getMinHeight();
            w.setSize(
                    Math.min(Math.max(width, minWidth), Math.max(w.getWidth(), minWidth)),
                    Math.min(Math.max(height, minHeight), Math.max(w.getHeight(), minHeight)));
            w.setPosition(
                    Math.max(left, Math.min(right - w.getWidth(), w.getX())),
                    Math.max(top, Math.min(bottom - w.getHeight(), w.getY())));
        }
    }
}
