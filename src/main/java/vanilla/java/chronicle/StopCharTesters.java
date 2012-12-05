package vanilla.java.chronicle;

/**
 * @author plawrey
 */
public enum StopCharTesters {
    ;

    public static StopCharTester forChars(CharSequence sequence) {
        if (sequence.length() == 1)
            return forChar(sequence.charAt(0));
        return new CSCSTester(sequence);
    }

    public static StopCharTester forChar(char ch) {
        return new CharCSTester(ch);
    }

    static class CSCSTester implements StopCharTester {
        private final String seperators;

        public CSCSTester(CharSequence cs) {
            seperators = cs.toString();
        }

        @Override
        public boolean isStopChar(int ch) {
            return seperators.indexOf(ch) >= 0;
        }
    }

    static class CharCSTester implements StopCharTester {
        private final char ch;

        public CharCSTester(char ch) {
            this.ch = ch;
        }

        @Override
        public boolean isStopChar(int ch) {
            return this.ch == ch;
        }
    }
}
