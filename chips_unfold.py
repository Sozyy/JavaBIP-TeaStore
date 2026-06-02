#!/usr/bin/env python3
"""
chips_unfold.py

Préprocesseur qui déplie les foreach d'un XMI Chips.

Pour chaque <system_statements xsi:type="...:foreach">, le préprocesseur :
  1. Évalue la borne (typiquement range(nbUsers) avec nbUsers assigné à 10)
  2. Pour chaque indice i ∈ [0, borne), copie le contenu interne du foreach
  3. Dans chaque copie, remplace les références à la variable iterator par
     un direct_int avec value=i
  4. Insère les copies à la place du foreach (à la racine de system_statements)

Le résultat est un nouveau XMI Chips sémantiquement équivalent mais sans foreach.
Les feedings avec collective_cast, expressions arithmétiques en index, ou autres
constructs complexes sont copiés tels quels (le ATL en aval les ignorera).

Usage:
    python3 chips_unfold.py <input.xmi> <output.xmi>
"""

import sys
import copy
import argparse
import xml.etree.ElementTree as ET
from xml.etree.ElementTree import Element


# Namespaces XMI/EMF utilisés par Chips
XSI_NS = 'http://www.w3.org/2001/XMLSchema-instance'
XMI_NS = 'http://www.omg.org/XMI'

ET.register_namespace('xsi', XSI_NS)
ET.register_namespace('xmi', XMI_NS)


# =============================================================================
# UTILITAIRES DE PARSING/RÉSOLUTION
# =============================================================================

def xsi_type(elem):
    """Retourne l'attribut xsi:type d'un élément, ou None."""
    return elem.get(f'{{{XSI_NS}}}type')


def is_foreach(elem):
    """Vrai si l'élément est un <system_statements xsi:type='...foreach'>."""
    t = xsi_type(elem)
    return t is not None and t.endswith(':foreach')


def is_int_assignment(elem):
    """Vrai si c'est un int_assignment (au niveau system)."""
    t = xsi_type(elem)
    return t is not None and t.endswith(':int_assignment')


def is_direct_int(elem):
    """Vrai si c'est un direct_int (constante entière)."""
    t = xsi_type(elem)
    return t is not None and t.endswith(':direct_int')


def is_int_variable_expression(elem):
    """Vrai si c'est un int_variable_expression."""
    t = xsi_type(elem)
    return t is not None and t.endswith(':int_variable_expression')


def xpath_for(elem, root):
    """
    Calcule le fragment XMI d'un élément, du style
    "//@system/@system_statements.5/@variable".
    Sert pour générer les références vers les iterators dépliés.
    NB: utilisé pour debug, pas indispensable au dépliage.
    """
    # Non implémenté ici : on n'a pas besoin de générer de nouvelles refs,
    # on substitue uniquement le contenu des éléments par valeur.
    return None


# =============================================================================
# RÉSOLUTION DES RÉFÉRENCES INTERNES AU XMI
# =============================================================================

def build_xpath_index(root):
    """
    Construit une table { xpath_fragment -> Element } qui permet de résoudre
    les attributs comme variable="//@system/@system_statements.27/@iterator/@variable".

    Le format EMF est : //@feature1/@feature2.index/@feature3...
    Chaque token est soit @feature (1ère occurrence), soit @feature.N (N-ième).
    """
    index = {}

    def visit(elem, path):
        # On enregistre le chemin courant
        index[path] = elem

        # Compter les enfants par balise pour pouvoir indexer
        children_by_tag = {}
        for child in elem:
            # On enlève le namespace de la balise
            tag = child.tag.split('}', 1)[-1] if '}' in child.tag else child.tag
            children_by_tag.setdefault(tag, []).append(child)

        # Visiter récursivement
        for tag, siblings in children_by_tag.items():
            for i, child in enumerate(siblings):
                # //@tag pour le premier, //@tag.N pour les suivants
                if len(siblings) == 1:
                    sub_path = f'{path}/@{tag}'
                else:
                    sub_path = f'{path}/@{tag}.{i}'
                visit(child, sub_path)

    visit(root, '/')
    return index


def resolve_ref(ref, xpath_index):
    """
    Résout une référence EMF du style "//@system/@system_statements.27/@iterator/@variable"
    en l'élément XML correspondant. Retourne None si non trouvé.
    """
    if ref is None:
        return None
    # EMF utilise // pour la racine
    if ref.startswith('//'):
        ref = '/' + ref[2:]
    return xpath_index.get(ref)


# =============================================================================
# ÉVALUATION DES BORNES DE FOREACH
# =============================================================================

def find_system_statements(root):
    """Retourne la liste des <system_statements> enfants du <system>."""
    # On cherche le sous-élément <system>
    system = None
    for child in root:
        tag = child.tag.split('}', 1)[-1] if '}' in child.tag else child.tag
        if tag == 'system':
            system = child
            break
    if system is None:
        return None, []
    # Et ses <system_statements>
    stmts = []
    for child in system:
        tag = child.tag.split('}', 1)[-1] if '}' in child.tag else child.tag
        if tag == 'system_statements':
            stmts.append(child)
    return system, stmts


def resolve_int_value(variable_elem, system, system_stmts, xpath_index):
    """
    Étant donné l'élément <variable> d'un int_declaration (nbUsers, nbCaches, etc.),
    cherche dans system_stmts un int_assignment dont la lvalue.variable pointe vers
    cette variable, et retourne la valeur du rvalue (direct_int).
    Retourne None si impossible à résoudre.
    """
    for stmt in system_stmts:
        if not is_int_assignment(stmt):
            continue
        # Chercher <lvalue> puis l'attribut "variable"
        lvalue = None
        rvalue = None
        for child in stmt:
            tag = child.tag.split('}', 1)[-1] if '}' in child.tag else child.tag
            if tag == 'lvalue':
                lvalue = child
            elif tag == 'rvalue':
                rvalue = child
        if lvalue is None or rvalue is None:
            continue

        # L'attribut variable de la lvalue est une référence
        lvalue_var_ref = lvalue.get('variable')
        lvalue_var = resolve_ref(lvalue_var_ref, xpath_index)
        if lvalue_var is not variable_elem:
            continue

        # OK, c'est la bonne assignation. Lire la rvalue
        if is_direct_int(rvalue):
            value_str = rvalue.get('value')
            try:
                return int(value_str)
            except (TypeError, ValueError):
                return None

    return None


def evaluate_foreach_bound(foreach_elem, system, system_stmts, xpath_index):
    """
    Évalue la borne d'un foreach. Le foreach Chips a typiquement :
      <iterable_expr xsi:type="chips.rvalues.system:function" name="range">
        <parameters xsi:type="...:int_variable_expression" variable="//@system/@..." />
      </iterable_expr>
    On extrait le paramètre de range(), on le résout en variable système, et on lit sa valeur.
    Retourne None si impossible à évaluer.
    """
    iterable_expr = None
    for child in foreach_elem:
        tag = child.tag.split('}', 1)[-1] if '}' in child.tag else child.tag
        if tag == 'iterable_expr':
            iterable_expr = child
            break
    if iterable_expr is None:
        return None

    # On suppose que c'est range(N). On regarde son premier parameter.
    name = iterable_expr.get('name', '')
    if name != 'range':
        # Autre fonction : pas supporté
        return None

    # Trouver le premier <parameters>
    param = None
    for child in iterable_expr:
        tag = child.tag.split('}', 1)[-1] if '}' in child.tag else child.tag
        if tag == 'parameters':
            param = child
            break
    if param is None:
        return None

    # param peut être un direct_int (cas : range(5)) ou un int_variable_expression
    if is_direct_int(param):
        try:
            return int(param.get('value'))
        except (TypeError, ValueError):
            return None
    elif is_int_variable_expression(param):
        var_ref = param.get('variable')
        var_elem = resolve_ref(var_ref, xpath_index)
        if var_elem is None:
            return None
        return resolve_int_value(var_elem, system, system_stmts, xpath_index)

    return None


# =============================================================================
# EXTRACTION DE L'ITERATOR DU FOREACH
# =============================================================================

def get_iterator_variable(foreach_elem):
    """
    Récupère l'élément <variable> de l'iterator du foreach :
      <iterator xsi:type="...:int_declaration">
        <variable name="i" />
      </iterator>
    Retourne l'élément <variable> ou None.
    """
    for child in foreach_elem:
        tag = child.tag.split('}', 1)[-1] if '}' in child.tag else child.tag
        if tag == 'iterator':
            for sub in child:
                sub_tag = sub.tag.split('}', 1)[-1] if '}' in sub.tag else sub.tag
                if sub_tag == 'variable':
                    return sub
    return None


def get_foreach_statements(foreach_elem):
    """
    Récupère les <statements> internes du foreach (ses enfants directs du tag
    'statements'), dans l'ordre.
    """
    result = []
    for child in foreach_elem:
        tag = child.tag.split('}', 1)[-1] if '}' in child.tag else child.tag
        if tag == 'statements':
            result.append(child)
    return result


# =============================================================================
# SUBSTITUTION DE L'ITERATOR DANS UNE COPIE DE STATEMENT
# =============================================================================

def make_direct_int_element(value):
    """
    Crée un élément <int_variable_expression> remplacé par <direct_int value="N"/>.
    En XMI Chips, on injectera le xsi:type approprié.

    Attention : on doit produire un élément qui aura le BON tag (celui du parent
    qui le contenait) et le BON xsi:type. Ici on retourne un dict d'attributs
    que l'appelant utilisera.
    """
    # Ce helper n'est pas utilisé directement, voir substitute_iterator_in_subtree
    pass


def substitute_iterator_in_subtree(elem, iterator_var_elem, iterator_value,
                                    iterator_var_xpath):
    """
    Parcourt récursivement un sous-arbre XML et remplace toute référence à
    l'iterator par sa valeur entière courante.

    Une référence à l'iterator se présente comme un élément <X xsi:type="...:int_variable_expression"
    variable="//@system/@system_statements.27/@iterator/@variable" />.
    On le transforme en <X xsi:type="...:direct_int" value="N" />.

    iterator_var_xpath est la chaîne EMF qui désigne la variable iterator
    (ex: "//@system/@system_statements.27/@iterator/@variable").
    """
    # Parcours en profondeur ; on doit modifier les enfants au passage.
    # On utilise une approche : pour chaque élément, examiner ses attributs
    # pour voir s'il référence l'iterator. Si oui, on transforme l'élément.

    # 1. Si elem lui-même est un int_variable_expression pointant vers l'iterator
    if is_int_variable_expression(elem):
        var_ref = elem.get('variable')
        if var_ref is not None and normalize_ref(var_ref) == normalize_ref(iterator_var_xpath):
            # Transformer : changer xsi:type en :direct_int, retirer attr "variable",
            # ajouter attr "value"
            # Trouver le préfixe ":direct_int" approprié - on récupère depuis le type actuel
            current_type = xsi_type(elem)
            # int_variable_expression apparaît dans plusieurs packages.
            # On choisit le direct_int correspondant.
            # Format type : "chips.xvalues.system:int_variable_expression"
            # → "chips.rvalues.system:direct_int" (pour les indices, c'est rvalues qu'on veut)
            if current_type:
                # Conversion empirique vers le package rvalues correspondant
                # int_variable_expression peut être dans xvalues (cas indices)
                # ou dans rvalues. Le direct_int correspond à rvalues.
                if 'xvalues.system' in current_type:
                    new_type = current_type.replace('xvalues.system:int_variable_expression',
                                                     'rvalues.system:direct_int')
                elif 'xvalues.primitive' in current_type:
                    new_type = current_type.replace('xvalues.primitive:int_variable_expression',
                                                     'rvalues.primitive:direct_int')
                elif 'xvalues.collective' in current_type:
                    new_type = current_type.replace('xvalues.collective:int_variable_expression',
                                                     'rvalues.collective:direct_int')
                else:
                    # Fallback : on garde le type tel quel mais on change le nom de classe
                    new_type = current_type.rsplit(':', 1)[0] + ':direct_int'
                elem.set(f'{{{XSI_NS}}}type', new_type)
            elem.set('value', str(iterator_value))
            if 'variable' in elem.attrib:
                del elem.attrib['variable']

    # 2. Récursion sur les enfants
    for child in elem:
        substitute_iterator_in_subtree(child, iterator_var_elem, iterator_value,
                                        iterator_var_xpath)


def normalize_ref(ref):
    """Normalise une référence EMF (//... ou /...) en forme canonique."""
    if ref is None:
        return None
    if ref.startswith('//'):
        return '/' + ref[2:]
    return ref


def find_iterator_xpath(system_stmts, foreach_elem, xpath_index):
    """
    Construit le xpath EMF de la variable iterator d'un foreach donné.
    Format produit : "//@system/@system_statements.N/@iterator/@variable"
    où N est l'indice du foreach dans system_statements.
    """
    for i, stmt in enumerate(system_stmts):
        if stmt is foreach_elem:
            return f'//@system/@system_statements.{i}/@iterator/@variable'
    return None


# =============================================================================
# DÉPLIAGE PRINCIPAL
# =============================================================================

def unfold_foreach(foreach_elem, foreach_idx, bound, system, system_stmts,
                   xpath_index):
    """
    Déplie un foreach en générant `bound` copies de ses statements internes,
    avec substitution de l'iterator par 0, 1, ..., bound-1.
    Retourne la liste des nouveaux statements à ajouter au system.
    """
    iterator_var = get_iterator_variable(foreach_elem)
    if iterator_var is None:
        print(f'  [warn] foreach #{foreach_idx} sans iterator, ignoré')
        return []

    iterator_xpath = find_iterator_xpath(system_stmts, foreach_elem, xpath_index)

    inner_statements = get_foreach_statements(foreach_elem)
    if not inner_statements:
        return []

    print(f'  Foreach #{foreach_idx} : déroulement sur [0..{bound-1}]'
          f' × {len(inner_statements)} statements internes'
          f' = {bound * len(inner_statements)} nouveaux statements')

    new_statements = []
    for i in range(bound):
        for stmt in inner_statements:
            # Deep copy pour isoler la nouvelle copie
            stmt_copy = copy.deepcopy(stmt)
            # Substitution de l'iterator par i
            substitute_iterator_in_subtree(stmt_copy, iterator_var, i, iterator_xpath)
            # Renommer le tag de 'statements' à 'system_statements'
            # Le namespace est conservé
            ns = stmt_copy.tag.split('}', 1)[0] + '}' if '}' in stmt_copy.tag else ''
            stmt_copy.tag = ns + 'system_statements'
            new_statements.append(stmt_copy)

    return new_statements


# =============================================================================
# MAIN
# =============================================================================

def main():
    parser = argparse.ArgumentParser(
        description='Déplie les foreach d\'un XMI Chips.')
    parser.add_argument('input', help='Fichier XMI Chips en entrée')
    parser.add_argument('output', help='Fichier XMI Chips déplié en sortie')
    args = parser.parse_args()

    print(f'Lecture de {args.input}...')
    tree = ET.parse(args.input)
    root = tree.getroot()

    # Indexer les xpath EMF pour la résolution des références
    print('Indexation des xpath EMF...')
    xpath_index = build_xpath_index(root)

    # Trouver le system et ses statements
    system, system_stmts = find_system_statements(root)
    if system is None:
        print('Erreur : aucun <system> trouvé', file=sys.stderr)
        return 1

    print(f'Trouvé {len(system_stmts)} system_statements au total')

    # Identifier les foreach et leurs bornes
    foreaches = [(i, s) for i, s in enumerate(system_stmts) if is_foreach(s)]
    print(f'Trouvé {len(foreaches)} foreach à déplier')

    # Pour chaque foreach, calculer la borne et déplier
    additions = []  # statements à ajouter
    to_remove = []  # foreach à retirer
    for idx, fe in foreaches:
        bound = evaluate_foreach_bound(fe, system, system_stmts, xpath_index)
        if bound is None:
            print(f'  [warn] foreach #{idx} : borne non évaluable, conservé tel quel')
            continue
        if bound <= 0:
            print(f'  [warn] foreach #{idx} : borne <= 0 ({bound}), conservé tel quel')
            continue

        unfolded = unfold_foreach(fe, idx, bound, system, system_stmts, xpath_index)
        additions.extend(unfolded)
        to_remove.append(fe)

    # Supprimer les foreach dépliés
    for fe in to_remove:
        system.remove(fe)

    # Ajouter les statements dépliés à la fin du system
    for stmt in additions:
        system.append(stmt)

    print(f'Total : {len(to_remove)} foreach supprimés, '
          f'{len(additions)} nouveaux statements ajoutés')

    # Écrire le résultat
    tree.write(args.output, encoding='ASCII', xml_declaration=True)
    print(f'Écrit dans {args.output}')

    return 0


if __name__ == '__main__':
    sys.exit(main())