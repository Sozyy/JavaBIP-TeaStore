package javabipteastore;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
        @Port(name = "sendUp", type = PortType.enforceable),
        @Port(name = "reset",  type = PortType.enforceable),
        @Port(name = "input1", type = PortType.enforceable),
        @Port(name = "input2", type = PortType.enforceable),
        @Port(name = "input3", type = PortType.enforceable)
})
@ComponentType(name = "CollectConnector3", initial = "IDLE")
public class CollectConnector3 extends CollectConnector {
    public CollectConnector3(int maxClient) { super(3, maxClient); }
}