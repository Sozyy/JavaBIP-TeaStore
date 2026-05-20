package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "recv_authProvided", type = PortType.enforceable),
    @Port(name = "send_isAuthenticated", type = PortType.enforceable)
})
@ComponentType(name = "authenticator", initial = "INIT")
public class Authenticator {

    public Authenticator() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "recv_authProvided", source = "INIT", target = "authProvided_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_isAuthenticated", source = "authProvided_RCVD", target = "isAuthenticated_SENT", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "isAuthenticated_SENT", target = "INIT", guard = "")
    public void loopBack() {
        // TODO: logique de la transition
    }

    // === DATA WIRES (getters) ===

    // Importer DataOut pour AccessType.any :
    // import org.javabip.api.DataOut;

    @Data(name = "isAuthenticated", accessTypePort = DataOut.AccessType.any)
    public Object getIsAuthenticated() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
