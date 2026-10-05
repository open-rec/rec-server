# rec-server v0.1.0

Released: 2026-10-05

Online recommendation service. First coordinated OpenRec source release.

## Features

- Independent item and user recommendation DAGs with typed node contracts, bounded execution, request deadlines, and per-node traces and metrics.
- ItemCF, content, UserCF, embedding, query-vector, BM25, hot and new recall; configurable combination, filtering, ranking and PF4J operation rules.
- Runtime graph validation, experiment routing, publication and rollback with snapshots for in-flight requests.
- Redis ingestion in standalone; acknowledged, versioned Kafka mutations in cluster, including stable identities and deletion tombstones.
- Recommendation warmup, `/ready` traffic admission, and debug recall diagnostics before filtering and final selection.

## Installation and compatibility

Requires Java 21. The `rec-graph`, `rec-proto`, `rec-contrib` and online server Maven artifacts are versioned `0.1.0` together. Build matching SDK and data-processor sources after installing these artifacts. The distribution updates jar paths and plugin loading accordingly.

Standalone can bypass ranking and emit synthetic exposures. Cluster expects real client exposures and rank-engine. `/health` is liveness; traffic admission uses `/ready`. Supply warmup samples for user and experiment routes beyond the default item graph.

## Validation and known boundaries

See this repository's README for build/test commands and deployment requirements. The coordinated release's [validation record](https://github.com/open-rec/openrec/blob/v0.1.0/release/VALIDATION.md) distinguishes checks executed for this release from historical integration evidence.

This initial release establishes a versioned source baseline. Source archives and checksums are published; external package registries and container registries are not populated by the source-release workflow. Upgrade the complete compatible distribution, retain data/checkpoints/artifacts, and preserve prior component refs for rollback.
