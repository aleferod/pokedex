/** Header navigation link style, shared by the layout and feature navigation. */
export function navLinkClass({ isActive }: { isActive: boolean }): string {
  return `rounded px-2 py-1 font-medium hover:bg-red-700 ${isActive ? 'underline underline-offset-4' : ''}`;
}
