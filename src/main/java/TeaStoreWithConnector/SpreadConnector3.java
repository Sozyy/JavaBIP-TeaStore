package TeaStoreWithConnector;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
        @Port(name = "sendToClient", type = PortType.enforceable),
        @Port(name = "sendToNext",   type = PortType.enforceable),
        @Port(name = "reset",        type = PortType.enforceable),
        @Port(name = "input",        type = PortType.enforceable)
})
@ComponentType(name = "SpreadConnector3", initial = "IDLE")
public class SpreadConnector3 extends SpreadConnector {
    public SpreadConnector3(int maxClient, boolean hasNext) { super(3, maxClient, hasNext); }
}