package javabipteastore;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
        @Port(name = "sendToClient", type = PortType.enforceable),
        @Port(name = "sendToNext",   type = PortType.enforceable),
        @Port(name = "reset",        type = PortType.enforceable),
        @Port(name = "input",        type = PortType.enforceable)
})
@ComponentType(name = "SpreadConnector1", initial = "IDLE")
public class SpreadConnector1 extends SpreadConnector {
    public SpreadConnector1(int maxClient, boolean hasNext) { super(1, maxClient, hasNext); }
}