/*
 * Copyright (c) 2008-2012, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.fontgen.gui;

import de.matthiasmann.twl.EditField;
import de.matthiasmann.twl.GUI;
import de.matthiasmann.twl.TextWidget;
import de.matthiasmann.twl.renderer.CacheContext;
import de.matthiasmann.twl.renderer.Font;
import de.matthiasmann.twl.renderer.FontParameter;
import de.matthiasmann.twl.renderer.Renderer;
import de.matthiasmann.twl.utils.StateSelect;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

/**
 *
 * @author Matthias Mann
 */
public class TestEditField extends EditField {

    private final TextWidget textRenderer;
    
    private CacheContext fontTestCacheContext;
    
    @SuppressWarnings("LeakingThisInConstructor")
    public TestEditField() {
        setTheme("editfield");
        try {
            Field textRendererField = EditField.class.getDeclaredField("textRenderer");
            textRendererField.setAccessible(true);
            textRenderer = (TextWidget)textRendererField.get(this);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void setFont(URL url) {
        destroyCache();
        
        Font font = null;
        GUI gui = getGUI();
        if(gui != null && url != null) {
            Renderer renderer = gui.getRenderer();
            CacheContext activeCacheContext = renderer.getActiveCacheContext();
            try {
                fontTestCacheContext = renderer.createNewCacheContext();
                renderer.setActiveCacheContext(fontTestCacheContext);
                font = renderer.loadFont(url, StateSelect.EMPTY, new FontParameter());  
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                renderer.setActiveCacheContext(activeCacheContext);
            }
        }
        textRenderer.setFont(font);
        invalidateLayout();
    }

    @Override
    public void destroy() {
        destroyCache();
    }
    
    private void destroyCache() {
        textRenderer.setFont(null);
        if(fontTestCacheContext != null) {
            fontTestCacheContext.destroy();
            fontTestCacheContext = null;
        }
    }
    
}
