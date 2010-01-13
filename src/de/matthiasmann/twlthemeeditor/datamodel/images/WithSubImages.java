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
import de.matthiasmann.twlthemeeditor.datamodel.DomXPPParser;
import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.Textures;
import java.io.IOException;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
abstract class WithSubImages extends Image {

    protected WithSubImages(Textures textures, TreeTableNode parent, Element element) throws IOException {
        super(textures, parent, element);
    }

    protected abstract int getRequiredChildren();
    
    private static final Element ALIAS_REF_NONE = new Element("alias").setAttribute("ref", "none");

    @Override
    @SuppressWarnings("unchecked")
    public void addToXPP(DomXPPParser xpp) {
        xpp.addStartTag(this, element.getName(), element.getAttributes());
        int generated = 0;
        int required = getRequiredChildren();
        for (int i = 0, n = getNumChildren(); i < n && generated < required; i++) {
            TreeTableNode child = getChild(i);
            if (child instanceof ThemeTreeNode) {
                ((ThemeTreeNode) child).addToXPP(xpp);
                generated++;
            }
        }
        for (; generated < required; generated++) {
            xpp.addElement(this, ALIAS_REF_NONE);
        }
        xpp.addEndTag(element.getName());
    }
}
