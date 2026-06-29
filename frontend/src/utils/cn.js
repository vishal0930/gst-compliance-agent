/**
 * Simple class name merge utility.
 * Combines multiple class name arguments, filtering out falsy values.
 */
export const cn = (...classes) => {
  return classes.filter(Boolean).join(' ');
};
