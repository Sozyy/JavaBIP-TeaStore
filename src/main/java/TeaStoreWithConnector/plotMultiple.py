# #!/usr/bin/env python3
# """
# Script pour comparer l'évolution de la capacité du cache entre plusieurs runs.
# Affiche plusieurs CSV sur le même plot avec une couleur par courbe et une légende.
#
# Usage :
#     python plot_cache_compare.py file1.csv file2.csv file3.csv ...
#     python plot_cache_compare.py run_a.csv:"PID Kp=0.5" run_b.csv:"PID Kp=1.0"
#
# Le suffixe ":label" après un chemin permet de personnaliser le nom dans la légende.
# Sans suffixe, le nom du fichier (sans extension) est utilisé.
# """
#
# import os
# import sys
# from pathlib import Path
#
# import matplotlib.pyplot as plt
# import numpy as np
# import pandas as pd
#
#
# def _smooth_curve(x_values, y_values, window=5, dense_points=300):
#     """Retourne une courbe lissée via moyenne glissante + interpolation dense."""
#     if len(x_values) == 0:
#         return x_values, y_values
#
#     window = max(1, min(window, len(y_values)))
#     smoothed = (
#         pd.Series(y_values)
#         .rolling(window=window, center=True, min_periods=1)
#         .mean()
#         .to_numpy()
#     )
#
#     if len(x_values) < 2:
#         return x_values, smoothed
#
#     dense_x = np.linspace(
#         np.min(x_values), np.max(x_values),
#         num=max(dense_points, len(x_values)),
#     )
#     dense_y = np.interp(dense_x, x_values, smoothed)
#     return dense_x, dense_y
#
#
# def _parse_arg(arg):
#     """Parse un argument 'chemin' ou 'chemin:label' et retourne (path, label)."""
#     if ':' in arg and not arg[1:3] == ':\\':  # éviter de casser les chemins Windows C:\...
#         path, label = arg.rsplit(':', 1)
#         return path, label
#     return arg, None
#
#
# def _load_csv(path):
#     """Charge un CSV et valide les colonnes requises."""
#     try:
#         df = pd.read_csv(path)
#     except FileNotFoundError:
#         print(f"Erreur : Fichier '{path}' non trouvé.")
#         return None
#     except Exception as e:
#         print(f"Erreur lors de la lecture de '{path}' : {e}")
#         return None
#
#     if 'step' not in df.columns or 'capacity' not in df.columns:
#         print(f"Erreur : '{path}' doit contenir les colonnes 'step' et 'capacity'.")
#         return None
#
#     return df.sort_values('step')
#
#
# def plot_cache_comparison(file_args, target_capacity=None):
#     """
#     Compare plusieurs CSV sur le même plot.
#
#     file_args : liste de chemins, optionnellement suffixés par ':label'
#     target_capacity : valeur cible (setpoint PID) à afficher en ligne horizontale
#     """
#     fig, ax = plt.subplots(figsize=(13, 7))
#     fig.suptitle('Comparaison de l\'évolution de la capacité du cache',
#                  fontsize=16, fontweight='bold')
#
#     # Palette de couleurs distinctes (tab10 fonctionne bien jusqu'à 10 courbes)
#     cmap = plt.get_cmap('tab10')
#
#     stats_summary = []
#     plotted = 0
#
#     for idx, arg in enumerate(file_args):
#         path, label = _parse_arg(arg)
#         df = _load_csv(path)
#         if df is None:
#             continue
#
#         if label is None:
#             label = Path(path).stem
#
#         x_values = df['step'].to_numpy(dtype=float)
#         y_values = df['capacity'].to_numpy(dtype=float)
#         smooth_x, smooth_y = _smooth_curve(x_values, y_values, window=5, dense_points=400)
#
#         color = cmap(idx % 10)
#
#         # Courbe brute discrète, sans légende
#         ax.plot(x_values, y_values, color=color, alpha=0.15, linewidth=1)
#
#         # Courbe lissée principale, avec légende
#         ax.plot(smooth_x, smooth_y, color=color, linewidth=2.0, label=label)
#
#         stats_summary.append((label, df))
#         plotted += 1
#
#     if plotted == 0:
#         print("Aucune courbe à afficher.")
#         sys.exit(1)
#
#     # Setpoint optionnel
#     if target_capacity is not None:
#         ax.axhline(target_capacity, color='black', linestyle='--',
#                    linewidth=1.2, alpha=0.6,
#                    label=f'Cible = {target_capacity}')
#
#     ax.set_xlabel('Step')
#     ax.set_ylabel('Capacity')
#     ax.set_title('Cache Capacity Over Time')
#     ax.grid(True, alpha=0.25)
#     ax.legend(loc='best', framealpha=0.9)
#
#     plt.tight_layout()
#     plt.show()
#
#     # Stats par run
#     print("\n" + "=" * 60)
#     print("STATISTIQUES COMPARATIVES")
#     print("=" * 60)
#     for label, df in stats_summary:
#         print(f"\n[{label}]  ({len(df)} steps)")
#         print(f"  Capacity     : min={df['capacity'].min()}  "
#               f"max={df['capacity'].max()}  "
#               f"moy={df['capacity'].mean():.1f}  "
#               f"std={df['capacity'].std():.1f}")
#         if 'responseTime' in df.columns:
#             print(f"  ResponseTime : min={df['responseTime'].min():.1f}  "
#                   f"max={df['responseTime'].max():.1f}  "
#                   f"moy={df['responseTime'].mean():.1f} ms")
#         if 'hits' in df.columns and 'misses' in df.columns:
#             total = df['hits'].sum() + df['misses'].sum()
#             if total > 0:
#                 hit_rate = 100 * df['hits'].sum() / total
#                 print(f"  Hit rate     : {hit_rate:.1f}%  "
#                       f"(hits={df['hits'].sum()}, misses={df['misses'].sum()})")
#     print("=" * 60 + "\n")
#
#
# def _print_usage():
#     print(__doc__)
#
#
# if __name__ == '__main__':
#     if len(sys.argv) < 2:
#         _print_usage()
#         sys.exit(1)
#
#     args = sys.argv[1:]
#
#     # Option facultative : --target=80 pour tracer une ligne de setpoint
#     target = None
#     filtered = []
#     for a in args:
#         if a.startswith('--target='):
#             try:
#                 target = float(a.split('=', 1)[1])
#             except ValueError:
#                 print(f"Valeur --target invalide : {a}")
#                 sys.exit(1)
#         elif a in ('-h', '--help'):
#             _print_usage()
#             sys.exit(0)
#         else:
#             filtered.append(a)
#
#     if not filtered:
#         _print_usage()
#         sys.exit(1)
#
#     plot_cache_comparison(filtered, target_capacity=target)

#!/usr/bin/env python3
"""
Script pour comparer l'évolution de la capacité du cache entre plusieurs runs
avec un plot INTERACTIF (Plotly).

Interactions :
- Survol d'une courbe -> tooltip avec le nom du run + valeurs
- Clic sur un nom dans la légende -> masque/réaffiche la courbe
- Double-clic sur un nom dans la légende -> isole cette courbe (cache toutes les autres)
- Zoom à la souris, pan, reset, export PNG (boutons en haut à droite)

Usage :
    python plot_cache_compare_interactive.py file1.csv file2.csv ...
    python plot_cache_compare_interactive.py "run_a.csv:PID Kp=0.5" "run_b.csv:Kp=1.0"
    python plot_cache_compare_interactive.py *.csv --target=100

Dépendance : pip install plotly pandas numpy
"""

import sys
from pathlib import Path

import numpy as np
import pandas as pd

try:
    import plotly.graph_objects as go
except ImportError:
    print("Erreur : plotly n'est pas installé. Fais : pip install plotly")
    sys.exit(1)


def _smooth_curve(x_values, y_values, window=5, dense_points=300):
    """Retourne une courbe lissée via moyenne glissante + interpolation dense."""
    if len(x_values) == 0:
        return x_values, y_values

    window = max(1, min(window, len(y_values)))
    smoothed = (
        pd.Series(y_values)
        .rolling(window=window, center=True, min_periods=1)
        .mean()
        .to_numpy()
    )

    if len(x_values) < 2:
        return x_values, smoothed

    dense_x = np.linspace(
        np.min(x_values), np.max(x_values),
        num=max(dense_points, len(x_values)),
    )
    dense_y = np.interp(dense_x, x_values, smoothed)
    return dense_x, dense_y


def _parse_arg(arg):
    """Parse 'chemin' ou 'chemin:label'. Gère les chemins Windows C:\\..."""
    if ':' in arg and not (len(arg) > 2 and arg[1:3] in (':\\', ':/')):
        path, label = arg.rsplit(':', 1)
        return path, label
    return arg, None


def _load_csv(path):
    try:
        df = pd.read_csv(path)
    except FileNotFoundError:
        print(f"Erreur : Fichier '{path}' non trouvé.")
        return None
    except Exception as e:
        print(f"Erreur lors de la lecture de '{path}' : {e}")
        return None

    if 'step' not in df.columns or 'capacity' not in df.columns:
        print(f"Erreur : '{path}' doit contenir les colonnes 'step' et 'capacity'.")
        return None

    return df.sort_values('step')


# Palette de couleurs distinctes (équivalent tab10 / tab20)
PALETTE = [
    '#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd',
    '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf',
    '#aec7e8', '#ffbb78', '#98df8a', '#ff9896', '#c5b0d5',
    '#c49c94', '#f7b6d2', '#c7c7c7', '#dbdb8d', '#9edae5',
]


def plot_cache_comparison(file_args, target_capacity=None, output_html=None):
    fig = go.Figure()
    stats_summary = []
    plotted = 0

    for idx, arg in enumerate(file_args):
        path, label = _parse_arg(arg)
        df = _load_csv(path)
        if df is None:
            continue

        if label is None:
            label = Path(path).stem

        x_values = df['step'].to_numpy(dtype=float)
        y_values = df['capacity'].to_numpy(dtype=float)
        smooth_x, smooth_y = _smooth_curve(x_values, y_values, window=5, dense_points=400)

        color = PALETTE[idx % len(PALETTE)]

        # Courbe brute discrète, regroupée avec la lissée via legendgroup
        # -> elle se cache/s'affiche en même temps que la lissée
        fig.add_trace(go.Scatter(
            x=x_values, y=y_values,
            mode='lines',
            line=dict(color=color, width=1),
            opacity=0.18,
            name=label,
            legendgroup=label,
            showlegend=False,
            hoverinfo='skip',  # pas de tooltip sur la brute, sinon ça spamme
        ))

        # Courbe lissée principale
        fig.add_trace(go.Scatter(
            x=smooth_x, y=smooth_y,
            mode='lines',
            line=dict(color=color, width=2.2),
            name=label,
            legendgroup=label,
            hovertemplate=(
                f"<b>{label}</b><br>"
                "step = %{x:.0f}<br>"
                "capacity = %{y:.2f}"
                "<extra></extra>"
            ),
        ))

        stats_summary.append((label, df))
        plotted += 1

    if plotted == 0:
        print("Aucune courbe à afficher.")
        sys.exit(1)

    # Ligne de cible (setpoint)
    if target_capacity is not None:
        fig.add_hline(
            y=target_capacity,
            line=dict(color='black', width=1.2, dash='dash'),
            annotation_text=f'Cible = {target_capacity}',
            annotation_position='top right',
            opacity=0.6,
        )

    fig.update_layout(
        title=dict(
            text='<b>Cache capacity over time (P=0.8 / I=0.05 / D=0.2)</b>',
            x=0.5, xanchor='center',
        ),
        xaxis_title='Step',
        yaxis_title='Capacity',
        hovermode='closest',          # met en valeur la courbe la plus proche
        template='plotly_white',
        legend=dict(
            title='Runs',
            bgcolor='rgba(255,255,255,0.85)',
            bordercolor='lightgray',
            borderwidth=1,
        ),
        margin=dict(l=60, r=40, t=80, b=60),
    )

    # Effet "highlight on hover" : la courbe survolée passe au premier plan
    # via la propriété hoverlabel
    fig.update_traces(
        hoverlabel=dict(bgcolor='white', font_size=12, bordercolor='gray'),
    )

    # Affichage : navigateur par défaut, ou HTML sur disque si demandé
    if output_html:
        fig.write_html(output_html, include_plotlyjs='cdn')
        print(f"Plot sauvegardé : {output_html}")
    else:
        fig.show()

    # Stats console
    print("\n" + "=" * 60)
    print("STATISTIQUES COMPARATIVES")
    print("=" * 60)
    for label, df in stats_summary:
        print(f"\n[{label}]  ({len(df)} steps)")
        print(f"  Capacity     : min={df['capacity'].min()}  "
              f"max={df['capacity'].max()}  "
              f"moy={df['capacity'].mean():.1f}  "
              f"std={df['capacity'].std():.1f}")
        if 'responseTime' in df.columns:
            print(f"  ResponseTime : min={df['responseTime'].min():.1f}  "
                  f"max={df['responseTime'].max():.1f}  "
                  f"moy={df['responseTime'].mean():.1f} ms")
        if 'hits' in df.columns and 'misses' in df.columns:
            total = df['hits'].sum() + df['misses'].sum()
            if total > 0:
                hit_rate = 100 * df['hits'].sum() / total
                print(f"  Hit rate     : {hit_rate:.1f}%  "
                      f"(hits={df['hits'].sum()}, misses={df['misses'].sum()})")
    print("=" * 60 + "\n")


def _print_usage():
    print(__doc__)


if __name__ == '__main__':
    if len(sys.argv) < 2:
        _print_usage()
        sys.exit(1)

    args = sys.argv[1:]
    target = None
    output_html = None
    filtered = []

    for a in args:
        if a.startswith('--target='):
            try:
                target = float(a.split('=', 1)[1])
            except ValueError:
                print(f"Valeur --target invalide : {a}")
                sys.exit(1)
        elif a.startswith('--output='):
            output_html = a.split('=', 1)[1]
        elif a in ('-h', '--help'):
            _print_usage()
            sys.exit(0)
        else:
            filtered.append(a)

    if not filtered:
        _print_usage()
        sys.exit(1)

    plot_cache_comparison(filtered, target_capacity=target, output_html=output_html)