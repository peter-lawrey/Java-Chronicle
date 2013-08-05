package vanilla.java.stages.api;

import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.ExcerptMarshallable;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:40
 */
public class UpdateLevel implements ExcerptMarshallable {
    private double bp;
    private double bq;
    private double ap;
    private double aq;

    public void init(double bp, double bq, double ap, double aq) {
        this.bp = bp;
        this.bq = bq;
        this.ap = ap;
        this.aq = aq;
    }

    @Override
    public void readMarshallable(Excerpt in) throws IllegalStateException {
        bp = in.readCompactDouble();
        bq = in.readCompactDouble();
        ap = in.readCompactDouble();
        aq = in.readCompactDouble();
    }

    @Override
    public void writeMarshallable(Excerpt out) {
        out.writeCompactDouble(bp);
        out.writeCompactDouble(bq);
        out.writeCompactDouble(ap);
        out.writeCompactDouble(aq);
    }

    @Override
    public String toString() {
        return "UpdateLevel{" +
                "bp=" + bp +
                ", bq=" + bq +
                ", ap=" + ap +
                ", aq=" + aq +
                '}';
    }
}
