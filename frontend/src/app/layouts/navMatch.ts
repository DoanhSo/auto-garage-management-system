/** A sidebar entry that opens one `?tab=` of a page. Entries without `tab` match their whole path. */
export interface NavTarget {
  to: string;
  tab?: string;
  /** Other `?tab=` values of the same page that should keep this entry highlighted. */
  alsoTabs?: readonly string[];
  /** Highlighted when the page is open without a (known) `?tab=`. */
  defaultTab?: boolean;
}

export function navHref(link: NavTarget): string {
  return link.tab ? `${link.to}?tab=${link.tab}` : link.to;
}

/**
 * Whether `link` is the highlighted entry for the current location. Several entries can share one path
 * (one per tab), so exactly one of them must match.
 */
export function isNavActive(
  link: NavTarget,
  pathname: string,
  search: string,
  siblings: readonly NavTarget[],
): boolean {
  const onPath = pathname === link.to || pathname.startsWith(`${link.to}/`);
  if (!onPath) return false;
  if (!link.tab) return true;
  if (pathname !== link.to) return false;

  const current = new URLSearchParams(search).get('tab');
  const claims = (target: NavTarget, value: string) =>
    target.tab === value || (target.alsoTabs?.includes(value) ?? false);
  if (current && claims(link, current)) return true;

  const known = current !== null && siblings.some((s) => s.to === link.to && claims(s, current));
  return !known && !!link.defaultTab;
}
