#!/usr/bin/env python3
"""
Script pour visualiser l'historique du cache depuis cache_history.csv.
Version simplifiée : uniquement l'évolution de la capacité du cache, avec une courbe lissée.
"""

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
    fig.suptitle('Evolution of cache capacity (P=0.9 / I=0.0 / D=0.1)', fontsize=16, fontweight='bold')

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

if __name__ == '__main__':
    csv_file = './output/cache_history_KP0.9_KI0.05_KD0.1.csv' if len(sys.argv) < 2 else sys.argv[1]
    plot_cache_history(csv_file)


#################################################################

# #!/usr/bin/env python3
# """
# Script pour visualiser l'historique du cache depuis cache_history.csv
# """
#
# import pandas as pd
# import matplotlib.pyplot as plt
# import sys
#
# def plot_cache_history(csv_file='cache_history.csv'):
#     """
#     Lit le fichier CSV et affiche plusieurs graphiques sur l'évolution du cache
#     """
#     try:
#         df = pd.read_csv(csv_file)
#     except FileNotFoundError:
#         print(f"Erreur : Fichier '{csv_file}' non trouvé.")
#         sys.exit(1)
#     except Exception as e:
#         print(f"Erreur lors de la lecture du CSV : {e}")
#         sys.exit(1)
#
#     # Créer une figure avec 2x2 sous-graphiques
#     fig, axes = plt.subplots(2, 2, figsize=(14, 10))
#     fig.suptitle('Cache History Analysis', fontsize=16, fontweight='bold')
#
#     # 1. Evolution de la capacité du cache
#     axes[0, 0].plot(df['step'], df['capacity'], marker='o', linestyle='-', color='blue', linewidth=2)
#     axes[0, 0].set_xlabel('Step')
#     axes[0, 0].set_ylabel('Capacity', color='blue')
#     axes[0, 0].set_title('Cache Capacity Over Time')
#     axes[0, 0].grid(True, alpha=0.3)
#     axes[0, 0].tick_params(axis='y', labelcolor='blue')
#
#     # 2. Response Time
#     axes[0, 1].plot(df['step'], df['responseTime'], marker='o', linestyle='-', color='orange', linewidth=2)
#     axes[0, 1].set_xlabel('Step')
#     axes[0, 1].set_ylabel('Response Time (ms)', color='orange')
#     axes[0, 1].set_title('Response Time Over Time')
#     axes[0, 1].grid(True, alpha=0.3)
#     axes[0, 1].tick_params(axis='y', labelcolor='orange')
#
#     # 3. Misses et Hits
#     axes[1, 0].bar(df['step'], df['misses'], label='Misses', color='red', alpha=0.7)
#     axes[1, 0].bar(df['step'], df['hits'], bottom=df['misses'], label='Hits', color='green', alpha=0.7)
#     axes[1, 0].set_xlabel('Step')
#     axes[1, 0].set_ylabel('Count')
#     axes[1, 0].set_title('Cache Hits vs Misses')
#     axes[1, 0].legend()
#     axes[1, 0].grid(True, alpha=0.3, axis='y')
#
#     # 4. Request size
#     axes[1, 1].plot(df['step'], df['request'], marker='o', linestyle='-', color='purple', linewidth=2)
#     axes[1, 1].set_xlabel('Step')
#     axes[1, 1].set_ylabel('Request Size', color='purple')
#     axes[1, 1].set_title('Request Size Over Time')
#     axes[1, 1].grid(True, alpha=0.3)
#     axes[1, 1].tick_params(axis='y', labelcolor='purple')
#
#     plt.tight_layout()
#
#     # Afficher le graphique
#     plt.show()
#
#     # Afficher quelques statistiques
#     print("\n" + "="*50)
#     print("STATISTIQUES DU CACHE")
#     print("="*50)
#     print(f"Nombre d'étapes : {len(df)}")
#     print(f"\nCapacité du cache :")
#     print(f"  Min : {df['capacity'].min()}")
#     print(f"  Max : {df['capacity'].max()}")
#     print(f"  Moyenne : {df['capacity'].mean():.1f}")
#     print(f"\nResponse Time :")
#     print(f"  Min : {df['responseTime'].min():.1f} ms")
#     print(f"  Max : {df['responseTime'].max():.1f} ms")
#     print(f"  Moyenne : {df['responseTime'].mean():.1f} ms")
#     print(f"\nHits/Misses total :")
#     print(f"  Total Hits : {df['hits'].sum()}")
#     print(f"  Total Misses : {df['misses'].sum()}")
#     print(f"  Hit Rate : {100 * df['hits'].sum() / (df['hits'].sum() + df['misses'].sum()):.1f}%")
#     print("="*50 + "\n")
#
# if __name__ == '__main__':
#     csv_file = 'cache_history.csv' if len(sys.argv) < 2 else sys.argv[1]
#     plot_cache_history(csv_file)
#
