package vanilla.java.stages.api;

/**
 * User: peter
 * Date: 05/08/13
 * Time: 17:58
 */
public enum TimingStage {
    Start,
    SourceWrite, SourceRead,
    EngineWrite, EngineRead,
    SinkWrite, SinkRead;

    static final TimingStage[] VALUES = values();
}
