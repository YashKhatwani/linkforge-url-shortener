# GitHub Release Checklist

- [ ] Verify `README.md` commands from a clean checkout.
- [ ] Run ` .\mvnw.cmd test` from the repository root.
- [ ] Run `npm install` and `npm run build` inside `frontend/`.
- [ ] Verify `docker compose up -d` starts the dependencies.
- [ ] Review Compose credentials; keep development values out of production.
- [ ] Search for tokens, certificates, passwords, and private URLs.
- [ ] Review `git status` and `git diff` before committing.
- [ ] Add dashboard screenshots to `docs/images/`.
- [ ] Confirm Mermaid diagrams render on GitHub.
- [ ] Add a license only if you want to permit reuse.

Suggested first documentation commit:

```powershell
git status
git add README.md .gitignore docs
git diff --cached
git commit -m "docs: add project README and architecture"
```
