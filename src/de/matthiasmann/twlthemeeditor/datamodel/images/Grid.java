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

import de.matthiasmann.twlthemeeditor.datamodel.Image;
import de.matthiasmann.twlthemeeditor.datamodel.ModifyableTreeTableNode;
import de.matthiasmann.twlthemeeditor.datamodel.NameGenerator;
import de.matthiasmann.twlthemeeditor.datamodel.Textures;
import de.matthiasmann.twlthemeeditor.datamodel.Weights;
import java.io.IOException;
import org.jdom.Element;

/**
 *
 * @author Matthias Mann
 */
public class Grid extends WithSubImages implements NameGenerator {

    public Grid(Textures textures, ModifyableTreeTableNode parent, Element element) throws IOException {
        super(textures, parent, element);
        this.properties = new GridProperties(textures, element);
        addChildImages(textures, this, element);
    }

    @Override
    public GridProperties getProperties() {
        return (GridProperties) properties;
    }

    @Override
    protected int getRequiredChildren() {
        Weights weightsX = getProperties().getWeightsX();
        Weights weightsY = getProperties().getWeightsY();
        if (weightsX == null || weightsY == null) {
            return 0;
        }
        return weightsX.getNumWeights() * weightsY.getNumWeights();
    }

    public String generateName(Image image) {
        int idx = getChildIndex(image);
        int cols = Math.max(1, getProperties().getWeightsX().getNumWeights());

        int row = idx / cols;
        int col = idx % cols;

        return "row " + row + " col " + col;
    }

    public class GridProperties extends ImageProperties {

        public GridProperties(Textures textures, Element node) {
            super(textures, node);
        }

        public Weights getWeightsX() {
            String value = getAttribute("weightsX");
            return (value != null) ? new Weights(value) : null;
        }

        public void setWeightsX(Weights weightsX) {
            setAttribute("weightsX", weightsX.toString());
        }

        public Weights getWeightsY() {
            String value = getAttribute("weightsY");
            return (value != null) ? new Weights(value) : null;
        }

        public void setWeightsY(Weights weightsY) {
            setAttribute("weightsY", weightsY.toString());
        }
    }
}
