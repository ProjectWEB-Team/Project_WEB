# Planning branch policy — 2026-10-26

- The established planning branch is **project-plans**; "nhánh plan" means this existing branch, not a new branch named plan.
- Main contains runnable application source/tests/resources and setup documentation. Keep execution plans, requirements, roadmap, state and review guides on project-plans.
- Synchronize **main into project-plans**, never the reverse. This correction merges the cleaned main into the planning branch and preserves existing planning history and files.
- Read the TV3 Spring Tools guide on GitHub/project-plans, or use a separate documentation checkout. Run the application from main using Spring Tools. Do not switch the source branch while STS is running.
- Wrong publication: 7b6ab31 added planning to main. Correct it with a new deletion/cleanup commit, not reset/force-push. Preserve e99257c source fixes and shared history. The old commit stays in history; the current main tree no longer includes those planning files.
- Phase01–05 implementation is complete per owner confirmation. Historical missing summaries/verification and unobserved realtime/manual checks are recorded honestly; TV3 evaluates Phase05.1, TV2 does Phase06 in parallel, and the owner reviews last.
- GUIDE-01 means delivery of one manual guide and is complete. It does not mean TV3 has executed/approved that guide. HARD-01–03 remain pending.
- Local .local/, .agents/, .gsd/, .planning/debug/, .planning/tmp/ and review scratch files are excluded from publication. No credentials, raw reports or OTPs are part of this change.
