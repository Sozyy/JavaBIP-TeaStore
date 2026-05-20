package generated;

import org.javabip.glue.TwoSynchronGlueBuilder;

/**
 * Glue générée à partir du modèle JavaBIP "TeaStoreVariation.chips".
 */
public class GeneratedGlue extends TwoSynchronGlueBuilder {

    @Override
    public void configure() {

        // === SYNCHRONS ===
        // Chaque RequireRule { effect, causes } => synchron(cause).to(effect)

        synchron(Wpservice.class, "send_idToRespondTo").to(Smachine.class, "recv_wpServed");
        synchron(Smachine.class, "send_nbData").to(Uainterpreter.class, "recv_nbDataRcvd");
        synchron(Smachine.class, "send_userAuth").to(Uainterpreter.class, "recv_authDataRcvd");
        synchron(Smachine.class, "send_userID").to(Uainterpreter.class, "recv_userIDRcvd");
        synchron(Uainterpreter.class, "send_authProvided").to(Authenticator.class, "recv_authProvided");
        synchron(Uainterpreter.class, "send_nbData").to(Rvalidator.class, "recv_nbData");
        synchron(Uainterpreter.class, "send_reqID").to(Rvalidator.class, "recv_requestID");
        synchron(Authenticator.class, "send_isAuthenticated").to(Rvalidator.class, "recv_isAuthenticated");
        synchron(Rvalidator.class, "send_userID").to(Rlimiter.class, "recv_userID");
        synchron(Rvalidator.class, "send_nbDataRequested").to(Rlimiter.class, "recv_nbDataToFetch");
        synchron(Rvalidator.class, "send_redirection").to(Rlimiter.class, "recv_redirection");

        // === DATA WIRES ===

        data(Wpservice.class, "idToRespondTo").to(Smachine.class, "wpServed");
        data(Smachine.class, "nbData").to(Uainterpreter.class, "nbDataRcvd");
        data(Smachine.class, "userAuth").to(Uainterpreter.class, "authDataRcvd");
        data(Smachine.class, "userID").to(Uainterpreter.class, "userIDRcvd");
        data(Uainterpreter.class, "authProvided").to(Authenticator.class, "authProvided");
        data(Uainterpreter.class, "nbData").to(Rvalidator.class, "nbData");
        data(Uainterpreter.class, "reqID").to(Rvalidator.class, "requestID");
        data(Authenticator.class, "isAuthenticated").to(Rvalidator.class, "isAuthenticated");
        data(Rvalidator.class, "userID").to(Rlimiter.class, "userID");
        data(Rvalidator.class, "nbDataRequested").to(Rlimiter.class, "nbDataToFetch");
        data(Rvalidator.class, "redirection").to(Rlimiter.class, "redirection");
    }
}
