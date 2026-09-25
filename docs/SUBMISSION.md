# Submission checklist

The repository already has an origin/main commit created during implementation. The final polish, tests and documents are being added locally; push the final main and feature branch after review.

1. Run the acceptance checklist using your own TMDB account.
2. Review README, DECISIONS, AI_LOG and RELATORIO. Do not claim unchecked manual tests passed.
3. Confirm the supplied seed is unchanged and .env is ignored.
4. Inspect local history with git log --graph --oneline --all. The final implementation should include a real feature branch and merge; do not invent historical work you did not do.
5. Create your chosen empty GitHub repository. Add its URL as origin, then push main and the feature branch. Choose visibility appropriate to the assignment.
6. Give the evaluator the repository URL, startup instructions and the documents. Do not share your TMDB token.

Before pushing, run:
```bash
git status --short
git check-ignore .env
git ls-files .env
git log --graph --oneline --all
```

The .env check-ignore command should print .env; git ls-files .env must print nothing. Review any other files that could contain private data. target/, work/ and IDE metadata should remain ignored.
