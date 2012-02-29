package vanilla.java.chronicle.tcp;

import vanilla.java.chronicle.Excerpt;

/**
 * @author peterlawrey
 */
public enum NullExcerptListener implements ExcerptListener {
    INSTANCE;

    @Override
    public void onExcerpt(Excerpt excerpt) {
    }
}
