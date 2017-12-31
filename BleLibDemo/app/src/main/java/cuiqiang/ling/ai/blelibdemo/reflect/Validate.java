
package cuiqiang.ling.ai.blelibdemo.reflect;

class Validate {

    static void assertTrue(final boolean expression, final String message, final Object... values) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }
}
