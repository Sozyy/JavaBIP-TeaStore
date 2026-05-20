package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "recv_nbData", type = PortType.enforceable),
    @Port(name = "recv_requestID", type = PortType.enforceable),
    @Port(name = "recv_isAuthenticated", type = PortType.enforceable),
    @Port(name = "send_nbDataRequested", type = PortType.enforceable),
    @Port(name = "send_userID", type = PortType.enforceable),
    @Port(name = "send_redirection", type = PortType.enforceable)
})
@ComponentType(name = "rvalidator", initial = "INIT")
public class Rvalidator {

    public Rvalidator() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "recv_nbData", source = "INIT", target = "nbData_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_requestID", source = "nbData_RCVD", target = "requestID_RCVD", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_isAuthenticated", source = "requestID_RCVD", target = "isAuthenticated_RCVD", guard = "")
    public void step3() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_nbDataRequested", source = "isAuthenticated_RCVD", target = "nbDataRequested_SENT", guard = "")
    public void step4() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_userID", source = "nbDataRequested_SENT", target = "userID_SENT", guard = "")
    public void step5() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_redirection", source = "userID_SENT", target = "redirection_SENT", guard = "")
    public void step6() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "redirection_SENT", target = "INIT", guard = "")
    public void loopBack() {
        // TODO: logique de la transition
    }

    // === DATA WIRES (getters) ===

    // Importer DataOut pour AccessType.any :
    // import org.javabip.api.DataOut;

    @Data(name = "nbDataRequested", accessTypePort = DataOut.AccessType.any)
    public Object getNbDataRequested() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "userID", accessTypePort = DataOut.AccessType.any)
    public Object getUserID() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "redirection", accessTypePort = DataOut.AccessType.any)
    public Object getRedirection() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
