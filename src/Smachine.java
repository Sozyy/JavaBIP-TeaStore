package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "sense_fromInternet", type = PortType.spontaneous),
    @Port(name = "recv_wpServed", type = PortType.enforceable),
    @Port(name = "recv_maxNbData", type = PortType.enforceable),
    @Port(name = "send_nbData", type = PortType.enforceable),
    @Port(name = "send_userID", type = PortType.enforceable),
    @Port(name = "send_userAuth", type = PortType.enforceable),
    @Port(name = "actuate_fromServerToInternet", type = PortType.enforceable)
})
@ComponentType(name = "smachine", initial = "INIT")
public class Smachine {

    public Smachine() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "sense_fromInternet", source = "INIT", target = "fromInternet_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_wpServed", source = "fromInternet_RCVD", target = "wpServed_RCVD", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_maxNbData", source = "wpServed_RCVD", target = "maxNbData_RCVD", guard = "")
    public void step3() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_nbData", source = "maxNbData_RCVD", target = "nbData_SENT", guard = "")
    public void step4() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_userID", source = "nbData_SENT", target = "userID_SENT", guard = "")
    public void step5() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_userAuth", source = "userID_SENT", target = "userAuth_SENT", guard = "")
    public void step6() {
        // TODO: logique de la transition
    }

    @Transition(name = "actuate_fromServerToInternet", source = "userAuth_SENT", target = "fromServerToInternet_SENT", guard = "")
    public void step7() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "fromServerToInternet_SENT", target = "INIT", guard = "")
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

    @Data(name = "userID", accessTypePort = DataOut.AccessType.any)
    public Object getUserID() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "userAuth", accessTypePort = DataOut.AccessType.any)
    public Object getUserAuth() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "fromServerToInternet", accessTypePort = DataOut.AccessType.any)
    public Object getFromServerToInternet() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
