export function pageEntries(page: number, last: number): Array<number | 'gap'> {
  if (last <= 4) return Array.from({ length: last + 1 }, (_, index) => index);

  const pages = [...new Set([0, page - 1, page, page + 1, last])]
    .filter((value) => value >= 0 && value <= last)
    .sort((a, b) => a - b);
  const result: Array<number | 'gap'> = [];
  pages.forEach((value, index) => {
    if (index > 0 && value - pages[index - 1] > 1) result.push('gap');
    result.push(value);
  });
  return result;
}
