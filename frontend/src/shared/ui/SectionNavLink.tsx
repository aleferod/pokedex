import type { ReactNode } from 'react';
import { NavLink, useMatch } from 'react-router';

import { navLinkClass } from './navLinkClass';

/**
 * A main-navigation link to a section ("Catalog", "My Pokédex"): the current page on its own path,
 * and the current item (`aria-current="true"`) on the pages below it, so a page inside the section
 * (a Pokémon, a record) is the only current page.
 */
export function SectionNavLink({ to, children }: { to: string; children: ReactNode }) {
  const onSectionPage = useMatch(to) !== null;
  return (
    <NavLink to={to} aria-current={onSectionPage ? 'page' : 'true'} className={navLinkClass}>
      {children}
    </NavLink>
  );
}
