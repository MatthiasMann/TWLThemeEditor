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
package de.matthiasmann.twlthemeeditor.datamodel.images;

import de.matthiasmann.twl.Dimension;
import de.matthiasmann.twl.Rect;
import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.HotSpot;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.Image.BaseProperties;
import de.matthiasmann.twlthemeeditor.datamodel.Textures;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Cursor extends Image {

    public Cursor(Textures textures, TreeTableNode parent, Element node) {
        super(textures, parent, node);
        this.properties = new CursorProperties(textures, node);
    }

    @Override
    public Kind getKind() {
        return Kind.CURSOR;
    }

    public class CursorProperties extends BaseProperties {

        public CursorProperties(Textures textures, Element node) {
            super(textures, node);
        }

        public Rect getRect() {
            return new Rect(
                    parseIntFromAttribute("x"),
                    parseIntFromAttribute("y"),
                    parseIntFromAttribute("width"),
                    parseIntFromAttribute("height"));
        }

        public Dimension getRectLimit() {
            return textures.getTextureDimensions();
        }

        public void setRect(Rect rect) {
            setAttribute("x", rect.getX());
            setAttribute("y", rect.getY());
            setAttribute("width", rect.getWidth());
            setAttribute("height", rect.getHeight());
        }

        public HotSpot getHotSpot() {
            return new HotSpot(
                    parseIntFromAttribute("hotSpotX"),
                    parseIntFromAttribute("hotSpotY"));
        }

        public Dimension getHotSpotLimit() {
            return getRect().getSize();
        }

        public void setHotSpot(HotSpot hotspot) {
            setAttribute("hotSpotX", hotspot.getX());
            setAttribute("hotSpotY", hotspot.getY());
        }
    }
}
