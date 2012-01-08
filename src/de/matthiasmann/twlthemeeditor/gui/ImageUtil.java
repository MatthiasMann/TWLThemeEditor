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
package de.matthiasmann.twlthemeeditor.gui;

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.renderer.Image;
import de.matthiasmann.twl.utils.StateExpression;
import de.matthiasmann.twl.utils.StateSelect;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;

/**
 *
 * @author Matthias Mann
 */
public class ImageUtil {
    
    public static void getAllImages(Image img, ArrayList<Image> result) {
        if(img == null) {
            return;
        }
        result.add(img);
        try {
            Class<? extends Image> imgClass = img.getClass();
            for(Field f : imgClass.getDeclaredFields()) {
                Class<?> type = f.getType();
                if(type == Image.class) {
                    try {
                        f.setAccessible(true);
                        getAllImages((Image)f.get(img), result);
                    } catch(Exception ex) {
                    }
                } else if(type.isArray() && type.getComponentType() == Image.class) {
                    try {
                        f.setAccessible(true);
                        Image[] images = (Image[])f.get(img);
                        if(images != null) {
                            for(Image image : images) {
                                getAllImages(image, result);
                            }
                        }
                    } catch(Exception ex) {
                    }
                }
            }
        } catch(Exception ex) {
        }
    }
    
    public static<T> T getField(Object obj, String fieldName, Class<T> type) {
        if(obj == null) {
            return null;
        }
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            if(!type.isAssignableFrom(field.getType())) {
                return null;
            }
            field.setAccessible(true);
            return type.cast(field.get(obj));
        } catch(Exception ex) {
            return null;
        }
    }
    
    public static Border getInset(Image img) {
        return getField(img, "inset", Border.class);
    }
    
    public static void getAllStateKeys(Image img, BitSet usedStateKeys) {
        StateExpression expr = getField(img, "condition", StateExpression.class);
        if(expr != null) {
            getUsedStateKeys(expr, usedStateKeys);
        }
        StateSelect ss = getField(img, "select", StateSelect.class);
        if(ss != null) {
            for(int i=0,n=ss.getNumExpressions() ; i<n ; i++) {
                getUsedStateKeys(ss.getExpression(i), usedStateKeys);
            }
        }
    }
    
    public static void getUsedStateKeys(StateExpression se, BitSet bs) {
        try {
            Method m = StateExpression.class.getDeclaredMethod("getUsedStateKeys", BitSet.class);
            m.setAccessible(true);
            m.invoke(se, bs);
        } catch(Exception ex) {
        }
    }
}
