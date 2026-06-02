package TeaStoreWithConnector;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
        @Port(name = "sendToClient", type = PortType.enforceable),
        @Port(name = "sendToNext",   type = PortType.enforceable),
        @Port(name = "reset",        type = PortType.enforceable),
        @Port(name = "input",        type = PortType.enforceable)
})
@ComponentType(name = "SpreadConnector2", initial = "IDLE")
public class SpreadConnector2 extends SpreadConnector {
    public SpreadConnector2(int maxClient, boolean hasNext) { super(2, maxClient, hasNext); }
}