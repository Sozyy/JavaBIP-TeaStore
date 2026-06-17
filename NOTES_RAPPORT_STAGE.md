# Notes pour le rapport de stage — Contrôleur de cache PID en JavaBIP

> Document de travail listant en détail tout ce qui a été implémenté dans le module
> `Sources/services/tools.descartes.teastore.controller`, à réutiliser/reformuler pour le rapport de stage.

---

## 1. Contexte du stage

Le projet d'accueil est **Adaptable TeaStore**, une version modulaire et adaptable du benchmark
microservices [TeaStore](https://github.com/DescartesResearch/TeaStore) développée par un doctorant
de l'Inria. L'objectif général du projet est de rendre les services du TeaStore **observables et
adaptables dynamiquement** face aux variations de charge, via une bibliothèque dédiée développée par
le doctorant : **AdaptiFlow**.

AdaptiFlow fournit les briques génériques de boucle d'adaptation :

- `IMetricsCollector<T>` : collecte de métriques (ex. nombre de requêtes reçues par un service),
- `Observer<T>` : notification d'un composant lorsqu'une nouvelle métrique est disponible,
- `ConditionEvaluator<T>` : évaluation de conditions/seuils sur ces métriques,
- des actions d'adaptation déclenchées automatiquement quand une condition est remplie.

**Ma contribution** a consisté à concevoir et implémenter la boucle d'adaptation qui gère
**la taille du cache d'images** du service Image de l'Adaptable TeaStore, en s'appuyant sur :

- **JavaBIP** (*Behavior-Interaction-Priority*), un framework de programmation par composants où
  chaque composant est un automate à états, et où la coordination (synchronisation, échange de
  données) entre composants est déclarée séparément dans une "glue" plutôt que codée en dur dans
  les composants eux-mêmes ;
- un **contrôleur PID** (Proportionnel-Intégral-Dérivé), classiquement utilisé en automatique, ici
  détourné pour piloter une taille de cache logicielle en fonction d'un temps de traitement observé.

Le travail s'est déroulé en **deux grandes étapes**, correspondant à deux packages distincts du
module : une étape de prise en main / preuve de concept simulée (`javabipteastore`), suivie d'une
étape d'intégration réelle avec les services Docker de l'Adaptable TeaStore (`adapteastore`).

---

## 2. Étape 1 — `javabipteastore` : preuve de concept simulée

### 2.1 Objectif

Avant de connecter quoi que ce soit à de vrais services réseau, l'objectif a été d'apprendre à
modéliser un système de bout en bout avec JavaBIP, à partir d'un projet existant
([`CollectiveTeaStoreVariation`](https://github.com/NwaitDev/CollectiveTeaStoreVariation)) traduit
et adapté (3 clients au lieu de 10). Tout le système est **simulé en mémoire** : pas d'appel réseau,
les "clients" sont des composants BIP qui tirent des requêtes aléatoires.

### 2.2 Architecture

```
Client1 ─┐
Client2 ─┼─ CollectConnectors (CC1 → CC2 → CC3)
Client3 ─┘          │
              Looper1 / Looper2
                    │
                 Server
                    │
             DataProvider ──── LRUCache (simulé)
                    │
             PIDController
                    │
             DataProvider
                    │
                 Server
                    │
         SpreadConnectors (SP1 → SP2 → SP3)
           │         │         │
        Client1   Client2   Client3
```

### 2.3 Composants implémentés

- **`Client` / `Client1` / `Client2` / `Client3`** : chaque client tire une requête (nombre
  d'images à charger), l'envoie, attend la réponse, met à jour son compteur de progression, et
  s'arrête lorsqu'il atteint son objectif (`CLIENT_OBJECTIVES = 600` images). Les classes `Client1-3`
  sont de simples spécialisations (même comportement, identifiants différents) nécessaires car
  JavaBIP a besoin d'un type concret par composant déclaré dans la glue.
- **`CollectConnector` (+ `CollectConnector1/2/3`)** : pattern d'agrégation en cascade. Chaque
  connecteur attend 3 signaux (`input1` venant du connecteur suivant dans la chaîne, `input2`
  réservé pour une extension future, `input3` venant du client local) avant de pouvoir transmettre
  la somme agrégée des requêtes au connecteur précédent (`sendUp`), jusqu'au `Server`. Implémenté
  avec des **gardes** JavaBIP (`@Guard`) pour ne déclencher la transition `sendUp` que lorsque les
  trois entrées ont été reçues (`rec1 & rec2 & rec3`), puis un `reset` remet l'automate à zéro pour
  le cycle suivant.
- **`SpreadConnector` (+ `SpreadConnector1/2/3`)** : pattern symétrique de diffusion descendante :
  le `Server` envoie les réponses agrégées à `SpreadConnector1`, qui retransmet à son client
  ET au connecteur suivant, etc.
- **`Looper` / `Looper1` / `Looper2`** : composants "spontanés" qui bouclent en continu pour
  fournir le port `input1`/`input2` du premier `CollectConnector` de la chaîne (qui n'a pas de
  connecteur "suivant" lui fournissant ces données).
- **`Server`** : automate à 4 états (`IDLE → CALC → SENT → CALC → IDLE`) qui itère sur la liste des
  requêtes clients une par une (`currentRequestId`), les transmet au `DataProvider`, récupère la
  réponse, et recommence jusqu'à ce que toutes les requêtes du cycle aient été traitées (garde
  `allRequestsTreated`).
- **`DataProvider`** : simule un cache LRU. Pour chaque requête de *N* images, tire *N* identifiants
  aléatoires dans un univers de 100 images (`IMAGE_UNIVERSE_SIZE`), regarde s'ils sont déjà dans le
  cache (hit) ou pas (miss, avec insertion), puis calcule un temps de traitement simulé :

  ```
  responseTime = MISS_WEIGHT × misses + HIT_WEIGHT × hits   (2.0 et 0.2 dans cette version)
  ```
- **`PIDController`** : voir détail commun aux deux versions en section 4.
- **`LRUCache`** : implémentation simple d'un cache LRU (`LinkedHashMap` en mode *access-order*),
  avec un historique enregistré à chaque étape pour export.
- **`TeaStoreWithConnectorGlue`** : classe de *glue* JavaBIP qui déclare déclarativement toutes les
  synchronisations (`synchron(...).to(...)`) et tous les flux de données (`data(...).to(...)`) entre
  les composants ci-dessus. C'est le cœur de la démonstration de l'approche "BIP" : le comportement
  de coordination est **entièrement séparé** du code des composants.

### 2.4 Outils annexes

- **Export CSV** de l'historique du cache (capacité, temps de réponse, hits, misses, requêtes) à
  chaque cycle, nommé dynamiquement avec les paramètres PID utilisés
  (`cache_history_KP{...}_KI{...}_KD{...}.csv`).
- **`plot.py`** et **`plotMultiple.py`** : scripts Python pour visualiser l'évolution de la taille de
  cache et du temps de réponse au cours du temps, et comparer plusieurs configurations de
  paramètres PID entre elles.
- Dans `Main.java`, un bloc (laissé en commentaire) implémente un **balayage exhaustif** (grid
  search) des paramètres `Kp`, `Ki`, `Kd` sur une plage donnée, pour explorer empiriquement de bonnes
  combinaisons — utile à mentionner dans le rapport comme démarche de réglage du PID, même si le
  balayage complet est trop long pour être exécuté systématiquement.

### 2.5 Paramètres de configuration utilisés (`Main.java`)

| Paramètre                       | Valeur                     |
| ------------------------------- | -------------------------- |
| Nombre de clients               | 3                          |
| Objectif par client             | 600 images                 |
| Taille de requête max           | 30 images                  |
| Univers d'images                | 100 identifiants distincts |
| Capacité initiale du cache      | 20 items                   |
| Capacité min / max du cache     | 2 / 500 items              |
| Temps de réponse cible (target) | 20 ms                      |
| Kp / Ki / Kd                    | 0.8 / 0.05 / 0.2           |

---

## 3. Étape 2 — `adapteastore` : intégration réelle avec l'Adaptable TeaStore

### 3.1 Objectif

Une fois la modélisation BIP validée en simulation, l'étape suivante a été de **connecter réellement**
le contrôleur aux services Docker de l'Adaptable TeaStore (service `image` et service `persistence`),
en passant par AdaptiFlow pour respecter l'architecture d'adaptation du projet, et en généralisant le
cache à plusieurs stratégies d'éviction interchangeables.

### 3.2 Architecture

```
 Adaptable TeaStore (conteneurs Docker)
 ┌──────────────────────────────────┐
 │  Persistence service (:8082)     │
 │  Image service       (:8083)     │
 └──────────┬───────────────┬───────┘
            │ GET /metrics  │ POST /setCacheSize
            │               │
 Adapteastore (mon contrôleur)
 ┌──────────v───────────────^───────┐
 │  ServiceCollector                │  ← AdaptiFlow IMetricsCollector
 │         │ delta de requêtes      │
 │       Bridge                     │  ← AdaptiFlow Observer + composant BIP
 │         │                        │
 │  ┌──────v──────────────────┐     │
 │  │  DataProvider           │     │
 │  │  ICache (LRU/LFU/FIFO/  │     │
 │  │  LIFO/MRU/RR), sans BIP │     │
 │  │  PIDController          │     │
 │  └──────────────┬──────────┘     │
 │               newCacheSize       │
 │         CacheUpdater             │
 └──────────────────────────────────┘
```

### 3.3 Cycle d'exécution (boucle principale de `Main.java`)

1. Toutes les 2 secondes (`POLL_INTERVAL_MS`), `ServiceCollector.get()` interroge
   `GET /tools.descartes.teastore.image/rest/metrics/requests` et renvoie le **delta** de requêtes
   reçues depuis le dernier appel.
1. Si `delta > 0`, `Bridge.update(delta, ...)` est appelé : la donnée est mémorisée et un signal
   (`dataAvailable`) réveille le moteur JavaBIP, qui était bloqué en attente sur la transition
   `sendRequest` du composant `Bridge`.
1. Le pipeline BIP s'exécute : `Bridge → DataProvider → PIDController → DataProvider → Bridge`
   (détaillé en 3.5).
1. `Bridge.waitForCycleAndGetCacheSize(timeout)` débloque le thread principal une fois le cycle BIP
   terminé, et renvoie la nouvelle taille de cache calculée (avec un timeout de sécurité de 10s pour
   ne jamais bloquer indéfiniment si le moteur BIP n'a pas répondu).
1. `CacheUpdater.update(newSize)` envoie cette taille via `POST /rest/image/setCacheSize` au service
   Image réel, et interprète la réponse (acceptée ou non par le TeaStore, erreur HTTP, erreur réseau).
1. Le résultat de chaque itération est journalisé (`[Monitor] iter=... delta=... -> PID_cache=... -> image=...`).

Le programme s'arrête proprement sur `Ctrl+C` grâce à un *shutdown hook* qui interrompt le thread
principal et arrête le moteur JavaBIP/Akka.

### 3.4 Pont AdaptiFlow ↔ JavaBIP (`Bridge.java`)

C'est la pièce d'intégration la plus délicate du stage : faire coexister deux modèles de concurrence
différents :

- **AdaptiFlow**, basé sur un polling classique côté `Main` qui pousse des valeurs (push, synchrone,
  thread "normal") ;
- **JavaBIP/Akka**, un moteur asynchrone à base d'acteurs, où les transitions sont déclenchées par
  le moteur lui-même dès que les gardes/synchronisations le permettent.

`Bridge` implémente à la fois l'interface AdaptiFlow `Observer<Integer>` (côté entrée) et un
composant JavaBIP avec deux transitions (`sendRequest`, `receiveResp`). La synchronisation entre les
deux mondes est assurée avec un `ReentrantLock` et deux `Condition` (`dataAvailable`, `cycleDone`) :

- `update()` (appelé par AdaptiFlow) pose la donnée et signale `dataAvailable` ; si une donnée
  précédente n'a pas encore été consommée par le moteur BIP, la nouvelle est **délibérément ignorée**
  (log "DROPPED") pour éviter d'empiler les cycles plus vite que le moteur ne peut les traiter.
- `sendRequest()` (transition BIP) attend `dataAvailable` puis expose la donnée via un port `@Data`.
- `receiveResp()` (transition BIP, appelée en fin de cycle) relit la capacité courante du cache et
  signale `cycleDone`.
- `waitForCycleAndGetCacheSize(timeout)` (appelé côté `Main`) attend `cycleDone` avec un délai
  maximal, pour ne jamais bloquer indéfiniment le thread de polling.

### 3.5 Glue JavaBIP (`Glue.java`)

Pipeline BIP simplifié par rapport à la v1 (plus de clients/connecteurs simulés, remplacés par le
vrai trafic du TeaStore) :

```
Bridge.sendRequest      → DataProvider.receiveRequest
DataProvider.sendResponseTime → PIDController.receiveResponseTime
PIDController.sendCacheSize   → DataProvider.receiveNewCacheSize
DataProvider.notifyServer     → Bridge.receiveResp
```

avec les flux de données associés (`request`, `responseTime`, `newCacheSize`).

### 3.6 `DataProvider`

Fonctionnellement identique à la v1 mais découplé d'une implémentation de cache fixe : il reçoit une
implémentation de l'interface `ICache` au lieu d'un `LRUCache` concret, ce qui permet de changer de
stratégie de cache sans toucher au reste du pipeline. À chaque requête :

- tire `nbImages` identifiants aléatoires dans `imageUniverseSize`,
- compte hits/misses via `cache.contains()` / `cache.addItem()`,
- calcule `responseTime = missWeight × misses + hitWeight × hits` (poids paramétrables,
  `HIT_WEIGHT = 0.2`, `MISS_WEIGHT = 2.0`),
- enregistre l'étape dans l'historique du cache (`cache.recordStep(...)`).

> Remarque présente dans le code (`@Important note` du `PIDController`) : cette version du
> `DataProvider`/`PIDController` reprend des paramètres calibrés pour le `CollectiveTeaStore` simulé
> et travaille en **nombre d'items**, alors que l'Adaptable TeaStore réel attend une taille en
> **octets** côté service Image — point identifié comme à retravailler/affiner.

### 3.7 Couche réseau HTTP

Deux classes écrites avec `java.net.http.HttpClient` (Java 11+, sans dépendance tierce) :

- **`ServiceCollector`** (implémente `IMetricsCollector<Integer>` d'AdaptiFlow) : générique, peut
  interroger n'importe quel service du TeaStore exposant un endpoint `GET /rest/metrics/requests`
  (persistence, image, webui...). Calcule le **delta** entre deux appels successifs (et non la
  valeur absolue), avec une détection automatique au premier appel du nom du champ JSON pertinent
  parmi une liste de candidats (`receivedRequests`, `requestCount`, `totalRequests`, `count`,
  `requests`, `nbRequests`, `totalReceivedRequests`) — pensé pour être robuste si le nom exact du
  champ JSON change ou diffère selon le service.
- **`CacheUpdater`** : envoie la nouvelle taille de cache en `POST /rest/image/setCacheSize`, avec
  une classe de résultat typée `UpdateResult` (`SUCCESS`, `HTTP_ERROR`, `TRANSPORT_ERROR`,
  `REJECTED_LOCALLY`) qui distingue proprement : taille négative rejetée localement avant tout appel
  réseau, erreur HTTP (code ≠ 200), erreur de transport (timeout, connexion refusée...), et succès
  avec acceptation/refus explicite par le TeaStore (lecture du corps de réponse booléen).

### 3.8 Abstraction et stratégies de cache (`adapteastore.cache`)

Généralisation par rapport à la v1 (un seul `LRUCache` figé) :

- **`ICache`** : interface commune (`contains`, `addItem`, `getCapacity`, `setCapacity`,
  `recordStep`, `exportHistoryCsv`).
- **`AbstractAdapteaStoreCache`** : classe abstraite mutualisant la gestion de l'historique
  (capacité, temps de réponse, hits, misses, requêtes à chaque étape) et son export CSV, pour éviter
  de dupliquer ce code dans chaque stratégie. Ne laisse à chaque sous-classe que `size()` et
  `evict()` à définir.
- **6 stratégies d'éviction implémentées**, toutes redimensionnables dynamiquement
  (`setCapacity` évince immédiatement le surplus si la nouvelle capacité est plus petite) :

  | Stratégie                       | Fichier          | Principe                                                                           | Structures de données                                                                      |
  | ------------------------------- | ---------------- | ---------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
  | **LRU** (Least Recently Used)   | `LRUCache.java`  | évince l'élément le moins récemment consulté                                       | `LinkedHashMap` en mode access-order                                                       |
  | **MRU** (Most Recently Used)    | `MRUCache.java`  | évince l'élément le **plus** récemment consulté (politique inverse de LRU)         | `LinkedHashMap` en mode access-order                                                       |
  | **LFU** (Least Frequently Used) | `LFUCache.java`  | évince l'élément le moins fréquemment consulté ; égalité départagée par ancienneté | compteurs de fréquence + buckets `LinkedHashSet` par fréquence, `minFreq` suivi en O(1)    |
  | **FIFO** (First In First Out)   | `FIFOCache.java` | évince l'élément le plus anciennement **inséré**, indépendamment des accès         | `Queue` + `Set`                                                                            |
  | **LIFO** (Last In First Out)    | `LIFOCache.java` | évince l'élément le plus récemment **inséré**                                      | `Deque` (pile) + `Set`                                                                     |
  | **RR** (Random Replacement)     | `RRCache.java`   | évince un élément choisi aléatoirement                                             | `List` + `Set`, `Random` (constructeur avec seed disponible pour des tests reproductibles) |

  La stratégie est sélectionnable par une simple constante (`DEFAULT_CACHE_STRATEGY`) dans `Main`,
  via une fabrique `createCache(strategy, capacity)` — pensé pour comparer facilement l'effet de la
  politique de cache sur la performance du système adaptatif, indépendamment du contrôleur PID.

### 3.9 Paramètres de configuration utilisés (`Main.java`)

| Paramètre                       | Valeur                     |
| ------------------------------- | -------------------------- |
| Stratégie de cache par défaut   | LRU                        |
| Capacité initiale du cache      | 80 items                   |
| Capacité min / max              | 2 / 500 items              |
| Univers d'images                | 100 identifiants distincts |
| Temps de réponse cible (target) | 10 ms                      |
| Kp / Ki / Kd                    | 0.9 / 0.05 / 0.15          |
| Poids hit / miss                | 0.2 / 2.0                  |
| Intervalle de polling           | 2000 ms                    |
| Timeout d'un cycle BIP          | 10 000 ms                  |

---

## 4. Le contrôleur PID (commun aux deux versions)

Composant central du stage, modélisé comme un automate BIP à deux états (`IDLE` → `COMPUTING` →
`IDLE`), avec deux ports enforceable (`receiveResponseTime`, `sendCacheSize`) et un port de données
exposant la nouvelle taille calculée.

**Calcul à chaque cycle :**

```java
erreur      = responseTime - targetResponseTime;
intégrale  += erreur;
dérivée     = erreur - erreur_précédente;

commande = Kp * erreur + Ki * intégrale + Kd * dérivée;

nouvelleTaille = clamp(tailleActuelle + round(commande), tailleMin, tailleMax);
erreur_précédente = erreur;
```

Points à expliciter dans le rapport :

- **Pourquoi un PID** plutôt qu'une règle de seuil statique : permet de réagir proportionnellement à
  l'écart instantané (terme P), de corriger un biais persistant (terme I, ex. cache structurellement
  trop petit pour la charge), et d'anticiper/amortir les oscillations (terme D) — pertinent ici car la
  "taille de cache idéale" dépend d'une charge de trafic qui varie en continu.
- **`clamp`** : borne la sortie entre une taille minimale et maximale pour éviter des tailles de
  cache absurdes (négatives, nulles, ou disproportionnées par rapport à la mémoire disponible).
  C'est aussi un garde-fou contre l'effet "windup" classique des contrôleurs PID (l'intégrale qui
  s'accumule sans borne lorsque la sortie est saturée) — point qui pourrait être discuté comme
  limite/amélioration possible (anti-windup explicite non implémenté).
- Les paramètres `Kp`, `Ki`, `Kd` diffèrent entre la v1 (0.8/0.05/0.2, cible 20 ms) et la v2
  (0.9/0.05/0.15, cible 10 ms) : à documenter comme un travail de **réglage empirique** (tuning) du
  PID, éventuellement appuyé sur le grid search préparé en v1.

---

## 5. Stack technique mobilisée

- **JavaBIP** : `org.javabip.api`, `engine.core/bdd/coordinator/factory`, `glue`, `executor`
  (jars installés localement dans le `~/.m2` via `install-javabip-jars.sh`, absents d'un dépôt Maven
  public).
- **AdaptiFlow** (`io.github.brice10:adaptiflow`, `adaptationactionsbase`, `metricscollectorbase`) —
  bibliothèque du doctorant encadrant, dont l'usage (`IMetricsCollector`, `Observer`,
  `ConditionEvaluator`) a dû être compris et interfacé avec JavaBIP via la classe `Bridge`.
- **Akka Actor** (`akka-actor_2.11`) : runtime sous-jacent utilisé par le moteur JavaBIP
  (`EngineFactory`, `ActorSystem`).
- **Jackson Databind** : parsing JSON des réponses de métriques REST.
- **`java.net.http.HttpClient`** (Java 11+) : appels REST GET/POST, sans dépendance HTTP tierce.
- **Maven** : build du module, compilation Java 11.
- Le `pom.xml` du module embarque aussi d'autres dépendances héritées du projet plus large
  (Apache Camel, Spring Beans, ANTLR4, JGraphT, FreeMarker, Spoon, JAXB) qui ne sont pas utilisées
  directement par mon code mais font partie du module partagé.

---

## 6. Synthèse — ce qui a été personnellement implémenté

- Modélisation BIP complète d'un pipeline de bout en bout : composants, transitions, gardes, ports de
  données, et glue de synchronisation (deux glues différentes pour les deux versions).
- Le contrôleur **PID** lui-même (logique de calcul + modélisation en composant JavaBIP).
- Le **pont de synchronisation** entre le monde "AdaptiFlow / thread classique" et le monde
  "JavaBIP / Akka asynchrone" (`Bridge`), avec gestion explicite de la concurrence (locks/conditions,
  anti-accumulation de cycles, timeout de sécurité).
- La **couche réseau** complète vers l'Adaptable TeaStore réel : collecte générique de métriques
  (`ServiceCollector`) et mise à jour de la taille de cache (`CacheUpdater`), avec gestion fine des
  cas d'erreur.
- L'**abstraction du cache** (`ICache`/`AbstractAdapteaStoreCache`) et l'implémentation de **6
  stratégies d'éviction** (LRU, MRU, LFU, FIFO, LIFO, RR), avec suivi d'historique et export CSV pour
  l'analyse.
- L'architecture de **simulation** initiale (clients, connecteurs de collecte/diffusion en cascade,
  serveur) ayant servi de preuve de concept et de terrain d'apprentissage de JavaBIP avant
  l'intégration réelle.
- Les **outils d'analyse** : export CSV de l'historique, scripts Python de visualisation
  (`plot.py`, `plotMultiple.py`), et préparation d'un balayage de paramètres PID pour le réglage.
- La **documentation** des deux sous-modules (`README.md` dans `javabipteastore` et `adapteastore`),
  avec schémas d'architecture.

---

## 7. Pistes à développer dans le rapport (non encore tranchées)

Points identifiés dans le code lui-même (commentaires `TODO`/notes) qui peuvent nourrir une partie
"limites et perspectives" du rapport :

- Conversion **items → octets** pour la taille de cache envoyée au vrai service Image (le PID
  raisonne en nombre d'items simulés, l'API REST de l'Adaptable TeaStore attend une taille en
  octets) — un `SCALE_FACTOR` existe déjà comme point d'extension dans `Main.java` mais vaut `1`
  actuellement, donc la conversion n'est pas encore faite. Pistes à discuter dans le rapport : facteur
  fixe basé sur une taille moyenne d'image, ou mesure dynamique de la taille réelle des images servies.
- Absence de mécanisme anti-windup explicite sur le terme intégral du PID.
- Le `DataProvider` simule encore les hits/misses par tirage aléatoire plutôt que de mesurer le
  comportement réel du cache du service Image — à clarifier si la version finale du stage va plus
  loin sur ce point.
- Comparaison chiffrée entre les 6 stratégies de cache (résultats d'expérimentation à ajouter si
  disponibles).