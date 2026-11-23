#!/usr/bin/env bash
# Trusted default-branch maintenance only. No upstream code is executed with this job's token.
set -euo pipefail

repo="${GITHUB_REPOSITORY:?}"
if [[ "$repo" != 'wxyu71/Lumi_Books' || "${GITHUB_REF:?}" != 'refs/heads/main' ]]; then
    echo 'Upstream sync can run only on the community default branch.' >&2
    exit 1
fi
: "${GH_TOKEN:?}"
: "${GITHUB_RUN_ID:?}"
summary="${GITHUB_STEP_SUMMARY:?}"

log_summary() { printf '%s\n' "$*" >> "$summary"; }

git config user.name 'github-actions[bot]'
git config user.email '41898282+github-actions[bot]@users.noreply.github.com'
gh auth setup-git

git remote add upstream https://github.com/huangder/Lumi_Books.git
git fetch --no-tags upstream main
git fetch --no-tags origin main
community_head="$(git rev-parse origin/main)"
upstream_head="$(git rev-parse upstream/main)"
if [[ "$(git rev-parse HEAD)" != "$community_head" ]]; then
    echo 'Community main advanced before preparation; retry later.' >&2
    exit 1
fi
if git merge-base --is-ancestor "$upstream_head" "$community_head"; then
    log_summary 'No unmerged upstream commits; main is unchanged.'
    exit 0
fi

git_base="$(git merge-base "$community_head" "$upstream_head")"
review_ref="sync/upstream-${upstream_head:0:12}"
candidate_ref="sync/verify-${GITHUB_RUN_ID}"

# A clean three-way merge can still change a file with community-specific edits.
# Treat concurrent edits to any same file as a semantic conflict requiring review.
mapfile -d '' -t touched_both < <(
    comm -z -12 \
        <(git diff --no-renames --name-only -z "$git_base" "$community_head" | LC_ALL=C sort -zu) \
        <(git diff --no-renames --name-only -z "$git_base" "$upstream_head" | LC_ALL=C sort -zu)
)
protected=()
while IFS= read -r -d '' path; do
    case "$path" in
        .github/workflows/*|.github/actions/*|tools/ci_*|\
        gradle.properties|app/build.gradle.kts|app/src/oss/*|\
        app/src/main/java/com/huangder/lumibooks/edition/*|\
        app/src/main/AndroidManifest.xml|app/src/main/assets/changelog.md|\
        app/src/main/res/values*/strings*.xml|COMMUNITY.md|AGENTS.md)
            protected+=("$path") ;;
    esac
done < <(git diff --no-renames --name-only -z "$git_base" "$upstream_head")

report_changed_paths() {
    if (( ${#touched_both[@]} > 0 )); then
        printf '%s\n' '#### Files changed by both forks' '```text'
        printf '%s\n' "${touched_both[@]:0:60}"
        printf '%s\n' '```'
    fi
    if (( ${#protected[@]} > 0 )); then
        printf '%s\n' '#### Community-protected paths changed upstream' '```text'
        printf '%s\n' "${protected[@]:0:60}"
        printf '%s\n' '```'
    fi
}

open_review_pr() {
    local reason="$1" source_sha="$2" existing temp
    existing="$(gh pr list --repo "$repo" --base main --head "$review_ref" \
        --state open --json number --jq '.[0].number // empty')"
    if [[ -n "$existing" ]]; then
        log_summary "Upstream sync needs review: existing PR #${existing} ($reason)."
        return 0
    fi
    if git ls-remote --exit-code origin "refs/heads/$review_ref" >/dev/null 2>&1; then
        # Do not force-push over a previously reviewed branch.
        review_ref="sync/upstream-${upstream_head:0:12}-${GITHUB_RUN_ID}"
    fi
    git push origin "${source_sha}:refs/heads/${review_ref}"
    temp="$(mktemp)"
    {
        printf '%s\n\n' "Automated upstream sync stopped: **${reason}**. No update was pushed to \`main\`."
        printf '%s\n' "- Community base: \`${community_head}\`"
        printf '%s\n' "- Upstream commit: \`${upstream_head}\`"
        printf '%s\n\n' '- Preserve the Community application ID/version, OSS-only edition, locally managed book and annotation tags, optional reading journal, exports, onboarding/user autonomy, and release/security workflows.'
        printf '%s\n\n' '- This is an upstream review branch. Resolve any Git conflicts or product-policy overlaps on this PR branch (or replace it with a properly resolved branch), then run the standard Android CI before merging.'
        printf '%s\n\n' '- An automatic three-way merge is never considered sufficient if the same file was modified by the Community fork, or upstream touched protected release/edition files.'
        report_changed_paths
    } > "$temp"
    gh pr create --repo "$repo" --draft --base main --head "$review_ref" \
        --title "chore(sync): review upstream ${upstream_head:0:12}" --body-file "$temp"
    rm -f "$temp"
    log_summary "Upstream sync needs review: $reason. Opened Draft PR from \`${review_ref}\`."
}

git switch -c "$candidate_ref" "$community_head"
if ! git merge --no-ff --no-edit "$upstream_head"; then
    git merge --abort
    open_review_pr 'Git merge conflicts' "$upstream_head"
    exit 0
fi
candidate_sha="$(git rev-parse HEAD)"
if (( ${#touched_both[@]} > 0 || ${#protected[@]} > 0 )); then
    open_review_pr 'Community edit overlap / protected path changed upstream' "$candidate_sha"
    exit 0
fi
if ! git diff --check "$community_head" "$candidate_sha"; then
    open_review_pr 'Merge result contains whitespace errors' "$candidate_sha"
    exit 0
fi

# GITHUB_TOKEN branch pushes do NOT trigger push/pull_request workflow runs.
# Explicit workflow_dispatch is required; Beta runs without release secrets on this branch.
git push origin "${candidate_sha}:refs/heads/${candidate_ref}"
gh workflow run android-beta.yml --repo "$repo" --ref "$candidate_ref"

run_id=''
status=''
conclusion=''
for ((attempt=0; attempt<240; attempt++)); do
    info="$(gh api -X GET "repos/${repo}/actions/workflows/android-beta.yml/runs" \
        -f event=workflow_dispatch -f branch="$candidate_ref" -f per_page=20 \
        --jq ".workflow_runs[] | select(.head_sha == \"${candidate_sha}\") | [.id, .status, (.conclusion // \"\")] | @tsv")"
    if [[ -n "$info" ]]; then
        IFS=$'\t' read -r run_id status conclusion <<< "$info"
        if [[ "$status" == completed ]]; then break; fi
    fi
    sleep 30
done
if [[ "$status" != completed || "$conclusion" != success ]]; then
    open_review_pr "Candidate CI did not pass (run ${run_id:-unknown}; ${status:-not-started}/${conclusion:-unknown})" "$candidate_sha"
    exit 0
fi

# Never promote a merge based on a stale main or outdated upstream tip.
git fetch --no-tags origin main
git fetch --no-tags upstream main
if [[ "$(git rev-parse origin/main)" != "$community_head" || "$(git rev-parse upstream/main)" != "$upstream_head" ]]; then
    open_review_pr 'Main or upstream advanced while CI was running' "$candidate_sha"
    exit 0
fi

# This is an ordinary fast-forward, not a force-push. A concurrent main update rejects it.
if ! git push origin "${candidate_sha}:refs/heads/main"; then
    open_review_pr 'Main rejected the fast-forward; a branch rule or concurrent push may block it' "$candidate_sha"
    exit 0
fi

# A GITHUB_TOKEN push suppresses on:push. Dispatch main explicitly and opt in to
# its normal gated release behavior (which skips versions already published).
gh workflow run android-beta.yml --repo "$repo" --ref main -f publish=true
log_summary "Merged upstream \`${upstream_head}\` into main at \`${candidate_sha}\` after full CI run #${run_id}. Started a separate main CI/release gate."
git push origin --delete "$candidate_ref"
