/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

/**
 *
 * @author MannMat
 */
public final class ImageReference {

    private final String name;

    public ImageReference(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
    
}
