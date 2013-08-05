package vanilla.java.stages.api;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:34
 */
public interface Events {
    public void onMarketData(MetaData metaData, Update update);
}
