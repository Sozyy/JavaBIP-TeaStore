# JavaBIP-TeaStore

This project contains two JavaBIP implementations of an adaptive cache control system for a TeaStore.

## Context

This repository contains two complementary projects :

- **[javabipteastore](src/main/java/javabipteastore/)** — A JavaBIP translation of the [CollectiveTeaStoreVariation](https://github.com/NwaitDev/CollectiveTeaStoreVariation), adapted for 3 clients instead of 10. 
It reproduces the behavior of a multi-client system with a PID controller that dynamically adjusts an LRU cache size.

- **[adapteastore](src/main/java/adapteastore/)** — A BIP controller that interfaces with the [Adaptable TeaStore](https://gitlab.inria.fr/adaptable-teastore/experimentation-platform) using the [AdaptiFlow](https://github.com/brice10/adaptiflow-core/tree/main) framework. It monitors persistence layer metrics, computes an adjustment via the same PID controller, and updates the image cache size through REST service.
Note that the PID controller is not adapted to the Adaptable TeaStore's behavior, it is a direct reuse of the javabipteastore controller.