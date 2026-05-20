package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "recv_redirection", type = PortType.enforceable),
    @Port(name = "recv_maxRequests", type = PortType.enforceable),
    @Port(name = "recv_userID", type = PortType.enforceable),
    @Port(name = "recv_nbDataToFetch", type = PortType.enforceable),
    @Port(name = "recv_nbAnsweredReqs", type = PortType.enforceable),
    @Port(name = "send_redirection", type = PortType.enforceable),
    @Port(name = "send_userID", type = PortType.enforceable),
    @Port(name = "send_dataQty", type = PortType.enforceable)
})
@ComponentType(name = "rlimiter", initial = "INIT")
public class Rlimiter {

    public Rlimiter() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "recv_redirection", source = "INIT", target = "redirection_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_maxRequests", source = "redirection_RCVD", target = "maxRequests_RCVD", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_userID", source = "maxRequests_RCVD", target = "userID_RCVD", guard = "")
    public void step3() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_nbDataToFetch", source = "userID_RCVD", target = "nbDataToFetch_RCVD", guard = "")
    public void step4() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_nbAnsweredReqs", source = "nbDataToFetch_RCVD", target = "nbAnsweredReqs_RCVD", guard = "")
    public void step5() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_redirection", source = "nbAnsweredReqs_RCVD", target = "redirection_SENT", guard = "")
    public void step6() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_userID", source = "redirection_SENT", target = "userID_SENT", guard = "")
    public void step7() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_dataQty", source = "userID_SENT", target = "dataQty_SENT", guard = "")
    public void step8() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "dataQty_SENT", target = "INIT", guard = "")
    public void loopBack() {
        // TODO: logique de la transition
    }

    // === DATA WIRES (getters) ===

    // Importer DataOut pour AccessType.any :
    // import org.javabip.api.DataOut;

    @Data(name = "redirection", accessTypePort = DataOut.AccessType.any)
    public Object getRedirection() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "userID", accessTypePort = DataOut.AccessType.any)
    public Object getUserID() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "dataQty", accessTypePort = DataOut.AccessType.any)
    public Object getDataQty() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
