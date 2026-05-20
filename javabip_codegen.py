#!/usr/bin/env python3
"""
javabip_codegen.py

Génère du code Java JavaBIP à partir d'un modèle XMI conforme au métamodèle
JavaBIP produit par la transformation Chips → JavaBIP.

Usage:
    python3 javabip_codegen.py <input.xmi> <output_directory> [--package NAME] [--glue-class NAME]

Le script produit :
  * Une classe Java par ComponentType (avec annotations @Port, @ComponentType,
    @Transition et @Data getters).
  * Une classe Java de glue (héritant de TwoSynchronGlueBuilder) avec un
    configure() qui appelle synchron(...).to(...) pour chaque RequireRule
    et data(...).to(...) pour chaque DataWire.

"""



import sys
import os
import argparse
import xml.etree.ElementTree as ET
from collections import OrderedDict


# Namespaces utilisés dans le XMI JavaBIP
NS = {
    'xmi':      'http://www.omg.org/XMI',
    'behavior': 'http://JavaBIP/behavior',
    'glue':     'http://JavaBIP/glue',
}


# =============================================================================
# PARSING DU XMI
# =============================================================================

def parse_xmi(xmi_path):
    """
    Parse le fichier XMI et retourne :
      - components : liste de dicts décrivant chaque ComponentType
      - data_wires : liste de tuples (from_component, from_port, to_component, to_port)
      - require_rules : liste de tuples (effect_component, effect_port, cause_component, cause_port)

    Le XMI référence ses sous-éléments via des indices XPath comme "/22".
    On commence par construire une table indice → élément en parcourant les
    enfants directs de la racine dans l'ordre du document.
    """
    tree = ET.parse(xmi_path)
    root = tree.getroot()

    # Table indice → élément. L'indice "/N" désigne le N-ième enfant à plat
    # de la racine. Attention : le XMI XMI/EMF utilise des fragment URIs qui
    # comptent à partir de 0 pour le premier enfant.
    children = list(root)
    index_to_elem = {f'/{i}': child for i, child in enumerate(children)}

    def resolve(ref):
        """Résoudre une référence '/N' en élément XML."""
        return index_to_elem.get(ref.strip())

    def resolve_attr(elem, attr_name):
        """Résoudre un attribut qui contient une référence '/N'."""
        ref = elem.get(attr_name)
        return resolve(ref) if ref else None

    # --- Étape 1 : trouver le JavaBIPModel et son GlueSpec ---
    model = None
    for child in children:
        if child.tag.endswith('JavaBIPModel'):
            model = child
            break
    if model is None:
        raise RuntimeError("Aucun JavaBIPModel trouvé dans le XMI.")

    # --- Étape 2 : extraire les ComponentTypes ---
    component_refs = model.get('components', '').split()
    components = []
    for cref in component_refs:
        c = resolve(cref)
        if c is None:
            continue
        components.append(parse_component(c, resolve, index_to_elem))

    # --- Étape 3 : extraire la glue ---
    glue_ref = model.get('glue')
    glue = resolve(glue_ref) if glue_ref else None
    data_wires = []
    require_rules = []

    if glue is not None:
        # DataWires
        dw_refs = glue.get('dataWires', '').split()
        for dwref in dw_refs:
            dw = resolve(dwref)
            if dw is None:
                continue
            from_pr = resolve_attr(dw, 'from')
            to_pr   = resolve_attr(dw, 'to')
            data_wires.append(parse_port_ref_pair(from_pr, to_pr, resolve))

        # RequireRules
        rr_refs = glue.get('requires', '').split()
        for rrref in rr_refs:
            rr = resolve(rrref)
            if rr is None:
                continue
            effect_pr = resolve_attr(rr, 'effect')
            # "causes" peut contenir plusieurs refs séparées par espace.
            cause_refs = rr.get('causes', '').split()
            for cref in cause_refs:
                cause_pr = resolve(cref)
                require_rules.append(parse_port_ref_pair(effect_pr, cause_pr, resolve))

    return components, data_wires, require_rules


def parse_component(elem, resolve, index_to_elem):
    """Extrait toutes les infos d'un ComponentType."""
    name = elem.get('name')
    java_class = elem.get('javaClassName') or name
    cardinality = elem.get('cardinality', '1')
    initial_ref = elem.get('initialState')
    initial_elem = resolve(initial_ref) if initial_ref else None
    initial_state = initial_elem.get('name') if initial_elem is not None else 'INIT'

    # Liste d'états : on récupère juste leurs noms
    state_refs = elem.get('states', '').split()
    states = []
    for sref in state_refs:
        s = resolve(sref)
        if s is not None:
            states.append(s.get('name'))

    # Liste de ports : nom + type (enforceable par défaut)
    port_refs = elem.get('ports', '').split()
    ports = []
    for pref in port_refs:
        p = resolve(pref)
        if p is not None:
            ports.append({
                'name': p.get('name'),
                'type': p.get('type', 'enforceable'),
            })

    # Transitions : nom, méthode, source/target (noms d'états), port (nom), type
    transition_refs = elem.get('transitions', '').split()
    transitions = []
    for tref in transition_refs:
        t = resolve(tref)
        if t is None:
            continue
        src_elem = resolve(t.get('source'))
        tgt_elem = resolve(t.get('target'))
        port_elem = resolve(t.get('port')) if t.get('port') else None
        transitions.append({
            'name': t.get('name'),
            'transition_method': t.get('transitionMethod', t.get('name')),
            'source': src_elem.get('name') if src_elem is not None else '?',
            'target': tgt_elem.get('name') if tgt_elem is not None else '?',
            'port': port_elem.get('name') if port_elem is not None else None,
            'type': t.get('type', 'enforceable'),
        })

    return {
        'name': name,
        'java_class': java_class,
        'cardinality': cardinality,
        'initial_state': initial_state,
        'states': states,
        'ports': ports,
        'transitions': transitions,
    }


def parse_port_ref_pair(pr1, pr2, resolve):
    """
    Parse une paire de PortRef. Retourne un tuple (comp1, port1, comp2, port2)
    où comp* est le NOM du composant et port* le NOM du port.
    """
    def pr_to_pair(pr):
        if pr is None:
            return ('?', '?')
        comp_elem = resolve(pr.get('component'))
        port_elem = resolve(pr.get('port'))
        comp_name = comp_elem.get('name') if comp_elem is not None else '?'
        port_name = port_elem.get('name') if port_elem is not None else '?'
        return (comp_name, port_name)

    c1, p1 = pr_to_pair(pr1)
    c2, p2 = pr_to_pair(pr2)
    return (c1, p1, c2, p2)


# =============================================================================
# GÉNÉRATION DE CODE
# =============================================================================

# Nom de la classe Java cible (CamelCase à partir du name du composant)
def class_name_for(comp_name):
    """Convertit 'umachine' → 'Umachine', 'uainterpreter' → 'Uainterpreter'."""
    # On garde simple : majuscule sur la première lettre, le reste inchangé.
    # L'utilisateur peut renommer plus tard s'il préfère un autre style.
    if not comp_name:
        return 'Component'
    return comp_name[0].upper() + comp_name[1:]


def generate_component_java(comp, package):
    """Génère le code Java d'une classe @ComponentType."""
    cls_name = class_name_for(comp['name'])

    # --- En-tête ---
    lines = [
        f'package {package};',
        '',
        'import org.javabip.annotations.*;',
        'import org.javabip.api.PortType;',
        '',     # TODO : importer DataOut si on a des getters @Data
    ]

    # --- @Ports : liste des ports avec leur type ---
    lines.append('@Ports({')
    port_decls = []
    for p in comp['ports']:
        port_type = p['type']
        # PortType.enforceable / PortType.spontaneous
        port_decls.append(f'    @Port(name = "{p["name"]}", type = PortType.{port_type})')
    lines.append(',\n'.join(port_decls))
    lines.append('})')

    # --- @ComponentType ---
    lines.append(f'@ComponentType(name = "{comp["name"]}", initial = "{comp["initial_state"]}")')
    lines.append(f'public class {cls_name} {{')
    lines.append('')

    # --- Constructeur vide ---
    lines.append(f'    public {cls_name}() {{')
    lines.append('        // TODO: initialisation des champs')
    lines.append('    }')
    lines.append('')

    # --- Transitions ---
    lines.append('    // === TRANSITIONS ===')
    lines.append('')
    for t in comp['transitions']:
        # Pour la transition "internal" (loop_back), pas de port à indiquer.
        if t['type'] == 'internal' or t['port'] is None:
            lines.append(f'    @Transition(name = "{t["name"]}", source = "{t["source"]}", target = "{t["target"]}", guard = "")')
        else:
            # Note : JavaBIP infère souvent le port à partir du nom de la transition,
            # mais on est explicite ici pour éviter les ambiguïtés (notre name="t1"
            # diffère du port qu'on veut faire tirer).
            # On utilise donc le NOM DU PORT comme name de la transition,
            # ce qui est la convention JavaBIP standard quand un port déclenche
            # exactement une transition.
            lines.append(f'    @Transition(name = "{t["port"]}", source = "{t["source"]}", target = "{t["target"]}", guard = "")')
        method = t['transition_method'] or t['name']
        lines.append(f'    public void {method}() {{')
        lines.append('        // TODO: logique de la transition')
        lines.append('    }')
        lines.append('')

    # --- @Data getters : un par port "send_" ou "actuate_" ---
    # Heuristique : chaque port qui envoie une donnée doit exposer un getter @Data.
    senders = [p for p in comp['ports']
               if p['name'].startswith('send_') or p['name'].startswith('actuate_')]
    if senders:
        lines.append('    // === DATA WIRES (getters) ===')
        lines.append('')
        lines.append('    // Importer DataOut pour AccessType.any :')
        lines.append('    // import org.javabip.api.DataOut;')
        lines.append('')
        for p in senders:
            # On extrait le "vrai" nom de la donnée derrière le préfixe.
            data_name = p['name'].split('_', 1)[1] if '_' in p['name'] else p['name']
            # Le type Java est inconnu ici (Chips n'apparaît plus à ce niveau),
            # on met Object par défaut avec un commentaire à compléter.
            getter_name = 'get' + data_name[0].upper() + data_name[1:]
            lines.append(f'    @Data(name = "{data_name}", accessTypePort = DataOut.AccessType.any)')
            lines.append(f'    public Object {getter_name}() {{')   # TODO : ajouter le type de l'élément renvoyé
            lines.append('        // TODO: retourner la valeur de la donnée')   
            lines.append('        return null;')    # TODO : renvoyer le bon élément
            lines.append('    }')
            lines.append('')

    lines.append('}')
    lines.append('')
    return '\n'.join(lines)


def generate_glue_java(model_name, components, data_wires, require_rules,
                      package, glue_class_name):
    """Génère la classe de glue (TwoSynchronGlueBuilder)."""
    # Construire un mapping name → class_name pour pouvoir référencer .class
    name_to_cls = {c['name']: class_name_for(c['name']) for c in components}

    lines = [
        f'package {package};',
        '',
        'import org.javabip.glue.TwoSynchronGlueBuilder;',
        '',
        f'/**',
        f' * Glue générée à partir du modèle JavaBIP "{model_name}".',
        f' */',
        f'public class {glue_class_name} extends TwoSynchronGlueBuilder {{',
        '',
        '    @Override',
        '    public void configure() {',
    ]

    # --- Synchrons (issus des RequireRules) ---
    if require_rules:
        lines.append('')
        lines.append('        // === SYNCHRONS ===')
        lines.append('        // Chaque RequireRule { effect, causes } => synchron(cause).to(effect)')
        lines.append('')
        for (eff_c, eff_p, cause_c, cause_p) in require_rules:
            eff_cls = name_to_cls.get(eff_c, eff_c)
            cause_cls = name_to_cls.get(cause_c, cause_c)
            lines.append(f'        synchron({cause_cls}.class, "{cause_p}")'
                         f'.to({eff_cls}.class, "{eff_p}");')

    # --- DataWires ---
    if data_wires:
        lines.append('')
        lines.append('        // === DATA WIRES ===')
        lines.append('')
        for (from_c, from_p, to_c, to_p) in data_wires:
            from_cls = name_to_cls.get(from_c, from_c)
            to_cls   = name_to_cls.get(to_c, to_c)
            # data(SourceClass.class, "port_send").to(TargetClass.class, "port_recv")
            # Le nom de la donnée côté destination est le nom du port "recv_X" sans préfixe.
            # Idem côté source : send_X → X.
            from_data = from_p.split('_', 1)[1] if '_' in from_p else from_p
            to_data   = to_p.split('_', 1)[1] if '_' in to_p else to_p
            lines.append(f'        data({from_cls}.class, "{from_data}")'
                         f'.to({to_cls}.class, "{to_data}");')

    lines.append('    }')
    lines.append('}')
    lines.append('')
    return '\n'.join(lines)


# =============================================================================
# MAIN
# =============================================================================

def main():
    parser = argparse.ArgumentParser(
        description='Génère du code Java JavaBIP à partir d\'un XMI JavaBIP.')
    parser.add_argument('xmi', help='Chemin vers le fichier XMI JavaBIP')
    parser.add_argument('outdir', help='Répertoire de sortie pour les .java')
    parser.add_argument('--package', default='generated',
                        help='Nom du package Java (défaut : generated)')
    parser.add_argument('--glue-class', default='GeneratedGlue',
                        help='Nom de la classe de glue (défaut : GeneratedGlue)')
    args = parser.parse_args()

    print(f'Parsing {args.xmi}...')
    components, data_wires, require_rules = parse_xmi(args.xmi)

    print(f'Trouvé : {len(components)} ComponentType, '
          f'{len(data_wires)} DataWire, {len(require_rules)} RequireRule')

    # Créer le répertoire de sortie
    os.makedirs(args.outdir, exist_ok=True)

    # Générer une classe par composant
    for comp in components:
        cls = class_name_for(comp['name'])
        path = os.path.join(args.outdir, f'{cls}.java')
        with open(path, 'w') as f:
            f.write(generate_component_java(comp, args.package))
        print(f'  Généré : {path}')

    # Générer la classe de glue
    glue_path = os.path.join(args.outdir, f'{args.glue_class}.java')
    with open(glue_path, 'w') as f:
        # On a besoin du nom du modèle pour la doc
        tree = ET.parse(args.xmi)
        root = tree.getroot()
        model_elem = next((c for c in root if c.tag.endswith('JavaBIPModel')), None)
        model_name = model_elem.get('name', 'Unknown') if model_elem is not None else 'Unknown'
        f.write(generate_glue_java(model_name, components, data_wires,
                                   require_rules, args.package, args.glue_class))
    print(f'  Généré : {glue_path}')



if __name__ == '__main__':
    main()