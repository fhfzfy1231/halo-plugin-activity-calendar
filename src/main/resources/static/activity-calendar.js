(() => {
  const API = '/apis/api.activity.foxbridge.team/v1alpha1/calendar';
  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
  const cache = new Map();

  const node = (tag, className, text) => {
    const el = document.createElement(tag);
    if (className) el.className = className;
    if (text !== undefined) el.textContent = text;
    return el;
  };

  function localDate(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  function fetchYear(year) {
    if (!cache.has(year)) {
      cache.set(year, fetch(`${API}?year=${year}`, { credentials: 'same-origin' }).then(response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
      }));
    }
    return cache.get(year);
  }

  function tooltipText(day, showUsers) {
    if (!day || !day.score) return ['当天没有创作活动'];
    const lines = [`${day.score} 活跃分`];
    if (showUsers) {
      day.users.forEach(user => {
        const parts = [];
        if (user.addedWords) parts.push(`新增 ${user.addedWords} 字`);
        if (user.modifiedWords) parts.push(`修改 ${user.modifiedWords} 字`);
        if (user.publishedCount) parts.push(`发布 ${user.publishedCount} 篇`);
        if (user.republishedCount) parts.push(`更新发布 ${user.republishedCount} 次`);
        lines.push(`${user.displayName || user.username}：${parts.join('，') || `${user.score} 分`}`);
      });
    }
    return lines;
  }

  function placeTooltip(tooltip, target) {
    const rect = target.getBoundingClientRect();
    const margin = 8;
    tooltip.hidden = false;
    const box = tooltip.getBoundingClientRect();
    let left = rect.left + rect.width / 2 - box.width / 2;
    left = Math.max(margin, Math.min(left, window.innerWidth - box.width - margin));
    let top = rect.top - box.height - margin;
    if (top < margin) top = rect.bottom + margin;
    tooltip.style.left = `${left}px`;
    tooltip.style.top = `${top}px`;
  }

  function showTooltip(tooltip, target, date, day, showUsers) {
    tooltip.replaceChildren();
    tooltip.append(node('strong', '', `${date} · ${day?.score || 0} 活跃分`));
    tooltipText(day, showUsers).slice(1).forEach(line => tooltip.append(node('div', 'hac-tooltip-user', line)));
    if (!day?.score) tooltip.append(node('div', 'hac-tooltip-user', '当天没有创作活动'));
    placeTooltip(tooltip, target);
  }

  function renderChart(root, data, year, showUsers) {
    const card = root.querySelector('.hac-card');
    card.replaceChildren();
    const header = node('div', 'hac-header');
    header.append(node('h3', 'hac-title', root.dataset.title || '全站创作活跃度'));
    header.append(node('span', 'hac-total', `${data.totalScore || 0} 活跃分 · ${year}`));
    card.append(header);

    const scroll = node('div', 'hac-scroll');
    const chart = node('div', 'hac-chart');
    const first = new Date(year, 0, 1);
    const last = new Date(year, 11, 31);
    const gridStart = new Date(year, 0, 1 - first.getDay());
    const totalCells = Math.ceil(((last - gridStart) / 86400000 + 1) / 7) * 7;
    const totalWeeks = totalCells / 7;

    const monthRow = node('div', 'hac-months');
    monthRow.style.gridTemplateColumns = `repeat(${totalWeeks}, var(--hac-cell))`;
    months.forEach((label, month) => {
      const monthFirst = new Date(year, month, 1);
      const week = Math.floor((monthFirst - gridStart) / 86400000 / 7) + 1;
      const el = node('span', 'hac-month', label);
      el.style.gridColumn = String(week);
      monthRow.append(el);
    });
    chart.append(monthRow);

    const body = node('div', 'hac-body');
    const weekdays = node('div', 'hac-weekdays');
    ['', '一', '', '三', '', '五', ''].forEach(label => weekdays.append(node('span', '', label)));
    body.append(weekdays);
    const days = node('div', 'hac-days');
    const byDate = new Map((data.days || []).map(day => [day.date, day]));
    const tooltip = root.querySelector('.hac-tooltip');

    for (let index = 0; index < totalCells; index++) {
      const date = new Date(gridStart);
      date.setDate(gridStart.getDate() + index);
      const key = localDate(date);
      const day = byDate.get(key);
      const cell = node('button', 'hac-day');
      cell.type = 'button';
      cell.dataset.level = date.getFullYear() === year ? String(day?.level || 0) : '0';
      cell.style.visibility = date.getFullYear() === year ? 'visible' : 'hidden';
      cell.setAttribute('aria-label', `${key}，${day?.score || 0} 活跃分`);
      cell.addEventListener('mouseenter', () => showTooltip(tooltip, cell, key, day, showUsers));
      cell.addEventListener('focus', () => showTooltip(tooltip, cell, key, day, showUsers));
      cell.addEventListener('mouseleave', () => { tooltip.hidden = true; });
      cell.addEventListener('blur', () => { tooltip.hidden = true; });
      days.append(cell);
    }
    body.append(days);
    chart.append(body);

    const legend = node('div', 'hac-legend');
    legend.append(node('span', '', '少'));
    for (let i = 0; i < 5; i++) legend.append(node('i'));
    legend.append(node('span', '', '多'));
    chart.append(legend);
    scroll.append(chart);
    card.append(scroll);
  }

  function init(root) {
    if (root.dataset.hacReady === 'true') return;
    root.dataset.hacReady = 'true';
    root.style.setProperty('--hac-color', root.dataset.color || '#216e39');
    root.replaceChildren();
    const shell = node('div', 'hac-shell');
    const card = node('div', 'hac-card');
    card.append(node('div', 'hac-loading', '正在加载活跃度…'));
    const years = node('div', 'hac-years');
    const tooltip = node('div', 'hac-tooltip');
    tooltip.hidden = true;
    root.append(shell, tooltip);
    shell.append(card, years);

    const current = Number(root.dataset.year) || new Date().getFullYear();
    const count = Math.max(1, Math.min(10, Number(root.dataset.years) || 5));
    const showUsers = root.dataset.showUsers !== 'false';
    const select = year => {
      years.querySelectorAll('.hac-year').forEach(button =>
        button.setAttribute('aria-current', String(Number(button.dataset.year) === year)));
      card.replaceChildren(node('div', 'hac-loading', '正在加载活跃度…'));
      fetchYear(year)
        .then(data => renderChart(root, data, year, showUsers))
        .catch(() => card.replaceChildren(node('div', 'hac-error', '活跃度数据加载失败')));
    };

    for (let i = 0; i < count; i++) {
      const year = current - i;
      const button = node('button', 'hac-year', String(year));
      button.type = 'button';
      button.dataset.year = String(year);
      button.addEventListener('click', () => select(year));
      years.append(button);
    }
    select(current);
  }

  function initAll(scope = document) {
    scope.querySelectorAll('.halo-activity-calendar').forEach(init);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => initAll(), { once: true });
  } else {
    initAll();
  }
  new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(added => {
    if (added.nodeType === 1) {
      if (added.matches?.('.halo-activity-calendar')) init(added);
      initAll(added);
    }
  }))).observe(document.documentElement, { childList: true, subtree: true });
})();
