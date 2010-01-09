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

import de.matthiasmann.twl.model.TreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.Split;
import de.matthiasmann.twlthemeeditor.datamodel.Textures;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class HVSplitSimple extends Texture {

    public HVSplitSimple(Textures textures, TreeTableNode parent, Element node) {
        super(textures, parent, node);
        this.properties = new HVSplitSimpleProperties(textures, node);
    }

    public class HVSplitSimpleProperties extends TextureProperties {

        public HVSplitSimpleProperties(Textures textures, Element node) {
            super(textures, node);
        }

        public Split getSplitX() {
            String value = getAttribute("splitx");
            return (value != null) ? new Split(value) : null;
        }

        public int getSplitXLimit() {
            return getRect().getWidth();
        }

        public void setSplitX(Split splitX) {
            setAttribute("splitx", splitX.toString());
        }

        public Split getSplitY() {
            String value = getAttribute("splity");
            return (value != null) ? new Split(value) : null;
        }

        public int getSplitYLimit() {
            return getRect().getHeight();
        }

        public void setSplitY(Split splitY) {
            setAttribute("splity", splitY.toString());
        }
    }
}
