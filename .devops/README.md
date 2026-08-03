# .devops

Initial Helm scaffold. Not wired to CI yet — apply manually until a
deploy phase is actually needed.

## helm/mon-home-ass/

```bash
helm install mon-home-ass ./helm/mon-home-ass \
  --set image.repository=<registry>/mon-home-ass \
  --set image.tag=0.0.1 \
  --set db.url=<host:5432> \
  --set db.name=home_assistant \
  --set db.user=home_ass_user \
  --set db.password=<readonly-password>
```

Image name/tag match `build.gradle.kts` (`ktor { docker { ... } }`):
`mon-home-ass:<version>`.

skipped: Ingress, HPA, NetworkPolicy — add when the app actually needs to be
reachable from outside the cluster / needs to scale / needs isolation.
