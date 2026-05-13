package TeaStoreWithConnector;

import org.javabip.annotations.ComponentType;
import org.javabip.annotations.Port;
import org.javabip.annotations.Ports;

@Ports({
        @Port(name = "loop", type = org.javabip.api.PortType.enforceable)
})
@ComponentType(name = "Looper1", initial = "IDLE")
public class Looper1 extends Looper {
    public Looper1(int maxClient) { super(maxClient); }
}