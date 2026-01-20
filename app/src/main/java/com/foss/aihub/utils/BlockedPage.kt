package com.foss.aihub.utils

import com.foss.aihub.models.AiService

fun buildBlockedPage(url: String, service: AiService): String {
    return """
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Page Blocked</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box
        }

        body {
            background: #F3EDF7;
            color: #1C1B1F;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px
        }

        .card {
            background: white;
            border-radius: 24px;
            padding: 32px;
            max-width: 480px;
            width: 100%;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08)
        }

        .header {
            text-align: center;
            margin-bottom: 28px
        }

        .icon {
            font-size: 48px;
            margin-bottom: 16px
        }

        .title {
            font-size: 24px;
            font-weight: 500;
            color: #1C1B1F;
            margin-bottom: 8px
        }

        .description {
            color: #49454F;
            font-size: 16px;
            line-height: 1.5
        }

        .info-box {
            background: #F3EDF7;
            border-radius: 16px;
            padding: 20px;
            margin-bottom: 24px;
            border: 1px solid #E6E0E9
        }

        .info-row {
            margin-bottom: 16px
        }

        .info-row:last-child {
            margin-bottom: 0
        }

        .info-label {
            font-size: 14px;
            color: #6750A4;
            font-weight: 500;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 8px
        }

        .info-content {
            display: flex;
            gap: 12px;
            align-items: flex-start
        }

        .info-text {
            flex: 1;
            background: white;
            padding: 12px 16px;
            border-radius: 12px;
            border: 1px solid #CAC4D0;
            font-family: monospace, Consolas, 'Courier New';
            font-size: 14px;
            color: #1C1B1F;
            word-break: break-all;
            line-height: 1.4
        }

        .copy-btn {
            background: #6750A4;
            color: white;
            border: none;
            border-radius: 20px;
            padding: 10px 20px;
            font-size: 14px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.2s;
            flex-shrink: 0;
            min-width: 80px
        }

        .copy-btn:hover {
            background: #5A4B8C;
            transform: translateY(-1px)
        }

        .copy-btn:active {
            transform: translateY(0)
        }

        .copy-btn.copied {
            background: #4CAF50
        }

        .footer {
            color: #79747E;
            font-size: 14px;
            text-align: center;
            margin-top: 24px;
            line-height: 1.4
        }

        @media (max-width:480px) {
            .card {
                padding: 24px
            }

            .info-content {
                flex-direction: column
            }

            .copy-btn {
                width: 100%
            }
        }
    </style>
</head>

<body>
    <div class="card">
        <div class="header">
            <div class="icon">🚫</div>
            <h1 class="title">Page Blocked</h1>
            <div class="description">This page can't load due to security
                settings.</div>
        </div>
        <div class="info-box">
            <div class="info-row">
                <div class="info-label"><span>🔗</span><span>Blocked
                        URL</span></div>
                <div class="info-content">
                    <div class="info-text">${url.replace("<", "&lt;").replace(">", "&gt;")}</div>
                    <button class="copy-btn" onclick="copyText('${url.replace("'", "\\'")}',this)">Copy</button>
                </div>
            </div>
            <div class="info-row">
                <div class="info-label"><span>📱</span><span>Service</span></div>
                <div class="info-content">
                    <div class="info-text">${service.name}</div>
                    <button class="copy-btn" onclick="copyText('${service.name.replace("'", "\\'")}',this)">Copy</button>
                </div>
            </div>
        </div>
        <div class="footer">Copy these details if you need to report this
            issue</div>
    </div>
    <script>
        function copyText(t, b) { const e = document.createElement("input"); e.value = t; e.style.position = "fixed"; e.style.opacity = "0"; document.body.appendChild(e); e.select(); try { if (document.execCommand("copy")) { const t = b.textContent; b.textContent = "Copied!"; b.classList.add("copied"); setTimeout(() => { b.textContent = t; b.classList.remove("copied") }, 1500) } } catch (t) { console.log("Copy failed") } document.body.removeChild(e) }
    </script>
</body>

</html>""".trimIndent()
}