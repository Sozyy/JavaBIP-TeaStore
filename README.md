<<<<<<< Updated upstream
# javabip-experiments
=======
# Chips_to_JavaBIP

Needed : 
- Chips MetaModel (Chips_Public -> Chips1.1.ecore)
- JavaBip MetaModel (to create ?)
- in.xmi (anna's chips project ?)
- transformation.atl (no u)
- out.xmi (generated)

# JavaBIP MetaModel

BIP_Project
  - name : EString
  - description : EString
  - engineOutput : Asset 
  - componentTypes : Component_Type[*] {containment}
  - connectorMotifs : Connector_Motif[*] {containment}

Component_Type
  - name : EString
  - cardinality : EString
  - definitions : EString
  - guards : Guard[*] {containment}
  - states : State_Base[1..*] {containment}
  - transitions : Transition_Base[*] {containment}

Guard
  - name : EString
  - guardMethod : EString

abstract State_Base
  - name : EString
  - outgoing : Transition_Base[*]
  - incoming : Transition_Base[*]

Initial_State extends State_Base
State extends State_Base

abstract Transition_Base
  - name : EString
  - guardExpression : EString
  - transitionMethod : EString
  - src : State_Base[1]
  - dst : State_Base[1]

Internal_Transition extends Transition_Base
Spontaneous_Transition extends Transition_Base
Enforceable_Transition extends Transition_Base

Connector_Motif
  - ends : Connector_Motif_End[1..*] {containment}

abstract Connector_Motif_End
  - degree : EString
  - multiplicity : EString
  - port : Enforceable_Transition[1]

Synchron extends Connector_Motif_End
Trigger extends Connector_Motif_End
>>>>>>> Stashed changes

