package TeaStoreWithConnector;

import org.javabip.annotations.ComponentType;
import org.javabip.annotations.Port;
import org.javabip.annotations.Ports;

@Ports({
        @Port(name = "loop", type = org.javabip.api.PortType.enforceable)
})
@ComponentType(name = "Looper2", initial = "IDLE")
public class Looper2 extends Looper {
    public Looper2() { super(0); }
}