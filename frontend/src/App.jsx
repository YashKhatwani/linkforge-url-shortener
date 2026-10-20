import { useCallback, useEffect, useState } from "react";
import "./App.css";

const API_BASE = "http://localhost:8080";
const PAGE_SIZE = 10;

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const body = await response.text();
    let message = body;

    try {
      const parsed = JSON.parse(body);
      message = parsed.message || parsed.error || body;
    } catch {
      // Keep the original response text if it isn't JSON.
    }

    if (response.status === 0) {
      throw new Error("Could not connect to the backend.");
    }

    throw new Error(message || `Request failed (${response.status})`);
  }

  return response.json();
}

function formatDate(value) {
  if (!value) return "No expiry";
  return new Date(value).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function App() {
  const [originalUrl, setOriginalUrl] = useState("");
  const [customAlias, setCustomAlias] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [createdUrl, setCreatedUrl] = useState("");
  const [formError, setFormError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [creating, setCreating] = useState(false);

  const [urlPage, setUrlPage] = useState({
    content: [],
    page: 0,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  });
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState("");
  const [currentPage, setCurrentPage] = useState(0);
  const [copiedUrl, setCopiedUrl] = useState("");

  const loadUrls = useCallback(async (page = 0) => {
    setLoading(true);
    setListError("");

    try {
      const data = await apiRequest(
        `/api/urls?page=${page}&size=${PAGE_SIZE}`
      );
      setUrlPage(data);
      setCurrentPage(data.page);
    } catch (error) {
      setListError(
        `${error.message}. Make sure Spring Boot is running on port 8080.`
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUrls(0);
  }, [loadUrls]);

  const handleCreate = async (event) => {
    event.preventDefault();
    setFormError("");
    setSuccessMessage("");
    setCreatedUrl("");

    const trimmedUrl = originalUrl.trim();
    if (!/^https?:\/\/.+/i.test(trimmedUrl)) {
      setFormError("Enter a valid URL beginning with http:// or https://.");
      return;
    }

    setCreating(true);

    try {
      const payload = { originalUrl: trimmedUrl };

      if (customAlias.trim()) {
        payload.customAlias = customAlias.trim();
      }

      if (expiresAt) {
        const expiryDate = new Date(expiresAt);

        if (Number.isNaN(expiryDate.getTime()) || expiryDate <= new Date()) {
          setFormError("Choose an expiration date and time in the future.");
          setCreating(false);
          return;
        }

        payload.expiresAt = expiryDate.toISOString();
      }

      const result = await apiRequest("/api/urls", {
        method: "POST",
        body: JSON.stringify(payload),
      });

      setCreatedUrl(result.shortUrl);
      setSuccessMessage("Your short link is ready!");
      setOriginalUrl("");
      setCustomAlias("");
      setExpiresAt("");

      await loadUrls(0);
    } catch (error) {
      setFormError(
        error.message ||
          "Unable to create your link. Please check the backend and try again."
      );
    } finally {
      setCreating(false);
    }
  };

  const copyToClipboard = async (url) => {
    try {
      await navigator.clipboard.writeText(url);
      setCopiedUrl(url);
      window.setTimeout(() => {
        setCopiedUrl((current) => (current === url ? "" : current));
      }, 1800);
    } catch {
      setListError("Clipboard access failed. Please copy the URL manually.");
    }
  };

  const pageClicks = urlPage.content.reduce(
    (sum, url) => sum + (url.totalClicks || 0),
    0
  );

  const getExpiryStatus = (expiresAtValue) => {
    if (!expiresAtValue) return { label: "Never expires", type: "neutral" };

    if (new Date(expiresAtValue) <= new Date()) {
      return { label: "Expired", type: "danger" };
    }

    return { label: `Until ${formatDate(expiresAtValue)}`, type: "active" };
  };

  return (
    <main className="app-shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="LinkForge home">
          <span className="brand-mark">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path
                d="M10 13.5a4.5 4.5 0 0 0 6.36 0l3-3a4.5 4.5 0 0 0-6.36-6.36l-1.72 1.72"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
              <path
                d="M14 10.5a4.5 4.5 0 0 0-6.36 0l-3 3A4.5 4.5 0 0 0 11 19.86l1.72-1.72"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
              />
            </svg>
          </span>
          <span>LinkForge</span>
        </a>

        <div className="topbar-right">
          <span className="status-dot" />
          <span>Local environment</span>
        </div>
      </header>

      {/* <section className="welcome">
        <div>
          <p className="eyebrow">YOUR LINK WORKSPACE</p>
          <h1>Short links. <span>Clear insights.</span></h1>
          <p className="welcome-copy">
            Create memorable links and keep an eye on their performance.
          </p>
        </div>
        <div className="welcome-decoration" aria-hidden="true">
          <span className="decoration-circle circle-one" />
          <span className="decoration-circle circle-two" />
          <span className="decoration-link">↗</span>
        </div>
      </section> */}

    

      <section className="content-grid">
        <article className="panel create-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">GET STARTED</p>
              <h2>Create a short link</h2>
              <p className="panel-description">
                Turn a long URL into a simple, shareable link.
              </p>
            </div>
            <div className="heading-icon">✳</div>
          </div>

          <form onSubmit={handleCreate} className="create-form">
            <label htmlFor="originalUrl">Original URL <span>*</span></label>
            <div className="input-wrap">
              <span className="input-icon">↗</span>
              <input
                id="originalUrl"
                type="url"
                placeholder="https://example.com/your-long-url"
                value={originalUrl}
                onChange={(event) => setOriginalUrl(event.target.value)}
                required
              />
            </div>

            <label htmlFor="customAlias">
              Custom alias <span className="optional">(optional)</span>
            </label>
            <div className="alias-input">
              <span>localhost:8080/</span>
              <input
                id="customAlias"
                type="text"
                placeholder="my-link"
                value={customAlias}
                onChange={(event) => setCustomAlias(event.target.value)}
                pattern="[A-Za-z0-9]+"
                title="Use letters and numbers only."
              />
            </div>
            <p className="field-hint">
              Leave blank to let the server generate an alias. Letters and
              numbers only.
            </p>

            <label htmlFor="expiresAt">
              Expiration date <span className="optional">(optional)</span>
            </label>
            <input
              id="expiresAt"
              className="date-input"
              type="datetime-local"
              value={expiresAt}
              min={new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
                .toISOString()
                .slice(0, 16)}
              onChange={(event) => setExpiresAt(event.target.value)}
            />
            <p className="field-hint">
              Leave blank if you don't want the link to expire.
            </p>

            {formError && <div className="message error-message">{formError}</div>}

            <button className="primary-button" type="submit" disabled={creating}>
              {creating ? "Creating link…" : "Create short link"}
              {!creating && <span>→</span>}
            </button>
          </form>

          {successMessage && createdUrl && (
            <div className="created-result">
              <div className="success-heading">
                <span>✓</span> {successMessage}
              </div>
              <div className="created-url-row">
                <a href={createdUrl} target="_blank" rel="noreferrer">
                  {createdUrl}
                </a>
                <button
                  type="button"
                  className="copy-button"
                  onClick={() => copyToClipboard(createdUrl)}
                >
                  {copiedUrl === createdUrl ? "Copied!" : "Copy"}
                </button>
              </div>
            </div>
          )}
        </article>


        <article className="panel tips-panel">
          <div className="tips-art" aria-hidden="true">
            <div className="art-card art-card-back" />
            <div className="art-card art-card-front">
              <span className="art-link-icon">↗</span>
              <span className="art-line line-long" />
              <span className="art-line line-short" />
              <span className="art-check">✓</span>
            </div>
            <span className="art-sparkle sparkle-one">✦</span>
            <span className="art-sparkle sparkle-two">✧</span>
          </div>
          <p className="eyebrow">MADE SIMPLE</p>
          <h2>One link, less clutter.</h2>
          <p className="panel-description">
            Use a custom alias to make a link easier to remember, and set an
            expiration date when access should be temporary.
          </p>
          <div className="tip-row">
            <span className="tip-check">✓</span>
            <span>Custom links you can remember</span>
          </div>
          <div className="tip-row">
            <span className="tip-check">✓</span>
            <span>Click analytics from your backend</span>
          </div>
          <div className="tip-row">
            <span className="tip-check">✓</span>
            <span>Optional link expiration</span>
          </div>
        </article>
      </section>

        <section className="stats-grid" aria-label="Link statistics">
        <article className="stat-card">
          <div className="stat-icon purple-icon">↗</div>
          <p>Total links</p>
          <div className="stat-value">
            {loading ? "—" : urlPage.totalElements}
          </div>
          <span className="stat-note">Across your workspace</span>
        </article>

        <article className="stat-card">
          <div className="stat-icon green-icon">◎</div>
          <p>Clicks on displayed links</p>
          <div className="stat-value">{loading ? "—" : pageClicks}</div>
          <span className="stat-note">Sum of displayed link counts</span>
        </article>

        <article className="stat-card">
          <div className="stat-icon orange-icon">▤</div>
          <p>Current page</p>
          <div className="stat-value">
            {loading ? "—" : urlPage.totalPages === 0 ? "0 / 0" : `${currentPage + 1} / ${urlPage.totalPages}`}
          </div>
          <span className="stat-note">Showing up to {PAGE_SIZE} links</span>
        </article>
      </section>

      <section className="panel links-panel">
        <div className="links-heading">
          <div>
            <p className="eyebrow">YOUR WORKSPACE</p>
            <h2>Your shortened links</h2>
            <p className="panel-description">
              Manage your links and see their click counts.
            </p>
          </div>
          <button
            type="button"
            className="secondary-button"
            onClick={() => loadUrls(currentPage)}
            disabled={loading}
          >
            <span className={loading ? "refresh-icon spinning" : "refresh-icon"}>↻</span>
            Refresh
          </button>
        </div>

        {listError && (
          <div className="message error-message list-error">
            {listError}
            <button type="button" onClick={() => loadUrls(currentPage)}>
              Retry
            </button>
          </div>
        )}

        {loading ? (
          <div className="empty-state">
            <div className="loader" />
            <p>Loading your links…</p>
          </div>
        ) : !listError && urlPage.content.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">↗</div>
            <h3>No links yet</h3>
            <p>Create your first short link using the form above.</p>
          </div>
        ) : (
          !listError && (
            <>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>SHORT LINK</th>
                      <th>DESTINATION</th>
                      <th>CLICKS</th>
                      <th>EXPIRES</th>
                      <th>ACTIONS</th>
                    </tr>
                  </thead>
                  <tbody>
                    {urlPage.content.map((url) => {
                      const expiry = getExpiryStatus(url.expiresAt);
                      const destinationHost = (() => {
                        try {
                          return new URL(url.originalUrl).hostname;
                        } catch {
                          return url.originalUrl;
                        }
                      })();

                      return (
                        <tr key={url.shortCode}>
                          <td>
                            <a
                              className="short-link"
                              href={url.shortUrl}
                              target="_blank"
                              rel="noreferrer"
                            >
                              {url.shortCode}
                            </a>
                            <div className="cell-subtitle">{url.shortUrl}</div>
                          </td>
                          <td>
                            <div className="destination">
                              <span className="destination-icon">↗</span>
                              <span title={url.originalUrl}>{destinationHost}</span>
                            </div>
                            <div className="cell-subtitle destination-url">
                              {url.originalUrl}
                            </div>
                          </td>
                          <td>
                            <span className="click-count">{url.totalClicks ?? 0}</span>
                          </td>
                          <td>
                            <span className={`expiry-badge ${expiry.type}`}>
                              {expiry.label}
                            </span>
                          </td>
                          <td>
                            <button
                              type="button"
                              className="table-copy-button"
                              onClick={() => copyToClipboard(url.shortUrl)}
                              aria-label={`Copy ${url.shortCode}`}
                            >
                              {copiedUrl === url.shortUrl ? "Copied!" : "Copy link"}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <div className="pagination">
                <span>
                  Showing {urlPage.content.length} of {urlPage.totalElements} links
                </span>
                <div className="pagination-actions">
                  <button
                    type="button"
                    className="page-button"
                    disabled={currentPage === 0 || loading}
                    onClick={() => loadUrls(currentPage - 1)}
                  >
                    ← Previous
                  </button>
                  <span className="page-number">
                    {currentPage + 1} / {Math.max(urlPage.totalPages, 1)}
                  </span>
                  <button
                    type="button"
                    className="page-button"
                    disabled={currentPage + 1 >= urlPage.totalPages || loading}
                    onClick={() => loadUrls(currentPage + 1)}
                  >
                    Next →
                  </button>
                </div>
              </div>
            </>
          )
        )}
      </section>

      <footer className="footer">
        <span>LinkForge</span>
        <span>Built with React, Spring Boot, Redis & Kafka</span>
      </footer>
    </main>
  );
}

export default App;