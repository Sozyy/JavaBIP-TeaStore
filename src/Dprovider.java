package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "recv_redirection_dp", type = PortType.enforceable),
    @Port(name = "recv_userID", type = PortType.enforceable),
    @Port(name = "recv_dataQty", type = PortType.enforceable),
    @Port(name = "recv_cacheResponse", type = PortType.enforceable),
    @Port(name = "send_userID", type = PortType.enforceable),
    @Port(name = "send_cacheRequest", type = PortType.enforceable),
    @Port(name = "send_oneLessPendingRequest", type = PortType.enforceable),
    @Port(name = "send_timeSpentOnReq", type = PortType.enforceable),
    @Port(name = "send_enableTimeReading", type = PortType.enforceable)
})
@ComponentType(name = "dprovider", initial = "INIT")
public class Dprovider {

    public Dprovider() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "recv_redirection_dp", source = "INIT", target = "redirection_dp_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_userID", source = "redirection_dp_RCVD", target = "userID_RCVD", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_dataQty", source = "userID_RCVD", target = "dataQty_RCVD", guard = "")
    public void step3() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_cacheResponse", source = "dataQty_RCVD", target = "cacheResponse_RCVD", guard = "")
    public void step4() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_userID", source = "cacheResponse_RCVD", target = "userID_SENT", guard = "")
    public void step5() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_cacheRequest", source = "userID_SENT", target = "cacheRequest_SENT", guard = "")
    public void step6() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_oneLessPendingRequest", source = "cacheRequest_SENT", target = "oneLessPendingRequest_SENT", guard = "")
    public void step7() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_timeSpentOnReq", source = "oneLessPendingRequest_SENT", target = "timeSpentOnReq_SENT", guard = "")
    public void step8() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_enableTimeReading", source = "timeSpentOnReq_SENT", target = "enableTimeReading_SENT", guard = "")
    public void step9() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "enableTimeReading_SENT", target = "INIT", guard = "")
    public void loopBack() {
        // TODO: logique de la transition
    }

    // === DATA WIRES (getters) ===

    // Importer DataOut pour AccessType.any :
    // import org.javabip.api.DataOut;

    @Data(name = "userID", accessTypePort = DataOut.AccessType.any)
    public Object getUserID() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "cacheRequest", accessTypePort = DataOut.AccessType.any)
    public Object getCacheRequest() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "oneLessPendingRequest", accessTypePort = DataOut.AccessType.any)
    public Object getOneLessPendingRequest() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "timeSpentOnReq", accessTypePort = DataOut.AccessType.any)
    public Object getTimeSpentOnReq() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "enableTimeReading", accessTypePort = DataOut.AccessType.any)
    public Object getEnableTimeReading() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
