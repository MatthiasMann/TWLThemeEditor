/*
 * Copyright (c) 2008-2011, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 *
 * @author Matthias Mann
 */
public class BufferedPipe {
    
    private static final int BLOCK_SIZE_LOG2 = 12;
    private static final int BLOCK_SIZE      = 1 << BLOCK_SIZE_LOG2;
    
    final ArrayList<byte[]> blocks;
    byte[] lastBlock;
    int lastBlockUsed;

    public BufferedPipe() {
        blocks = new ArrayList<byte[]>();
        lastBlock = new byte[BLOCK_SIZE];
    }
    
    /**
     * Creates a output stream which appends to this buffered pipe
     * @return a output stream
     */
    public OutputStream getOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) {
                checkBlockFull();
                lastBlock[lastBlockUsed++] = (byte)b;
            }
            @Override
            public void write(byte[] b, int off, int len) {
                if((off|len) < 0 || off > b.length || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                }
                while(len > 0) {
                    checkBlockFull();
                    int copy = Math.min(BLOCK_SIZE - lastBlockUsed, len);
                    System.arraycopy(b, off, lastBlock, lastBlockUsed, copy);
                    lastBlockUsed += copy;
                    off += copy;
                    len -= copy;
                }
            }
        };
    }
    
    /**
     * Creates a new input stream which starts reading at the start of the data
     * @return a new input stream
     */
    public InputStream getInputStream() {
        return new InputStream() {
            int pos;
            int mark;
            
            byte[] block;
            int blockPos;
            int blockLen;
            
            @Override
            public int read() {
                if(access()) {
                    ++pos;
                    return block[blockPos] & 255;
                } else {
                    return -1;
                }
            }

            @Override
            public int available() {
                access();
                return blockLen - blockPos;
            }

            @Override
            public void mark(int readlimit) {
                this.mark = pos;
            }

            @Override
            public void reset() {
                this.pos = mark;
            }

            @Override
            public boolean markSupported() {
                return true;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if((off|len) < 0 || off > b.length || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                }
                if(len == 0) {
                    return 0;
                }
                if(access()) {
                    int copy = Math.min(blockLen - blockPos, len);
                    System.arraycopy(block, blockPos, b, off, copy);
                    pos += copy;
                    return copy;
                }
                return -1;
            }

            @Override
            public long skip(long n) {
                int skip = (int)Math.min(available(), n);
                pos += skip;
                return skip;
            }
            
            private boolean access() {
                int blockIdx = (pos >> BLOCK_SIZE_LOG2);
                if(blockIdx < blocks.size()) {
                    block    = blocks.get(blockIdx);
                    blockLen = BLOCK_SIZE;
                } else {
                    block    = lastBlock;
                    blockLen = lastBlockUsed;
                }
                blockPos = pos & (BLOCK_SIZE - 1);
                return blockPos < blockLen;
            }
        };
    }
    
    public int getSize() {
        return blocks.size() * BLOCK_SIZE + lastBlockUsed;
    }
    
    void checkBlockFull() {
        if(lastBlockUsed == BLOCK_SIZE) {
            blocks.add(lastBlock);
            lastBlock = new byte[BLOCK_SIZE];
            lastBlockUsed = 0;
        }
    }
}
