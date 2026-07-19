// @vitest-environment jsdom

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { expect, test } from 'vitest'

test('shows the author score when hovering an active day', async () => {
  const payload = {
    pluginVersion: '2.1.4',
    years: {
      '2026': {
        year: 2026,
        totalScore: 2264,
        days: [
          {
            date: '2026-02-23',
            score: 2264,
            level: 4,
            users: [
              {
                username: 'fhfzfy1231',
                displayName: 'Akagi_Zen',
                addedWords: 664,
                modifiedWords: 0,
                publishedCount: 2,
                republishedCount: 0,
                score: 2264,
              },
            ],
          },
        ],
      },
    },
  }

  document.head.innerHTML = `<script type="application/json" id="halo-activity-calendar-data">${JSON.stringify(payload)}</script>`
  document.body.innerHTML =
    '<div class="halo-activity-calendar" data-year="2026" data-years="1" data-show-users="true"></div>'

  const source = readFileSync(
    resolve(process.cwd(), '../src/main/resources/static/activity-calendar.js'),
    'utf8',
  )
  window.eval(source)
  document.dispatchEvent(new Event('DOMContentLoaded'))
  await Promise.resolve()
  await Promise.resolve()

  const day = document.querySelector<HTMLButtonElement>(
    '.hac-day[aria-label="2026-02-23，2264 活跃分"]',
  )
  expect(day).not.toBeNull()
  day?.dispatchEvent(new MouseEvent('mouseenter'))

  const tooltip = document.querySelector<HTMLElement>('body > .hac-tooltip')
  expect(tooltip?.hidden).toBe(false)
  expect(tooltip?.textContent).toContain('Akagi_Zen：2264 活跃分')
  expect(tooltip?.textContent).toContain('新增 664 字')
  expect(tooltip?.textContent).toContain('发布 2 篇')
})
