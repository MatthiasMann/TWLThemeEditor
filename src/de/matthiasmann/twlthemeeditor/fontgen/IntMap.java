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
package de.matthiasmann.twlthemeeditor.fontgen;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author Matthias Mann
 */
public class IntMap<T> implements Iterable<IntMap.Entry<T>> {

    private static final int PAGE_SIZE = 256;

    Object[][] pages;

    public IntMap() {
        pages = new Object[16][];
    }

    public void put(int idx, T obj) {
        int pageNr  = idx / PAGE_SIZE;
        int pageIdx = idx % PAGE_SIZE;
        Object[] page;
        if(pageNr >= pages.length || (page = pages[pageNr]) == null) {
            page = newPage(pageNr);
        }
        page[pageIdx] = obj;
    }

    @SuppressWarnings("unchecked")
    public T get(int idx) {
        int pageNr  = idx / PAGE_SIZE;
        int pageIdx = idx % PAGE_SIZE;
        Object[] page;
        if(pageNr < pages.length && (page = pages[pageNr]) != null) {
            return (T)page[pageIdx];
        }
        return null;
    }

    public Iterator<Entry<T>> iterator() {
        return new Iterator<Entry<T>>() {
            int pageNr;
            int pageIdx;
            Object[] page = pages[0];

            public boolean hasNext() {
                for(;;) {
                    if(pageIdx == PAGE_SIZE) {
                        pageIdx = 0;
                        page = null;
                    }
                    while(page == null) {
                        if(++pageNr >= pages.length) {
                            return false;
                        }
                        page = pages[pageNr];
                    }
                    if(page != null) {
                        while(pageIdx < PAGE_SIZE) {
                            if(page[pageIdx] != null) {
                                return true;
                            }
                            ++pageIdx;
                        }
                    }
                }
            }

            public Entry<T> next() {
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }
                final int key = pageNr * PAGE_SIZE + pageIdx;
                @SuppressWarnings("unchecked")
                final T value = (T)page[pageIdx++];
                return new Entry<T>(key, value);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private Object[] newPage(int pageNr) {
        if(pageNr >= pages.length) {
            int numPages = pages.length;
            while(numPages <= pageNr) {
                numPages *= 2;
            }
            Object[][] tmp = new Object[numPages][];
            System.arraycopy(pages, 0, tmp, 0, pages.length);
            pages = tmp;
        }
        Object[] page = new Object[PAGE_SIZE];
        pages[pageNr] = page;
        return page;
    }

    public static class Entry<T> {
        final int key;
        final T value;

        public Entry(int key, T value) {
            this.key = key;
            this.value = value;
        }
    }
}
