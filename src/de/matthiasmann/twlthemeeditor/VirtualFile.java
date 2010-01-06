/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author MannMat
 */
public interface VirtualFile {

    public InputStream openStream() throws IOException;

}
