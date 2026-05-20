package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "recv_respondToThisIDs", type = PortType.enforceable),
    @Port(name = "recv_requiresAuth", type = PortType.enforceable),
    @Port(name = "send_idToRespondTo", type = PortType.enforceable),
    @Port(name = "send_reqAuth", type = PortType.enforceable),
    @Port(name = "send_isNewPage", type = PortType.enforceable)
})
@ComponentType(name = "wpservice", initial = "INIT")
public class Wpservice {

    public Wpservice() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "recv_respondToThisIDs", source = "INIT", target = "respondToThisIDs_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_requiresAuth", source = "respondToThisIDs_RCVD", target = "requiresAuth_RCVD", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_idToRespondTo", source = "requiresAuth_RCVD", target = "idToRespondTo_SENT", guard = "")
    public void step3() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_reqAuth", source = "idToRespondTo_SENT", target = "reqAuth_SENT", guard = "")
    public void step4() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_isNewPage", source = "reqAuth_SENT", target = "isNewPage_SENT", guard = "")
    public void step5() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "isNewPage_SENT", target = "INIT", guard = "")
    public void loopBack() {
        // TODO: logique de la transition
    }

    // === DATA WIRES (getters) ===

    // Importer DataOut pour AccessType.any :
    // import org.javabip.api.DataOut;

    @Data(name = "idToRespondTo", accessTypePort = DataOut.AccessType.any)
    public Object getIdToRespondTo() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "reqAuth", accessTypePort = DataOut.AccessType.any)
    public Object getReqAuth() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

    @Data(name = "isNewPage", accessTypePort = DataOut.AccessType.any)
    public Object getIsNewPage() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
