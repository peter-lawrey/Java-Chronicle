package vanilla.java.stages.testing;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 19:07
 */
public class VanillaDifferencer implements Differencer {
    @Override
    public long sample(long startTime, long endTime) {
        return endTime - startTime;
    }
}
