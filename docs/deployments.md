# Platform dependencies (PostgreSQL, Elasticsearch, Redis, RabbitMQ)

Installing shared infrastructure is **not** part of this repository.

- **Scripts and Helm values** live in the sibling repo **`ai-monitoring-dependencies`** (GitHub: **`kere-sifon/ai-monitoring-dependencies`**), under **`deployments/`**.
- On disk (typical layout): **`../ai-monitoring-dependencies`** next to this repo.
- Run **`deployments/install-dependencies.sh`** from that repo (or use its **Deploy infrastructure** GitHub Action) **before** deploying **alert-service**.
- This repo only needs the **alert-service** Helm chart under **`charts/`**, which expects the `postgres` / `elasticsearch` services and **`ai-monitoring-secrets`** in the same namespace.

See that repository’s **`deployments/README.md`** and **`deployments/OPENSHIFT.md`**.
