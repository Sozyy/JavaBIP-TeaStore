# Adapteastore

A JavaBIP controller that interfaces with the [Adaptable TeaStore](https://gitlab.inria.fr/adaptable-teastore/experimentation-platform) via the [AdaptiFlow](https://github.com/brice10/adaptiflow-core/tree/main) framework to dynamically adjust the image cache size.

The controller monitors traffic metrics from the Adaptable TeaStore's persistence layer, feeds them into a BIP pipeline composed of a `DataProvider` and a `PIDController`, and sends the computed cache size back to the image service through REST.

## Architecture

```
 Adaptable TeaStore
 ┌──────────────────────────────────┐
 │  Persistence service (:8082)     │
 │  Image service       (:8083)     │
 └──────────┬───────────────┬───────┘
            │ GET /metrics  │ POST /setCacheSize
            │               │
 Adapteastore (this project)
 ┌──────────v───────────────^───────┐
 │  PersistenceCollector            │  < AdaptiFlow IMetricsCollector
 │         │ request delta          │
 │       Bridge                     │  < AdaptiFlow Observer + BIP Component
 │         │                        │
 │  ┌──────v──────────────────┐     │
 │  │  DataProvider           │     │
 │  │  LRUCache (no JavaBIP)  │     │
 │  │  PIDController          │     │
 │  └──────────────┬──────────┘     │
 │               newCacheSize       │
 │         CacheUpdater             │
 └──────────────────────────────────┘
```

### Role of AdaptiFlow

**AdaptiFlow** is the communication framework between Adapteastore and the Adaptable TeaStore. It provides:

- **`IMetricsCollector<T>`**: interface implemented by `PersistenceCollector` to collect REST metrics in a standardized way.
- **Observer**: interface implemented by `Bridge` to receive metric updates and inject them into the BIP engine.

This decouples the REST collection logic (AdaptiFlow) from the BIP adaptation logic (JavaBIP).

### Execution Flow

1. The polling loop (`Main`) calls `PersistenceCollector.collect()` every 2 seconds.
2. If the request is not empty, `Bridge.update(delta)` is called.
3. `Bridge` signals the BIP engine that data is available (`sendRequest` unblocked).
4. The `DataProvider` processes the delta, simulates LRU hits/misses, and computes response time. (to be changed)
5. The `PIDController` receives the response time and computes the new cache size.
6. `Bridge` retrieves the computed size and returns it to `Main` via `waitForCycleAndGetCacheSize()`.
7. `CacheUpdater` sends the new size to the image service via POST.
8. Metrics are recorded for CSV export.

