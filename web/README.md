# Samjho English School website

The website for schools: owner, headmaster and teacher logins, plus the API the app sends progress to.
It runs on Cloudflare Pages with a D1 database, on the free plan.

## Run it on this laptop

```
cd web
npm install
npm run db:local
npx wrangler pages dev --port 8788 --ip 127.0.0.1
```

Open http://127.0.0.1:8788. On a new database, first go to http://127.0.0.1:8788/setup.html
and use the setup key from `.dev.vars` to create the owner account.

To start again with an empty database, stop the server, delete `web/.wrangler/state`, and run `npm run db:local`.

API tests need an empty database. With the server running on one:

```
$env:BASE="http://127.0.0.1:8788"; npm test
```

## Test with a real phone on this laptop's server

Plug the phone in with USB debugging on, then from the project folder:

```
adb reverse tcp:8788 tcp:8788
gradlew installDebug -PschoolApiUrl=http://localhost:8788
```

In the app, open the parent section, choose the child, tap "School", and enter a code from the Students tab.

## Publish on Cloudflare

Run these from `web/`. Nothing goes online until you do. If PowerShell blocks `npx`, use `npx.cmd` instead.

1. `npx wrangler login`
2. `npx wrangler d1 create samjho-school`, then put the `database_id` it prints into `wrangler.toml`.
3. `npx wrangler pages project create samjho-school --production-branch main`
4. `npx wrangler d1 migrations apply samjho-school --remote`
5. `npx wrangler pages secret put SETUP_KEY --project-name samjho-school`, and choose a long random key.
6. `npx wrangler pages deploy`
7. Open `https://<your-site>/setup.html` once to create the owner account. It refuses to run a second time.

If the site address isn't `https://samjho-school.pages.dev`, build the app with
`-PschoolApiUrl=https://<your-site>`, otherwise phones will send to the wrong address.

## Changing the rules later

- Who sees what: `src/visibility.js`.
- The consent words in the app: change the text in `UiStrings.kt`, raise `CONSENT_VERSION` in `SchoolSync.kt`,
  and add the new version to `CONSENT_VERSIONS` in `src/api.js`.
- Skill names: after changing them in the app, refresh `public/skills.json` by running the app's unit tests with `UPDATE_WEB_SKILLS=1`.

The phone sends only what `SyncPayload` in `SchoolSync.kt` holds. Voice recordings, other-app usage and app locks are not part of it.
