package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "recv_nbDataRcvd", type = PortType.enforceable),
    @Port(name = "recv_authDataRcvd", type = PortType.enforceable),
    @Port(name = "recv_userIDRcvd", type = PortType.enforceable),
    @Port(name = "send_nbData", type = PortType.enforceable),
    @Port(name = "send_reqID", type = PortType.enforceable),
    @Port(name = "send_authProvided", type = PortType.enforceable)
})
@ComponentType(name = "uainterpreter", initial = "INIT")
public class Uainterpreter {

    public Uainterpreter() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "recv_nbDataRcvd", source = "INIT", target = "nbDataRcvd_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_authDataRcvd", source = "nbDataRcvd_RCVD", target = "authDataRcvd_RCVD", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_userIDRcvd", source = "authDataRcvd_RCVD", target = "userIDRcvd_RCVD", guard = "")
    public void step3() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_nbData", source = "userIDRcvd_RCVD", target = "nbData_SENT", guard = "")
    public void step4() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_reqID", source = "nbData_SENT", target = "reqID_SENT", guard = "")
    public void step5() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_authProvided", source = "reqID_SENT", target = "authProvided_SENT", guard = "")
    public void step6() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "authProvided_SENT", target = "INIT", guard = "")
    public void loopBack() {
        // TODO: logique de la transition
    }

    // === DATA WIRES (getters) ===

    // Importer DataOut pour AccessType.any :
    // import org.javabip.api.DataOut;

    @Data(name = "nbData", accessTypePort = DataOut.AccessType.any)
    public Object getNbData() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "reqID", accessTypePort = DataOut.AccessType.any)
    public Object getReqID() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "authProvided", accessTypePort = DataOut.AccessType.any)
    public Object getAuthProvided() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
