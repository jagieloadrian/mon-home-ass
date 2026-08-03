# Quickstart: Validating CI Build Check & Deploy Scaffold

## 1. Build check triggers on both push-to-main and PR (FR-001/FR-002/FR-003)

```sh
cat .github/workflows/build.yml
```

Expected: a single `on:` block with both `push: branches: [main]` and
`pull_request:`, one job, running `./gradlew build`.

## 2. Coverage gate (FR-010)

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 sh gradlew clean build
```

Expected: `jacocoTestCoverageVerification` runs as part of `check`;
`BUILD SUCCESSFUL` only if overall line coverage ≥ 74% (the enforced floor —
see spec.md Clarifications for why this is 74% rather than the original 80%
target). If it fails here, see `research.md`'s coverage risk note before
adjusting anything.

```sh
find build/reports/jacoco -name "*.xml"
```

Expected: a JaCoCo XML report exists (input for the Codecov upload step).

## 3. Coverage reporting step present (FR-011/FR-012)

```sh
grep -A5 codecov .github/workflows/build.yml
```

Expected: a `codecov/codecov-action` step referencing the JaCoCo XML report,
with `fail_ci_if_error: false` (or equivalent) so upload failures don't fail
the build.

## 4. Deploy manifests still valid (FR-005–FR-009, already shipped — confirm, don't rebuild)

```sh
kubectl apply --dry-run=client -f .devops/k8s/deployment.yaml -f .devops/k8s/service.yaml
helm lint .devops/helm/mon-home-ass
helm template .devops/helm/mon-home-ass \
  --set image.repository=example/mon-home-ass \
  --set image.tag=0.0.1 \
  --set db.url=localhost:5432 \
  --set db.name=home_assistant \
  --set db.user=home_ass_user \
  --set db.password=placeholder
```

Expected: no errors; rendered output shows one Deployment, one Service, one
Secret, config mounted at the path the app already expects.

## 5. Manual, credential-gated steps (cannot be verified by an automated run — do these by hand)

- **Branch protection (FR-004a)**: GitHub repo → Settings → Branches → add
  rule on `main` → require the `build` status check before merging.
- **Codecov token**: GitHub repo → Settings → Secrets and variables →
  Actions → add `CODECOV_TOKEN` (from codecov.io for this repo).

Neither step is scriptable from inside this repository without GitHub admin
credentials — treat both as required manual follow-ups after this
feature's PR merges, not blockers to merging the PR itself.
