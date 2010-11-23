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
import de.matthiasmann.twlthemeeditor.datamodel.DomXPPParser;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeTreeNode;
import de.matthiasmann.twlthemeeditor.datamodel.Images;
import de.matthiasmann.twlthemeeditor.datamodel.NameGenerator;
import de.matthiasmann.twlthemeeditor.datamodel.ThemeFile;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateChildOperation;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewSimple;
import de.matthiasmann.twlthemeeditor.datamodel.operations.CreateNewArea;
import de.matthiasmann.twlthemeeditor.properties.AttributeProperty;
import de.matthiasmann.twlthemeeditor.properties.IntegerProperty;
import java.io.IOException;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Repeat extends WithSubImages implements NameGenerator {

    Repeat(Images textures, TreeTableNode parent, Element element) throws IOException {
        super(textures, parent, element);
        addProperty(new IntegerProperty(new AttributeProperty(element, "count", "Count", true), 0, Short.MAX_VALUE));
    }

    @Override
    protected int getRequiredChildren() {
        return Math.max(1, getNumChildren());
    }

    public String generateName(ThemeTreeNode node) {
        int idx = getChildIndex(node);
        return "Frame " + (idx+1);
    }

    @Override
    public void addChildren() throws IOException {
        addChildren(textures.getThemeFile(), element, new DomWrapper() {
            public TreeTableNode wrap(ThemeFile themeFile, ThemeTreeNode parent, Element element) throws IOException {
                String tagName = element.getName();

                if("repeat".equals(tagName)) {
                    return new Repeat(textures, parent, element);
                }
                if("frame".equals(tagName)) {
                    return new Frame(textures, parent, element);
                }
                if("frames".equals(tagName)) {
                    convertToXYWH(themeFile, element);
                    return new Frames(textures, parent, element);
                }
                return null;
            }
        });
    }

    @Override
    protected void addMissingChild(DomXPPParser xpp) {
        xpp.addElement(this, new Element("frame").setAttribute("ref", "none").setAttribute("duration", "100"));
    }

    @Override
    protected void addCreateOperations(List<CreateChildOperation> operations) {
        operations.add(new CreateNewSimple(this, element, "repeat", "count", "1"));
        operations.add(new CreateNewSimple(this, element, "frame", "duration", "100", "ref", "none"));
        operations.add(new CreateNewArea(this, element, "frames", "duration", "100", "count", "2", "offsetx", "10"));
    }

}
