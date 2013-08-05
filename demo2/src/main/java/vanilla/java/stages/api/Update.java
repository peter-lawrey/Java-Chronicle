package vanilla.java.stages.api;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.ExcerptMarshallable;

import java.util.ArrayList;
import java.util.List;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:39
 */
public class Update implements ExcerptMarshallable {
    private String instrument;
    private final List<UpdateLevel> levels = new ArrayList<UpdateLevel>();
    private final List<UpdateLevel> levelPool = new ArrayList<UpdateLevel>();

    @Override
    public void readMarshallable(Excerpt in) throws IllegalStateException {
        resetLevels(in.readEnum(String.class));
        int len = (int) in.readStopBit();
        for (int i = 0; i < len; i++)
            acquireLevel().readMarshallable(in);
    }

    public void resetLevels(String instrument) {
        this.instrument = instrument;
        levels.clear();
    }

    public UpdateLevel acquireLevel() {
        if (levels.size() == levelPool.size())
            levelPool.add(new UpdateLevel());
        UpdateLevel level = levelPool.get(levels.size());
        levels.add(level);
        return level;
    }

    @Override
    public void writeMarshallable(Excerpt out) {
        out.writeEnum(instrument);
        out.writeStopBit(levels.size());
        for (int i = 0, len = levels.size(); i < len; i++)
            levels.get(i).writeMarshallable(out);
    }
}
