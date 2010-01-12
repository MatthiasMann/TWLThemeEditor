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
import de.matthiasmann.twlthemeeditor.datamodel.DomWrapper;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.Textures;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeFile;
import de.matthiasmann.twlthemeeditor.datamodel.Utils;
import de.matthiasmann.twlthemeeditor.properties.MinValueI;
import de.matthiasmann.twlthemeeditor.properties.Optional;
import java.io.IOException;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Repeat extends WithSubImages {

    Repeat(Textures textures, TreeTableNode parent, Element element) throws IOException {
        super(textures, parent, element);
        this.properties = new RepeatProperties(textures, element);
    }

    @Override
    protected int getRequiredChildren() {
        return Math.max(1, getNumChildren());
    }

    @Override
    public void addChildren() throws IOException {
        Utils.addChildren(textures.getThemeFile(), this, element, new DomWrapper() {
            public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                String tagName = element.getName();

                if("repeat".equals(tagName)) {
                    return new Repeat(textures, parent, element);
                }
                if("frame".equals(tagName)) {
                    return new Frame(textures, parent, element);
                }
                return null;
            }
        });
    }

    public class RepeatProperties extends ImageProperties {

        RepeatProperties(Textures textures, Element node) {
            super(textures, node);
        }

        @Optional
        @MinValueI(0)
        public Integer getRepeatCunt() {
            String value = getAttribute("count");
            return (value != null) ? Integer.valueOf(value) : null;
        }

        public void setRepeatCount(Integer repeatCount) {
            setAttribute("count", (repeatCount != null) ? repeatCount.toString() : null);
        }
    }
}
