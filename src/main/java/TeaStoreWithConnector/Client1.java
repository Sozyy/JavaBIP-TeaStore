package TeaStoreWithConnector;

import org.javabip.annotations.ComponentType;
import org.javabip.annotations.Port;
import org.javabip.annotations.Ports;
import org.javabip.api.PortType;

@Ports({
        @Port(name = "sendRequest",     type = PortType.enforceable),
        @Port(name = "receiveResp",     type = PortType.enforceable)
})
@ComponentType(name = "Client1", initial = "IDLE")
public class Client1 extends Client {
    public Client1(int id, int objective, int maxRequestSize, int maxClient) {
        super(id, objective, maxRequestSize, maxClient);
    }
}