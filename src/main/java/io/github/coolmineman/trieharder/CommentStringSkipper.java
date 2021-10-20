package io.github.coolmineman.trieharder;

import java.io.IOException;

public class CommentStringSkipper implements ReplacerCharIn {
    ReplacerCharIn in;
    Appendable out;
    int[] readbuffer = new int[3];
    int readbuffersize = 0;

    public CommentStringSkipper(ReplacerCharIn in, Appendable out) {
        this.in = in;
        this.out = out;
    }

    enum State {
        STRING,
        CHAR,
        TEXT,
        EOL_COMMENT,
        TRADITIONAL_COMMENT,
        NONE
    }

    State state = State.NONE;

    @Override
    public int read() {
        switch (state){
            case NONE:
                return d();
            case STRING:
                for (;;) {
                    int r0 = rw();
                    if (r0 < 0) return r0;
                    if (r0 == '\\') {
                        rw();
                    } else if (r0 == '"') {
                        state = State.NONE;
                        return d();
                    }
                }
            case CHAR:
                for (;;) {
                    int r0 = rw();
                    if (r0 < 0) return r0;
                    if (r0 == '\\') {
                        rw();
                    } else if (r0 == '\'') {
                        state = State.NONE;
                        return d();
                    }
                }
            case TEXT:
                int a = 0;
                for (;;) {
                    int r0 = rw();
                    if (r0 < 0) return r0;
                    if (r0 == '\\') {
                        rw();
                    } else if (r0 == '"') {
                        a++;
                        if (a >= 3) {
                            state = State.NONE;
                            return d();
                        }
                    } else {
                        a = 0;
                    }
                }
            case EOL_COMMENT:
                int r00 = 0;
                while (r00 != '\n') {
                    if (r00 < 0) return r00;
                    r00 = rw();
                }
                state = State.NONE;
                return d();
            case TRADITIONAL_COMMENT:
                for (;;) {
                    int r0 = rw();
                    if (r0 < 0) return r0;
                    if (r0 == '*' && rw() == '/') {
                        state = State.NONE;
                        return d();
                    }
                }
            default:
                throw new Error("Unreachable");
        }
        
    }

    int d() {
        int r = r();
        if (r == '"') {
            w(r);
            int r2 = rw();
            if (r2 == '"') {
                int r3 = r();
                if (r3 == '"') {
                    w(r3);
                    state = State.TEXT;
                    return -2;
                } else {
                    return -2;
                }
            } else {
                state = State.STRING;
                return -2;
            }
        } else if (r == '\'') {
            w(r);
            state = State.CHAR;
            return -2;
        } else if (r == '/') {
            int r2 = r();
            if (r2 == '/') {
                w(r);
                w(r2);
                state = State.EOL_COMMENT;
                return -2;
            } else if (r2 == '*') {
                w(r);
                w(r2);
                state = State.TRADITIONAL_COMMENT;
                return -2;
            } else {
                buffer(r2);
                return r;
            }
        }
        return r;
    }

    int r() {
        int result;
        if (readbuffersize > 0) {
            result = readbuffer[0];
            readbuffer[0] = readbuffer[1];
            readbuffer[1] = readbuffer[2];
        } else {
            result = in.read();
        }
        return result;
    }

    void w(int a) {
        if (a > 0) {
            try {
                out.append((char)a);
            } catch (IOException e) {
                throw Util.sneak(e);
            }
        }
    }

    int rw() {
        int r = r();
        w(r);
        return r;
    }

    void buffer(int c) {
        readbuffer[readbuffersize++] = c;
    }
    
}
