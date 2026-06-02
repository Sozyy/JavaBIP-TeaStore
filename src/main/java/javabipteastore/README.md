# JavaBIP TeaStore

A JavaBIP translation of [CollectiveTeaStoreVariation](https://github.com/NwaitDev/CollectiveTeaStoreVariation), adapted for 3 clients instead of 10.

The system simulates a TeaStore coordinated by the **JavaBIP** framework: three clients send image loading requests, a server routes them to a data provider that simulates an LRU cache, and a PID controller dynamically adjusts the cache size to maintain a target response time.

## Architecture

```
Client1 ─┐
Client2 ─┼─ CollectConnectors (CC1 -> CC2 -> CC3)
Client3 ─┘          │
              Looper1/Looper2
                    │
                 Server
                    │
             DataProvider ──── LRUCache
                    │
             PIDController
                    │
             DataProvider
                    │
                 Server
                    │
         SpreadConnectors (SP1 -> SP2 -> SP3)
           │         │         │
        Client1   Client2   Client3
```

### Execution Flow

1. Clients generate requests of random size.
2. `CollectConnectors` aggregate requests bottom-up to the `Server`.
3. The `Server` forwards the request to the `DataProvider`.
4. The `DataProvider` simulates LRU cache hits/misses and computes:
   `responseTime = 2 × misses + 0.2 × hits`
5. The `PIDController` receives the response time and computes the new cache size.
6. The `DataProvider` updates the cache and notifies the `Server`.
7. The `Server` sends responses back to clients via the `SpreadConnectors`.
8. The system stops when all clients have reached their objective.
9. Metrics are recorded for CSV export and can be visualized using Python files.

## Configuration Parameters (Main.java)

| Parameter | Value |
|---|---|
| Number of clients | 3 |
| Objective per client | 600 images |
| Max request size | 30 images |
| Image universe size | 100 distinct IDs |
| Initial cache capacity | 20 items |
| Min / max cache capacity | 2 / 500 items |
| Target response time | 20 ms |
| Kp / Ki / Kd | 0.8 / 0.05 / 0.2 |

