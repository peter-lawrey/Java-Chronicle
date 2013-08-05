package vanilla.java.stages.api;

import com.higherfrequencytrading.chronicle.Excerpt;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:34
 */
public class EventsReader {
    private final Excerpt excerpt;
    private final Events events;
    private final MetaData metaData;
    private final Update update = new Update();

    public EventsReader(Excerpt excerpt, Events events, TimingStage readStage, TimingStage writeStage) {
        this.excerpt = excerpt;
        this.events = events;
        metaData = new MetaData(readStage, writeStage);
    }

    public boolean read() {
        if (!excerpt.nextIndex())
            return false;
        MessageType mt = excerpt.readEnum(MessageType.class);
        metaData.readMarshallable(excerpt);
        switch (mt) {
            case update: {
                update.readMarshallable(excerpt);
                events.onMarketData(metaData, update);
                break;
            }
        }
        return true;
    }
}
