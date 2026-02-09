(() => {
  const layout = document.querySelector('.layout');
  if (!layout) {
    return;
  }

  const apiPath = layout.dataset.apiPath || '/ui/api/dashboard';
  const generatedAt = document.getElementById('generated-at');
  const totalEvents = document.getElementById('kpi-total-events');
  const averageSpeed = document.getElementById('kpi-average-speed');
  const maxCongestion = document.getElementById('kpi-max-congestion');
  const userLabel = document.getElementById('kpi-user');
  const summaryTableBody = document.querySelector('#summary-table tbody');
  const recentTableBody = document.querySelector('#recent-table tbody');
  const regionFilter = document.getElementById('region-filter');
  const limitFilter = document.getElementById('limit-filter');
  const refreshButton = document.getElementById('refresh-dashboard');
  const flashStack = document.querySelector('.flash-stack');

  let refreshing = false;

  // 현재 필터를 기준으로 대시보드 스냅샷 API를 호출한다.
  async function refreshDashboard() {
    if (refreshing) {
      return;
    }

    refreshing = true;
    setRefreshPending(true);

    try {
      const url = `${apiPath}?region=${encodeURIComponent(currentRegion())}&limit=${currentLimit()}`;
      const response = await fetch(url, {
        headers: {
          Accept: 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`대시보드 갱신 실패(${response.status})`);
      }

      const payload = await response.json();
      renderKpis(payload.summary, payload.authenticated, payload.username);
      renderSummaryRows(payload.summary.regions || []);
      renderRecentRows(payload.recentEvents || []);
      renderGeneratedAt(payload.generatedAt);
    } catch (error) {
      showFlash('error', error.message || '데이터 갱신 중 오류가 발생했습니다.');
    } finally {
      refreshing = false;
      setRefreshPending(false);
    }
  }

  // 지역 필터 값을 API 규칙에 맞게 보정한다.
  function currentRegion() {
    const value = (regionFilter?.value || 'ALL').trim().toUpperCase();
    return value === 'ALL' ? '' : value;
  }

  // 조회 개수 필터 값을 안전한 범위로 보정한다.
  function currentLimit() {
    const numeric = Number.parseInt(limitFilter?.value || '20', 10);
    if (Number.isNaN(numeric)) {
      return 20;
    }
    return Math.min(100, Math.max(5, numeric));
  }

  // 새 데이터 기준으로 KPI 카드 숫자/텍스트를 갱신한다.
  function renderKpis(summary, authenticated, username) {
    animateNumber(totalEvents, Number(summary.totalEvents || 0));

    const regions = summary.regions || [];
    const avg = regions.length === 0
      ? 0
      : regions.reduce((acc, item) => acc + Number(item.averageSpeedKph || 0), 0) / regions.length;

    const max = regions.reduce(
      (acc, item) => Math.max(acc, Number(item.latestCongestionLevel || 0)),
      0,
    );

    averageSpeed.textContent = regions.length === 0 ? '-' : avg.toFixed(1);
    maxCongestion.textContent = max === 0 ? '-' : String(max);
    userLabel.textContent = authenticated ? username || '운영자' : '익명 모드';
  }

  // 지역 요약 테이블 본문을 최신 스냅샷으로 다시 렌더링한다.
  function renderSummaryRows(rows) {
    summaryTableBody.innerHTML = '';

    if (rows.length === 0) {
      summaryTableBody.appendChild(createEmptyRow(4, '표시할 데이터가 없습니다.'));
      return;
    }

    rows.forEach((row) => {
      const tr = document.createElement('tr');
      tr.setAttribute('data-row-animate', 'true');
      tr.appendChild(createCell(row.region));
      tr.appendChild(createCell(String(row.totalEvents)));
      tr.appendChild(createCell(Number(row.averageSpeedKph || 0).toFixed(1)));

      const badge = document.createElement('span');
      badge.className = `congestion level-${Number(row.latestCongestionLevel || 1)}`;
      badge.textContent = String(row.latestCongestionLevel || 1);
      const congestionCell = document.createElement('td');
      congestionCell.appendChild(badge);
      tr.appendChild(congestionCell);

      summaryTableBody.appendChild(tr);
    });
  }

  // 최근 이벤트 테이블 본문을 최신 스냅샷으로 다시 렌더링한다.
  function renderRecentRows(rows) {
    recentTableBody.innerHTML = '';

    if (rows.length === 0) {
      recentTableBody.appendChild(createEmptyRow(5, '최근 이벤트가 없습니다.'));
      return;
    }

    rows.forEach((event) => {
      const tr = document.createElement('tr');
      tr.setAttribute('data-row-animate', 'true');
      tr.appendChild(createCell(formatIsoDate(event.observedAt)));
      tr.appendChild(createCell(event.region));
      tr.appendChild(createCell(event.roadName));
      tr.appendChild(createCell(String(event.averageSpeedKph)));

      const badge = document.createElement('span');
      badge.className = `congestion level-${Number(event.congestionLevel || 1)}`;
      badge.textContent = String(event.congestionLevel || 1);
      const congestionCell = document.createElement('td');
      congestionCell.appendChild(badge);
      tr.appendChild(congestionCell);

      recentTableBody.appendChild(tr);
    });
  }

  // 공통 테이블 셀 엘리먼트를 생성한다.
  function createCell(value) {
    const td = document.createElement('td');
    td.textContent = value;
    return td;
  }

  // 공통 빈 데이터 행을 생성한다.
  function createEmptyRow(colspan, message) {
    const tr = document.createElement('tr');
    const td = document.createElement('td');
    td.className = 'empty';
    td.colSpan = colspan;
    td.textContent = message;
    tr.appendChild(td);
    return tr;
  }

  // ISO 시간을 읽기 쉬운 로컬 시각으로 변환한다.
  function formatIsoDate(value) {
    if (!value) {
      return '-';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date);
  }

  // 숫자 카드의 값 변화에 카운트업 애니메이션을 적용한다.
  function animateNumber(node, nextValue) {
    if (!node) {
      return;
    }

    const previous = Number.parseInt(node.dataset.value || '0', 10);
    const target = Number.isFinite(nextValue) ? nextValue : 0;
    const distance = target - previous;

    if (distance === 0) {
      node.textContent = String(target);
      node.dataset.value = String(target);
      return;
    }

    const steps = 18;
    const increment = distance / steps;
    let current = previous;
    let frame = 0;

    const timer = window.setInterval(() => {
      frame += 1;
      current += increment;
      if (frame >= steps) {
        current = target;
        window.clearInterval(timer);
      }
      node.textContent = String(Math.round(current));
      node.dataset.value = String(Math.round(current));
    }, 16);
  }

  // 마지막 갱신 시각 텍스트를 업데이트한다.
  function renderGeneratedAt(timestamp) {
    if (!generatedAt) {
      return;
    }
    generatedAt.textContent = formatIsoDate(timestamp);
  }

  // 갱신 버튼 상태를 요청 진행 여부에 맞춰 토글한다.
  function setRefreshPending(isPending) {
    if (!refreshButton) {
      return;
    }
    refreshButton.disabled = isPending;
    refreshButton.textContent = isPending ? '갱신 중...' : '즉시 새로고침';
  }

  // 상단 플래시 영역에 일시 오류 메시지를 표시한다.
  function showFlash(type, message) {
    if (!flashStack) {
      return;
    }

    const div = document.createElement('div');
    div.className = `flash ${type}`;
    div.textContent = message;
    flashStack.prepend(div);
    window.setTimeout(() => div.remove(), 4000);
  }

  // 사용자 액션 이벤트를 등록한다.
  function bindEvents() {
    refreshButton?.addEventListener('click', refreshDashboard);
    regionFilter?.addEventListener('change', refreshDashboard);
    limitFilter?.addEventListener('change', refreshDashboard);

    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        refreshDashboard();
      }
    });
  }

  bindEvents();
  refreshDashboard();
  window.setInterval(refreshDashboard, 5000);
})();
