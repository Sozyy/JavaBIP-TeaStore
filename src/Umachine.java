package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "sense_httpResponse", type = PortType.spontaneous),
    @Port(name = "recv_startTime", type = PortType.enforceable),
    @Port(name = "recv_endTime", type = PortType.enforceable),
    @Port(name = "recv_id", type = PortType.enforceable),
    @Port(name = "recv_maxNbData", type = PortType.enforceable),
    @Port(name = "actuate_fromUserToInternet", type = PortType.enforceable)
})
@ComponentType(name = "umachine", initial = "INIT")
public class Umachine {

    public Umachine() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "sense_httpResponse", source = "INIT", target = "httpResponse_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_startTime", source = "httpResponse_RCVD", target = "startTime_RCVD", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_endTime", source = "startTime_RCVD", target = "endTime_RCVD", guard = "")
    public void step3() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_id", source = "endTime_RCVD", target = "id_RCVD", guard = "")
    public void step4() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_maxNbData", source = "id_RCVD", target = "maxNbData_RCVD", guard = "")
    public void step5() {
        // TODO: logique de la transition
    }

    @Transition(name = "actuate_fromUserToInternet", source = "maxNbData_RCVD", target = "fromUserToInternet_SENT", guard = "")
    public void step6() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "fromUserToInternet_SENT", target = "INIT", guard = "")
    public void loopBack() {
        // TODO: logique de la transition
    }

    // === DATA WIRES (getters) ===

    // Importer DataOut pour AccessType.any :
    // import org.javabip.api.DataOut;

    @Data(name = "fromUserToInternet", accessTypePort = DataOut.AccessType.any)
    public Object getFromUserToInternet() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
