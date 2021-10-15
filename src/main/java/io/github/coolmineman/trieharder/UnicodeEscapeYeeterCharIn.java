package io.github.coolmineman.trieharder;

\u002f\u002f I love java
\u002f\u002f Excludes comments and strings
\u002f\u002f https://docs.oracle.com/javase/specs/jls/se17/html/jls-3.html
public class UnicodeEscapeYeeterCharIn implements CharIn {
    CharIn parent;

    enum State {
        NONE,
        EXTRAOUT,
    }

    State state = State.NONE;
    int extraout = 0;
    int[] hex = new int[4];

    public UnicodeEscapeYeeterCharIn(CharIn parent) {
        this.parent = parent;
    }

    @Override
    public int read() {
        int r = parent.read();
        int r2;
        switch (state) {
            case NONE:
                if (r == '\\') {
                    r2 = parent.read();
                    if (r2 == '\\') {
                        state = State.EXTRAOUT;
                        extraout = '\\';
                        return '\\';
                    } else {
                        if (r2 == 'u') {
                            while ((r2 = parent.read()) == 'u');
                            hex[0] = (char) r2;
                            for (int i = 1; i < 4; i++) {
                                hex[i] = parent.read();
                            }
                            int out = 0;
                            for (int i = 0; i < 4; i++) {
                                out += Character.digit(hex[i], 16) << (4 * (3 - i));
                            }
                            return out;
                        } else {
                            state = State.EXTRAOUT;
                            extraout = r2;
                            return '\\';
                        }
                    }
                } else {
                    return r;
                }
            case EXTRAOUT:
                state = State.NONE;
                return extraout;
            default:
                throw new Error("Unreachable");
        }
    }
    
}
