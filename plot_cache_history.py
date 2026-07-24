#!/usr/bin/env python3
"""
Script pour visualiser l'historique du cache depuis cache_history.csv.
Version simplifiée : uniquement l'évolution de la capacité du cache, avec une courbe lissée.
"""

import os
import re
import sys

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

def _smooth_curve(x_values, y_values, window=5, dense_points=300):
    """Retourne une courbe lissée via moyenne glissante + interpolation dense."""
    if len(x_values) == 0:
        return x_values, y_values

    window = max(1, min(window, len(y_values)))
    smoothed = pd.Series(y_values).rolling(window=window, center=True, min_periods=1).mean().to_numpy()

    if len(x_values) < 2:
        return x_values, smoothed

    dense_x = np.linspace(np.min(x_values), np.max(x_values), num=max(dense_points, len(x_values)))
    dense_y = np.interp(dense_x, x_values, smoothed)
    return dense_x, dense_y


def _title_from_filename(csv_file):
    """Extrait KP/KI/KD du nom de fichier (ex: cache_history_KP0.9_KI0.05_KD0.15.csv)."""
    match = re.search(r"KP([\d.]+)_KI([\d.]+)_KD([\d.]+)", os.path.basename(csv_file))
    if not match:
        return "Evolution of cache capacity"
    kp, ki, kd = match.groups()
    return f"Evolution of cache capacity (P={kp} / I={ki} / D={kd})"


def plot_cache_history(csv_file):
    """Lit le fichier CSV et affiche uniquement l'évolution de la capacité du cache."""
    try:
        df = pd.read_csv(csv_file)
    except FileNotFoundError:
        print(f"Erreur : Fichier '{csv_file}' non trouvé.")
        sys.exit(1)
    except Exception as e:
        print(f"Erreur lors de la lecture du CSV : {e}")
        sys.exit(1)

    if 'step' not in df.columns or 'capacity' not in df.columns:
        print("Erreur : le CSV doit contenir au minimum les colonnes 'step' et 'capacity'.")
        sys.exit(1)

    df = df.sort_values('step')
    x_values = df['step'].to_numpy(dtype=float)
    y_values = df['capacity'].to_numpy(dtype=float)
    smooth_x, smooth_y = _smooth_curve(x_values, y_values, window=5, dense_points=400)

    fig, ax = plt.subplots(figsize=(12, 6))
    fig.suptitle(_title_from_filename(csv_file), fontsize=16, fontweight='bold')

    # Courbe brute très discrète pour garder la tendance sans afficher de gros points
    ax.plot(x_values, y_values, color='steelblue', alpha=0.18, linewidth=1)

    # Courbe lissée principale
    ax.plot(smooth_x, smooth_y, color='royalblue', linewidth=2.0)
    ax.set_xlabel('Step')
    ax.set_ylabel('Capacity')
    ax.set_title('Cache Capacity Over Time')
    ax.grid(True, alpha=0.25)

    plt.tight_layout()
    plt.show()

    # Afficher quelques statistiques utiles
    print("\n" + "="*50)
    print("STATISTIQUES DU CACHE")
    print("="*50)
    print(f"Nombre d'étapes : {len(df)}")
    print(f"\nCapacité du cache :")
    print(f"  Min : {df['capacity'].min()}")
    print(f"  Max : {df['capacity'].max()}")
    print(f"  Moyenne : {df['capacity'].mean():.1f}")
    print(f"\nResponse Time :")
    print(f"  Min : {df['responseTime'].min():.1f} ms")
    print(f"  Max : {df['responseTime'].max():.1f} ms")
    print(f"  Moyenne : {df['responseTime'].mean():.1f} ms")
    print(f"\nHits/Misses total :")
    if 'hits' in df.columns and 'misses' in df.columns and (df['hits'].sum() + df['misses'].sum()) > 0:
        print(f"\nHits/Misses total :")
        print(f"  Total Hits : {df['hits'].sum()}")
        print(f"  Total Misses : {df['misses'].sum()}")
        print(f"  Hit Rate : {100 * df['hits'].sum() / (df['hits'].sum() + df['misses'].sum()):.1f}%")
    print("="*50 + "\n")

DEFAULT_CSV = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "output/cache_history_KP0.9_KI0.05_KD0.15.csv", # choisir le bon fichier CSV
)

if __name__ == '__main__':
    csv_file = DEFAULT_CSV if len(sys.argv) < 2 else sys.argv[1]
    plot_cache_history(csv_file)
