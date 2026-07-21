package tools.spirals.cerberus237.siphonix.strategies.javabip.component;

import org.javabip.glue.TwoSynchronGlueBuilder;

public class Glue extends TwoSynchronGlueBuilder {

    @Override
    public void configure() {
        // Bridge -> DataProvider
        synchron(Bridge.class, "sendRequest").to(DataProvider.class, "receiveRequest");

        // DataProvider -> PID
        synchron(DataProvider.class, "sendResponseTime").to(PIDController.class, "receiveResponseTime");

        // PID -> DataProvider
        synchron(PIDController.class, "sendCacheSize").to(DataProvider.class, "receiveNewCacheSize");

        // DataProvider -> Bridge
        synchron(DataProvider.class, "notifyServer").to(Bridge.class, "receiveResp");

        // Data wires
        data(Bridge.class, "request").to(DataProvider.class, "request");
        data(DataProvider.class, "responseTime").to(PIDController.class, "responseTime");
        data(PIDController.class, "newCacheSize").to(DataProvider.class, "newCacheSize");
    }
}
