export function navigateTo(path: string) {
  window.history.pushState(null, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

export function readDatasetIdFromSearch(search = window.location.search): string | null {
  const datasetId = new URLSearchParams(search).get('datasetId');
  return datasetId?.trim() || null;
}

export function withDatasetQuery(path: string, datasetId?: string | null): string {
  if (!datasetId?.trim()) {
    return path;
  }
  const [pathname, search = ''] = path.split('?');
  const params = new URLSearchParams(search);
  params.set('datasetId', datasetId.trim());
  const query = params.toString();
  return query ? `${pathname}?${query}` : pathname;
}
