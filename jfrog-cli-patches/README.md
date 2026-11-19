# JFrog CLI release workflows: patches

`jfrog-cli-patches.zip` contains the three patches below and this README. Each patch is a single commit in `git am`
format.

| Patch | Apply to | Base |
|---|---|---|
| `workflows-jfrog-cli.patch` | zugazagoitia/workflows | fork `main` merged with `e-CODEX/workflows` `main` (2188cc7) |
| `connector-jfrog-cli.patch` | zugazagoitia/connector | `develop` (07ac39a, same as upstream) |
| `connector-ng-jfrog-cli.patch` | zugazagoitia/connector-ng | `e-CODEX/connector-ng` `develop` (897cd755) |

## What changes

**workflows**
- `docker-build-push.yaml`: same steps as before, plus:
  - one `jf rt build-docker-create` line that records the image in a JFrog build-info (`<repo>-docker`);
  - `provenance: mode=max` next to the existing BuildKit `sbom: true`, so the SBOM and the provenance are stored with the image;
  - an attestation subject of `oci://<registry>/<image>`, so `setup-jfrog-cli` attaches the GitHub attestation to the
    image in Artifactory as evidence. `setup-jfrog-cli` also publishes the build-info at the end of the job;
  - a `refresh` mode (see below).
- `maven-publish-*.yaml` / `gradle-publish-*.yaml`: these deploy with `jf mvn deploy` / `jf gradle artifactoryPublish`
  (OIDC, no settings.xml credentials) and publish the build-info. The CycloneDX SBOM is deployed with the artifacts.
  Every artifact gets a GitHub provenance attestation and an SBOM attestation, recorded in Artifactory as JFrog evidence.
  The new action `.github/actions/jfrog-publish-attest` does this.
- `setup-jfrog-cli` v5.3.5 with the JFrog CLI pinned to 2.133.0. All actions are pinned by SHA.

**connector / connector-ng**
- `publish-release.yaml` / `publish-snapshot.yaml`: each job gets explicit least-privilege permissions. The OIDC login
  needs `id-token: write`, which callers that set no permissions don't get. connector-ng moves to the `@main`
  workflows (pin a SHA after merging).
- New `docker-refresh.yaml` (manual): rebuilds the image of a released tag and overwrites it.
- connector-ng `Dockerfile`: runs `apt-get upgrade` in the runtime stage (connector already does), so refreshed images pick up OS fixes.
  A refresh builds with the Dockerfile of the old tag, so this only helps tags released after this change.

## Refreshing a released Docker image

Actions → *Refresh Docker Image* → enter the released tag, e.g. `7.1.0`. The image is rebuilt from that git tag with
`--pull --no-cache`: the newest base image and OS packages, with the same application source. It is pushed over the
existing `7.1.0` tag. Tick *floating-tags* / *latest* only when refreshing the newest patch / release, so an old
release line never takes over `7.1` / `7` / `latest`.

## How to apply (Codespaces)

```bash
# 0. get the patches
git fetch https://github.com/zugazagoitia/connector-ng claude/jfrog-cli-workflows-bdvr86
git checkout FETCH_HEAD -- jfrog-cli-patches && unzip -o jfrog-cli-patches/jfrog-cli-patches.zip -d /tmp/jfrog-patches
# (or download the zip from the branch on GitHub)

# 1. sync the fork with upstream (or use GitHub's "Sync fork" button)
#   workflows:    git fetch upstream && git checkout main    && git merge --no-edit upstream/main
#   connector-ng: git fetch upstream && git checkout develop && git merge --ff-only upstream/develop
#   connector:    already up to date
#   (git remote add upstream https://github.com/e-CODEX/<repo>.git if needed)

# 2. apply
git checkout -b feat/jfrog-cli-releases
git am /tmp/jfrog-patches/<repo>-jfrog-cli.patch
```

The connector-ng branch `claude/jfrog-cli-workflows-bdvr86` already contains the connector-ng change, so in that fork
you can use the branch directly instead of the patch.

### Testing from your forks

The callers reference `e-CODEX/workflows/...@main`, so the patches are ready to go upstream as-is. To run them from your
forks, run this in all three repos (the workflows repo also references its own actions by repo path):

```bash
grep -rl 'e-CODEX/workflows/' .github | xargs sed -i 's#e-CODEX/workflows/#zugazagoitia/workflows/#g'
```

## JFrog prerequisites

- The OIDC integration `setup-jfrog`, with an identity mapping for each repository. This is already used today.
- The mapped identity needs:
  - Deploy on `libs-release`/`libs-snapshot` (connector-ng), `ecodex-releases`/`ecodex-snapshots` (connector) and the docker repos;
  - **Delete/Overwrite on the docker repos**, because a refresh overwrites a tag;
  - permission to publish build-info and to annotate (evidence).
- JFrog evidence needs the Evidence service (Enterprise+). Without it the evidence steps only warn; `evidence: false`
  skips them in the Maven/Gradle workflows. GitHub attestations are created either way.

## Not run end-to-end

These patches have not been run against scm.ecodex.eu. Check these on the first runs:
- `jf gradle` with Gradle 9.8. It runs with `--no-configuration-cache`; the snapshot workflow on `develop` will show it first.
- JFrog evidence for Maven/Gradle attaches one multi-subject attestation to each artifact.
- Overwriting a docker tag needs the permission listed above.
