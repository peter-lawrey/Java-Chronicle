package vanilla.java.stages.api;

import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:34
 */
public class EventsWriter implements Events {
    private final Excerpt excerpt;
    private final MetaData metaData = new MetaData(null, TimingStage.SourceWrite);

    public EventsWriter(Chronicle chronicle) {
        excerpt = chronicle.createExcerpt();
    }

    @Override
    public void onMarketData(MetaData metaData, Update update) {
        if (metaData == null) {
            metaData = this.metaData;
            metaData.startTiming();
        }
        excerpt.startExcerpt(1024); // a guess
        excerpt.writeEnum(MessageType.update);
        metaData.writeMarshallable(excerpt);
        update.writeMarshallable(excerpt);
        excerpt.finish();
    }
}
