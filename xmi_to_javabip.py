#!/usr/bin/env python3
"""
xmi_to_javabip.py

Generate JavaBIP project from an XMI model conforming to the JavaBIP metamodel.
The output XMI file is produced by the ATL transformation CHIPS to JavaBIP.

Usage:
    python3 xmi_to_javabip.py <model.xmi> <output_directory> [--package NAME] [--glue-class NAME]
    python3 xmi_to_javabip.py <model.xmi> --src-root <maven_src_root> [--package NAME] [--glue-class NAME]

Avec --src-root, le répertoire de sortie effectif est <src_root>/<package/as/path>/,
ce qui permet de déposer directement les sources dans un projet Maven existant
(ex: --src-root myproject/src/main/java --package com.example.generated).

This script parses the XMI file to produce :
  - A JavaBIP class per ComponentType (with @Port, @ComponentType, @Transition and @Data getters annotations).
  - A glue Java class (extending TwoSynchronGlueBuilder) with a configure() that calls synchron(...).to(...) for each RequireRule and data(...).to(...) for each DataWire
  - A Main class that initializes the BIP engine, registers all components, glue them together and starts the engine.

Important note :
  - The generated code is not working, only a base is generated and all of the Java logic is not done yet
"""

import sys
import os
import argparse
import xml.etree.ElementTree as ET
from collections import OrderedDict


NS = {
    'xmi':      'http://www.omg.org/XMI',
    'behavior': 'http://JavaBIP/behavior',
    'glue':     'http://JavaBIP/glue',
}


def parse_xmi(xmi_path):
    """
    Parses XMI files and returns :
      - components : list of ComponentType
      - data_wires : list of tuples (from_component, from_port, to_component, to_port)
      - require_rules : list of tuples (effect_component, effect_port, cause_component, cause_port)

    The XMI references its subelements via indices as '\42'.
    It builds an index table by iterating over the children of the root in order.
    """
    tree = ET.parse(xmi_path)
    root = tree.getroot()

    children = list(root)
    index_to_elem = {f'/{i}': child for i, child in enumerate(children)}

    def resolve(ref):
        """Resolve a reference as '\42' into the corresponding XML."""
        return index_to_elem.get(ref.strip())

    def resolve_attr(elem, attr_name):
        """Resolve an attribute that contains a reference '/N'."""
        ref = elem.get(attr_name)
        return resolve(ref) if ref else None

    # Find the JavaBIPModel
    model = None
    for child in children:
        if child.tag.endswith('JavaBIPModel'):
            model = child
            break
    if model is None:
        raise RuntimeError("No JavaBIPModel found in the XMI")

    # Extracts ComponentType
    component_refs = model.get('components', '').split()
    components = []
    for cref in component_refs:
        c = resolve(cref)
        if c is None:
            continue
        components.append(parse_component(c, resolve, index_to_elem))

    # Glue
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
            cause_refs = rr.get('causes', '').split()

            for cref in cause_refs:
                cause_pr = resolve(cref)
                require_rules.append(parse_port_ref_pair(effect_pr, cause_pr, resolve))

    return components, data_wires, require_rules


def parse_component(elem, resolve, index_to_elem):
    """
    Gets informations from a ComponentType.
    """
    name = elem.get('name')
    java_class = elem.get('javaClassName') or name
    cardinality = elem.get('cardinality', '1')
    initial_ref = elem.get('initialState')
    initial_elem = resolve(initial_ref) if initial_ref else None
    initial_state = initial_elem.get('name') if initial_elem is not None else 'INIT'

    # states
    state_refs = elem.get('states', '').split()
    states = []
    for sref in state_refs:
        s = resolve(sref)
        if s is not None:
            states.append(s.get('name'))

    # ports
    port_refs = elem.get('ports', '').split()
    ports = []
    for pref in port_refs:
        p = resolve(pref)
        if p is not None:
            ports.append({
                'name': p.get('name'),
                'type': p.get('type', 'enforceable'),
            })

    # transitions
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
    Parses a pair of PortRef to a tuple (comp1, port1, comp2, port2)
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


def class_name_for(comp_name):
    """
    Put the first letter in uppercase
    """
    if not comp_name:
        return 'Component'
    return comp_name[0].upper() + comp_name[1:]


def generate_component_java(comp, package, data_wires):
    """
    Code generator for a ComponentType
    """
    cls_name = class_name_for(comp['name'])

    recv_data = {}
    for (_, _, to_c, to_p) in data_wires:
        if to_c == comp['name']:
            recv_data[to_p] = to_p.split('_', 1)[1] if '_' in to_p else to_p

    lines = [
        f'package {package};',
        '',
        'import org.javabip.annotations.*;',
        'import org.javabip.api.PortType;',
        'import org.javabip.api.DataOut;',
        '', 
    ]

    lines.append('@Ports({')
    port_decls = []
    declared_ports = set()

    for p in comp['ports']:
        port_decls.append(f'    @Port(name = "{p["name"]}", type = PortType.{p["type"]})')
        declared_ports.add(p['name'])

    for t in comp['transitions']:
        if (t['type'] == 'internal' or t['port'] is None) and t['name'] not in declared_ports:
            port_decls.append(f'    @Port(name = "{t["name"]}", type = PortType.enforceable)')
            declared_ports.add(t['name'])
    lines.append(',\n'.join(port_decls))
    lines.append('})')

    lines.append(f'@ComponentType(name = "{comp["name"]}", initial = "{comp["initial_state"]}")')
    lines.append(f'public class {cls_name} {{')
    lines.append('')

    lines.append(f'    public {cls_name}() {{')
    lines.append('        // TODO : vars declaration, initialization...')
    lines.append('    }')
    lines.append('')

    lines.append('    // === TRANSITIONS ===')
    lines.append('')
    for t in comp['transitions']:
        port_name = t['port'] if t['port'] is not None else t['name']
        lines.append(f'    @Transition(name = "{port_name}", source = "{t["source"]}", target = "{t["target"]}", guard = "")')
        method = t['transition_method'] or t['name']
        data_name = recv_data.get(port_name)
        if data_name:
            lines.append(f'    public void {method}(@Data(name = "{data_name}") Object {data_name}) {{')
        else:
            lines.append(f'    public void {method}() {{')
        lines.append(f'        System.out.println("[{cls_name}] {t["source"]} -> {t["target"]} (port: {port_name})");')
        lines.append('        // TODO : transition logic')
        lines.append('    }')
        lines.append('')


    senders = [p for p in comp['ports']
               if p['name'].startswith('send_') or p['name'].startswith('actuate_')]
    if senders:
        lines.append('    // === DATA WIRES ===')
        lines.append('')
        for p in senders:
            data_name = p['name'].split('_', 1)[1] if '_' in p['name'] else p['name']
            # just a base, not the actual type
            getter_name = 'get' + data_name[0].upper() + data_name[1:]
            lines.append(f'    @Data(name = "{data_name}", accessTypePort = DataOut.AccessType.any)')
            lines.append(f'    public Object {getter_name}() {{')   # TODO get the not null ofc type
            lines.append('        // TODO : return the actual data')   
            lines.append('        return null;')    # TODO not return null ofc
            lines.append('    }')
            lines.append('')

    lines.append('}')
    lines.append('')
    return '\n'.join(lines)


def generate_glue_java(model_name, components, data_wires, require_rules, package, glue_class_name):
    """
    Glue class generator using TwoSynchronGlueBuilder
    """
    name_to_cls = {c['name']: class_name_for(c['name']) for c in components}

    lines = [
        f'package {package}.glue;',
        '',
        f'import {package}.*;',
        'import org.javabip.glue.TwoSynchronGlueBuilder;',
        '',
        f'public class {glue_class_name} extends TwoSynchronGlueBuilder {{',
        '',
        '    @Override',
        '    public void configure() {',
    ]

    if require_rules:
        lines.append('')
        lines.append('        // === SYNCHRONS ===')
        lines.append('')
        for (eff_c, eff_p, cause_c, cause_p) in require_rules:
            eff_cls = name_to_cls.get(eff_c, eff_c)
            cause_cls = name_to_cls.get(cause_c, cause_c)
            lines.append(f'        synchron({cause_cls}.class, "{cause_p}")'
                         f'.to({eff_cls}.class, "{eff_p}");')

    if data_wires:
        lines.append('')
        lines.append('        // === DATA WIRES ===')
        lines.append('')
        for (from_c, from_p, to_c, to_p) in data_wires:
            from_cls = name_to_cls.get(from_c, from_c)
            to_cls   = name_to_cls.get(to_c, to_c)
            from_data = from_p.split('_', 1)[1] if '_' in from_p else from_p
            to_data   = to_p.split('_', 1)[1] if '_' in to_p else to_p
            lines.append(f'        data({from_cls}.class, "{from_data}")'
                         f'.to({to_cls}.class, "{to_data}");')

    lines.append('    }')
    lines.append('}')
    lines.append('')
    return '\n'.join(lines)

def generate_main_java(package, glue_class_name, components):
    """
    Generates a Main class that initializes the BIP engine, registers all components,
    glue them together and starts the engine. 
    The end condition is a single sleep but have to be changed.
    """
    reg_lines = []
    for comp in components:
        cls = class_name_for(comp['name'])
        var_name = comp['name'][0].lower() + comp['name'][1:]

        reg_lines.append(f'            {cls} {var_name} = new {cls}();')
        # reg_lines.append(f'            engine.register({var_name}, "{var_name}", true);')

    reg_lines.append('')

    for comp in components:
        cls = class_name_for(comp['name'])
        var_name = comp['name'][0].lower() + comp['name'][1:]
        reg_lines.append(f'            engine.register({var_name}, "{var_name}", true);')

    lines = [
        f'package {package}.executor;',
        '',
        f'import {package}.*;',
        f'import {package}.glue.*;',
        'import akka.actor.ActorSystem;',
        'import org.javabip.api.BIPEngine;',
        'import org.javabip.api.BIPGlue;',
        'import org.javabip.engine.factory.EngineFactory;',
        '',
        'public class Main {',
        '    private ActorSystem system;',
        '    private EngineFactory engineFactory;',
        '',
        '    private void initialize() {',
        '        system = ActorSystem.create("BIPSystem");',
        '        engineFactory = new EngineFactory(system);',
        '    }',
        '',
        '    private void cleanup() {',
        '        if (system != null) {',
        '            system.terminate();',
        '        }',
        '    }',
        '',
        '    public void runDemo() {',
        '        BIPEngine engine = null;',
        '        initialize();',
        '',
        '        try {',
        f'            BIPGlue glue = new {glue_class_name}().build();',
        '            engine = engineFactory.create("glue", glue);',
        '',
    ]

    lines.extend(reg_lines)
    lines += [
        '',
        '            engine.start();',
        '            Thread.sleep(10000); // TODO : while (true) ?',
        '            engine.stop();',
        '            engineFactory.destroy(engine);',
        '        } catch (Exception e) {',
        '            System.err.println(e.getMessage());',
        '        } finally {',
        '            cleanup();',
        '        }',
        '    }',
        '',
        '    public static void main(String[] args) {',
        '        new Main().runDemo();',
        '    }',
        '}',
        '',
    ]
    return '\n'.join(lines)


###

def main():
    parser = argparse.ArgumentParser(
        description='Generate JavaBIP code from a JavaBIP XMI file..')
    parser.add_argument('xmi', help='Path to the XMI file')
    parser.add_argument('outdir', nargs='?', default=None,
                        help='Output directory for the java files')
    parser.add_argument('--src-root', default=None,
                        help='Maven source root (e.g. myproject/src/main/java). '
                             'When set, files are written to <src-root>/<package/path>/. '
                             'Takes precedence over outdir.')
    parser.add_argument('--package', default='generated',
                        help='Package name (default : "generated")')
    parser.add_argument('--glue-class', default='GeneratedGlue',
                        help='Name of the glue class (default : GeneratedGlue)')
    args = parser.parse_args()

    if args.src_root:
        package_path = args.package.replace('.', os.sep)
        effective_outdir = os.path.join(args.src_root, package_path)
    elif args.outdir:
        effective_outdir = args.outdir
    else:
        parser.error('outdir is required when --src-root is not specified.')

    print(f'Parsing {args.xmi}...')
    components, data_wires, require_rules = parse_xmi(args.xmi)

    print(f'Found: {len(components)} ComponentType, '
          f'{len(data_wires)} DataWire, {len(require_rules)} RequireRule')

    os.makedirs(effective_outdir, exist_ok=True)
    print(f'Output directory: {effective_outdir}')

    components = [c for c in components if c['ports'] or c['transitions']]
    print(f'After filtering empty components: {len(components)} ComponentType to generate')

    for comp in components:
        cls = class_name_for(comp['name'])
        path = os.path.join(effective_outdir, f'{cls}.java')
        with open(path, 'w') as f:
            f.write(generate_component_java(comp, args.package, data_wires))
        print(f'  Generated : {path}')

    glue_dir = os.path.join(effective_outdir, 'glue')
    os.makedirs(glue_dir, exist_ok=True)
    glue_path = os.path.join(glue_dir, f'{args.glue_class}.java')
    with open(glue_path, 'w') as f:
        tree = ET.parse(args.xmi)
        root = tree.getroot()
        model_elem = next((c for c in root if c.tag.endswith('JavaBIPModel')), None)
        model_name = model_elem.get('name', 'Unknown') if model_elem is not None else 'Unknown'
        f.write(generate_glue_java(model_name, components, data_wires,
                                   require_rules, args.package, args.glue_class))

    executor_dir = os.path.join(effective_outdir, 'executor')
    os.makedirs(executor_dir, exist_ok=True)
    main_path = os.path.join(executor_dir, 'Main.java')
    with open(main_path, 'w') as f:
        f.write(generate_main_java(args.package, args.glue_class, components))

    print(f'  Généré : {glue_path}')



if __name__ == '__main__':
    main()